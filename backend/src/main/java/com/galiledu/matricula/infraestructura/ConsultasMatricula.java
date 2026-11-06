package com.galiledu.matricula.infraestructura;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Lecturas de solo consulta para la matrícula del apoderado (HU-27, HU-30).
 * Los datos del estudiante y su familia se leen por SQL porque esos módulos aún no exponen un servicio para esto;
 * cuando lo hagan, reemplazar {@link #hijosDe(UUID)} y {@link #personaDeUsuario(UUID)} por esas llamadas.
 */
@Repository
public class ConsultasMatricula {
	private final JdbcClient jdbc;

	public ConsultasMatricula(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record HijoFila(UUID estudianteId, String nombre, String grado) {
	}

	public record PeriodoFila(UUID id, UUID anioEscolarId, int anio, OffsetDateTime inicio, OffsetDateTime cierre,
			String estado, boolean bloquearPorMorosidad) {
	}

	public record MatriculaFila(UUID id, String estado, String grado, String seccion, String codigo,
			OffsetDateTime confirmadoEn) {
	}

	public Optional<UUID> personaDeUsuario(UUID usuarioId) {
		return jdbc.sql("SELECT persona_id FROM seguridad.usuario WHERE id = ?")
				.param(usuarioId)
				.query(UUID.class)
				.optional();
	}

	/** Estudiantes vinculados a la persona (como padre, madre o apoderado): sus hijos, que entre sí son hermanos. */
	public List<HijoFila> hijosDe(UUID personaId) {
		return jdbc.sql("""
				SELECT DISTINCT e.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS nombre,
				       g.nombre AS grado
				FROM estudiantes.vinculo_familiar v
				JOIN estudiantes.estudiante e ON e.id = v.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				LEFT JOIN configuracion.grado g ON g.codigo = e.grado_codigo
				WHERE v.persona_id = ? AND NOT e.eliminado
				ORDER BY nombre""")
				.param(personaId)
				.query((rs, i) -> new HijoFila(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3)))
				.list();
	}

	/** RF-58: el periodo vigente es el HABILITADO; si no hay ninguno, el del año más reciente. */
	public Optional<PeriodoFila> periodoVigente() {
		return jdbc.sql("""
				SELECT pm.id, a.id, a.anio, pm.inicio, pm.cierre, pm.estado, pm.bloquear_por_morosidad
				FROM matricula.periodo_matricula pm
				JOIN configuracion.anio_escolar a ON a.id = pm.anio_escolar_id
				ORDER BY (pm.estado = 'HABILITADO') DESC, a.anio DESC
				LIMIT 1""")
				.query((rs, i) -> new PeriodoFila(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
						rs.getInt(3),
						rs.getObject(4, OffsetDateTime.class), rs.getObject(5, OffsetDateTime.class), rs.getString(6),
						rs.getBoolean(7)))
				.optional();
	}

	public Optional<MatriculaFila> matricula(UUID estudianteId, UUID anioEscolarId) {
		return jdbc.sql("""
				SELECT m.id, m.estado, g.nombre, g.numero || '.° ' || s.nombre, m.codigo, m.confirmado_en
				FROM matricula.matricula m
				JOIN configuracion.grado g ON g.codigo = m.grado_codigo
				LEFT JOIN configuracion.seccion s ON s.id = m.seccion_id
				WHERE m.estudiante_id = ? AND m.anio_escolar_id = ?""")
				.params(estudianteId, anioEscolarId)
				.query((rs, i) -> new MatriculaFila(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
						rs.getString(4), rs.getString(5), rs.getObject(6, OffsetDateTime.class)))
				.optional();
	}

	public int documentosRequeridos(UUID periodoId) {
		return jdbc.sql("SELECT count(*) FROM matricula.documento_requerido WHERE periodo_matricula_id = ?")
				.param(periodoId)
				.query(Integer.class)
				.single();
	}

	public record PeriodoPorId(UUID id, UUID anioEscolarId, int anio, OffsetDateTime inicio, OffsetDateTime cierre,
			boolean permiteExtemporanea) {
	}

	public Optional<PeriodoPorId> periodoPorId(UUID periodoId) {
		return jdbc.sql("""
				SELECT pm.id, a.id, a.anio, pm.inicio, pm.cierre, pm.permitir_extemporanea
				FROM matricula.periodo_matricula pm
				JOIN configuracion.anio_escolar a ON a.id = pm.anio_escolar_id
				WHERE pm.id = ?""")
				.param(periodoId)
				.query((rs, i) -> new PeriodoPorId(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
						rs.getInt(3), rs.getObject(4, OffsetDateTime.class), rs.getObject(5, OffsetDateTime.class),
						rs.getBoolean(6)))
				.optional();
	}

	/** Estados que muestra el listado del periodo: las anuladas y las retiradas no cuentan como "en el periodo". */
	private static final String ESTADOS_DEL_LISTADO = "('SECCION_RESERVADA', 'EN_VALIDACION', 'PENDIENTE_PAGO', 'MATRICULADO')";

	/** Cuántas matrículas del año hay por estado (solo los estados del listado). */
	public java.util.Map<String, Integer> contarPorEstado(UUID anioEscolarId) {
		java.util.Map<String, Integer> conteo = new java.util.HashMap<>();
		jdbc.sql("""
				SELECT m.estado, count(*) FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				WHERE m.anio_escolar_id = ? AND NOT e.eliminado AND m.estado IN """ + ESTADOS_DEL_LISTADO + """
				GROUP BY m.estado""")
				.param(anioEscolarId)
				.query((rs, i) -> {
					conteo.put(rs.getString(1), rs.getInt(2));
					return null;
				}).list();
		return conteo;
	}

	/** Un trozo del listado y el total que cumple los filtros. */
	public record PaginaFilas<T>(List<T> filas, long total) {
	}

	public record FilaListado(UUID id, UUID estudianteId, String nombre, String documento, String gradoCodigo,
			String grado, String seccion, String estado, String codigo, String tipo) {
	}

	/**
	 * Listado de A3. {@code texto} busca por nombre, documento o código; {@code orden} es una de las columnas de
	 * {@link #ORDENES} y {@code descendente} invierte el sentido.
	 */
	public PaginaFilas<FilaListado> listar(UUID anioEscolarId, String texto, String gradoCodigo, String estado,
			String orden, boolean descendente, int pagina, int tamano) {
		String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim() + "%";
		String filtros = """
				FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				JOIN configuracion.grado g ON g.codigo = m.grado_codigo
				LEFT JOIN configuracion.seccion s ON s.id = m.seccion_id
				WHERE m.anio_escolar_id = :anio AND NOT e.eliminado
				  AND m.estado IN """ + ESTADOS_DEL_LISTADO + """

				AND (CAST(:grado AS text) IS NULL OR m.grado_codigo = CAST(:grado AS text))
				AND (CAST(:estado AS text) IS NULL OR m.estado = CAST(:estado AS text))
				AND (CAST(:patron AS text) IS NULL
				     OR concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) ILIKE CAST(:patron AS text)
				     OR p.numero_documento ILIKE CAST(:patron AS text)
				     OR m.codigo ILIKE CAST(:patron AS text))""";
		long total = jdbc.sql("SELECT count(*) " + filtros)
				.param("anio", anioEscolarId).param("grado", gradoCodigo).param("estado", estado)
				.param("patron", patron)
				.query(Long.class).single();
		String columna = ORDENES.getOrDefault(orden, ORDENES.get("estudiante"));
		String sentido = descendente ? " DESC" : " ASC";
		List<FilaListado> filas = jdbc.sql("""
				SELECT m.id, e.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno),
				       p.numero_documento, g.codigo, g.nombre, CASE WHEN s.id IS NULL THEN NULL
				       ELSE g.numero || '.° ' || s.nombre END, m.estado, m.codigo, m.tipo_estudiante
				""" + filtros
				+ " ORDER BY " + columna + sentido + ", p.apellido_paterno, p.nombres, m.id LIMIT :tam OFFSET :desde")
				.param("anio", anioEscolarId).param("grado", gradoCodigo).param("estado", estado)
				.param("patron", patron).param("tam", tamano).param("desde", (long) (pagina - 1) * tamano)
				.query((rs, i) -> new FilaListado(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
						rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7),
						rs.getString(8), rs.getString(9), rs.getString(10)))
				.list();
		return new PaginaFilas<>(filas, total);
	}

	/** Columnas por las que se puede ordenar (lista cerrada: nunca se arma SQL con lo que escribe el usuario). */
	private static final java.util.Map<String, String> ORDENES = java.util.Map.of(
			"estudiante", "p.apellido_paterno",
			"grado", "g.orden",
			"seccion", "s.nombre",
			"estado", "m.estado");

	public record EstudianteFila(UUID id, String nombre, String documento, String gradoCodigo, String grado) {
	}

	/**
	 * Estudiantes activos inscritos para el año del periodo (su "año académico") que aún no tienen matrícula en él.
	 * {@code texto} busca por nombre o documento.
	 */
	public List<EstudianteFila> estudiantesSinMatricula(int anio, UUID anioEscolarId, String texto, int limite) {
		String patron = texto == null || texto.isBlank() ? null : "%" + texto.trim() + "%";
		return jdbc.sql("""
				SELECT e.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno), p.numero_documento,
				       g.codigo, g.nombre
				FROM estudiantes.estudiante e
				JOIN personas.persona p ON p.id = e.persona_id
				JOIN configuracion.grado g ON g.codigo = e.grado_codigo
				WHERE NOT e.eliminado AND e.estado = 'ACTIVO' AND e.anio_academico = :anio
				  AND NOT EXISTS (SELECT 1 FROM matricula.matricula m
				                  WHERE m.estudiante_id = e.id AND m.anio_escolar_id = :anioEscolar)
				  AND (CAST(:patron AS text) IS NULL
				       OR concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) ILIKE CAST(:patron AS text)
				       OR p.numero_documento ILIKE CAST(:patron AS text))
				ORDER BY p.apellido_paterno, p.apellido_materno, p.nombres
				LIMIT :limite""")
				.param("anio", anio).param("anioEscolar", anioEscolarId).param("patron", patron)
				.param("limite", limite)
				.query((rs, i) -> new EstudianteFila(rs.getObject(1, UUID.class), rs.getString(2), rs.getString(3),
						rs.getString(4), rs.getString(5)))
				.list();
	}

	/** El estudiante si está activo y no eliminado, con el año académico para el que se inscribió. */
	public Optional<EstudianteParaMatricula> estudianteParaMatricula(UUID estudianteId) {
		return jdbc.sql("""
				SELECT e.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno), p.numero_documento,
				       g.codigo, g.nombre, e.anio_academico
				FROM estudiantes.estudiante e
				JOIN personas.persona p ON p.id = e.persona_id
				JOIN configuracion.grado g ON g.codigo = e.grado_codigo
				WHERE e.id = ? AND NOT e.eliminado AND e.estado = 'ACTIVO'""")
				.param(estudianteId)
				.query((rs, i) -> new EstudianteParaMatricula(rs.getObject(1, UUID.class), rs.getString(2),
						rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6)))
				.optional();
	}

	public record EstudianteParaMatricula(UUID id, String nombre, String documento, String gradoCodigo, String grado,
			int anioAcademico) {
	}

	public boolean tieneMatricula(UUID estudianteId, UUID anioEscolarId) {
		return jdbc.sql("SELECT count(*) FROM matricula.matricula WHERE estudiante_id = ? AND anio_escolar_id = ?")
				.params(estudianteId, anioEscolarId).query(Integer.class).single() > 0;
	}

	/** CONTINUADOR si ya estuvo matriculado (o se retiró habiendo estado matriculado) en un año anterior. */
	public boolean fueMatriculadoAntes(UUID estudianteId, int anio) {
		return jdbc.sql("""
				SELECT count(*) FROM matricula.matricula m
				JOIN configuracion.anio_escolar a ON a.id = m.anio_escolar_id
				WHERE m.estudiante_id = ? AND a.anio < ? AND m.estado IN ('MATRICULADO', 'RETIRADO')""")
				.params(estudianteId, anio).query(Integer.class).single() > 0;
	}

	public record ApoderadoFila(String nombre, String celular, String correo) {
	}

	/** El responsable de la matrícula (el apoderado de Figma): quien paga y firma. */
	public Optional<ApoderadoFila> responsableDe(UUID estudianteId) {
		return jdbc.sql("""
				SELECT concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno), p.celular, p.correo
				FROM estudiantes.vinculo_familiar v
				JOIN personas.persona p ON p.id = v.persona_id
				WHERE v.estudiante_id = ? AND v.es_responsable_matricula""")
				.param(estudianteId)
				.query((rs, i) -> new ApoderadoFila(rs.getString(1), rs.getString(2), rs.getString(3)))
				.optional();
	}

	public record ContactoFila(String nombre, String parentesco, String celular) {
	}

	public Optional<ContactoFila> contactoDeEmergencia(UUID estudianteId) {
		return jdbc.sql("""
				SELECT nombre_completo, parentesco, celular FROM estudiantes.contacto_emergencia
				WHERE estudiante_id = ? ORDER BY orden LIMIT 1""")
				.param(estudianteId)
				.query((rs, i) -> new ContactoFila(rs.getString(1), rs.getString(2), rs.getString(3)))
				.optional();
	}

	public record FichaMedicaFila(boolean tieneAlergias, String alergiasAlimentarias, String alergiasMedicamentosas,
			boolean tieneEnfermedad, String enfermedadDetalle, boolean tomaMedicamento, String medicamentoDetalle) {
	}

	public Optional<FichaMedicaFila> fichaMedica(UUID estudianteId) {
		return jdbc.sql("""
				SELECT tiene_alergias, alergias_alimentarias, alergias_medicamentosas, tiene_enfermedad,
				       enfermedad_detalle, toma_medicamento, medicamento_detalle
				FROM estudiantes.ficha_medica WHERE estudiante_id = ?""")
				.param(estudianteId)
				.query((rs, i) -> new FichaMedicaFila(rs.getBoolean(1), rs.getString(2), rs.getString(3),
						rs.getBoolean(4), rs.getString(5), rs.getBoolean(6), rs.getString(7)))
				.optional();
	}

	public record DocumentoRequeridoFila(UUID id, String nombre, String descripcion, boolean tienePlantilla) {
	}

	public List<DocumentoRequeridoFila> documentosRequeridosDe(UUID periodoId) {
		return jdbc.sql("""
				SELECT id, nombre, descripcion, plantilla_contenido IS NOT NULL
				FROM matricula.documento_requerido WHERE periodo_matricula_id = ? ORDER BY orden, nombre""")
				.param(periodoId)
				.query((rs, i) -> new DocumentoRequeridoFila(rs.getObject(1, UUID.class), rs.getString(2),
						rs.getString(3), rs.getBoolean(4)))
				.list();
	}

	public record MatriculaDetalleFila(UUID id, String estado, String codigo, String tipo, UUID estudianteId,
			String nombre, String documento, String gradoCodigo, String grado, UUID seccionId, UUID anioEscolarId,
			int anio, UUID periodoId) {
	}

	/** La matrícula con su estudiante, grado, año y el periodo de matrícula de ese año. */
	public Optional<MatriculaDetalleFila> matriculaDetalle(UUID matriculaId) {
		return jdbc.sql("""
				SELECT m.id, m.estado, m.codigo, m.tipo_estudiante, e.id,
				       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno), p.numero_documento,
				       g.codigo, g.nombre, m.seccion_id, a.id, a.anio, pm.id
				FROM matricula.matricula m
				JOIN estudiantes.estudiante e ON e.id = m.estudiante_id
				JOIN personas.persona p ON p.id = e.persona_id
				JOIN configuracion.grado g ON g.codigo = m.grado_codigo
				JOIN configuracion.anio_escolar a ON a.id = m.anio_escolar_id
				JOIN matricula.periodo_matricula pm ON pm.anio_escolar_id = a.id
				WHERE m.id = ?""")
				.param(matriculaId)
				.query((rs, i) -> new MatriculaDetalleFila(rs.getObject(1, UUID.class), rs.getString(2),
						rs.getString(3), rs.getString(4), rs.getObject(5, UUID.class), rs.getString(6),
						rs.getString(7), rs.getString(8), rs.getString(9), rs.getObject(10, UUID.class),
						rs.getObject(11, UUID.class), rs.getInt(12), rs.getObject(13, UUID.class)))
				.optional();
	}

	public record DocumentoGuardadoFila(UUID documentoRequeridoId, String nombreArchivo, String tipoMime,
			int tamanoBytes, String estado, String observacion, OffsetDateTime subidoEn) {
	}

	public List<DocumentoGuardadoFila> documentosGuardados(UUID matriculaId) {
		return jdbc.sql("""
				SELECT documento_requerido_id, nombre_archivo, tipo_mime, tamano_bytes, estado, observacion, subido_en
				FROM matricula.documento_matricula WHERE matricula_id = ?""")
				.param(matriculaId)
				.query((rs, i) -> new DocumentoGuardadoFila(rs.getObject(1, UUID.class), rs.getString(2),
						rs.getString(3), rs.getInt(4), rs.getString(5), rs.getString(6),
						rs.getObject(7, OffsetDateTime.class)))
				.list();
	}

	public record ArchivoFila(String nombre, String tipoMime, byte[] contenido) {
	}

	public Optional<ArchivoFila> archivoDelDocumento(UUID matriculaId, UUID documentoRequeridoId) {
		return jdbc.sql("""
				SELECT nombre_archivo, tipo_mime, contenido FROM matricula.documento_matricula
				WHERE matricula_id = ? AND documento_requerido_id = ?""")
				.params(matriculaId, documentoRequeridoId)
				.query((rs, i) -> new ArchivoFila(rs.getString(1), rs.getString(2), rs.getBytes(3)))
				.optional();
	}

	/** La cuota de matrícula vigente (no anulada) de la matrícula. */
	public Optional<CuotaMatriculaFila> cuotaMatricula(UUID matriculaId) {
		return jdbc.sql("""
				SELECT q.id, q.nombre, q.monto_final, q.estado, q.fecha_vencimiento, q.fecha_pago
				FROM matricula.matricula m
				JOIN pagos.compromiso_pago cp ON cp.matricula_id = m.id
				    OR (cp.estudiante_id = m.estudiante_id AND cp.anio_escolar_id = m.anio_escolar_id)
				JOIN pagos.cuota q ON q.compromiso_id = cp.id
				WHERE m.id = ? AND q.concepto_codigo = 'MATRICULA' AND q.estado <> 'ANULADA'""")
				.param(matriculaId)
				.query((rs, i) -> new CuotaMatriculaFila(rs.getObject(1, UUID.class), rs.getString(2),
						rs.getBigDecimal(3), rs.getString(4), rs.getObject(5, LocalDate.class),
						rs.getObject(6, LocalDate.class)))
				.optional();
	}

	public record CuotaMatriculaFila(UUID cuotaId, String concepto, java.math.BigDecimal monto, String estado,
			LocalDate fechaVencimiento, LocalDate fechaPago) {
	}

	/** Documentos subidos que no fueron observados (en revisión o aprobados). */
	public int documentosEntregados(UUID matriculaId) {
		return jdbc.sql("""
				SELECT count(*) FROM matricula.documento_matricula
				WHERE matricula_id = ? AND estado <> 'OBSERVADO'""")
				.param(matriculaId)
				.query(Integer.class)
				.single();
	}
}
