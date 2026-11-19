package com.galiledu.asistencia.aplicacion;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.galiledu.asistencia.dominio.EstadoAsistencia;

/** Contrato de la API de Asistencia (HU-65). */
public final class DtosAsistencia {
	private DtosAsistencia() {
	}

	/** Sección del tutor: "5.° de secundaria · Sección A". {@code cargo} es TUTOR o COTUTOR. */
	public record SeccionAsistencia(UUID id, int anio, String grado, String nombre, String cargo) {
	}

	/** "Registrada hoy a las 08:05 · Selene Curo" y, si se corrigió, quién y cuándo. */
	public record RegistroDelDia(UUID id, int version, OffsetDateTime registradoEn, String registradoPor,
			OffsetDateTime corregidoEn, String corregidoPor) {
	}

	/** Fila de la tabla: {@code estado} y {@code horaLlegada} son null si aún no se registró el día. */
	public record FilaAsistencia(UUID matriculaId, UUID estudianteId, String nombres, String apellidos,
			String documento, EstadoAsistencia estado, LocalTime horaLlegada, boolean modificado) {
	}

	/** Tarjetas de AS-01: presentes, tardanzas, faltas y sin marcar. */
	public record Resumen(int presentes, int tardanzas, int faltas, int sinMarcar, int total) {
	}

	/**
	 * AS-01 a AS-06. {@code editable} es true solo si la fecha es hoy y es día escolar; si no es día escolar,
	 * {@code motivoNoEscolar} dice por qué. {@code registro} es null mientras no se guarde la asistencia del día.
	 */
	public record AsistenciaDelDia(SeccionAsistencia seccion, LocalDate fecha, LocalDate hoy, boolean editable,
			String motivoNoEscolar, RegistroDelDia registro, Resumen resumen, List<FilaAsistencia> estudiantes) {
	}

	/** Una marca del formulario. La hora va solo en la tardanza ("09:10"). */
	public record MarcaSolicitud(UUID matriculaId, EstadoAsistencia estado, LocalTime horaLlegada) {
	}

	/**
	 * Guardar (AS-04) o corregir (AS-06) la asistencia de hoy con todos los estudiantes marcados. {@code version} es
	 * null al registrar y la del {@link RegistroDelDia} al corregir.
	 */
	public record SolicitudAsistencia(Integer version, List<MarcaSolicitud> asistencias) {
	}
}
