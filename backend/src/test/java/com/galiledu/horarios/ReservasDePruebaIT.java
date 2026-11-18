package com.galiledu.horarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.galiledu.horarios.aplicacion.ResponsablesElegibles;

/**
 * HU-48: las reservas de los datos de prueba (R__datos_prueba.sql) respetan las reglas 1 a 8 entre sí. Así quien las
 * edite no deja datos que el propio servicio rechazaría. Los datos de prueba no traen horarios semanales, así que las
 * reglas 5 y 7a no tienen con qué chocar.
 */
class ReservasDePruebaIT extends PruebaReservas {
	@Autowired
	private ResponsablesElegibles responsables;

	private long contar(String sql) {
		return jdbc.sql(sql).query(Long.class).single();
	}

	/** Las seis del bloque de datos de prueba, por su prefijo de id. */
	private static final String SEMBRADAS = "id::text LIKE '71000000-%'";

	@Test
	void hayTresFuturasDosPasadasYUnaCancelada() {
		assertThat(contar("SELECT count(*) FROM horarios.reserva_ambiente WHERE " + SEMBRADAS)).isEqualTo(6);
		assertThat(contar("SELECT count(*) FROM horarios.reserva_ambiente WHERE " + SEMBRADAS
				+ " AND estado = 'ACTIVA' AND fecha BETWEEN '2026-11-19' AND '2026-12-11'")).isEqualTo(3);
		assertThat(contar("SELECT count(*) FROM horarios.reserva_ambiente WHERE " + SEMBRADAS
				+ " AND estado = 'ACTIVA' AND fecha < '2026-11-18'")).isEqualTo(2);
		assertThat(contar("SELECT count(*) FROM horarios.reserva_ambiente WHERE " + SEMBRADAS
				+ " AND estado = 'CANCELADA'")).isEqualTo(1);
	}

	@Test
	void unaFuturaCaeEstaSemanaYOtraEsteMes() {
		LocalDate lunes = LocalDate.of(2026, 11, 16);

		assertThat(contar("SELECT count(*) FROM horarios.reserva_ambiente WHERE " + SEMBRADAS
				+ " AND estado = 'ACTIVA' AND fecha BETWEEN '" + lunes + "' AND '" + lunes.plusDays(6) + "'"))
				.isGreaterThanOrEqualTo(1);
		assertThat(contar("SELECT count(*) FROM horarios.reserva_ambiente WHERE " + SEMBRADAS
				+ " AND estado = 'ACTIVA' AND fecha >= '2026-11-18' AND fecha < '2026-12-01'"))
				.isGreaterThanOrEqualTo(1);
	}

	@Test
	void regla1ElAmbienteEsUnEspacioEspecialActivo() {
		assertThat(contar("""
				SELECT count(*) FROM horarios.reserva_ambiente r
				JOIN configuracion.ambiente a ON a.id = r.ambiente_id
				WHERE r.id::text LIKE '71000000-%' AND (a.tipo <> 'ESPACIO_ESPECIAL' OR a.estado <> 'ACTIVO')"""))
				.isZero();
	}

	@Test
	void regla2LasSeccionesSonDel2026YCabenEnElAforo() {
		assertThat(contar("""
				SELECT count(*) FROM horarios.reserva_ambiente r
				JOIN configuracion.seccion s ON s.id = r.seccion_id
				WHERE r.id::text LIKE '71000000-%'
				  AND s.anio_escolar_id <> '30000000-0000-0000-0000-000000002026'""")).isZero();
		assertThat(
				contar("""
						SELECT count(*) FROM horarios.reserva_ambiente r
						JOIN configuracion.ambiente a ON a.id = r.ambiente_id
						WHERE r.id::text LIKE '71000000-%'
						  AND (SELECT count(*) FROM matricula.matricula m
						       WHERE m.seccion_id = r.seccion_id AND m.estado NOT IN ('RETIRADO', 'ANULADA')) > a.aforo_maximo"""))
				.isZero();
	}

	@Test
	void regla3LaFechaEsLectivaDentroDeUnPeriodoYLaJornadaDeLaSeccion() {
		// Fecha: día lectivo de la estructura principal del nivel de la sección y dentro de algún periodo del año.
		assertThat(contar("""
				SELECT count(*) FROM horarios.reserva_ambiente r
				JOIN configuracion.seccion s ON s.id = r.seccion_id
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				JOIN configuracion.estructura_horaria eh ON eh.anio_escolar_id = s.anio_escolar_id
				                                        AND eh.nivel = g.nivel AND eh.es_principal
				WHERE r.id::text LIKE '71000000-%'
				  AND NOT ((ARRAY['LUN','MAR','MIE','JUE','VIE','SAB'])[extract(isodow FROM r.fecha)::int]
				           = ANY (eh.dias_lectivos))""")).isZero();
		assertThat(contar("""
				SELECT count(*) FROM horarios.reserva_ambiente r
				JOIN configuracion.seccion s ON s.id = r.seccion_id
				WHERE r.id::text LIKE '71000000-%'
				  AND NOT EXISTS (SELECT 1 FROM configuracion.periodo_academico p
				                  WHERE p.anio_escolar_id = s.anio_escolar_id
				                    AND r.fecha BETWEEN p.fecha_inicio AND p.fecha_fin)""")).isZero();
		// Horas: entre la entrada y el fin de la jornada (bloques más recesos).
		assertThat(
				contar("""
						SELECT count(*) FROM horarios.reserva_ambiente r
						JOIN configuracion.seccion s ON s.id = r.seccion_id
						JOIN configuracion.grado g ON g.codigo = s.grado_codigo
						JOIN configuracion.estructura_horaria eh ON eh.anio_escolar_id = s.anio_escolar_id
						                                        AND eh.nivel = g.nivel AND eh.es_principal
						WHERE r.id::text LIKE '71000000-%'
						  AND (r.hora_inicio < eh.hora_entrada
						       OR r.hora_fin > eh.hora_entrada + make_interval(mins => eh.bloques_por_dia * eh.duracion_bloque_min
						           + coalesce((SELECT sum(rc.duracion_min) FROM configuracion.receso rc
						                       WHERE rc.estructura_horaria_id = eh.id
						                         AND rc.despues_de_bloque < eh.bloques_por_dia), 0)::int))"""))
				.isZero();
	}

	@Test
	void reglas4_7by8SinSuperposicionesEntreLasActivasPorAmbienteResponsableYSeccion() {
		for (String clave : List.of("ambiente_id", "responsable_id", "seccion_id")) {
			assertThat(contar("""
					SELECT count(*) FROM horarios.reserva_ambiente a
					JOIN horarios.reserva_ambiente b ON a.id < b.id AND a.%s = b.%s AND a.fecha = b.fecha
					     AND a.hora_inicio < b.hora_fin AND b.hora_inicio < a.hora_fin
					WHERE a.estado = 'ACTIVA' AND b.estado = 'ACTIVA'""".formatted(clave, clave))).as(clave).isZero();
		}
	}

	@Test
	void elResponsableEsMariaYElCoordinadorQuienLasRegistro() {
		UUID maria = usuarioPorNombre(DOCENTE);

		assertThat(responsables.existe(maria)).isTrue();
		assertThat(contar("SELECT count(*) FROM horarios.reserva_ambiente WHERE " + SEMBRADAS
				+ " AND responsable_id <> '" + maria + "'")).isZero();
		assertThat(contar("SELECT count(*) FROM horarios.reserva_ambiente WHERE " + SEMBRADAS
				+ " AND creado_por <> '" + coordinador() + "'")).isZero();
	}
}
