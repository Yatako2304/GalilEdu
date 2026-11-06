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
			DeudaFamiliar deuda,
			CuotaMatriculaApoderado cuotaMatricula) {
	}

	/**
	 * Figma Apoderado › "Matrícula 2027 · pendiente de pago" (HU-31): la cuota de matrícula del estudiante, para
	 * mostrar
	 * su monto y vencimiento y pagarla. {@code estado}: PENDIENTE, EN_PROCESO, PAGADA_PARCIAL o PAGADA;
	 * {@code fechaPago}
	 * solo si está pagada.
	 */
	public record CuotaMatriculaApoderado(UUID cuotaId, String concepto, BigDecimal monto, String estado,
			LocalDate fechaVencimiento, LocalDate fechaPago, String responsable) {
	}

	public record EstudianteMatricula(UUID id, String nombre, String grado) {
	}

	/**
	 * RF-58, RF-59: {@code abierto} indica si hoy se puede matricular; si es false, el frontend oculta la opción.
	 * {@code estado}: PROGRAMADO, ABIERTO o CERRADO (el estado visible que calcula Configuración).
	 */
	public record PeriodoMatricula(int anio, OffsetDateTime inicio, OffsetDateTime cierre, String estado,
			boolean abierto) {
	}

	/**
	 * {@code seccion} es null hasta que la matrícula esté MATRICULADO: el apoderado ve "Por definir".
	 * {@code confirmadoEn}
	 * es la fecha y hora exacta de la confirmación (solo MATRICULADO).
	 */
	public record MatriculaActual(UUID id, String estado, String grado, String seccion, String codigo,
			int documentosRequeridos, int documentosEntregados, OffsetDateTime confirmadoEn) {
	}

	/** RF-60: motivo por el que no puede continuar. tipo: PERIODO, SIN_SECCION, DOCUMENTOS o DEUDA. */
	public record Impedimento(String tipo, String mensaje) {
	}

	/**
	 * RF-64: deuda vencida del estudiante consultado. Solo bloquea su matrícula: la deuda de un hermano no bloquea la
	 * de
	 * los demás (corrección de la iteración 2).
	 */
	public record DeudaFamiliar(BigDecimal total, List<CuotaDeuda> cuotas) {
	}

	public record CuotaDeuda(UUID estudianteId, String estudiante, String concepto, LocalDate fechaVencimiento,
			BigDecimal monto) {
	}

	/**
	 * Figma Personal administrativo › A3 "Matrículas del periodo" (HU-24): encabezado con el aviso del periodo y los
	 * cuatro indicadores. {@code estado} es PROGRAMADO, ABIERTO o CERRADO (lo calcula Configuración).
	 */
	public record PeriodoMatriculas(UUID id, int anio, OffsetDateTime inicio, OffsetDateTime cierre, String estado,
			boolean permiteExtemporanea, IndicadoresPeriodo indicadores) {
	}

	/**
	 * {@code estudiantes}: con matrícula en el periodo (sin anuladas ni retiradas); {@code enProceso}: los que aún
	 * no están matriculados; {@code requierenAccion}: los que espera el colegio (sección reservada o en validación).
	 */
	public record IndicadoresPeriodo(int estudiantes, int matriculados, int enProceso, int requierenAccion) {
	}

	/** Fila de la tabla de A3. {@code seccion} es null si aún no se asigna. */
	public record FilaMatriculaPeriodo(UUID id, UUID estudianteId, String nombre, String documento,
			String gradoCodigo, String grado, String seccion, String estado, String codigo, String tipo) {
	}

	/** Resultado del buscador de "Nueva matrícula": estudiantes inscritos para el año del periodo y sin matrícula. */
	public record EstudianteSinMatricula(UUID id, String nombre, String documento, String gradoCodigo, String grado) {
	}

	/** Sección que el sistema asignaría hoy (primera del grado con cupo, en orden alfabético). */
	public record SeccionAsignada(UUID id, String etiqueta, String tutor, int capacidad, int ocupadas) {
	}

	/** Documento que pide el periodo (HU-21/22). */
	public record DocumentoPedido(UUID id, String nombre, String descripcion, boolean tienePlantilla) {
	}

	/**
	 * Figma A6 "Nueva matrícula": lo que se muestra al elegir al alumno. {@code seccion} es null si el grado no tiene
	 * ninguna sección con cupo (entonces no se puede guardar). {@code tipo}: NUEVO o CONTINUADOR.
	 */
	public record PreparacionMatricula(UUID estudianteId, String nombre, String documento, String gradoCodigo,
			String grado, String tipo, SeccionAsignada seccion, String apoderado, String contacto,
			String contactoEmergencia, String informacionMedica, List<DocumentoPedido> documentos) {
	}

	/** Respuesta de crear la matrícula: queda en SECCION_RESERVADA con la sección asignada. */
	public record MatriculaCreada(UUID id, String estado, String seccion, int documentosSubidos,
			boolean seccionReasignada) {
	}

	/** Un archivo ya leído del formulario, listo para guardar. */
	public record ArchivoMatricula(String nombre, String tipoMime, byte[] contenido) {
	}

	/** Una sección del grado en el modal "Cambiar sección": con tutor, capacidad y alumnos que ocupan vacante. */
	public record SeccionOpcion(UUID id, String etiqueta, String tutor, int capacidad, int ocupadas) {
	}

	/** Secciones del grado de la matrícula; {@code seleccionadaId} es la actual (o la propuesta al crear). */
	public record SeccionesDelGrado(String grado, UUID seleccionadaId, List<SeccionOpcion> secciones) {
	}

	/** Archivo ya guardado de un documento (sin su contenido). {@code estado}: EN_REVISION, APROBADO u OBSERVADO. */
	public record ArchivoGuardado(String nombre, String tipoMime, int tamanoBytes, String estado, String observacion,
			OffsetDateTime subidoEn) {
	}

	/**
	 * Documento que pide el periodo y, si ya se subió, su archivo ({@code archivo} es null mientras esté pendiente).
	 */
	public record DocumentoDeMatricula(UUID id, String nombre, String descripcion, boolean tienePlantilla,
			ArchivoGuardado archivo) {
	}

	/**
	 * Figma Personal administrativo › A5, B6 y B7 "Detalle de matrícula". {@code pasoActual} es el del indicador de
	 * pasos (2 con la sección reservada o en validación, 3 pendiente de pago, 4 matriculado).
	 */
	public record DetalleMatricula(UUID id, UUID periodoId, int anio, UUID estudianteId, String nombre,
			String documento, String tipo, String gradoCodigo, String grado, SeccionAsignada seccion, String estado,
			String codigo, int pasoActual, String apoderado, String contacto, String contactoEmergencia,
			String informacionMedica, boolean puedeCambiarSeccion, boolean puedeEditarDocumentos, boolean puedeValidar,
			List<DocumentoDeMatricula> documentos) {
	}

	/** Un documento de la matrícula visto por el apoderado: con su plantilla para descargar y lo que ya subió. */
	public record DocumentoApoderado(UUID id, String nombre, String descripcion, boolean tienePlantilla,
			ArchivoGuardado archivo) {
	}

	/** Documentos de la matrícula de un hijo y si ya se pueden subir o enviar a validación. */
	public record DocumentosApoderado(String estado, boolean puedeSubir, List<DocumentoApoderado> documentos) {
	}

	/** Archivo para ver en el navegador. */
	public record ArchivoParaVer(String nombre, String tipoMime, byte[] contenido) {
	}
}
