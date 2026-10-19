package com.galiledu.usuarios.infraestructura;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Reserva el siguiente correlativo del año de forma atómica (sin duplicados aunque haya concurrencia). */
@Component
public class CorrelativoUsuarios {
	private final JdbcClient jdbc;

	public CorrelativoUsuarios(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public int siguiente(int anio) {
		return jdbc.sql("""
				INSERT INTO seguridad.correlativo_usuario (anio, ultimo) VALUES (:anio, 1)
				ON CONFLICT (anio) DO UPDATE SET ultimo = seguridad.correlativo_usuario.ultimo + 1
				RETURNING ultimo
				""")
				.param("anio", anio)
				.query(Integer.class)
				.single();
	}
}
