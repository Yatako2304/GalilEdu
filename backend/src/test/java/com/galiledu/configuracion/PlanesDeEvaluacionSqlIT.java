package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.configuracion.aplicacion.PlanesDeEvaluacion;
import com.galiledu.configuracion.aplicacion.PlanesDeEvaluacion.GradoExcedido;

/**
 * La lectura de planes de evaluación que decide si se puede reducir o eliminar una estructura horaria. Se prueba
 * directo, con los planes armados por SQL en 2027 (año PLANIFICADO, sin planes). En 2026 solo se usa el plan de
 * 1.° de secundaria de los datos de prueba: 16 cursos, 35 horas por semana.
 */
class PlanesDeEvaluacionSqlIT extends PruebaIntegracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID ANIO_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");

	@Autowired
	private PlanesDeEvaluacion planes;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;

	private UUID cursoId(String nombre) {
		return jdbc.sql("SELECT id FROM academico.curso WHERE nombre = :nombre").param("nombre", nombre)
				.query(UUID.class).single();
	}

	/** Un curso planificado en el grado y año dados. El curso debe estar en curso_grado del grado. */
	private void planificar(UUID anioId, String gradoCodigo, String curso, int horas) {
		jdbc.sql("""
				INSERT INTO academico.plan_curso (anio_escolar_id, grado_codigo, curso_id, horas_semanales)
				VALUES (:anioId, :gradoCodigo, :cursoId, :horas)""")
				.param("anioId", anioId).param("gradoCodigo", gradoCodigo).param("cursoId", cursoId(curso))
				.param("horas", horas).update();
		entityManager.clear();
	}

	/** Los datos de prueba solo dictan cursos en secundaria: para planificar primaria hay que vincular el curso. */
	private void dictarEnGrado(String curso, String gradoCodigo) {
		jdbc.sql("INSERT INTO academico.curso_grado (curso_id, grado_codigo) VALUES (:cursoId, :gradoCodigo)")
				.param("cursoId", cursoId(curso)).param("gradoCodigo", gradoCodigo).update();
	}

	@Test
	void devuelveElGradoQueSePasaConSuNombreYSusHoras() {
		planificar(ANIO_2027, "SEC-1", "Álgebra", 10);
		planificar(ANIO_2027, "SEC-1", "Geometría", 10);
		planificar(ANIO_2027, "SEC-1", "Aritmética", 8);

		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 27))
				.contains(new GradoExcedido("1.° de secundaria", 28));
	}

	@Test
	void unGradoConTantasHorasComoBloquesNoSePasa() {
		planificar(ANIO_2027, "SEC-1", "Álgebra", 10);
		planificar(ANIO_2027, "SEC-1", "Geometría", 10);
		planificar(ANIO_2027, "SEC-1", "Aritmética", 8);

		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 28)).isEmpty();
		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 40)).isEmpty();
	}

	@Test
	void conVariosGradosPasadosEligeElDeMasHoras() {
		planificar(ANIO_2027, "SEC-1", "Álgebra", 10);
		planificar(ANIO_2027, "SEC-1", "Geometría", 10);
		planificar(ANIO_2027, "SEC-1", "Aritmética", 5);
		planificar(ANIO_2027, "SEC-3", "Álgebra", 15);
		planificar(ANIO_2027, "SEC-3", "Geometría", 15);
		planificar(ANIO_2027, "SEC-3", "Aritmética", 5);

		// Con 20 bloques se pasan SEC-1 (25 horas) y SEC-3 (35 horas): menciona el de más horas, no el primero.
		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 20))
				.contains(new GradoExcedido("3.° de secundaria", 35));
		// Con 30 bloques solo se pasa SEC-3.
		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 30))
				.contains(new GradoExcedido("3.° de secundaria", 35));
	}

	@Test
	void conLasMismasHorasEligeElGradoDeMenorOrden() {
		planificar(ANIO_2027, "SEC-4", "Álgebra", 12);
		planificar(ANIO_2027, "SEC-2", "Álgebra", 12);

		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 10))
				.contains(new GradoExcedido("2.° de secundaria", 12));
	}

	@Test
	void sumaLasHorasDeTodosLosCursosDelGradoAntesDeCompararlas() {
		// Ningún curso pasa de los bloques por sí solo; el total del grado sí.
		planificar(ANIO_2027, "SEC-1", "Álgebra", 6);
		planificar(ANIO_2027, "SEC-1", "Geometría", 6);

		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 11))
				.contains(new GradoExcedido("1.° de secundaria", 12));
	}

	@Test
	void unNivelSinPlanesNoTieneGradoQueExcedaNiHayPlanes() {
		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 1)).isEmpty();
		assertThat(planes.hayPlanes(ANIO_2027, "SECUNDARIA")).isFalse();
		assertThat(planes.hayPlanes(ANIO_2027, "PRIMARIA")).isFalse();
	}

	@Test
	void losPlanesDeUnAnioNoCuentanParaOtro() {
		// 2026 ya trae el plan de 1.° de secundaria con 35 horas (datos de prueba): 2027 no lo ve.
		assertThat(planes.gradoQueExcede(ANIO_2026, "SECUNDARIA", 34))
				.contains(new GradoExcedido("1.° de secundaria", 35));
		assertThat(planes.hayPlanes(ANIO_2026, "SECUNDARIA")).isTrue();
		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 1)).isEmpty();
		assertThat(planes.hayPlanes(ANIO_2027, "SECUNDARIA")).isFalse();

		planificar(ANIO_2027, "SEC-2", "Álgebra", 8);

		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 7))
				.contains(new GradoExcedido("2.° de secundaria", 8));
		// Y el plan de 2027 no altera el de 2026.
		assertThat(planes.gradoQueExcede(ANIO_2026, "SECUNDARIA", 34))
				.contains(new GradoExcedido("1.° de secundaria", 35));
	}

	@Test
	void losPlanesDeUnNivelNoCuentanParaElOtro() {
		dictarEnGrado("Álgebra", "PRI-1");
		planificar(ANIO_2027, "PRI-1", "Álgebra", 14);

		assertThat(planes.hayPlanes(ANIO_2027, "PRIMARIA")).isTrue();
		assertThat(planes.hayPlanes(ANIO_2027, "SECUNDARIA")).isFalse();
		assertThat(planes.gradoQueExcede(ANIO_2027, "PRIMARIA", 13))
				.contains(new GradoExcedido("1.° de primaria", 14));
		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 1)).isEmpty();

		planificar(ANIO_2027, "SEC-1", "Geometría", 9);

		assertThat(planes.hayPlanes(ANIO_2027, "SECUNDARIA")).isTrue();
		assertThat(planes.gradoQueExcede(ANIO_2027, "SECUNDARIA", 8))
				.contains(new GradoExcedido("1.° de secundaria", 9));
		assertThat(planes.gradoQueExcede(ANIO_2027, "PRIMARIA", 13))
				.contains(new GradoExcedido("1.° de primaria", 14));
	}
}
