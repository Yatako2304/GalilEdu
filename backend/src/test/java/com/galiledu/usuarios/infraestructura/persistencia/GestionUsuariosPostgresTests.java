package com.galiledu.usuarios.infraestructura.persistencia;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import com.galiledu.usuarios.aplicacion.GestionUsuarios;
import com.galiledu.usuarios.aplicacion.ResultadoInicioSesion;
import com.galiledu.usuarios.aplicacion.SolicitudInvalidaException;
import com.galiledu.usuarios.dominio.DatosPersonales;
import com.galiledu.usuarios.dominio.TipoDocumento;
import com.galiledu.usuarios.infraestructura.seguridad.AutenticadorPostgres;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.greaterThan;

/** Prueba real en PostgreSQL; todos los cambios se revierten al terminar. */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "GALILEDU_POSTGRES_TEST", matches = "true")
class GestionUsuariosPostgresTests {
	@Autowired private GestionUsuarios gestion;
	@Autowired private AutenticadorPostgres autenticador;
	@Autowired private JdbcOperations jdbc;
	@Autowired private MockMvc mvc;

	@DynamicPropertySource
	static void postgres(DynamicPropertyRegistry propiedades) {
		propiedades.add("spring.datasource.url", () -> System.getenv("DB_URL"));
		propiedades.add("spring.datasource.username", () -> System.getenv("DB_USER"));
		propiedades.add("spring.datasource.password", () -> System.getenv("DB_PASSWORD"));
		propiedades.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
		propiedades.add("spring.jpa.hibernate.ddl-auto", () -> "none");
	}

	@Test
	@Transactional
	void creaCuentasYVinculaVariosApoderadosEnUnaTransaccion() {
		String rol = "PRUEBA_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		jdbc.update("INSERT INTO seguridad.rol (id, nombre) VALUES (?, ?)", UUID.randomUUID(), rol);

		var apoderado1 = gestion.crearCuenta(datos("Ana"), Set.of("APODERADO"), List.of(), Set.of(rol));
		var apoderado2 = gestion.crearCuenta(datos("Luis"), Set.of("APODERADO"), List.of(), Set.of(rol));
		var estudiante = gestion.crearCuenta(datos("Celia"), Set.of("ESTUDIANTE"),
			List.of(apoderado1.personaId(), apoderado2.personaId()), Set.of(rol));

		assertThat(gestion.buscarPersona(estudiante.personaId()).apoderados())
			.containsExactlyInAnyOrder(apoderado1.personaId(), apoderado2.personaId());
		assertThat(autenticador.verificar(estudiante.nombreUsuario(), estudiante.contrasenaInicial()).estado())
			.isEqualTo(ResultadoInicioSesion.Estado.EXITOSO);

		gestion.actualizarPersona(estudiante.personaId(), datos("Celia Actualizada"));
		assertThat(gestion.buscarPersona(estudiante.personaId()).datos().nombres())
			.isEqualTo("Celia Actualizada");
		gestion.desactivarPersona(estudiante.personaId());
		assertThat(gestion.buscarPersona(estudiante.personaId()).activa()).isFalse();
	}

