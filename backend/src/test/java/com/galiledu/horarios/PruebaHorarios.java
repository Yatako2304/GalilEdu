package com.galiledu.horarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/**
 * Base de las pruebas de Horarios: arma por SQL, en 2027 (año PLANIFICADO, sin estructuras ni secciones), lo que cada
 * prueba necesita. Todo se deshace al final de la prueba (la transacción de {@link PruebaIntegracion}).
 */
abstract class PruebaHorarios extends PruebaIntegracion {
	protected static final UUID ANIO_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");

	@Autowired
	protected JdbcClient jdbc;

	/** Una celda del cuerpo del PUT. */
	protected record Celda(String dia, int numeroBloque, UUID planCursoId, UUID ambienteId) {
	}

	/**
	 * Estructura de 6 bloques de 45 min desde las 08:00 (con receso de 20 min después del bloque 3) de lunes a viernes.
	 */
	protected UUID estructura(String codigo, String nivel, boolean principal, int bloques) {
		return estructura(codigo, nivel, principal, bloques, "08:00");
	}

	protected UUID estructura(String codigo, String nivel, boolean principal, int bloques, String horaEntrada) {
		UUID id = jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria
				    (anio_escolar_id, codigo, dias_lectivos, hora_entrada, bloques_por_dia, duracion_bloque_min, nivel,
				     es_principal)
				VALUES (:anio, :codigo, ARRAY['LUN','MAR','MIE','JUE','VIE']::varchar[], CAST(:hora AS time), :bloques,
				        45, :nivel, :principal)
				RETURNING id""")
				.param("anio", ANIO_2027).param("codigo", codigo).param("bloques", bloques).param("nivel", nivel)
				.param("principal", principal).param("hora", horaEntrada).query(UUID.class).single();
		if (bloques >= 3) {
			jdbc.sql("""
					INSERT INTO configuracion.receso (estructura_horaria_id, despues_de_bloque, duracion_min)
					VALUES (:id, 3, 20)""").param("id", id).update();
		}
		return id;
	}

	protected UUID ambiente(String codigo, String estado) {
		return jdbc.sql("""
				INSERT INTO configuracion.ambiente (codigo, nombre, tipo, aforo_maximo, estado)
				VALUES (:codigo, :nombre, 'AULA_REGULAR', 30, :estado) RETURNING id""")
				.param("codigo", codigo).param("nombre", "Aula " + codigo).param("estado", estado)
				.query(UUID.class).single();
	}

	protected UUID docente() {
		return jdbc.sql("SELECT id FROM seguridad.usuario WHERE nombre_usuario = :u").param("u", DOCENTE)
				.query(UUID.class).single();
	}

	protected UUID seccion(String grado, String nombre, UUID ambienteId, UUID tutorId) {
		return jdbc.sql("""
				INSERT INTO configuracion.seccion
				    (anio_escolar_id, grado_codigo, nombre, capacidad_maxima, ambiente_id, tutor_id)
				VALUES (:anio, :grado, :nombre, 30, :ambiente, :tutor) RETURNING id""")
				.param("anio", ANIO_2027).param("grado", grado).param("nombre", nombre).param("ambiente", ambienteId)
				.param("tutor", tutorId).query(UUID.class).single();
	}

	/** Un curso planificado en 1.° de secundaria de 2027. */
	protected UUID plan(String curso, int horas) {
		return plan("SEC-1", curso, horas);
	}

	protected UUID plan(String grado, String curso, int horas) {
		return jdbc.sql("""
				INSERT INTO academico.plan_curso (anio_escolar_id, grado_codigo, curso_id, horas_semanales)
				SELECT :anio, :grado, c.id, :horas FROM academico.curso c WHERE c.nombre = :curso
				RETURNING id""")
				.param("anio", ANIO_2027).param("grado", grado).param("horas", horas).param("curso", curso)
				.query(UUID.class).single();
	}

	protected void asignarDocente(UUID planId, UUID seccionId, UUID docenteId) {
		jdbc.sql("INSERT INTO academico.docente_seccion (plan_curso_id, seccion_id, docente_id) VALUES (:p, :s, :d)")
				.param("p", planId).param("s", seccionId).param("d", docenteId).update();
	}

	protected UUID cabecera(UUID seccionId, UUID estructuraId) {
		return jdbc.sql("""
				INSERT INTO horarios.horario_seccion (seccion_id, estructura_horaria_id)
				VALUES (:s, :e) RETURNING id""").param("s", seccionId).param("e", estructuraId)
				.query(UUID.class).single();
	}

	protected void bloque(UUID cabeceraId, String dia, int numero, UUID planId, UUID ambienteId) {
		jdbc.sql("""
				INSERT INTO horarios.horario_bloque (horario_seccion_id, dia, numero_bloque, plan_curso_id, ambiente_id)
				VALUES (:h, :dia, :n, :p, :a)""")
				.param("h", cabeceraId).param("dia", dia).param("n", numero).param("p", planId)
				.param("a", ambienteId).update();
	}

	protected long cabeceras() {
		return jdbc.sql("SELECT count(*) FROM horarios.horario_seccion").query(Long.class).single();
	}

	protected long bloquesGuardados() {
		return jdbc.sql("SELECT count(*) FROM horarios.horario_bloque").query(Long.class).single();
	}

	protected ResultActions pedir(String ruta, String usuario) throws Exception {
		return mvc.perform(conToken(get(ruta), token(usuario), null));
	}

	/** PUT /horarios/secciones/{id} con la versión (null = sin horario) y las celdas. */
	protected ResultActions guardar(UUID seccionId, String usuario, Long version, Celda... celdas) throws Exception {
		return mvc.perform(conToken(put("/horarios/secciones/" + seccionId), token(usuario), cuerpo(version, celdas)));
	}

	protected static String cuerpo(Long version, Celda... celdas) {
		StringBuilder json = new StringBuilder("{\"version\":").append(version).append(",\"celdas\":[");
		for (int i = 0; i < celdas.length; i++) {
			Celda c = celdas[i];
			if (i > 0) {
				json.append(',');
			}
			json.append("{\"dia\":\"").append(c.dia()).append("\",\"numeroBloque\":").append(c.numeroBloque())
					.append(",\"planCursoId\":\"").append(c.planCursoId()).append("\",\"ambienteId\":\"")
					.append(c.ambienteId()).append("\"}");
		}
		return json.append("]}").toString();
	}
}
