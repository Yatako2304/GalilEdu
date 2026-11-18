package com.galiledu.academico.aplicacion;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.galiledu.academico.aplicacion.DtosAmbientes.AmbienteResumen;
import com.galiledu.academico.aplicacion.DtosAmbientes.ResumenAmbientes;
import com.galiledu.academico.aplicacion.DtosAmbientes.SolicitudAmbiente;
import com.galiledu.academico.aplicacion.DtosAmbientes.SolicitudEdicionAmbiente;
import com.galiledu.academico.dominio.Ambiente;
import com.galiledu.academico.dominio.CodigoAmbiente;
import com.galiledu.academico.dominio.EstadoAmbiente;
import com.galiledu.academico.dominio.TipoAmbiente;
import com.galiledu.academico.infraestructura.AmbienteRepositorio;
import com.galiledu.academico.infraestructura.ConsultasAmbientes;
import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * Ambientes físicos del colegio (Figma CI-26, HU-43): aulas regulares y espacios especiales. No se borran: se
 * desactivan, y eso se permite aunque estén en uso.
 * <p>
 * El código (A-001, E-001) lo genera este servicio y no cambia. El nombre es único sin distinguir mayúsculas ni
 * espacios sobrantes, e incluye a los inactivos. La unicidad del nombre la valida solo el servicio (la BD no tiene
 * restricción: pendiente un UNIQUE INDEX sobre lower(btrim(nombre))).
 * <p>
 * No usa el Clock: nada de aquí depende de la fecha.
 */
@Service
public class ServicioAmbientes {
	/** Intentos para obtener un código libre cuando otra petición se lleva el mismo código a la vez. */
	static final int MAXIMO_INTENTOS_CODIGO = 3;
	/** Restricción UNIQUE del código en V6 (el nombre que PostgreSQL le da a {@code codigo ... UNIQUE}). */
	static final String RESTRICCION_CODIGO = "ambiente_codigo_key";
	static final String MENSAJE_NO_REGISTRADO = "No pudimos registrar el ambiente. Inténtalo de nuevo.";
	static final String MENSAJE_NO_EXISTE = "El ambiente no existe.";

	private final AmbienteRepositorio ambientes;
	private final ConsultasAmbientes consultas;
	/** Una transacción nueva por intento de {@link #crear}; ver el comentario allí. */
	private final TransactionTemplate transaccionPorIntento;

