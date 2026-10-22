package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.configuracion.aplicacion.MatriculasDeSecciones;

/**
 * La lectura de matrícula que decide si una sección se puede eliminar: cualquier matrícula, en cualquier estado, o un
 * cambio de sección que la tenga de origen o de destino. Se prueba directo, sin pasar por el borrado: si la consulta
 * fallara, la restricción de la base de datos igual rechazaría el borrado y ocultaría el error.
 */
class MatriculasDeSeccionesSqlIT extends PruebaIntegracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID SOFIA_MATRICULA = UUID.fromString("50000000-0000-0000-0000-000000000001");

	@Autowired
	private MatriculasDeSecciones matriculasDeSecciones;
	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;

	private UUID seccionId(String gradoCodigo, String nombre) {
		return jdbc.sql("""
				SELECT id FROM configuracion.seccion
				WHERE anio_escolar_id = :anioId AND grado_codigo = :gradoCodigo AND nombre = :nombre""")
				.param("anioId", ANIO_2026).param("gradoCodigo", gradoCodigo).param("nombre", nombre)
				.query(UUID.class).single();
	}

	private void registrarCambioDeSeccion(UUID origenId, UUID destinoId) {
		jdbc.sql("""
				INSERT INTO matricula.cambio_seccion (matricula_id, seccion_origen_id, seccion_destino_id)
				VALUES (:matriculaId, :origenId, :destinoId)""")
				.param("matriculaId", SOFIA_MATRICULA).param("origenId", origenId).param("destinoId", destinoId)
				.update();
	}

	@Test
	void unaSeccionSinMatriculasNiCambiosNoTieneNada() {
		assertThat(matriculasDeSecciones.tieneMatriculasOCambios(seccionId("SEC-2", "B"))).isFalse();
	}

	@Test
	void unaSeccionConUnaMatriculaMatriculadaTieneMatriculas() {
		assertThat(matriculasDeSecciones.tieneMatriculasOCambios(seccionId("SEC-4", "A"))).isTrue();
	}

	@ParameterizedTest
	@ValueSource(strings = {"RETIRADO", "ANULADA"})
	void unaMatriculaRetiradaOAnuladaSigueContandoParaElBorrado(String estado) {
		jdbc.sql("""
				UPDATE matricula.matricula SET estado = :estado, fecha_retiro = current_date
				WHERE id = :matriculaId""")
				.param("estado", estado).param("matriculaId", SOFIA_MATRICULA).update();
		entityManager.clear();

		assertThat(matriculasDeSecciones.tieneMatriculasOCambios(seccionId("SEC-4", "A"))).isTrue();
	}

	@Test
	void unCambioDeSeccionCuentaParaLaSeccionDeOrigen() {
		UUID origen = seccionId("SEC-2", "B");
		registrarCambioDeSeccion(origen, seccionId("SEC-3", "B"));

		assertThat(matriculasDeSecciones.tieneMatriculasOCambios(origen)).isTrue();
	}

	@Test
	void unCambioDeSeccionCuentaParaLaSeccionDeDestino() {
		UUID destino = seccionId("SEC-3", "B");
		registrarCambioDeSeccion(seccionId("SEC-2", "B"), destino);

		assertThat(matriculasDeSecciones.tieneMatriculasOCambios(destino)).isTrue();
	}

	@Test
	void unaSeccionQueNoParticipaEnElCambioNoSeVeAfectada() {
		registrarCambioDeSeccion(seccionId("SEC-2", "B"), seccionId("SEC-3", "B"));

		assertThat(matriculasDeSecciones.tieneMatriculasOCambios(seccionId("SEC-1", "B"))).isFalse();
	}
}
