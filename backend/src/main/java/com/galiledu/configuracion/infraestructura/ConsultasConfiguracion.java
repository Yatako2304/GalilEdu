package com.galiledu.configuracion.infraestructura;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.Regimen;

/**
 * Lecturas de solo consulta de la configuración del año escolar (HU-74): el listado de años con sus conteos y los datos
 * de otros módulos (matrícula, plan de evaluación), que se leen por SQL sin usar sus entidades. No lee el estado de los
 * periodos: se calcula con las fechas.
 */
@Repository
public class ConsultasConfiguracion {
	private final JdbcClient jdbc;

	public ConsultasConfiguracion(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record AnioListado(UUID id, int anio, LocalDate fechaInicio, LocalDate fechaFin, Regimen regimen,
			EstadoAnioEscolar estado, int cantidadPeriodos, int seccionesActivas, int estructurasHorarias) {
	}

	/**
	 * Conteos con los que se arman los 3 requisitos obligatorios y los 2 recomendados de "Antes de activar el año"
	 * (Figma CI-06) y las cifras de la card del año en curso (CI-01).
	 * <ul>
	 * <li>{@code gradosConSecciones}: grados con al menos una sección activa.</li>
	 * <li>{@code gradosConSeccionesConPlan}: de esos, los que tienen plan de evaluación (academico.plan_curso) en el
	 * año. Se cuentan solo los grados con secciones para que un plan de un grado sin secciones no tape un
	 * faltante.</li>
	 * </ul>
	 */
	public record ConteosAnio(int periodos, int seccionesActivas, int estructurasHorarias, int seccionesSinTutor,
			int gradosConSecciones, int gradosConSeccionesConPlan) {
	}

	/** Del más reciente al más antiguo, como el listado "CI-01 · Año escolar" de Figma. */
	public List<AnioListado> anios() {
		return jdbc
				.sql("""
						SELECT a.id, a.anio, a.fecha_inicio, a.fecha_fin, a.regimen, a.estado,
						       (SELECT count(*) FROM configuracion.periodo_academico p WHERE p.anio_escolar_id = a.id),
						       (SELECT count(*) FROM configuracion.seccion s WHERE s.anio_escolar_id = a.id AND s.estado = 'ACTIVA'),
						       (SELECT count(*) FROM configuracion.estructura_horaria e WHERE e.anio_escolar_id = a.id)
						FROM configuracion.anio_escolar a
						ORDER BY a.anio DESC""")
				.query((rs, i) -> new AnioListado(rs.getObject(1, UUID.class), rs.getInt(2),
						rs.getObject(3, LocalDate.class), rs.getObject(4, LocalDate.class),
						Regimen.valueOf(rs.getString(5)), EstadoAnioEscolar.valueOf(rs.getString(6)), rs.getInt(7),
						rs.getInt(8), rs.getInt(9)))
				.list();
	}

	/** Conteos de un año; todos son 0 si el año no existe. */
	public ConteosAnio conteos(UUID anioId) {
		return jdbc.sql("""
				SELECT (SELECT count(*) FROM configuracion.periodo_academico p WHERE p.anio_escolar_id = :anio),
				       (SELECT count(*) FROM configuracion.seccion s
				         WHERE s.anio_escolar_id = :anio AND s.estado = 'ACTIVA'),
				       (SELECT count(*) FROM configuracion.estructura_horaria e WHERE e.anio_escolar_id = :anio),
				       (SELECT count(*) FROM configuracion.seccion s
				         WHERE s.anio_escolar_id = :anio AND s.estado = 'ACTIVA' AND s.tutor_id IS NULL),
				       (SELECT count(DISTINCT s.grado_codigo) FROM configuracion.seccion s
				         WHERE s.anio_escolar_id = :anio AND s.estado = 'ACTIVA'),
				       (SELECT count(DISTINCT s.grado_codigo) FROM configuracion.seccion s
				         WHERE s.anio_escolar_id = :anio AND s.estado = 'ACTIVA'
				           AND EXISTS (SELECT 1 FROM academico.plan_curso pc
				                        WHERE pc.anio_escolar_id = :anio AND pc.grado_codigo = s.grado_codigo))""")
				.param("anio", anioId)
				.query((rs, i) -> new ConteosAnio(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getInt(5),
						rs.getInt(6)))
				.single();
	}

	/** Códigos de las estructuras horarias del año (EH-PRI-26, EH-SEC-26…), en orden alfabético. */
	public List<String> codigosEstructuras(UUID anioId) {
		return jdbc.sql(
				"SELECT codigo FROM configuracion.estructura_horaria WHERE anio_escolar_id = :anio ORDER BY codigo")
				.param("anio", anioId)
				.query(String.class)
				.list();
	}

	/** Alumnos con matrícula confirmada en el año; no cuentan las reservas, las retiradas ni las anuladas. */
	public int alumnosMatriculados(UUID anioId) {
		return jdbc.sql("""
				SELECT count(*) FROM matricula.matricula
				WHERE anio_escolar_id = :anio AND estado = 'MATRICULADO'""")
				.param("anio", anioId)
				.query(Integer.class)
				.single();
	}
}
