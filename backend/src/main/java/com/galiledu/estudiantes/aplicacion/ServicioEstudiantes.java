package com.galiledu.estudiantes.aplicacion;

import java.util.ArrayList;
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
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.DocumentoAdjunto;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.EstudianteResumen;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.Familiar;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.FichaEstudiante;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.InscripcionRegistrada;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.PersonaEncontrada;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.SolicitudInscripcion;
import com.galiledu.estudiantes.dominio.AutorizadoRecojo;
import com.galiledu.estudiantes.dominio.ContactoEmergencia;
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
		Persona personaEstudiante = personas.crear(d.persona());
		Estudiante estudiante = new Estudiante(personaEstudiante);
		estudiante.lugarNacimiento(d.paisNacimiento(), d.departamentoNacimiento(), d.provinciaNacimiento(),
				d.distritoNacimiento());
		estudiante.datosSocioculturales(d.referenciaDomicilio(), d.tieneInternet(), d.lenguaMaterna(),
				d.segundaLengua(),
				d.autoidentificacionEtnica(), Boolean.TRUE.equals(d.tieneDiscapacidad()), d.discapacidadDetalle());
		estudiante.procedencia(d.colegioProcedencia(), d.gestionProcedencia());
		estudiante.datosAcademicos(d.anioAcademico().shortValue(), d.gradoCodigo(), d.formaIngreso());
		estudiante.setModoSalida(s.modoSalida());

		if (s.fichaMedica() != null) {
			estudiante.asignarFichaMedica(fichaMedica(s.fichaMedica()));
		}

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
		return new PersonaEncontrada(p.getId(), DatosPersona.de(p), usuarios.tieneCuenta(p.getId()), aCargo);
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

	private static FichaMedica fichaMedica(DatosFichaMedica f) {
		return new FichaMedica(vacio(f.tipoSangre()), f.pesoKg(), f.estaturaM(), vacio(f.tipoSeguro()),
				Boolean.TRUE.equals(f.tieneAlergias()), vacio(f.alergiasAlimentarias()),
				vacio(f.alergiasMedicamentosas()),
				vacio(f.fiebreMaxima()), Boolean.TRUE.equals(f.haConvulsionado()),
				Boolean.TRUE.equals(f.tieneEnfermedad()),
				vacio(f.enfermedadDetalle()), Boolean.TRUE.equals(f.tomaMedicamento()), vacio(f.medicamentoDetalle()),
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
