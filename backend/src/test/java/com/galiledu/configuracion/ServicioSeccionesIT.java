package com.galiledu.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;

import com.galiledu.PruebaIntegracion;
import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SeccionConVacantes;
import com.galiledu.configuracion.aplicacion.ServicioSecciones;
import com.galiledu.configuracion.dominio.Seccion;

/**
 * HU-75 y HU-23: el conteo de alumnos que ocupan vacante (el @Formula de {@link Seccion}) y lo que el servicio ofrece
 * a la configuración de matrícula, sin pasar por HTTP.
 */
class ServicioSeccionesIT extends PruebaIntegracion {
	private static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");
	private static final UUID ANIO_2027 = UUID.fromString("30000000-0000-0000-0000-000000002027");
	private static final UUID SOFIA_MATRICULA = UUID.fromString("50000000-0000-0000-0000-000000000001");
	private static final UUID MATEO_MATRICULA_2027 = UUID.fromString("50000000-0000-0000-0000-000000000002");

	@Autowired
	private ServicioSecciones servicio;
	@Autowired
	private EntityManager entityManager;
	@Autowired
	private JdbcClient jdbc;

	private Seccion seccion(UUID anioId, String gradoCodigo, String nombre) {
		return entityManager.createQuery("""
				select s from Seccion s
				where s.anioEscolarId = :anioId and s.gradoCodigo = :gradoCodigo and s.nombre = :nombre""",
				Seccion.class).setParameter("anioId", anioId).setParameter("gradoCodigo", gradoCodigo)
				.setParameter("nombre", nombre).getSingleResult();
	}

	private void cambiarEstadoDeMatricula(UUID matriculaId, String estado) {
		jdbc.sql("""
				UPDATE matricula.matricula
				SET estado = :estado, fecha_retiro = current_date, codigo = coalesce(codigo, 'MAT-2027-9001')
				WHERE id = :matriculaId""")
				.param("estado", estado).param("matriculaId", matriculaId).update();
		entityManager.clear();
	}

	// ---------- Alumnos que ocupan vacante (@Formula) ----------

	@Test
	void elConteoIncluyeLaMatriculaDeSofia() {
		assertThat(seccion(ANIO_2026, "SEC-4", "A").getAlumnosOcupanVacante()).isEqualTo(1);
		assertThat(seccion(ANIO_2026, "SEC-4", "B").getAlumnosOcupanVacante()).isZero();
	}

	@ParameterizedTest
	@ValueSource(strings = {"RETIRADO", "ANULADA"})
	void unaMatriculaRetiradaOAnuladaNoOcupaVacante(String estado) {
		cambiarEstadoDeMatricula(SOFIA_MATRICULA, estado);

		assertThat(seccion(ANIO_2026, "SEC-4", "A").getAlumnosOcupanVacante()).isZero();
	}

	@ParameterizedTest
	@ValueSource(strings = {"SECCION_RESERVADA", "EN_VALIDACION", "PENDIENTE_PAGO"})
	void unaMatriculaEnProcesoOcupaVacanteDesdeQueSeReservaLaSeccion(String estado) {
		cambiarEstadoDeMatricula(MATEO_MATRICULA_2027, estado);

		assertThat(seccion(ANIO_2027, "SEC-1", "A").getAlumnosOcupanVacante()).isEqualTo(1);
	}

	// ---------- listarSeccionesActivasConVacantes (HU-23) ----------

	@Test
	void listaLasSeccionesActivasPorGradoYNombreConSuTutorCapacidadYAlumnos() {
		List<SeccionConVacantes> secciones = servicio.listarSeccionesActivasConVacantes(ANIO_2026);

		assertThat(secciones).hasSize(10);
		SeccionConVacantes primeroA = secciones.get(0);
		assertThat(primeroA.gradoCodigo()).isEqualTo("SEC-1");
		assertThat(primeroA.gradoNombre()).isEqualTo("1.° de secundaria");
		assertThat(primeroA.nombre()).isEqualTo("A");
		assertThat(primeroA.tutor()).isEqualTo("María Fernández Ríos");
		assertThat(primeroA.capacidadMaxima()).isEqualTo(30);
		assertThat(primeroA.alumnosOcupanVacante()).isZero();
		assertThat(secciones.get(1).nombre()).isEqualTo("B");
		assertThat(secciones.get(1).tutor()).isNull();
		// 4.° A es la séptima (SEC-1 A y B, SEC-2 A y B, SEC-3 A y B, SEC-4 A): índice 6.
		assertThat(secciones.get(6).gradoCodigo()).isEqualTo("SEC-4");
		assertThat(secciones.get(6).alumnosOcupanVacante()).isEqualTo(1);
		assertThat(secciones).extracting(SeccionConVacantes::gradoCodigo)
				.isSortedAccordingTo((a, b) -> a.compareTo(b));
	}

	@Test
	void noIncluyeLasSeccionesInactivas() {
		UUID segundoB = seccion(ANIO_2026, "SEC-2", "B").getId();

		servicio.inactivar(segundoB);

		List<SeccionConVacantes> secciones = servicio.listarSeccionesActivasConVacantes(ANIO_2026);
		assertThat(secciones).hasSize(9);
		assertThat(secciones).extracting(SeccionConVacantes::id).doesNotContain(segundoB);
	}

	@Test
	void unaMatriculaReservadaCuentaComoAlumnoQueOcupaVacante() {
		List<SeccionConVacantes> secciones = servicio.listarSeccionesActivasConVacantes(ANIO_2027);

		assertThat(secciones).hasSize(10);
		assertThat(secciones.get(0).gradoCodigo()).isEqualTo("SEC-1");
		assertThat(secciones.get(0).alumnosOcupanVacante()).isEqualTo(1); // La reserva de Mateo.
		assertThat(secciones.get(0).tutor()).isNull();
	}

	@Test
	void listarSeccionesDeUnAnioQueNoExisteDa404() {
		assertThatThrownBy(() -> servicio.listarSeccionesActivasConVacantes(UUID.randomUUID()))
				.isInstanceOfSatisfying(ExcepcionNegocio.class, e -> {
					assertThat(e.estado()).isEqualTo(HttpStatus.NOT_FOUND);
					assertThat(e).hasMessage("El año escolar no existe.");
				});
	}
}