	public ServicioAmbientes(AmbienteRepositorio ambientes, ConsultasAmbientes consultas,
			PlatformTransactionManager transacciones) {
		this.ambientes = ambientes;
		this.consultas = consultas;
		this.transaccionPorIntento = new TransactionTemplate(transacciones);
		// REQUIRES_NEW: cada intento es independiente aunque quien llama ya tenga una transacción abierta. En
		// PostgreSQL una violación de unicidad deja abortada la transacción en curso, y dentro de ella no se puede
		// reintentar: hace falta una transacción (y una sesión de Hibernate) nueva por intento.
		this.transaccionPorIntento.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	// ---------- Consultas ----------

	/** Tarjetas de la cabecera: solo cuentan los ambientes activos, y no dependen de los filtros de la lista. */
	@Transactional(readOnly = true)
	public ResumenAmbientes resumen() {
		List<Ambiente> todos = ambientes.findAll();
		List<Ambiente> activos = todos.stream().filter(Ambiente::estaActivo).toList();
		return new ResumenAmbientes(activos.size(), todos.size(),
				activos.stream().filter(a -> a.getTipo() == TipoAmbiente.AULA_REGULAR).count(),
				activos.stream().filter(a -> a.getTipo() == TipoAmbiente.ESPACIO_ESPECIAL).count(),
				activos.stream().mapToLong(Ambiente::getAforoMaximo).sum());
	}

	/**
	 * Lista simple, por nombre. Todos los filtros son opcionales: {@code q} busca solo en el nombre, sin distinguir
	 * mayúsculas (tampoco tildes distintas: "Cómputo" no encuentra "Computo").
	 */
	@Transactional(readOnly = true)
	public List<AmbienteResumen> listar(TipoAmbiente tipo, EstadoAmbiente estado, String q) {
		String buscado = q == null ? "" : q.replaceAll("\\s+", " ").strip().toLowerCase(Locale.ROOT);
		return ambientes.findAllByOrderByNombreAsc().stream()
				.filter(a -> tipo == null || a.getTipo() == tipo)
				.filter(a -> estado == null || a.getEstado() == estado)
				.filter(a -> buscado.isEmpty() || a.getNombre().toLowerCase(Locale.ROOT).contains(buscado))
				.map(AmbienteResumen::de).toList();
	}

	// ---------- Cambios ----------

	/**
	 * Registra un ambiente con el siguiente código de su tipo. <b>No es {@code @Transactional}</b>: hace hasta
	 * {@value #MAXIMO_INTENTOS_CODIGO} intentos y cada uno corre en su propia transacción, con {@code saveAndFlush}
	 * para que el INSERT (y su posible choque de código) ocurra dentro del intento y no al confirmar.
	 * <p>
	 * El código se calcula con los códigos que ve la transacción del intento. Si otra petición confirma el mismo código
	 * en medio, la restricción UNIQUE de la BD lo rechaza al INSERT; ese intento se deshace entero y el siguiente
	 * vuelve a leer los códigos, ya con el nuevo. Solo se reintenta ante esa restricción: cualquier otra violación o
	 * el tercer choque dan 409.
	 */
	public AmbienteResumen crear(SolicitudAmbiente solicitud) {
		// Se valida antes de abrir ninguna transacción: un dato inválido no llega a la BD.
		TipoAmbiente tipo = solicitud.tipo();
		ExcepcionNegocio.exigir(solicitud.aforoMaximo(), "Ingresa el aforo máximo.");
		int aforoMaximo = solicitud.aforoMaximo();
		String nombre = construir(() -> Ambiente.normalizarNombre(solicitud.nombre()));
		validar(() -> Ambiente.validarAforo(tipo, aforoMaximo));
		List<String> recursos = construir(() -> Ambiente.normalizarRecursos(solicitud.recursos()));

		for (int intento = 1;; intento++) {
			try {
				return transaccionPorIntento.execute(estado -> guardarNuevo(tipo, nombre, aforoMaximo, recursos));
			} catch (DataIntegrityViolationException violacion) {
				boolean quedanIntentos = intento < MAXIMO_INTENTOS_CODIGO;
				if (!esChoqueDeCodigo(violacion) || !quedanIntentos) {
					throw ExcepcionNegocio.conflicto(MENSAJE_NO_REGISTRADO);
				}
			}
		}
	}

	/** Un intento, dentro de su transacción: nombre libre, código siguiente e INSERT inmediato. */
	private AmbienteResumen guardarNuevo(TipoAmbiente tipo, String nombre, int aforoMaximo, List<String> recursos) {
		// Primero el bloqueo y luego las lecturas: si otra petición está creando o renombrando, se espera a que
		// confirme y la comprobación del nombre ve su resultado. El bloqueo se suelta al confirmar este intento.
		ambientes.bloquearNombres();
		if (ambientes.existsByNombreIgnoreCase(nombre)) {
			throw nombreRepetido(nombre);
		}
		CodigoAmbiente codigo = CodigoAmbiente.siguiente(tipo, ambientes.codigosConPrefijo(tipo.prefijo()));
		Ambiente nuevo = construir(() -> Ambiente.crear(tipo, codigo, nombre, aforoMaximo, recursos));
		return AmbienteResumen.de(ambientes.saveAndFlush(nuevo));
	}

	/**
	 * Cambia nombre, aforo y recursos. Todo se valida antes de modificar la entidad (Hibernate hace flush de lo que se
	 * modifique): primero los datos de la solicitud (400), después el nombre repetido y la matrícula (409).
	 */
	@Transactional
	public AmbienteResumen actualizar(UUID id, SolicitudEdicionAmbiente solicitud) {
		Ambiente ambiente = obtener(id);
		ExcepcionNegocio.exigir(solicitud.aforoMaximo(), "Ingresa el aforo máximo.");
		int nuevoAforo = solicitud.aforoMaximo();

		// Reglas de forma, sobre copias de los valores: la entidad todavía no se toca.
		String nombre = construir(() -> Ambiente.normalizarNombre(solicitud.nombre()));
		validar(() -> Ambiente.validarAforo(ambiente.getTipo(), nuevoAforo));
		List<String> recursos = construir(() -> Ambiente.normalizarRecursos(solicitud.recursos()));

		// Igual que al crear: se espera a quien esté creando o renombrando antes de mirar si el nombre está libre. El
		// bloqueo dura hasta confirmar esta transacción, es decir, hasta después del flush del cambio.
		ambientes.bloquearNombres();
		if (ambientes.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
			throw nombreRepetido(nombre);
		}
		if (nuevoAforo < ambiente.getAforoMaximo()) {
			validarAforoContraMatricula(ambiente, nuevoAforo);
		}

		ambiente.editar(nombre, nuevoAforo, recursos);
		return AmbienteResumen.de(ambiente);
	}

	/** RF-111: el aforo no puede quedar por debajo de la matrícula de una sección que usa el aula. */
	private void validarAforoContraMatricula(Ambiente ambiente, int nuevoAforo) {
		consultas.seccionMasOcupada(ambiente.getId()).filter(s -> s.matriculados() > nuevoAforo).ifPresent(s -> {
			throw ExcepcionNegocio
					.conflicto("No puedes bajar el aforo a %d: la sección %s de %s tiene %d estudiantes matriculados."
							.formatted(nuevoAforo, s.seccion(), s.grado(), s.matriculados()));
		});
	}

	/** Se permite aunque el ambiente esté en uso. Si ya estaba inactivo, no hace nada. */
	@Transactional
	public AmbienteResumen desactivar(UUID id) {
		Ambiente ambiente = obtener(id);
		ambiente.desactivar();
		return AmbienteResumen.de(ambiente);
	}

	/** Si ya estaba activo, no hace nada. */
	@Transactional
	public AmbienteResumen reactivar(UUID id) {
		Ambiente ambiente = obtener(id);
		ambiente.reactivar();
		return AmbienteResumen.de(ambiente);
	}

	// ---------- Apoyo ----------

	private Ambiente obtener(UUID id) {
		return ambientes.findById(id).orElseThrow(() -> ExcepcionNegocio.noEncontrado(MENSAJE_NO_EXISTE));
	}

	private static ExcepcionNegocio nombreRepetido(String nombre) {
		return ExcepcionNegocio.conflicto("Ya existe un ambiente llamado \"%s\".".formatted(nombre));
	}

	/**
	 * ¿La violación es la restricción UNIQUE del código? Se busca su nombre en el mensaje de toda la cadena de causas
	 * (Spring envuelve a Hibernate, que envuelve a PostgreSQL), sin depender de la clase de cada capa.
	 */
	private static boolean esChoqueDeCodigo(Throwable violacion) {
		for (Throwable causa = violacion; causa != null; causa = causa.getCause()) {
			String mensaje = causa.getMessage();
			if (mensaje != null && mensaje.contains(RESTRICCION_CODIGO)) {
				return true;
			}
		}
		return false;
	}

	/** Las reglas de forma del dominio lanzan IllegalArgumentException: aquí pasan a 400 con el mismo mensaje. */
	private static void validar(Runnable reglaDeDominio) {
		construir(() -> {
			reglaDeDominio.run();
			return null;
		});
	}

	private static <T> T construir(Supplier<T> reglaDeDominio) {
		try {
			return reglaDeDominio.get();
		} catch (IllegalArgumentException argumentoInvalido) {
			throw ExcepcionNegocio.invalido(argumentoInvalido.getMessage());
		}
	}
}
