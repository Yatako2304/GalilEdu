package com.galiledu.estudiantes.aplicacion;

import static com.galiledu.compartido.web.ExcepcionNegocio.exigir;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.DatosAutorizado;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.DatosContacto;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.DatosEstudiante;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.DatosFamiliar;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.DatosFichaMedica;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.Disponibilidad;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.DocumentoAdjunto;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.EstudianteResumen;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.Familiar;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.FichaEstudiante;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.InscripcionRegistrada;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.PersonaEncontrada;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.SolicitudInscripcion;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.VerificacionDocumento;
import com.galiledu.estudiantes.dominio.AutorizadoRecojo;
import com.galiledu.estudiantes.dominio.ContactoEmergencia;
import com.galiledu.estudiantes.dominio.EstadoEstudiante;
import com.galiledu.estudiantes.dominio.Estudiante;
import com.galiledu.estudiantes.dominio.FichaMedica;
import com.galiledu.estudiantes.dominio.Parentesco;
import com.galiledu.estudiantes.dominio.SituacionEstudiante;
import com.galiledu.estudiantes.dominio.VinculoFamiliar;
import com.galiledu.estudiantes.infraestructura.DocumentoVinculoRepositorio;
import com.galiledu.estudiantes.infraestructura.EstudianteRepositorio;
import com.galiledu.personas.aplicacion.DatosPersona;
import com.galiledu.personas.aplicacion.ServicioPersonas;
import com.galiledu.personas.dominio.Persona;
import com.galiledu.usuarios.aplicacion.ServicioUsuarios;
import com.galiledu.usuarios.dominio.Rol;

/** HU-7: inscripción del estudiante con sus padres o apoderado, y consultas del listado/ficha. */
@Service
public class ServicioEstudiantes {
	/** Único país con departamento, provincia y distrito (catálogo de ubigeo del INEI en el frontend). */
	private static final String PAIS_CON_UBIGEO = "Perú";
	/** Nacionalidades del Paso 1 (NACIONALIDADES_ESTUDIANTE en el frontend). */
	private static final List<String> NACIONALIDADES = List.of("Peruana", "Extranjera");
	private static final String DOCUMENTO_EXISTE = "El documento ya existe";

	private final EstudianteRepositorio estudiantes;
	private final ServicioPersonas personas;
	private final ServicioUsuarios usuarios;
	private final DocumentoVinculoRepositorio documentos;

	public ServicioEstudiantes(EstudianteRepositorio estudiantes, ServicioPersonas personas,
			ServicioUsuarios usuarios, DocumentoVinculoRepositorio documentos) {
		this.documentos = documentos;
		this.estudiantes = estudiantes;
		this.personas = personas;
		this.usuarios = usuarios;
	}

