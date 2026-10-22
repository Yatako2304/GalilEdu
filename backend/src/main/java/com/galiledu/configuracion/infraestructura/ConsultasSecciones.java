package com.galiledu.configuracion.infraestructura;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.galiledu.configuracion.dominio.Grado;

/** Lecturas de solo consulta del catálogo de grados (configuracion.grado, V4), que no tiene entidad. */
@Repository
public class ConsultasSecciones {
	private final JdbcClient jdbc;

	public ConsultasSecciones(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Los 11 grados en orden: 1.° a 6.° de primaria y 1.° a 5.° de secundaria. */
	public List<Grado> grados() {
		return jdbc.sql("SELECT codigo, nivel, numero, nombre, orden FROM configuracion.grado ORDER BY orden")
				.query(Grado.class).list();
	}
}
