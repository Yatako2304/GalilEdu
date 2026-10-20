package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;

/**
 * Tablas de la configuración del año escolar (V4). Verifica que las reglas que guarda la propia BD
 * se cumplan, para que el servicio pueda confiar en ellas.
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
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (anio, fecha_inicio, fecha_fin, regimen, estado)
				VALUES (2030, '2030-03-01', '2030-12-15', 'BIMESTRAL', 'ACTIVO')""").update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void noSeRepiteElNombreDeUnaSeccionEnElMismoGrado() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima)
				VALUES (?::uuid, 'SEC-1', 'A', 30)""").param(ANIO_2026).update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void laCapacidadDeUnaSeccionVaDe1A45() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO configuracion.seccion (anio_escolar_id, grado_codigo, nombre, capacidad_maxima)
				VALUES (?::uuid, 'SEC-1', 'Z', 46)""").param(ANIO_2026).update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void dosCuotasDelTarifarioNoPuedenVencerElMismoDia() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO pagos.cuota_tarifario (tarifario_id, numero, nombre, fecha_vencimiento)
				VALUES (?::uuid, 11, 'Cuota repetida', '2027-03-31')""").param(TARIFARIO_2027).update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void losDiasLectivosSoloAceptanLunesASabado() {
		assertThatThrownBy(() -> jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria (anio_escolar_id, codigo, dias_lectivos, hora_entrada,
				                                              bloques_por_dia, duracion_bloque_min)
				VALUES (?::uuid, 'EH-DOM', ARRAY['DOM'], '08:00', 4, 45)""").param(ANIO_2026).update())
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	private int contar(String sql, Object... parametros) {
		var consulta = jdbc.sql(sql);
		for (Object p : parametros) {
			consulta = consulta.param(p);
		}
		return consulta.query(Integer.class).single();
	}
}
