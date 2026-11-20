package com.galiledu.asistencia.aplicacion;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.galiledu.asistencia.aplicacion.DtosAsistencia.SeccionAsistencia;
import com.galiledu.asistencia.aplicacion.RangoReporte.Opcion;
import com.galiledu.asistencia.aplicacion.RangoReporte.Tipo;

/** Contrato del reporte de inasistencias y tardanzas (HU-68, RF-146). */
public final class DtosReporteAsistencia {
	private DtosReporteAsistencia() {
	}

	/**
	 * Fila por estudiante, por apellidos. {@code faltas} y {@code tardanzas} incluyen las justificadas;
	 * {@code faltasJustificadas} y {@code tardanzasJustificadas} son la parte justificada.
	 */
	public record FilaReporte(UUID matriculaId, String nombres, String apellidos, String documento, int faltas,
			int faltasJustificadas, int tardanzas, int tardanzasJustificadas) {
	}

	public record TotalesReporte(int faltas, int faltasJustificadas, int tardanzas, int tardanzasJustificadas) {
	}

	/** Opciones del selector: meses (del más reciente) y periodos que ya empezaron. */
	public record OpcionesReporte(List<Opcion> meses, List<Opcion> periodos) {
	}

	/**
	 * El reporte del rango elegido. {@code diasRegistrados} cuenta los días del rango con asistencia guardada; si es 0,
	 * todas las filas están en cero.
	 */
	public record ReporteAsistencia(SeccionAsistencia seccion, Tipo tipo, String valor, String etiqueta,
			LocalDate desde, LocalDate hasta, int diasRegistrados, OpcionesReporte opciones, List<FilaReporte> filas,
			TotalesReporte totales) {
	}

	/** PDF listo para descargar. */
	public record ArchivoReporte(String nombre, byte[] contenido) {
	}
}
