package com.galiledu.matricula.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.ServicioPeriodoMatricula;
import com.galiledu.configuracion.dominio.EstadoVisiblePeriodoMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.CuotaDeuda;
import com.galiledu.matricula.aplicacion.DtosMatricula.DeudaFamiliar;
import com.galiledu.matricula.aplicacion.DtosMatricula.EstadoMatriculaApoderado;
import com.galiledu.matricula.aplicacion.DtosMatricula.EstudianteMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.HijoApoderado;
import com.galiledu.matricula.aplicacion.DtosMatricula.Impedimento;
import com.galiledu.matricula.aplicacion.DtosMatricula.MatriculaActual;
import com.galiledu.matricula.aplicacion.DtosMatricula.PeriodoMatricula;
import com.galiledu.matricula.infraestructura.ConsultasMatricula;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.HijoFila;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.MatriculaFila;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.PeriodoFila;
import com.galiledu.pagos.aplicacion.ServicioEstadoCuenta;

/**
 * HU-27 (consulta del periodo y estado de matrícula) y HU-30 (validación de deuda). Componente "Proceso de
 * Matrícula": la deuda se pide a Pagos por su servicio (IEstadoCuenta) y el estado del periodo a Configuración
 * ({@link ServicioPeriodoMatricula#estadoEn}); aquí no se calculan.
 */
@Service
public class ServicioEstadoMatricula {
	private final ConsultasMatricula consultas;
	private final ServicioEstadoCuenta estadoCuenta;
	private final ServicioPeriodoMatricula periodos;
	private final Clock reloj;

	public ServicioEstadoMatricula(ConsultasMatricula consultas, ServicioEstadoCuenta estadoCuenta,
			ServicioPeriodoMatricula periodos, Clock reloj) {
		this.consultas = consultas;
		this.estadoCuenta = estadoCuenta;
		this.periodos = periodos;
		this.reloj = reloj;
	}

	/** RF-58: hijos del apoderado con su grado y el estado de su matrícula en el periodo vigente. */
	@Transactional(readOnly = true)
	public List<HijoApoderado> hijos(UUID usuarioId) {
		Optional<PeriodoFila> periodo = consultas.periodoVigente();
		return hijosDelUsuario(usuarioId).stream()
				.map(h -> new HijoApoderado(h.estudianteId(), h.nombre(), h.grado(),
						periodo.flatMap(p -> consultas.matricula(h.estudianteId(), p.anioEscolarId()))
								.map(MatriculaFila::estado)
								.orElse(null)))
				.toList();
	}

	@Transactional(readOnly = true)
	public EstadoMatriculaApoderado estado(UUID usuarioId, UUID estudianteId) {
		HijoFila hijo = hijosDelUsuario(usuarioId).stream().filter(h -> h.estudianteId().equals(estudianteId))
				.findFirst()
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No tienes a este estudiante a tu cargo."));

		List<Impedimento> impedimentos = new ArrayList<>();
		Optional<PeriodoFila> periodoFila = consultas.periodoVigente();
		PeriodoMatricula periodo = periodoFila.map(this::aPeriodo).orElse(null);
		if (periodo == null) {
			impedimentos.add(new Impedimento("PERIODO", "Aún no hay un periodo de matrícula configurado."));
		} else if (!periodo.abierto()) {
			impedimentos
					.add(new Impedimento("PERIODO", "El periodo de matrícula " + periodo.anio() + " no está abierto."));
		}

		MatriculaActual matricula = periodoFila.flatMap(p -> consultas.matricula(estudianteId, p.anioEscolarId())
				.map(m -> new MatriculaActual(m.id(), m.estado(), m.grado(), m.seccion(), m.codigo(),
						consultas.documentosRequeridos(p.id()), consultas.documentosEntregados(m.id()))))
				.orElse(null);
		if (periodo != null && matricula == null) {
			impedimentos.add(new Impedimento("SIN_SECCION", "El colegio aún no reserva una sección para "
					+ hijo.nombre() + " en " + periodo.anio() + "."));
		}
		if (matricula != null && "SECCION_RESERVADA".equals(matricula.estado())
				&& matricula.documentosEntregados() < matricula.documentosRequeridos()) {
			int faltan = matricula.documentosRequeridos() - matricula.documentosEntregados();
			impedimentos.add(new Impedimento("DOCUMENTOS", "Faltan " + faltan + " documento(s) firmado(s) por subir."));
		}

		// RF-64 (corregido en la iteración 2): solo la deuda de este estudiante bloquea su matrícula; la de un hermano
		// no bloquea la de los demás.
		DeudaFamiliar deuda = deudaDe(hijo);
		boolean bloquea = periodoFila.map(PeriodoFila::bloquearPorMorosidad).orElse(true);
		if (!deuda.cuotas().isEmpty() && bloquea) {
			impedimentos.add(new Impedimento("DEUDA", hijo.nombre() + " tiene una deuda pendiente de "
					+ soles(deuda.total()) + ". Págala para habilitar el proceso de matrícula."));
		}

		int paso = matricula == null ? 0 : pasoDe(matricula.estado());
		boolean puedeContinuar = impedimentos.isEmpty() && matricula != null
				&& !"MATRICULADO".equals(matricula.estado());
		return new EstadoMatriculaApoderado(new EstudianteMatricula(hijo.estudianteId(), hijo.nombre(), hijo.grado()),
				periodo, matricula, paso, puedeContinuar, impedimentos, deuda);
	}

	private List<HijoFila> hijosDelUsuario(UUID usuarioId) {
		UUID persona = consultas.personaDeUsuario(usuarioId)
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("No existe el usuario."));
		return consultas.hijosDe(persona);
	}

	private DeudaFamiliar deudaDe(HijoFila hijo) {
		List<CuotaDeuda> cuotas = estadoCuenta.cuotasVencidas(List.of(hijo.estudianteId())).stream()
				.map(c -> new CuotaDeuda(c.estudianteId(), c.estudiante(), c.concepto(), c.fechaVencimiento(),
						c.saldo()))
				.toList();
		BigDecimal total = cuotas.stream().map(CuotaDeuda::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new DeudaFamiliar(total, cuotas);
	}

	/** Estado visible (PROGRAMADO, ABIERTO o CERRADO) calculado por Configuración con el reloj de la app. */
	private PeriodoMatricula aPeriodo(PeriodoFila p) {
		EstadoVisiblePeriodoMatricula estado = periodos.estadoEn(p.id(), reloj.instant());
		return new PeriodoMatricula(p.anio(), p.inicio(), p.cierre(), estado.name(),
				estado == EstadoVisiblePeriodoMatricula.ABIERTO);
	}

	/** Pasos del stepper de Figma: 1 Datos y documentos, 2 Validación, 3 Pago de matrícula, 4 Matriculado. */
	private static int pasoDe(String estado) {
		return switch (estado) {
			case "SECCION_RESERVADA" -> 1;
			case "EN_VALIDACION" -> 2;
			case "PENDIENTE_PAGO" -> 3;
			case "MATRICULADO" -> 4;
			default -> 0;
		};
	}

	private static String soles(BigDecimal monto) {
		return String.format(Locale.US, "S/ %,.2f", monto);
	}
}
