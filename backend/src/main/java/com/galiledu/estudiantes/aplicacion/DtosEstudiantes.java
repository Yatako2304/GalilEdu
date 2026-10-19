package com.galiledu.estudiantes.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.galiledu.estudiantes.dominio.EstadoEstudiante;
import com.galiledu.estudiantes.dominio.ModoSalida;
import com.galiledu.estudiantes.dominio.Nivel;
import com.galiledu.estudiantes.dominio.Parentesco;
import com.galiledu.personas.aplicacion.DatosPersona;

/** Contratos JSON del módulo de estudiantes (HU-7). Deben coincidir con frontend/src/types/api.ts. */
public final class DtosEstudiantes {
	private DtosEstudiantes() {
	}

	// ---------- Solicitud de inscripción (los 5 pasos de Figma) ----------

	public record SolicitudInscripcion(
			@NotNull @Valid DatosEstudiante estudiante,
			@Valid DatosFichaMedica fichaMedica,
			@NotNull @Valid DatosFamiliar padre,
			@NotNull @Valid DatosFamiliar madre,
			@Valid DatosFamiliar apoderado,
			@NotNull @Size(min = 2, message = "Registra al menos 2 contactos de emergencia") List<@Valid DatosContacto> contactos,
			ModoSalida modoSalida,
			List<@Valid DatosAutorizado> autorizados,
			@NotNull(message = "Elige al responsable de matrícula") Parentesco responsable,
			String correoResponsable,
			String celularResponsable) {
	}

	/** Paso 1. */
	public record DatosEstudiante(
			@NotNull @Valid DatosPersona persona,
			String paisNacimiento, String departamentoNacimiento, String provinciaNacimiento, String distritoNacimiento,
			String referenciaDomicilio, Boolean tieneInternet, String lenguaMaterna, String segundaLengua,
			String autoidentificacionEtnica, Boolean tieneDiscapacidad, String discapacidadDetalle,
			String colegioProcedencia, String gestionProcedencia,
			@NotNull(message = "Indica el año académico") @Min(2000) @Max(2100) Integer anioAcademico,
			@NotNull(message = "Indica el nivel") Nivel nivel,
			@NotBlank(message = "Indica el grado") String grado,
			String formaIngreso) {
	}

	/** Paso 2. */
	public record DatosFichaMedica(
			String tipoSangre, BigDecimal pesoKg, BigDecimal estaturaM, String tipoSeguro,
			Boolean tieneAlergias, String alergiasAlimentarias, String alergiasMedicamentosas,
			String fiebreMaxima, Boolean haConvulsionado, Boolean tieneEnfermedad, String enfermedadDetalle,
			Boolean tomaMedicamento, String medicamentoDetalle, String observaciones) {
	}

	/**
	 * Paso 3. Tres casos: "no registra" (noRegistra + motivo), persona ya registrada (personaId)
	 * o persona nueva (persona con sus datos).
	 */
	public record DatosFamiliar(
			Boolean noRegistra, String motivo, String observacion,
			UUID personaId,
			@Valid DatosPersona persona,
			Boolean viveConEstudiante, Boolean tienePatriaPotestad,
			String relacion) {
		public boolean registra() {
			return !Boolean.TRUE.equals(noRegistra) && (personaId != null || persona != null);
		}
	}

	/** Paso 4. */
	public record DatosContacto(
			@NotBlank(message = "Ingresa el nombre del contacto") String nombreCompleto,
			@NotBlank(message = "Ingresa el parentesco del contacto") String parentesco,
			String telefonoFijo,
			String celular) {
	}

	public record DatosAutorizado(@NotBlank String nombreCompleto, @NotBlank String relacion) {
	}

	public record InscripcionRegistrada(UUID id, String estudiante, String responsable, String usuarioResponsable,
			boolean cuentaNueva) {
	}

	// ---------- Consultas ----------

	/** Fila del "Listado de estudiantes" de Figma. */
	public record EstudianteResumen(UUID id, String nombreCompleto, String documento, String grado,
			int anioAcademico, EstadoEstudiante estado, String responsable) {
	}

	/** Respuesta de "Buscar por documento" del paso 3. */
	public record PersonaEncontrada(UUID personaId, DatosPersona persona, boolean tieneCuenta,
			List<String> estudiantesACargo) {
	}

	public record FichaEstudiante(UUID id, EstadoEstudiante estado, String grado, int anioAcademico,
			DatosPersona persona, DatosEstudiante datos, DatosFichaMedica fichaMedica, List<Familiar> familia,
			List<DatosContacto> contactos, ModoSalida modoSalida, List<DatosAutorizado> autorizados,
			List<EstudianteResumen> hermanos) {
	}

	public record Familiar(UUID personaId, Parentesco parentesco, String nombreCompleto, String documento,
			String celular, String relacion, boolean viveConEstudiante, boolean esResponsable,
			String motivoNoRegistra, DocumentoAdjunto acreditacion) {
	}

	/** Documento que acredita al apoderado (sin el contenido; se descarga aparte). */
	public record DocumentoAdjunto(String tipo, String nombreArchivo, Instant subidoEn) {
	}
}
