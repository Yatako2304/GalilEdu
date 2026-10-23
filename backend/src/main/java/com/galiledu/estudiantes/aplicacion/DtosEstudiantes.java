package com.galiledu.estudiantes.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.galiledu.estudiantes.dominio.ModoSalida;
import com.galiledu.estudiantes.dominio.Parentesco;
import com.galiledu.estudiantes.dominio.SituacionEstudiante;
import com.galiledu.personas.aplicacion.DatosPersona;

/** Contratos JSON del módulo de estudiantes (HU-7). Deben coincidir con frontend/src/types/api.ts. */
public final class DtosEstudiantes {
	private DtosEstudiantes() {
	}

	/**
	 * Enteros que acepta @Digits en peso y estatura: holgado a propósito, para que un 118 en estatura responda con el
	 * mensaje del rango y @Digits solo controle los decimales.
	 */
	private static final int DIGITOS_ENTEROS = 10;

	// ---------- Solicitud de inscripción (los 5 pasos de Figma) ----------

	public record SolicitudInscripcion(
			@NotNull @Valid DatosEstudiante estudiante,
			@NotNull(message = "Completa la ficha médica") @Valid DatosFichaMedica fichaMedica,
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

	/** Paso 1. Los largos son los de estudiantes.estudiante (V2); los obligatorios los exige ServicioEstudiantes. */
	public record DatosEstudiante(
			@NotNull @Valid DatosPersona persona,
			@Size(max = 60) String paisNacimiento, @Size(max = 60) String departamentoNacimiento,
			@Size(max = 60) String provinciaNacimiento, @Size(max = 60) String distritoNacimiento,
			@Size(max = 200) String referenciaDomicilio, Boolean tieneInternet, @Size(max = 40) String lenguaMaterna,
			@Size(max = 40) String segundaLengua, @Size(max = 60) String autoidentificacionEtnica,
			Boolean tieneDiscapacidad, @Size(max = 200) String discapacidadDetalle,
			@Size(max = 150) String colegioProcedencia, @Size(max = 20) String gestionProcedencia,
			@NotNull(message = "Indica el año académico") @Min(2000) @Max(2100) Integer anioAcademico,
			@NotBlank(message = "Indica el grado") @Pattern(regexp = "PRI-[1-6]|SEC-[1-5]", message = "El grado no es válido") String gradoCodigo,
			@Size(max = 40) String formaIngreso) {
	}

	/**
	 * Paso 2. Largos de estudiantes.ficha_medica (V2) y los mismos rangos que LIMITES_FICHA del frontend; los
	 * obligatorios los exige ServicioEstudiantes.
	 */
	public record DatosFichaMedica(
			@Size(max = 5) String tipoSangre,
			@DecimalMin(value = "5", message = "El peso debe estar entre 5 y 200 kg") @DecimalMax(value = "200", message = "El peso debe estar entre 5 y 200 kg") @Digits(integer = DIGITOS_ENTEROS, fraction = 1, message = "El peso admite 1 decimal (###.#)") BigDecimal pesoKg,
			@DecimalMin(value = "0.5", message = "La estatura debe estar entre 0.50 y 2.50 m") @DecimalMax(value = "2.5", message = "La estatura debe estar entre 0.50 y 2.50 m") @Digits(integer = DIGITOS_ENTEROS, fraction = 2, message = "La estatura admite 2 decimales (#.##)") BigDecimal estaturaM,
			@Size(max = 40) String tipoSeguro,
			Boolean tieneAlergias, @Size(max = 200) String alergiasAlimentarias,
			@Size(max = 200) String alergiasMedicamentosas,
			// Vacío lo deja pasar: "obligatorio" lo revisa ServicioEstudiantes con su propio mensaje.
			@Pattern(regexp = "^$|^\\d{1,3}(\\.\\d)?$", message = "Ingresa un número (ej. 39 o 39.5)") String fiebreMaxima,
			Boolean haConvulsionado, Boolean tieneEnfermedad, @Size(max = 200) String enfermedadDetalle,
			Boolean tomaMedicamento, @Size(max = 200) String medicamentoDetalle,
			@Size(max = 500) String observaciones) {
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
			int anioAcademico, SituacionEstudiante estado, String responsable) {
	}

	/** Qué pasa si se intenta inscribir un estudiante con ese documento (paso 1 de la inscripción). */
	public enum Disponibilidad {
		/** No existe nadie con ese documento. */
		LIBRE,
		/** Era un estudiante eliminado: se puede inscribir y sus datos se actualizan con la nueva inscripción. */
		REUTILIZABLE, ESTUDIANTE_ACTIVO,
		/** Retirado (inactivo): no se puede inscribir como nuevo. */
		ESTUDIANTE_RETIRADO,
		/** El documento es de una persona que no es estudiante (padre, apoderado, usuario). */
		OTRA_PERSONA
	}

	public record VerificacionDocumento(Disponibilidad estado, boolean permitido, String mensaje) {
	}

	/**
	 * Respuesta de "Buscar por documento" del paso 3. {@code cuentaEliminada}: su cuenta de usuario está
	 * eliminada; "Nuevo usuario" la usa para dejar volver a registrar ese documento.
	 */
	public record PersonaEncontrada(UUID personaId, DatosPersona persona, boolean tieneCuenta,
			boolean cuentaEliminada, List<String> estudiantesACargo) {
	}

	public record FichaEstudiante(UUID id, SituacionEstudiante estado, String grado, int anioAcademico,
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