	@Test
	@Transactional
	void soloAdministradorPuedeUsarLaApiYLasEscriturasExigenCsrf() throws Exception {
		jdbc.update("""
			INSERT INTO seguridad.rol (id, nombre, activo) VALUES (?, 'ADMINISTRADOR', TRUE)
			ON CONFLICT (nombre) DO UPDATE SET activo = TRUE
			""", UUID.randomUUID());
		var admin = gestion.crearCuenta(datos("Admin"), Set.of(), List.of(), Set.of("ADMINISTRADOR"));
		var persona = gestion.buscarPersona(admin.personaId());
		assertThat(persona.activa()).isTrue();

		mvc.perform(get("/api/personas/{id}", admin.personaId()))
			.andExpect(status().isUnauthorized());
		mvc.perform(get("/api/personas/{id}", admin.personaId())
			.with(httpBasic(admin.nombreUsuario(), admin.contrasenaInicial())))
			.andExpect(status().isOk());
		String rolLimitado = "PRUEBA_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
		jdbc.update("INSERT INTO seguridad.rol (id, nombre) VALUES (?, ?)", UUID.randomUUID(), rolLimitado);
		var limitado = gestion.crearCuenta(datos("Limitado"), Set.of(), List.of(), Set.of(rolLimitado));
		mvc.perform(get("/api/personas/{id}", admin.personaId())
			.with(httpBasic(limitado.nombreUsuario(), limitado.contrasenaInicial())))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/usuarios")
			.with(httpBasic(admin.nombreUsuario(), admin.contrasenaInicial())))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/usuarios").with(csrf())
			.with(httpBasic(admin.nombreUsuario(), admin.contrasenaInicial()))
			.contentType("application/json")
			.content("""
				{"persona":{"nombres":"Nuevo","primerApellido":"Prueba",
				"tipoDocumento":"DNI","numeroDocumento":"12345678",
				"correoElectronico":"nuevo-usuario-api@galiledu.test","telefono":"987654321"},
				"perfiles":[],"apoderados":[],"roles":["ADMINISTRADOR"]}
				"""))
			.andExpect(status().isCreated());
	}

	@Test
	void altaFallidaReviertePersonaYCuenta() {
		DatosPersonales datos = datos("Sin Rol");
		assertThatThrownBy(() -> gestion.crearCuenta(datos, Set.of(), List.of(),
			Set.of("ROL_INEXISTENTE"))).isInstanceOf(SolicitudInvalidaException.class);
		Integer restos = jdbc.queryForObject("""
			SELECT count(*) FROM personas.persona WHERE correo = ?
			""", Integer.class, datos.correoElectronico());
		assertThat(restos).isZero();
	}

	@Test
	@Transactional
	void apoderadoPuedeCambiarSuClaveSinAccederAlCrudAdministrativo() throws Exception {
		jdbc.update("""
			INSERT INTO seguridad.rol (id, nombre, activo) VALUES (?, 'APODERADO', TRUE)
			ON CONFLICT (nombre) DO UPDATE SET activo = TRUE
			""", UUID.randomUUID());
		var cuenta = gestion.crearCuenta(datos("Apoderado"), Set.of("APODERADO"), List.of(),
			Set.of("APODERADO"));
		mvc.perform(get("/api/auth/me")
			.with(httpBasic(cuenta.nombreUsuario(), cuenta.contrasenaInicial())))
			.andExpect(status().isOk());
		mvc.perform(get("/api/personas/{id}", cuenta.personaId())
			.with(httpBasic(cuenta.nombreUsuario(), cuenta.contrasenaInicial())))
			.andExpect(status().isForbidden());
		mvc.perform(put("/api/auth/password")
			.with(httpBasic(cuenta.nombreUsuario(), cuenta.contrasenaInicial())))
			.andExpect(status().isForbidden());
		mvc.perform(put("/api/auth/password").with(csrf())
			.with(httpBasic(cuenta.nombreUsuario(), cuenta.contrasenaInicial()))
			.contentType("application/json")
			.content("{\"contrasenaActual\":\"incorrecta\",\"nuevaContrasena\":\"nueva-clave\"}"))
			.andExpect(status().isBadRequest());
		mvc.perform(put("/api/auth/password").with(csrf())
			.with(httpBasic(cuenta.nombreUsuario(), cuenta.contrasenaInicial()))
			.contentType("application/json")
			.content("{\"contrasenaActual\":\"" + cuenta.contrasenaInicial()
				+ "\",\"nuevaContrasena\":\"nueva-clave\"}"))
			.andExpect(status().isNoContent());
		assertThat(autenticador.verificar(cuenta.nombreUsuario(), cuenta.contrasenaInicial()).estado())
			.isEqualTo(ResultadoInicioSesion.Estado.CREDENCIALES_INVALIDAS);
		assertThat(autenticador.verificar(cuenta.nombreUsuario(), "nueva-clave").estado())
			.isEqualTo(ResultadoInicioSesion.Estado.EXITOSO);
	}

	@Test
	@Transactional
	void loginRestCreaSesionYComunicaTiempoRestanteDeBloqueo() throws Exception {
		jdbc.update("""
			INSERT INTO seguridad.rol (id, nombre, activo) VALUES (?, 'DOCENTE', TRUE)
			ON CONFLICT (nombre) DO UPDATE SET activo = TRUE
			""", UUID.randomUUID());
		var cuenta = gestion.crearCuenta(datos("Docente"), Set.of("DOCENTE"), List.of(),
			Set.of("DOCENTE"));
		String correcta = "{\"nombreUsuario\":\"" + cuenta.nombreUsuario()
			+ "\",\"contrasena\":\"" + cuenta.contrasenaInicial() + "\"}";
		var resultado = mvc.perform(post("/api/auth/login").with(csrf())
			.contentType("application/json").content(correcta))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombreUsuario").value(cuenta.nombreUsuario()))
			.andExpect(jsonPath("$.roles[0]").value("DOCENTE"))
			.andReturn();
		MockHttpSession sesion = (MockHttpSession) resultado.getRequest().getSession(false);
		assertThat(sesion).isNotNull();
		mvc.perform(get("/api/auth/me").session(sesion))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.nombreUsuario").value(cuenta.nombreUsuario()));

		String incorrecta = "{\"nombreUsuario\":\"" + cuenta.nombreUsuario()
			+ "\",\"contrasena\":\"incorrecta\"}";
		for (int intento = 1; intento < 5; intento++) {
			mvc.perform(post("/api/auth/login").with(csrf())
				.contentType("application/json").content(incorrecta))
				.andExpect(status().isUnauthorized());
		}
		mvc.perform(post("/api/auth/login").with(csrf())
			.contentType("application/json").content(incorrecta))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.segundosRestantes", greaterThan(0)));
	}

	@Test
	@Transactional
	void administradorReactivaCuentaYConservaRoles() throws Exception {
		jdbc.update("""
			INSERT INTO seguridad.rol (id, nombre, activo) VALUES (?, 'ADMINISTRADOR', TRUE)
			ON CONFLICT (nombre) DO UPDATE SET activo = TRUE
			""", UUID.randomUUID());
		jdbc.update("""
			INSERT INTO seguridad.rol (id, nombre, activo) VALUES (?, 'DOCENTE', TRUE)
			ON CONFLICT (nombre) DO UPDATE SET activo = TRUE
			""", UUID.randomUUID());
		var admin = gestion.crearCuenta(datos("Administrador"), Set.of(), List.of(), Set.of("ADMINISTRADOR"));
		var docente = gestion.crearCuenta(datos("Docente"), Set.of("DOCENTE"), List.of(), Set.of("DOCENTE"));
		Integer rolesAntes = jdbc.queryForObject("""
			SELECT count(*) FROM seguridad.usuario_rol ur
			JOIN seguridad.usuario u ON u.id = ur.usuario_id WHERE u.persona_id = ?
			""", Integer.class, docente.personaId());

		mvc.perform(delete("/api/personas/{id}", docente.personaId()).with(csrf())
			.with(httpBasic(admin.nombreUsuario(), admin.contrasenaInicial())))
			.andExpect(status().isNoContent());
		assertThat(autenticador.verificar(docente.nombreUsuario(), docente.contrasenaInicial()).estado())
			.isNotEqualTo(ResultadoInicioSesion.Estado.EXITOSO);
		mvc.perform(post("/api/personas/{id}/reactivacion", docente.personaId())
			.with(httpBasic(admin.nombreUsuario(), admin.contrasenaInicial())))
			.andExpect(status().isForbidden());
		mvc.perform(post("/api/personas/{id}/reactivacion", docente.personaId()).with(csrf())
			.with(httpBasic(docente.nombreUsuario(), docente.contrasenaInicial())))
			.andExpect(status().isUnauthorized());
		mvc.perform(post("/api/personas/{id}/reactivacion", docente.personaId()).with(csrf())
			.with(httpBasic(admin.nombreUsuario(), admin.contrasenaInicial())))
			.andExpect(status().isNoContent());

		assertThat(gestion.buscarPersona(docente.personaId()).activa()).isTrue();
		assertThat(autenticador.verificar(docente.nombreUsuario(), docente.contrasenaInicial()).estado())
			.isEqualTo(ResultadoInicioSesion.Estado.EXITOSO);
		mvc.perform(post("/api/personas/{id}/reactivacion", docente.personaId()).with(csrf())
			.with(httpBasic(docente.nombreUsuario(), docente.contrasenaInicial())))
			.andExpect(status().isForbidden());
		Integer rolesDespues = jdbc.queryForObject("""
			SELECT count(*) FROM seguridad.usuario_rol ur
			JOIN seguridad.usuario u ON u.id = ur.usuario_id WHERE u.persona_id = ?
			""", Integer.class, docente.personaId());
		assertThat(rolesDespues).isEqualTo(rolesAntes);
	}

	private static DatosPersonales datos(String nombre) {
		String sufijo = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		String documento = String.valueOf(ThreadLocalRandom.current().nextInt(10_000_000, 100_000_000));
		return new DatosPersonales(nombre, "Prueba", null, TipoDocumento.DNI,
			documento, "persona-" + sufijo + "@galiledu.test", "987654321");
	}
}
