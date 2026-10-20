package com.galiledu.academico.infraestructura;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Datos de otros módulos que el catálogo solo lee: grados, años escolares, secciones y estructura horaria
 * (configuracion, V4 y V7), y docentes (seguridad, V1). Mientras esos módulos no tengan servicio, se leen con SQL.
 */
@Component
public class ConsultasCatalogo {
	private final JdbcClient jdbc;

	public ConsultasCatalogo(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record AnioEscolar(UUID id, int anio, String estado) {
		public boolean estaCerrado() {
			return "CERRADO".equals(estado);
		}
	}

	public record SeccionGrado(UUID id, String nombre, String tutor) {
	}

	public record Docente(UUID id, String nombre) {
	}

	/** Códigos de grado en el orden del catálogo (PRI-1…PRI-6, SEC-1…SEC-5). */
	public List<String> codigosDeGrado() {
		return jdbc.sql("SELECT codigo FROM configuracion.grado ORDER BY orden").query(String.class).list();
	}

	/** Nombre del grado ("1.° de secundaria"). */
	public String nombreDeGrado(String codigo) {
		return jdbc.sql("SELECT nombre FROM configuracion.grado WHERE codigo = ?").param(codigo)
				.query(String.class).optional().orElse(codigo);
	}

	public Optional<Integer> anioActivo() {
		return jdbc.sql("SELECT anio FROM configuracion.anio_escolar WHERE estado = 'ACTIVO'")
				.query(Integer.class).optional();
	}

	/** Grados en los que el curso está en algún plan de evaluación: no se pueden quitar del curso. */
	public List<String> gradosPlanificados(UUID cursoId) {
		return jdbc.sql("SELECT DISTINCT grado_codigo FROM academico.plan_curso WHERE curso_id = ?")
				.param(cursoId).query(String.class).list();
	}

	/** Años escolares, del más reciente al más antiguo. */
	public List<AnioEscolar> aniosEscolares() {
		return jdbc.sql("SELECT id, anio, estado FROM configuracion.anio_escolar ORDER BY anio DESC")
				.query(AnioEscolar.class).list();
	}

	public Optional<AnioEscolar> anioEscolar(UUID id) {
		return jdbc.sql("SELECT id, anio, estado FROM configuracion.anio_escolar WHERE id = ?").param(id)
				.query(AnioEscolar.class).optional();
	}

	/** Secciones activas del grado en ese año, con el nombre de su tutor (Figma: "Sección 1.° A · tutor: …"). */
	public List<SeccionGrado> secciones(UUID anioEscolarId, String gradoCodigo) {
		return jdbc.sql("""
				SELECT s.id, s.nombre,
				       concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS tutor
				FROM configuracion.seccion s
				LEFT JOIN seguridad.usuario u ON u.id = s.tutor_id
				LEFT JOIN personas.persona p ON p.id = u.persona_id
				WHERE s.anio_escolar_id = ? AND s.grado_codigo = ? AND s.estado = 'ACTIVA'
				ORDER BY s.nombre""")
				.params(anioEscolarId, gradoCodigo).query(SeccionGrado.class).list();
	}

	/**
	 * Bloques de clase por semana de la estructura horaria principal del nivel del grado (bloques por día × días
	 * lectivos). Vacío si el año aún no tiene estructura.
	 */
	public Optional<Integer> bloquesSemanales(UUID anioEscolarId, String gradoCodigo) {
		return jdbc.sql("""
				SELECT eh.bloques_por_dia * cardinality(eh.dias_lectivos)
				FROM configuracion.estructura_horaria eh
				JOIN configuracion.grado g ON g.nivel = eh.nivel
				WHERE eh.anio_escolar_id = ? AND g.codigo = ? AND eh.es_principal""")
				.params(anioEscolarId, gradoCodigo).query(Integer.class).optional();
	}

	/** Usuarios activos con rol Docente, por nombre. */
	public List<Docente> docentes() {
		return jdbc.sql("""
				SELECT u.id, concat_ws(' ', p.nombres, p.apellido_paterno, p.apellido_materno) AS nombre
				FROM seguridad.usuario u
				JOIN seguridad.usuario_rol r ON r.usuario_id = u.id AND r.rol_codigo = 'DOCENTE'
				JOIN personas.persona p ON p.id = u.persona_id
				WHERE u.estado = 'ACTIVO' AND NOT u.eliminado
				ORDER BY nombre""")
				.query(Docente.class).list();
	}
}
