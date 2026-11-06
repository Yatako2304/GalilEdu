package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.BloqueSolicitado;
import com.galiledu.horarios.aplicacion.CeldaHorario;
import com.galiledu.horarios.aplicacion.ConversorDeBloques;

/**
 * HU-45: el conversor pasa las celdas a horas reales con la jornada de la estructura de la sección y toma el docente
 * de academico.docente_seccion. Estructura de 6 bloques de 45 min desde las 08:00 con un receso de 20 min después del
 * bloque 3, en 2027 (año PLANIFICADO, sin estructuras ni secciones).
 */
class ConversorDeBloquesIT extends PruebaIntegracion {
	private static final UUID ANIO_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");

	@Autowired
	private ConversorDeBloques conversor;
	@Autowired
	private JdbcClient jdbc;

	private UUID plan(String curso, int horas) {
		return jdbc.sql("""
				INSERT INTO academico.plan_curso (anio_escolar_id, grado_codigo, curso_id, horas_semanales)
				SELECT :anio, 'SEC-1', c.id, :horas FROM academico.curso c WHERE c.nombre = :curso
				RETURNING id""")
				.param("anio", ANIO_2027).param("horas", horas).param("curso", curso).query(UUID.class).single();
	}

	@Test
	void calculaLasHorasRealesYOmiteLaCeldaSinDocente() {
		UUID estructura = jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria
				    (anio_escolar_id, codigo, dias_lectivos, hora_entrada, bloques_por_dia, duracion_bloque_min, nivel,
				     es_principal)
				VALUES (:anio, 'EH-T-27', ARRAY['LUN','MAR','MIE','JUE','VIE']::varchar[], TIME '08:00', 6, 45,
				        'SECUNDARIA', true)
				RETURNING id""").param("anio", ANIO_2027).query(UUID.class).single();
		jdbc.sql("""
				INSERT INTO configuracion.receso (estructura_horaria_id, despues_de_bloque, duracion_min)
				VALUES (:id, 3, 20)""").param("id", estructura).update();
		UUID ambiente = jdbc.sql("""
				INSERT INTO configuracion.ambiente (codigo, nombre, tipo, aforo_maximo)
				VALUES ('TMP-A1', 'Aula TMP-A1', 'AULA_REGULAR', 30) RETURNING id""").query(UUID.class).single();
		UUID docente = jdbc.sql("SELECT id FROM seguridad.usuario WHERE nombre_usuario = :u").param("u", DOCENTE)
				.query(UUID.class).single();
		UUID seccion = jdbc.sql("""
				INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima)
				VALUES (:anio, 'SEC-1', 'T1', 30) RETURNING id""").param("anio", ANIO_2027).query(UUID.class)
				.single();
		UUID algebra = plan("Álgebra", 5);
		UUID geometria = plan("Geometría", 3);
		jdbc.sql("INSERT INTO academico.docente_seccion (plan_curso_id, seccion_id, docente_id) VALUES (:p, :s, :d)")
				.param("p", algebra).param("s", seccion).param("d", docente).update();

		List<BloqueSolicitado> solicitados = conversor.aBloquesSolicitados(seccion,
				List.of(new CeldaHorario(DiaSemana.LUN, 1, algebra, ambiente),
						// Después del receso: el bloque 4 empieza a las 10:35, no a las 10:15.
						new CeldaHorario(DiaSemana.MIE, 4, algebra, ambiente),
						// Sin docente en la sección: se omite.
						new CeldaHorario(DiaSemana.MAR, 2, geometria, ambiente)));

		assertThat(solicitados).containsExactly(
				new BloqueSolicitado(DiaSemana.LUN, 1, LocalTime.of(8, 0), LocalTime.of(8, 45), algebra, docente,
						ambiente),
				new BloqueSolicitado(DiaSemana.MIE, 4, LocalTime.of(10, 35), LocalTime.of(11, 20), algebra, docente,
						ambiente));
	}

	@Test
	void sinCeldasDevuelveVacio() {
		assertThat(conversor.aBloquesSolicitados(UUID.randomUUID(), List.of())).isEmpty();
	}
}
