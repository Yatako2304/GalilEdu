package com.galiledu.usuarios.infraestructura.persistencia;

import java.util.Set;
import java.util.UUID;

import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.Rol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class ConsultaUsuarioRegistradoPostgresTests {
	private JdbcTemplate jdbc;
	private ConsultaUsuarioRegistradoPostgres usuarios;

	@BeforeEach
	void preparar() {
		var datos = new DriverManagerDataSource(
			"jdbc:h2:mem:usuarios_adapter;MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
		datos.setDriverClassName("org.h2.Driver");
		jdbc = new JdbcTemplate(datos);
		jdbc.execute("CREATE SCHEMA IF NOT EXISTS personas");
		jdbc.execute("CREATE SCHEMA IF NOT EXISTS seguridad");
		jdbc.execute("DROP TABLE IF EXISTS seguridad.usuario_rol");
		jdbc.execute("DROP TABLE IF EXISTS seguridad.rol");
		jdbc.execute("DROP TABLE IF EXISTS seguridad.usuario");
		jdbc.execute("DROP TABLE IF EXISTS personas.persona");
		jdbc.execute("CREATE TABLE personas.persona (id UUID PRIMARY KEY, correo VARCHAR(120))");
		jdbc.execute("CREATE TABLE seguridad.usuario (id UUID PRIMARY KEY, persona_id UUID NOT NULL, google_sub VARCHAR(100), estado VARCHAR(20) NOT NULL)");
		jdbc.execute("CREATE TABLE seguridad.rol (id UUID PRIMARY KEY, nombre VARCHAR(60) NOT NULL, activo BOOLEAN NOT NULL)");
		jdbc.execute("CREATE TABLE seguridad.usuario_rol (usuario_id UUID NOT NULL, rol_id UUID NOT NULL)");
		usuarios = new ConsultaUsuarioRegistradoPostgres(jdbc);
	}

	@Test
	void leeUnaCuentaConVariosRolesActivosDesdeLasTablasReales() {
		UUID usuarioId = UUID.randomUUID();
		UUID personaId = UUID.randomUUID();
		UUID docente = UUID.randomUUID();
		UUID apoderado = UUID.randomUUID();
		UUID inactivo = UUID.randomUUID();
		jdbc.update("INSERT INTO personas.persona (id, correo) VALUES (?, ?)",
			personaId, "persona@colegio.pe");
		jdbc.update("INSERT INTO seguridad.usuario (id, persona_id, estado) VALUES (?, ?, ?)",
			usuarioId, personaId, "ACTIVO");
		jdbc.update("INSERT INTO seguridad.rol (id, nombre, activo) VALUES (?, ?, ?)", docente, "DOCENTE", true);
		jdbc.update("INSERT INTO seguridad.rol (id, nombre, activo) VALUES (?, ?, ?)", apoderado, "APODERADO", true);
		jdbc.update("INSERT INTO seguridad.rol (id, nombre, activo) VALUES (?, ?, ?)", inactivo, "COORDINADOR", false);
		for (UUID rol : Set.of(docente, apoderado, inactivo)) {
			jdbc.update("INSERT INTO seguridad.usuario_rol (usuario_id, rol_id) VALUES (?, ?)", usuarioId, rol);
		}

		var cuenta = usuarios.buscarPorCorreo("persona@colegio.pe").orElseThrow();

		assertThat(cuenta.id()).isEqualTo(usuarioId);
		assertThat(cuenta.personaId()).isEqualTo(personaId);
		assertThat(cuenta.estado()).isEqualTo(EstadoUsuario.ACTIVO);
		assertThat(cuenta.googleSub()).isNull();
		assertThat(cuenta.correo()).isEqualTo("persona@colegio.pe");
		assertThat(cuenta.roles()).containsExactlyInAnyOrder(new Rol("DOCENTE"), new Rol("APODERADO"));
	}

	@Test
	void elCorreoSeLeeDePersonaAunqueCambie() {
		UUID usuarioId = UUID.randomUUID();
		UUID personaId = UUID.randomUUID();
		jdbc.update("INSERT INTO personas.persona (id, correo) VALUES (?, ?)",
			personaId, "anterior@colegio.pe");
		jdbc.update("INSERT INTO seguridad.usuario (id, persona_id, estado) VALUES (?, ?, ?)",
			usuarioId, personaId, "ACTIVO");
		jdbc.update("UPDATE personas.persona SET correo = ? WHERE id = ?",
			"actual@colegio.pe", personaId);

		assertThat(usuarios.buscarPorCorreo("anterior@colegio.pe")).isEmpty();
		assertThat(usuarios.buscarPorCorreo("actual@colegio.pe").orElseThrow().correo())
			.isEqualTo("actual@colegio.pe");
	}

	@Test
	void noInventaUsuarioNiRolesCuandoNoExisten() {
		assertThat(usuarios.buscarPorCorreo("desconocido@colegio.pe")).isEmpty();
		assertThat(usuarios.buscarPorCorreo(" ")).isEmpty();
	}
}