	@Transactional
	public InscripcionRegistrada inscribir(SolicitudInscripcion s) {
		// Regla de Figma (paso 3): al menos padre o madre; si no hay ninguno, el apoderado es obligatorio.
		boolean hayPadres = s.padre().registra() || s.madre().registra();
		boolean hayApoderado = s.apoderado() != null && s.apoderado().registra();
		if (!hayPadres && !hayApoderado) {
			throw ExcepcionNegocio.invalido("No hay padre ni madre registrados: debes registrar un apoderado");
		}
		exigirMotivo(s.padre(), "Indica el motivo por el que no se registra al padre");
		exigirMotivo(s.madre(), "Indica el motivo por el que no se registra a la madre");

		DatosEstudiante d = s.estudiante();
		exigirPaso1(d);
		exigirPaso2(s.fichaMedica());
		Estudiante estudiante = estudianteParaInscribir(d);
		Persona personaEstudiante = estudiante.getPersona();
		estudiante.lugarNacimiento(d.paisNacimiento(), d.departamentoNacimiento(), d.provinciaNacimiento(),
				d.distritoNacimiento());
		boolean tieneDiscapacidad = Boolean.TRUE.equals(d.tieneDiscapacidad());
		estudiante.datosSocioculturales(d.referenciaDomicilio(), d.tieneInternet(), d.lenguaMaterna(),
				d.segundaLengua(), d.autoidentificacionEtnica(), tieneDiscapacidad,
				tieneDiscapacidad ? d.discapacidadDetalle() : null);
		estudiante.procedencia(d.colegioProcedencia(), d.gestionProcedencia());
		estudiante.datosAcademicos(d.anioAcademico().shortValue(), d.gradoCodigo(), d.formaIngreso());
		estudiante.setModoSalida(s.modoSalida());

		estudiante.asignarFichaMedica(fichaMedica(s.fichaMedica()));

		Map<Parentesco, DatosFamiliar> familiares = new java.util.EnumMap<>(Parentesco.class);
		familiares.put(Parentesco.PADRE, s.padre());
		familiares.put(Parentesco.MADRE, s.madre());
		if (s.apoderado() != null) {
			familiares.put(Parentesco.APODERADO, s.apoderado());
		}
		VinculoFamiliar responsable = null;
		for (var entrada : familiares.entrySet()) {
			VinculoFamiliar vinculo = vinculo(entrada.getKey(), entrada.getValue());
			if (vinculo == null) {
				continue;
			}
			if (entrada.getKey() == s.responsable()) {
				vinculo.marcarResponsable();
				responsable = vinculo;
			}
			estudiante.agregarVinculo(vinculo);
		}
		if (responsable == null || responsable.getPersona() == null) {
			throw ExcepcionNegocio
					.invalido("El responsable de matrícula debe ser el padre, la madre o el apoderado registrado");
		}

		short orden = 1;
		for (DatosContacto c : s.contactos()) {
			estudiante.agregarContacto(new ContactoEmergencia(c.nombreCompleto().strip(), c.parentesco().strip(),
					vacio(c.telefonoFijo()), vacio(c.celular()), orden++));
		}
		if (s.autorizados() != null) {
			for (DatosAutorizado a : s.autorizados()) {
				estudiante.agregarAutorizado(new AutorizadoRecojo(a.nombreCompleto().strip(), a.relacion().strip()));
			}
		}
		estudiantes.save(estudiante);

		// Paso 5: el responsable tiene el usuario de la familia (rol Apoderado). Si no lo tiene, se crea.
		Persona personaResponsable = responsable.getPersona();
		completarContacto(personaResponsable, s.correoResponsable(), s.celularResponsable());
		boolean cuentaNueva = !usuarios.tieneCuenta(personaResponsable.getId());
		var cuenta = usuarios.asegurarRol(personaResponsable, Rol.APODERADO);

		return new InscripcionRegistrada(estudiante.getId(), personaEstudiante.nombreCompleto(),
				personaResponsable.nombreCompleto(), cuenta.getNombreUsuario(), cuentaNueva);
	}

	/**
	 * Un documento nuevo crea la persona y el estudiante. Si el documento es de un estudiante eliminado, se
	 * reutiliza su registro (mismo id, con su historial): se restaura y sus datos se reemplazan por los de la nueva
	 * inscripción. Un estudiante activo o retirado no se puede inscribir de nuevo.
	 */
	private Estudiante estudianteParaInscribir(DatosEstudiante d) {
		var previo = personas.buscarPorDocumento(d.persona().numeroDocumento())
				.flatMap(p -> estudiantes.findByPersonaId(p.getId()));
		if (previo.isEmpty()) {
			// Si el documento es de otra persona (no estudiante), crear() lo rechaza como duplicado.
			return new Estudiante(personas.crear(d.persona()));
		}
		Estudiante e = previo.get();
		VerificacionDocumento v = verificacion(e);
		if (!v.permitido()) {
			throw ExcepcionNegocio.conflicto(v.mensaje());
		}
		personas.actualizar(e.getPersona(), d.persona());
		e.restaurar();
		e.vaciarRegistro();
		// El DELETE de lo anterior debe ejecutarse antes del INSERT de lo nuevo (claves únicas de padre, madre y ficha).
		estudiantes.flush();
		return e;
	}

	/** Paso 1: ¿se puede inscribir un estudiante con este documento? Se consulta antes de pasar al paso 2. */
	@Transactional(readOnly = true)
	public VerificacionDocumento verificarDocumento(String numeroDocumento) {
		return personas.buscarPorDocumento(numeroDocumento)
				.map(p -> estudiantes.findByPersonaId(p.getId())
						.map(ServicioEstudiantes::verificacion)
						.orElseGet(
								() -> new VerificacionDocumento(Disponibilidad.OTRA_PERSONA, false, DOCUMENTO_EXISTE)))
				.orElseGet(() -> new VerificacionDocumento(Disponibilidad.LIBRE, true, null));
	}

	/** Los mensajes no dicen de quién es el documento. */
	private static VerificacionDocumento verificacion(Estudiante e) {
		if (e.isEliminado()) {
			// Sin mensaje: reutilizar el registro eliminado es interno, quien inscribe no lo ve.
			return new VerificacionDocumento(Disponibilidad.REUTILIZABLE, true, null);
		}
		if (e.getEstado() == EstadoEstudiante.RETIRADO) {
			return new VerificacionDocumento(Disponibilidad.ESTUDIANTE_RETIRADO, false,
					DOCUMENTO_EXISTE + ": es de un estudiante inactivo (retirado). Reactívalo desde el listado.");
		}
		return new VerificacionDocumento(Disponibilidad.ESTUDIANTE_ACTIVO, false, DOCUMENTO_EXISTE);
	}

