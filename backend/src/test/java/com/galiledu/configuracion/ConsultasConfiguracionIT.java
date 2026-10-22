package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.infraestructura.AnioEscolarRepositorio;
import com.galiledu.configuracion.infraestructura.ConsultasConfiguracion;
import com.galiledu.configuracion.infraestructura.ConsultasConfiguracion.ConteosAnio;

/** HU-74: consultas de solo lectura de la configuración, con los datos de prueba (2026 ACTIVO, 2027 PLANIFICADO). */
class ConsultasConfiguracionIT extends PruebaIntegracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID ANIO_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");

	@Autowired
	private ConsultasConfiguracion consultas;
	@Autowired
	private AnioEscolarRepositorio anios;
	@Autowired
	private JdbcClient jdbc;

	@Test
	void elListadoTraeLasSeccionesActivasYLasEstructurasDeCadaAnio() {
		var listado = consultas.anios();

		assertThat(listado).extracting(a -> a.anio()).containsExactly(2027, 2026);
		assertThat(listado.get(0).seccionesActivas()).isEqualTo(10);
		assertThat(listado.get(0).estructurasHorarias()).isZero();
		assertThat(listado.get(1).seccionesActivas()).isEqualTo(10);
		assertThat(listado.get(1).estructurasHorarias()).isEqualTo(2);
		assertThat(listado.get(1).cantidadPeriodos()).isEqualTo(4);
	}

	@Test
	void losConteosDelAnioActivoSalenDeLosDatosDePrueba() {
		ConteosAnio c = consultas.conteos(ANIO_2026);

		assertThat(c.periodos()).isEqualTo(4);
		assertThat(c.seccionesActivas()).isEqualTo(10);
		assertThat(c.estructurasHorarias()).isEqualTo(2);
		assertThat(c.seccionesSinTutor()).isEqualTo(9); // solo 1.° A tiene tutora
		assertThat(c.gradosConSecciones()).isEqualTo(5);
		assertThat(c.gradosConSeccionesConPlan()).isEqualTo(1); // el plan de evaluación solo existe para SEC-1
	}

	@Test
	void losConteosDelAnioPlanificadoNoTienenEstructurasNiPlan() {
		ConteosAnio c = consultas.conteos(ANIO_2027);

		assertThat(c.periodos()).isEqualTo(3);
		assertThat(c.seccionesActivas()).isEqualTo(10);
		assertThat(c.estructurasHorarias()).isZero();
		assertThat(c.seccionesSinTutor()).isEqualTo(10);
		assertThat(c.gradosConSecciones()).isEqualTo(5);
		assertThat(c.gradosConSeccionesConPlan()).isZero();
	}

	@Test
	void unAnioQueNoExisteTieneTodosLosConteosEnCero() {
		ConteosAnio c = consultas.conteos(UUID.randomUUID());

		assertThat(c).isEqualTo(new ConteosAnio(0, 0, 0, 0, 0, 0));
	}

	@Test
	void lasSeccionesInactivasNoCuentanNiComoSeccionNiComoGrado() {
		jdbc.sql(
				"UPDATE configuracion.seccion SET estado = 'INACTIVA' WHERE anio_escolar_id = :anio AND grado_codigo = 'SEC-5'")
				.param("anio", ANIO_2026).update();

		ConteosAnio c = consultas.conteos(ANIO_2026);

		assertThat(c.seccionesActivas()).isEqualTo(8);
		assertThat(c.gradosConSecciones()).isEqualTo(4);
		assertThat(c.seccionesSinTutor()).isEqualTo(7);
	}

	@Test
	void asignarUnTutorDescuentaLasSeccionesSinTutor() {
		jdbc.sql("""
				UPDATE configuracion.seccion SET tutor_id = '10000000-0000-0000-0000-000000000004'
				WHERE anio_escolar_id = :anio AND grado_codigo = 'SEC-1' AND nombre = 'B'""")
				.param("anio", ANIO_2026).update();

		assertThat(consultas.conteos(ANIO_2026).seccionesSinTutor()).isEqualTo(8);
	}

	@Test
	void losCodigosDeLasEstructurasVanEnOrden() {
		assertThat(consultas.codigosEstructuras(ANIO_2026)).containsExactly("EH-PRI-26", "EH-SEC-26");
		assertThat(consultas.codigosEstructuras(ANIO_2027)).isEmpty();
	}

	@Test
	void losAlumnosMatriculadosSalenDeLaMatriculaDelAnio() {
		assertThat(consultas.alumnosMatriculados(ANIO_2026)).isEqualTo(2);
		assertThat(consultas.alumnosMatriculados(ANIO_2027)).isZero(); // solo hay una sección reservada
		assertThat(consultas.alumnosMatriculados(UUID.randomUUID())).isZero();
	}

	@Test
	void losAlumnosRetiradosOAnuladosNoCuentanComoMatriculados() {
		jdbc.sql("""
				UPDATE matricula.matricula SET estado = 'RETIRADO', fecha_retiro = '2026-09-01'
				WHERE id = '50000000-0000-0000-0000-000000000001'""").update();
		assertThat(consultas.alumnosMatriculados(ANIO_2026)).isEqualTo(1);

		jdbc.sql("UPDATE matricula.matricula SET estado = 'ANULADA' WHERE id = '50000000-0000-0000-0000-000000000003'")
				.update();
		assertThat(consultas.alumnosMatriculados(ANIO_2026)).isZero();
	}

	@Test
	void elRepositorioEncuentraAnioPorEstado() {
		assertThat(anios.existsByEstado(EstadoAnioEscolar.ACTIVO)).isTrue();
		assertThat(anios.existsByEstado(EstadoAnioEscolar.CERRADO)).isFalse();
		assertThat(anios.findByEstado(EstadoAnioEscolar.ACTIVO)).extracting(a -> a.getAnio()).containsExactly(2026);
		assertThat(anios.findByEstado(EstadoAnioEscolar.PLANIFICADO)).extracting(a -> a.getAnio())
				.containsExactly(2027);
		assertThat(anios.findByEstado(EstadoAnioEscolar.CERRADO)).isEmpty();
	}
}
