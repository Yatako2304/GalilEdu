package com.galiledu.pagos.infraestructura.persistencia;

import java.math.BigDecimal;

import com.galiledu.pagos.aplicacion.VerificarMontoConfirmado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultaOrdenPagoPostgresTests {
	private JdbcTemplate jdbc;
	private ConsultaOrdenPagoPostgres ordenes;

	@BeforeEach
	void preparar() {
		var datos = new DriverManagerDataSource(
			"jdbc:h2:mem:pagos_adapter;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
		datos.setDriverClassName("org.h2.Driver");
		jdbc = new JdbcTemplate(datos);
		jdbc.execute("CREATE SCHEMA IF NOT EXISTS pagos");
		jdbc.execute("DROP TABLE IF EXISTS pagos.orden_pago");
		jdbc.execute("CREATE TABLE pagos.orden_pago (codigo_orden VARCHAR(40) PRIMARY KEY, monto_total NUMERIC(10,2) NOT NULL)");
		ordenes = new ConsultaOrdenPagoPostgres(jdbc);
	}

	@Test
	void leeMontoRealDeLaOrdenPorCodigo() {
		jdbc.update("INSERT INTO pagos.orden_pago (codigo_orden, monto_total) VALUES (?, ?)",
			"OP-8213", new BigDecimal("850.00"));

		var resultado = new VerificarMontoConfirmado(ordenes)
			.ejecutar("OP-8213", new BigDecimal("850.0")).orElseThrow();

		assertThat(resultado.coincidente()).isTrue();
		assertThat(resultado.montoOrden()).isEqualByComparingTo("850.00");
	}

	@Test
	void unaOrdenDesconocidaNoSeInventa() {
		assertThat(ordenes.buscarPorIdentificador("no-existe")).isEmpty();
	}
}
