package com.galiledu.configuracion.infraestructura;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.Seccion;

/**
 * Lecturas y escrituras de SQL del periodo de matrícula (pantalla 13): las matrículas y las entregas de documentos (del
 * módulo de matrícula, solo lectura), los años que aún no tienen periodo y el contenido de las plantillas. El contenido
 * (BYTEA, hasta 10 MB) no pasa por la entidad: se guarda, lee y quita aquí, y el nombre, el tipo y el contenido se
 * escriben en la misma sentencia porque ck_documento_requerido_plantilla exige que estén todos o ninguno.
 */
@Repository
public class ConsultasPeriodoMatricula {
	private final JdbcClient jdbc;

	public ConsultasPeriodoMatricula(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record AnioDisponible(UUID id, int anio, EstadoAnioEscolar estado) {
	}

	public record PlantillaGuardada(String nombreArchivo, String tipoMime, byte[] contenido) {
	}

	/** Años PLANIFICADO o ACTIVO que todavía no tienen periodo de matrícula, del más antiguo al más reciente. */
	public List<AnioDisponible> aniosSinPeriodo() {
		return jdbc.sql("""
				SELECT a.id, a.anio, a.estado FROM configuracion.anio_escolar a
				WHERE a.estado IN ('PLANIFICADO', 'ACTIVO')
				  AND NOT EXISTS (SELECT 1 FROM matricula.periodo_matricula p WHERE p.anio_escolar_id = a.id)
				ORDER BY a.anio""")
				.query((rs, i) -> new AnioDisponible(rs.getObject(1, UUID.class), rs.getInt(2),
						EstadoAnioEscolar.valueOf(rs.getString(3))))
				.list();
	}

	/**
	 * Matrículas del año que ocupan vacante: todas menos las RETIRADO y ANULADA, el mismo conteo de las secciones
	 * (HU-75).
	 */
	public int matriculasDelAnio(UUID anioEscolarId) {
		return jdbc.sql("SELECT count(*) FROM matricula.matricula m WHERE m.anio_escolar_id = ? AND "
				+ Seccion.SQL_OCUPA_VACANTE)
				.param(anioEscolarId)
				.query(Integer.class)
				.single();
	}

	/** Lo mismo para todos los años con matrículas (la columna "Matrículas" del listado). */
	public Map<UUID, Integer> matriculasPorAnio() {
		Map<UUID, Integer> conteos = new HashMap<>();
		jdbc.sql("""
				SELECT m.anio_escolar_id, count(*) FROM matricula.matricula m
				WHERE %s GROUP BY m.anio_escolar_id""".formatted(Seccion.SQL_OCUPA_VACANTE))
				.query((rs, i) -> {
					conteos.put(rs.getObject(1, UUID.class), rs.getInt(2));
					return null;
				})
				.list();
		return conteos;
	}

	/** ¿Hay alguna matrícula del año, en cualquier estado? Con una no se puede eliminar el periodo. */
	public boolean tieneMatriculas(UUID anioEscolarId) {
		return jdbc.sql("SELECT EXISTS (SELECT 1 FROM matricula.matricula WHERE anio_escolar_id = ?)")
				.param(anioEscolarId)
				.query(Boolean.class)
				.single();
	}

	/** ¿Algún apoderado ya entregó algún documento de este periodo? Con una entrega no se puede eliminar el periodo. */
	public boolean tieneEntregas(UUID periodoId) {
		return jdbc.sql("""
				SELECT EXISTS (SELECT 1 FROM matricula.documento_matricula dm
				               JOIN matricula.documento_requerido dr ON dr.id = dm.documento_requerido_id
				               WHERE dr.periodo_matricula_id = ?)""")
				.param(periodoId)
				.query(Boolean.class)
				.single();
	}

	/** Los documentos del periodo que ya tienen alguna entrega: no se pueden quitar. */
	public Set<UUID> documentosConEntregas(UUID periodoId) {
		return new HashSet<>(jdbc.sql("""
				SELECT DISTINCT dm.documento_requerido_id FROM matricula.documento_matricula dm
				JOIN matricula.documento_requerido dr ON dr.id = dm.documento_requerido_id
				WHERE dr.periodo_matricula_id = ?""")
				.param(periodoId)
				.query(UUID.class)
				.list());
	}

	/** Guarda o reemplaza la plantilla de un documento. Devuelve false si el documento no existe. */
	public boolean guardarPlantilla(UUID documentoId, String nombreArchivo, String tipoMime, byte[] contenido) {
		return jdbc.sql("""
				UPDATE matricula.documento_requerido
				SET plantilla_nombre_archivo = ?, plantilla_tipo_mime = ?, plantilla_contenido = ?
				WHERE id = ?""")
				.params(nombreArchivo, tipoMime, contenido, documentoId)
				.update() > 0;
	}

	public void quitarPlantilla(UUID documentoId) {
		jdbc.sql("""
				UPDATE matricula.documento_requerido
				SET plantilla_nombre_archivo = NULL, plantilla_tipo_mime = NULL, plantilla_contenido = NULL
				WHERE id = ?""")
				.param(documentoId)
				.update();
	}

	/** La plantilla de un documento; vacío si no tiene. */
	public Optional<PlantillaGuardada> plantilla(UUID documentoId) {
		return jdbc.sql("""
				SELECT plantilla_nombre_archivo, plantilla_tipo_mime, plantilla_contenido
				FROM matricula.documento_requerido
				WHERE id = ? AND plantilla_contenido IS NOT NULL""")
				.param(documentoId)
				.query((rs, i) -> new PlantillaGuardada(rs.getString(1), rs.getString(2), rs.getBytes(3)))
				.optional();
	}
}