	@Transactional
	public void retirar(UUID id) {
		Estudiante e = visible(id);
		if (e.getEstado() == EstadoEstudiante.RETIRADO) {
			throw ExcepcionNegocio.conflicto("El estudiante ya está inactivo");
		}
		e.retirar();
	}

	@Transactional
	public void reactivar(UUID id) {
		Estudiante e = visible(id);
		if (e.getEstado() != EstadoEstudiante.RETIRADO) {
			throw ExcepcionNegocio.conflicto("El estudiante ya está activo");
		}
		e.reactivar();
	}

	@Transactional
	public void eliminar(UUID id) {
		visible(id).eliminar();
	}

	private Estudiante visible(UUID id) {
		return estudiantes.findById(id).filter(x -> !x.isEliminado())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El estudiante no existe"));
	}

	@Transactional(readOnly = true)
	public Pagina<EstudianteResumen> listar(String texto, SituacionEstudiante estado, int pagina, int tamano) {
		Specification<Estudiante> filtro = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			condiciones.add(cb.isFalse(raiz.get("eliminado")));
			if (texto != null && !texto.isBlank()) {
				var p = raiz.join("persona");
				String patron = "%" + texto.strip().toLowerCase() + "%";
				var nombre = cb.lower(cb.concat(cb.concat(cb.concat(p.get("nombres"), " "),
						cb.concat(p.get("apellidoPaterno"), " ")), cb.coalesce(p.<String>get("apellidoMaterno"), "")));
				condiciones.add(cb.or(cb.like(nombre, patron), cb.like(p.get("numeroDocumento"), patron),
						cb.like(cb.lower(cb.coalesce(p.<String>get("correo"), "")), patron)));
			}
			if (estado != null) {
				condiciones.add(cb.equal(raiz.get("situacion"), estado.name()));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		var orden = Sort.by("creadoEn").descending();
		var resultado = estudiantes.findAll(filtro,
				PageRequest.of(Math.max(0, pagina - 1), Math.clamp(tamano, 1, 100), orden));
		return Pagina.de(resultado, ServicioEstudiantes::resumen);
	}

	@Transactional(readOnly = true)
	public FichaEstudiante ficha(UUID id) {
		Estudiante e = estudiantes.findById(id)
				.filter(x -> !x.isEliminado())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El estudiante no existe"));
		var familia = e.getVinculos().stream().map(v -> {
			Persona p = v.getPersona();
			return new Familiar(p == null ? null : p.getId(), v.getParentesco(),
					p == null ? null : p.nombreCompleto(), p == null ? null : documento(p),
					p == null ? null : p.getCelular(),
					v.getRelacion(), v.isViveConEstudiante(), v.isEsResponsableMatricula(), v.getMotivoNoRegistra(),
					documentos.findResumenByVinculoId(v.getId())
							.map(d -> new DocumentoAdjunto(d.getTipo(), d.getNombreArchivo(), d.getSubidoEn()))
							.orElse(null));
		}).toList();
		// Hermanos: otros estudiantes vinculados a los mismos padres/apoderado.
		var hermanos = e.getVinculos().stream()
				.map(VinculoFamiliar::getPersona).filter(Objects::nonNull)
				.flatMap(p -> estudiantes.vinculadosA(p.getId()).stream())
				.filter(h -> !h.getId().equals(e.getId()))
				.distinct()
				.map(ServicioEstudiantes::resumen)
				.toList();
		var datos = new DatosEstudiante(null, e.getPaisNacimiento(), e.getDepartamentoNacimiento(),
				e.getProvinciaNacimiento(), e.getDistritoNacimiento(), e.getReferenciaDomicilio(), e.getTieneInternet(),
				e.getLenguaMaterna(), e.getSegundaLengua(), e.getAutoidentificacionEtnica(), e.isTieneDiscapacidad(),
				e.getDiscapacidadDetalle(), e.getColegioProcedencia(), e.getGestionProcedencia(),
				(int) e.getAnioAcademico(),
				e.getGradoCodigo(), e.getFormaIngreso());
		FichaMedica f = e.getFichaMedica();
		var ficha = f == null
				? null
				: new DatosFichaMedica(f.getTipoSangre(), f.getPesoKg(), f.getEstaturaM(),
						f.getTipoSeguro(), f.isTieneAlergias(), f.getAlergiasAlimentarias(),
						f.getAlergiasMedicamentosas(),
						f.getFiebreMaxima(), f.isHaConvulsionado(), f.isTieneEnfermedad(), f.getEnfermedadDetalle(),
						f.isTomaMedicamento(), f.getMedicamentoDetalle(), f.getObservaciones());
		var contactos = e.getContactos().stream()
				.map(c -> new DatosContacto(c.getNombreCompleto(), c.getParentesco(), c.getTelefonoFijo(),
						c.getCelular()))
				.toList();
		var autorizados = e.getAutorizados().stream()
				.map(a -> new DatosAutorizado(a.getNombreCompleto(), a.getRelacion()))
				.toList();
		return new FichaEstudiante(e.getId(), e.getSituacion(), e.gradoDescriptivo(), e.getAnioAcademico(),
				DatosPersona.de(e.getPersona()), datos, ficha, familia, contactos, e.getModoSalida(), autorizados,
				hermanos);
	}

	/** "Busca por documento: si ya está registrado (por ejemplo, por un hermano), solo se vincula." */
	@Transactional(readOnly = true)
	public PersonaEncontrada buscarPersona(String numeroDocumento) {
		Persona p = personas.buscarPorDocumento(numeroDocumento)
				.orElseThrow(
						() -> ExcepcionNegocio.noEncontrado("No está registrada en GalilEdu. Completa sus datos."));
		if (estudiantes.existsByPersonaId(p.getId())) {
			throw ExcepcionNegocio.conflicto("El documento corresponde a un estudiante, no a un familiar");
		}
		var aCargo = estudiantes.vinculadosA(p.getId()).stream()
				.map(e -> e.getPersona().nombreCompleto() + " (" + e.gradoDescriptivo() + ")")
				.toList();
		return new PersonaEncontrada(p.getId(), DatosPersona.de(p), usuarios.tieneCuenta(p.getId()),
				usuarios.cuentaEliminada(p.getId()), aCargo);
	}

	private VinculoFamiliar vinculo(Parentesco parentesco, DatosFamiliar d) {
		if (d == null) {
			return null;
		}
		if (!d.registra()) {
			if (parentesco == Parentesco.APODERADO) {
				return null;
			}
			return VinculoFamiliar.noRegistra(parentesco, vacio(d.motivo()), vacio(d.observacion()));
		}
		Persona persona = d.personaId() != null ? personas.obtener(d.personaId()) : personas.crear(d.persona());
		return VinculoFamiliar.conPersona(parentesco, persona, vacio(d.relacion()),
				Boolean.TRUE.equals(d.viveConEstudiante()), Boolean.TRUE.equals(d.tienePatriaPotestad()));
	}

	/**
	 * Paso 1 (Figma, igual que estudianteSchema del frontend): todo es obligatorio salvo apellido materno, correo,
	 * referencia, teléfono de casa, segunda lengua y autoidentificación étnica. Departamento, provincia y distrito de
	 * nacimiento solo van (y son obligatorios) si nació en el Perú. El formato de cada dato lo valida DatosPersona.
	 */
	private static void exigirPaso1(DatosEstudiante d) {
		DatosPersona p = d.persona();
		exigir(p.sexo(), "Selecciona el sexo");
		exigir(p.fechaNacimiento(), "Ingresa la fecha de nacimiento");
		exigir(p.nacionalidad(), "Selecciona la nacionalidad");
		if (!NACIONALIDADES.contains(p.nacionalidad().strip())) {
			throw ExcepcionNegocio.invalido("La nacionalidad del estudiante debe ser Peruana o Extranjera");
		}
		exigir(d.paisNacimiento(), "Selecciona el país");
		if (PAIS_CON_UBIGEO.equals(d.paisNacimiento().strip())) {
			exigir(d.departamentoNacimiento(), "Selecciona el departamento");
			exigir(d.provinciaNacimiento(), "Selecciona la provincia");
			exigir(d.distritoNacimiento(), "Selecciona el distrito de nacimiento");
		} else if (!vacios(d.departamentoNacimiento(), d.provinciaNacimiento(), d.distritoNacimiento())) {
			throw ExcepcionNegocio
					.invalido("Departamento, provincia y distrito de nacimiento solo se indican si nació en el Perú");
		}
		exigir(p.direccion(), "Ingresa la dirección");
		exigir(p.distrito(), "Selecciona el distrito");
		exigir(d.tieneInternet(), "Selecciona si tiene acceso a internet / dispositivo");
		exigir(d.lenguaMaterna(), "Selecciona la lengua materna");
		if (Boolean.TRUE.equals(d.tieneDiscapacidad())) {
			exigir(d.discapacidadDetalle(), "Indica cuál es la discapacidad");
		}
		exigir(d.colegioProcedencia(), "Ingresa el colegio de procedencia");
		exigir(d.gestionProcedencia(), "Selecciona la gestión");
		exigir(d.formaIngreso(), "Selecciona la forma de ingreso");
	}

	private static boolean vacios(String... textos) {
		return Arrays.stream(textos).allMatch(t -> t == null || t.isBlank());
	}

	private static void exigirMotivo(DatosFamiliar d, String mensaje) {
		if (!d.registra() && (d.motivo() == null || d.motivo().isBlank())) {
			throw ExcepcionNegocio.invalido(mensaje);
		}
	}

	private static void completarContacto(Persona p, String correo, String celular) {
		if (correo != null && !correo.isBlank()) {
			p.setCorreo(correo.strip().toLowerCase());
		}
		if (celular != null && !celular.isBlank()) {
			p.setCelular(celular.replace(" ", ""));
		}
		if (p.getCorreo() == null) {
			throw ExcepcionNegocio.invalido("El responsable de matrícula necesita un correo de acceso");
		}
	}

	/**
	 * Paso 2 (igual que fichaMedicaSchema del frontend): todo es obligatorio salvo observaciones. Si tiene alergias, al
	 * menos una de las dos; si padece una enfermedad o toma un medicamento, "¿Cuál?". Formatos y rangos:
	 * DatosFichaMedica.
	 */
	private static void exigirPaso2(DatosFichaMedica f) {
		exigir(f.tipoSangre(), "Selecciona el tipo de sangre");
		exigir(f.pesoKg(), "Ingresa el peso");
		exigir(f.estaturaM(), "Ingresa la estatura");
		exigir(f.tipoSeguro(), "Selecciona el tipo de seguro");
		if (Boolean.TRUE.equals(f.tieneAlergias()) && vacios(f.alergiasAlimentarias(), f.alergiasMedicamentosas())) {
			throw ExcepcionNegocio.invalido("Indica al menos una alergia (alimentaria o medicamentosa)");
		}
		exigir(f.fiebreMaxima(), "Indica hasta cuántos grados de fiebre ha llegado");
		if (Boolean.TRUE.equals(f.tieneEnfermedad())) {
			exigir(f.enfermedadDetalle(), "Indica cuál es la enfermedad");
		}
		if (Boolean.TRUE.equals(f.tomaMedicamento())) {
			exigir(f.medicamentoDetalle(), "Indica cuál es el medicamento");
		}
	}

	/** Los detalles de una respuesta "No" no se guardan aunque lleguen. */
	private static FichaMedica fichaMedica(DatosFichaMedica f) {
		boolean alergias = Boolean.TRUE.equals(f.tieneAlergias());
		boolean enfermedad = Boolean.TRUE.equals(f.tieneEnfermedad());
		boolean medicamento = Boolean.TRUE.equals(f.tomaMedicamento());
		return new FichaMedica(vacio(f.tipoSangre()), f.pesoKg(), f.estaturaM(), vacio(f.tipoSeguro()),
				alergias, alergias ? vacio(f.alergiasAlimentarias()) : null,
				alergias ? vacio(f.alergiasMedicamentosas()) : null,
				vacio(f.fiebreMaxima()), Boolean.TRUE.equals(f.haConvulsionado()),
				enfermedad, enfermedad ? vacio(f.enfermedadDetalle()) : null,
				medicamento, medicamento ? vacio(f.medicamentoDetalle()) : null,
				vacio(f.observaciones()));
	}

	static EstudianteResumen resumen(Estudiante e) {
		String responsable = e.getVinculos().stream()
				.filter(VinculoFamiliar::isEsResponsableMatricula)
				.map(VinculoFamiliar::getPersona).filter(Objects::nonNull)
				.map(Persona::nombreCorto)
				.findFirst().orElse("—");
		return new EstudianteResumen(e.getId(), e.getPersona().nombreCompleto(), documento(e.getPersona()),
				e.gradoDescriptivo(), e.getAnioAcademico(), e.getSituacion(), responsable);
	}

	private static String documento(Persona p) {
		return (p.getTipoDocumento() == com.galiledu.personas.dominio.TipoDocumento.DNI ? "DNI " : "CE ")
				+ p.getNumeroDocumento();
	}

	private static String vacio(String v) {
		return v == null || v.isBlank() ? null : v.strip();
	}
}
