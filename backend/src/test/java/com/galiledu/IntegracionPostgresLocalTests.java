package com.galiledu;

import com.galiledu.pagos.aplicacion.puertos.ConsultaOrdenPago;
import com.galiledu.usuarios.aplicacion.puertos.ConsultaUsuarioRegistrado;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** Se ejecuta solo a petición y nunca modifica la BD local. */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GALILEDU_POSTGRES_TEST", matches = "true")
class IntegracionPostgresLocalTests {
	@Autowired
	private JdbcOperations jdbc;

	@Autowired
	private ConsultaUsuarioRegistrado usuarios;

	@Autowired
	private ConsultaOrdenPago ordenes;

	@DynamicPropertySource
	static void postgres(DynamicPropertyRegistry propiedades) {
		propiedades.add("spring.datasource.url", () -> System.getenv("DB_URL"));
		propiedades.add("spring.datasource.username", () -> System.getenv("DB_USER"));
		propiedades.add("spring.datasource.password", () -> System.getenv("DB_PASSWORD"));
		propiedades.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		propiedades.add("spring.jpa.hibernate.ddl-auto", () -> "none");
	}

	@Test
	void esquemaYConsultasRealesRespondenSinCambiarDatos() {
		assertThat(jdbc.queryForObject("SELECT current_database()", String.class)).isNotBlank();
		assertThat(jdbc.queryForObject("SELECT to_regclass('seguridad.usuario') IS NOT NULL", Boolean.class)).isTrue();
		assertThat(jdbc.queryForObject("SELECT to_regclass('pagos.orden_pago') IS NOT NULL", Boolean.class)).isTrue();
		assertThat(usuarios.buscarPorCorreo("consulta-inexistente@galiledu.invalid")).isEmpty();
		assertThat(ordenes.buscarPorIdentificador("orden-inexistente-integracion")).isEmpty();
	}
}
