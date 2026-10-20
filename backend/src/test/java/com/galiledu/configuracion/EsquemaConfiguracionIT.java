package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * Tablas de la configuración del año escolar (V4). Cada prueba de rechazo exige la restricción exacta que protege la
 * regla, así no pasa por accidente si el dato falla por otro motivo.
 */
class EsquemaConfiguracionIT extends PruebaIntegracion {
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String TARIFARIO_2027 = "32000000-0000-0000-0000-000000002027";

	@Autowired
	private JdbcClient jdbc;

	@Test
	void losDatosDePruebaTraenElAnioActivoConSusPeriodosYSecciones() {
		assertThat(jdbc.sql("SELECT anio FROM configuracion.anio_escolar WHERE estado = 'ACTIVO'")
				.query(Integer.class).single()).isEqualTo(2026);
		assertThat(contar("SELECT count(*) FROM configuracion.periodo_academico WHERE anio_escolar_id = ?::uuid",
				ANIO_2026)).isEqualTo(4);
		assertThat(contar("SELECT count(*) FROM configuracion.seccion WHERE anio_escolar_id = ?::uuid", ANIO_2026))
				.isEqualTo(10);
		assertThat(contar("SELECT count(*) FROM configuracion.grado")).isEqualTo(11);
		assertThat(contar("SELECT count(*) FROM pagos.cuota_tarifario WHERE tarifario_id = ?::uuid", TARIFARIO_2027))
				.isEqualTo(10);
	}

	@Test
	void soloUnAnioPuedeEstarActivo() {
		// 2030 es un año válido; solo falla porque 2026 ya está activo.
		rechazaPor("ux_anio_escolar_un_activo", () -> jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (anio, fecha_inicio, fecha_fin, regimen, estado)
				VALUES (2030, '2030-03-01', '2030-12-15', 'BIMESTRAL', 'ACTIVO')""").update());
	}

	@Test
	void noSeRepiteElNombreDeUnaSeccionEnElMismoGrado() {
		rechazaPor("uq_seccion_nombre", () -> jdbc.sql("""
				INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima)
				VALUES (?::uuid, 'SEC-1', 'A', 30)""").param(ANIO_2026).update());
	}

	@Test
	void laCapacidadDeUnaSeccionVaDe1A45() {
		rechazaPor("ck_seccion_capacidad", () -> jdbc.sql("""
				INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima)
				VALUES (?::uuid, 'SEC-1', 'Z', 46)""").param(ANIO_2026).update());
	}

	@Test
	void dosCuotasDelTarifarioNoPuedenVencerElMismoDia() {
		// La cuota 11 no existe; solo falla porque el 31/03/2027 ya lo usa la pensión de marzo.
		rechazaPor("uq_cuota_tarifario_vencimiento", () -> jdbc.sql("""
				INSERT INTO pagos.cuota_tarifario (tarifario_id, numero, nombre, fecha_vencimiento)
				VALUES (?::uuid, 11, 'Cuota repetida', '2027-03-31')""").param(TARIFARIO_2027).update());
	}

	@Test
	void losDiasLectivosSoloAceptanLunesASabado() {
		rechazaPor("ck_estructura_horaria_dias", () -> jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria (anio_escolar_id, codigo, dias_lectivos, hora_entrada,
				                                              bloques_por_dia, duracion_bloque_min, nivel, es_principal)
				VALUES (?::uuid, 'EH-DOM', ARRAY['DOM'], '08:00', 4, 45, 'SECUNDARIA', FALSE)""").param(ANIO_2026)
				.update());
	}

	private int contar(String sql, Object... parametros) {
		var consulta = jdbc.sql(sql);
		for (Object p : parametros) {
			consulta = consulta.param(p);
		}
		return consulta.query(Integer.class).single();
	}
}
