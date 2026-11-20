package com.galiledu.usuarios.infraestructura;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** HU-4: códigos de recuperación de contraseña (seguridad.codigo_recuperacion). Solo vale el más reciente. */
@Component
public class CodigosRecuperacion {
	private final JdbcClient jdbc;

	public CodigosRecuperacion(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record Codigo(UUID id, String codigoHash, Instant creadoEn, Instant expiraEn, int intentos,
			Instant usadoEn) {
	}

	/** El código más reciente del usuario, usado o no. */
	public Optional<Codigo> ultimo(UUID usuarioId) {
		return jdbc.sql("""
				SELECT id, codigo_hash, creado_en, expira_en, intentos, usado_en
				FROM seguridad.codigo_recuperacion
				WHERE usuario_id = :u
				ORDER BY creado_en DESC
				LIMIT 1""")
				.param("u", usuarioId)
				.query((rs, i) -> new Codigo(rs.getObject("id", UUID.class), rs.getString("codigo_hash"),
						rs.getObject("creado_en", OffsetDateTime.class).toInstant(),
						rs.getObject("expira_en", OffsetDateTime.class).toInstant(), rs.getInt("intentos"),
						Optional.ofNullable(rs.getObject("usado_en", OffsetDateTime.class))
								.map(OffsetDateTime::toInstant).orElse(null)))
				.optional();
	}

	/** Guarda un código nuevo y anula los anteriores que no se usaron. */
	public void guardar(UUID usuarioId, String codigoHash, Instant ahora, Instant expiraEn) {
		jdbc.sql("""
				UPDATE seguridad.codigo_recuperacion SET usado_en = :ahora
				WHERE usuario_id = :u AND usado_en IS NULL""")
				.param("u", usuarioId).param("ahora", enUtc(ahora)).update();
		jdbc.sql("""
				INSERT INTO seguridad.codigo_recuperacion (usuario_id, codigo_hash, creado_en, expira_en)
				VALUES (:u, :h, :creado, :expira)""")
				.param("u", usuarioId).param("h", codigoHash).param("creado", enUtc(ahora))
				.param("expira", enUtc(expiraEn)).update();
	}

	public void sumarIntento(UUID codigoId) {
		jdbc.sql("UPDATE seguridad.codigo_recuperacion SET intentos = intentos + 1 WHERE id = :id")
				.param("id", codigoId).update();
	}

	public void marcarUsado(UUID codigoId, Instant ahora) {
		jdbc.sql("UPDATE seguridad.codigo_recuperacion SET usado_en = :ahora WHERE id = :id")
				.param("id", codigoId).param("ahora", enUtc(ahora)).update();
	}

	private static OffsetDateTime enUtc(Instant instante) {
		return instante.atOffset(ZoneOffset.UTC);
	}
}
