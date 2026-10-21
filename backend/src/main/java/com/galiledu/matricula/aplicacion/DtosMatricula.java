package com.galiledu.matricula.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Contratos JSON de la matrícula del apoderado (HU-27, HU-30). Deben coincidir con frontend/src/types/api.ts. */
public final class DtosMatricula {
	private DtosMatricula() {
	}

	/** Opción del selector de hijos (Figma: "Valeria Sánchez Ferrara · 5.° de secundaria · Periodo abierto"). */
	public record HijoApoderado(UUID id, String nombre, String grado, String estadoMatricula) {
	}

	/** Figma Apoderado › Matrícula 2027: encabezado, stepper, deuda y si puede continuar. */
	public record EstadoMatriculaApoderado(
			EstudianteMatricula estudiante,
			PeriodoMatricula periodo,
			MatriculaActual matricula,
			int pasoActual,
			boolean puedeContinuar,
			List<Impedimento> impedimentos,
			DeudaFamiliar deuda) {
	}

	public record EstudianteMatricula(UUID id, String nombre, String grado) {
	}

	/** RF-58, RF-59: {@code abierto} indica si hoy se puede matricular; si es false, el frontend oculta la opción. */
	public record PeriodoMatricula(int anio, OffsetDateTime inicio, OffsetDateTime cierre, String estado,
			boolean abierto) {
	}

	/** {@code seccion} es null mientras no se asigne ("se asigna al confirmar la matrícula"). */
	public record MatriculaActual(UUID id, String estado, String grado, String seccion, String codigo,
			int documentosRequeridos, int documentosEntregados) {
	}

	/** RF-60: motivo por el que no puede continuar. tipo: PERIODO, SIN_SECCION, DOCUMENTOS o DEUDA. */
	public record Impedimento(String tipo, String mensaje) {
	}

	/** RF-64: deuda vencida del apoderado o de cualquiera de sus hijos, con el estudiante al que corresponde. */
	public record DeudaFamiliar(BigDecimal total, List<CuotaDeuda> cuotas) {
	}

	public record CuotaDeuda(UUID estudianteId, String estudiante, String concepto, LocalDate fechaVencimiento,
			BigDecimal monto) {
	}
}
