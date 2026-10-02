package com.galiledu.pagos.infraestructura.persistencia;

import java.util.Optional;

import com.galiledu.pagos.aplicacion.puertos.ConsultaOrdenPago;
import com.galiledu.pagos.dominio.OrdenPago;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

/** Lectura de órdenes existentes; no confirma ni modifica pagos. */
@Repository
public class ConsultaOrdenPagoPostgres implements ConsultaOrdenPago {
	private static final String BUSCAR_POR_CODIGO = """
		SELECT codigo_orden, monto_total
		FROM pagos.orden_pago
		WHERE codigo_orden = ?
		""";

	private final JdbcOperations jdbc;

	public ConsultaOrdenPagoPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<OrdenPago> buscarPorIdentificador(String identificador) {
		if (identificador == null || identificador.isBlank()) {
			return Optional.empty();
		}
		return jdbc.query(BUSCAR_POR_CODIGO, (resultado, fila) ->
			new OrdenPago(resultado.getString("codigo_orden"),
				resultado.getBigDecimal("monto_total")), identificador)
			.stream().findFirst();
	}
}
