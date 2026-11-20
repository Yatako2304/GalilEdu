package com.galiledu.usuarios.infraestructura;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** HU-2: interruptor del inicio de sesión con Google (seguridad.configuracion_acceso, una sola fila). */
@Component
public class ConfiguracionAcceso {
	private final JdbcClient jdbc;

	public ConfiguracionAcceso(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public boolean googleHabilitado() {
		return jdbc.sql("SELECT google_habilitado FROM seguridad.configuracion_acceso WHERE id = 1")
				.query(Boolean.class).optional().orElse(false);
	}

	public void cambiarGoogle(boolean habilitado, UUID administradorId) {
		jdbc.sql("""
				UPDATE seguridad.configuracion_acceso
				SET google_habilitado = :h, actualizado_en = now(), actualizado_por = :u
				WHERE id = 1""")
				.param("h", habilitado).param("u", administradorId).update();
	}
}
