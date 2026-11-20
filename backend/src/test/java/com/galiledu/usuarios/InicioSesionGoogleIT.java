package com.galiledu.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;
import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.usuarios.aplicacion.CuentasGoogle;
import com.galiledu.usuarios.aplicacion.CuentasGoogle.CuentaGoogle;
import com.jayway.jsonpath.JsonPath;

/**
 * HU-2 (el administrador activa o desactiva Google) y HU-3 (inicio de sesión con Google). Google se simula: la
 * prueba decide qué cuenta devuelve cada código de autorización.
 */
class InicioSesionGoogleIT extends PruebaIntegracion {
	private static final String ID_CLIENTE = "cliente-prueba.apps.googleusercontent.com";
	private static final String RUTA_CONFIGURACION = "/seguridad/configuracion-acceso";
	/** Correo de Selene Curo (U20260001, administradora) en los datos de prueba. */
	private static final String CORREO_ADMIN = "selene.curo@galiledu.edu.pe";
	private static final String USUARIO_ADMIN = "10000000-0000-0000-0000-000000000001";

	@MockitoBean
	private CuentasGoogle google;
	@Autowired
	private JdbcClient jdbc;

	@BeforeEach
	void conLlavesDeGoogle() {
		when(google.configurado()).thenReturn(true);
		when(google.idCliente()).thenReturn(ID_CLIENTE);
	}

	private void habilitarGoogle(boolean habilitado) {
		jdbc.sql("UPDATE seguridad.configuracion_acceso SET google_habilitado = :h WHERE id = 1")
				.param("h", habilitado).update();
	}

	private void googleDevuelve(String codigo, String correo, boolean verificado) {
		when(google.cuentaDe(codigo)).thenReturn(new CuentaGoogle(correo, verificado));
	}

	private ResultActions entrarConGoogle(String codigo) throws Exception {
		return mvc.perform(post("/auth/google").contentType(MediaType.APPLICATION_JSON).content("""
				{"codigo":"%s"}""".formatted(codigo)));
	}

	private ResultActions configurar(String token, boolean habilitado) throws Exception {
		return mvc.perform(conToken(put(RUTA_CONFIGURACION), token, """
				{"googleHabilitado":%s}""".formatted(habilitado)));
	}

	@Test
	void porDefectoGoogleEstaDesactivadoYElBotonNoSeMuestra() throws Exception {
		mvc.perform(get("/auth/metodos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.google").value(false))
				.andExpect(jsonPath("$.googleIdCliente").doesNotExist());
		mvc.perform(conToken(get(RUTA_CONFIGURACION), token(ADMIN))).andExpect(status().isOk())
				.andExpect(jsonPath("$.googleHabilitado").value(false))
				.andExpect(jsonPath("$.googleConfigurado").value(true));
	}

	@Test
	void elAdministradorActivaGoogleYElBotonAparece() throws Exception {
		String admin = token(ADMIN);
		configurar(admin, true).andExpect(status().isOk())
				.andExpect(jsonPath("$.googleHabilitado").value(true));
		mvc.perform(get("/auth/metodos")).andExpect(status().isOk())
				.andExpect(jsonPath("$.google").value(true))
				.andExpect(jsonPath("$.googleIdCliente").value(ID_CLIENTE));
		var auditoria = jdbc.sql("SELECT actualizado_por::text FROM seguridad.configuracion_acceso WHERE id = 1")
				.query(String.class).single();
		assertThat(auditoria).isEqualTo(USUARIO_ADMIN);

		configurar(admin, false).andExpect(status().isOk())
				.andExpect(jsonPath("$.googleHabilitado").value(false));
		mvc.perform(get("/auth/metodos")).andExpect(jsonPath("$.google").value(false));
	}

	@Test
	void soloElAdministradorCambiaLaConfiguracion() throws Exception {
		configurar(token(PERSONAL_ADMINISTRATIVO), true).andExpect(status().isForbidden());
		mvc.perform(conToken(get(RUTA_CONFIGURACION), token(DOCENTE))).andExpect(status().isForbidden());
		mvc.perform(get(RUTA_CONFIGURACION)).andExpect(status().isUnauthorized());
	}

	@Test
	void sinLlavesDeGoogleNoSePuedeActivarPeroSiDesactivar() throws Exception {
		when(google.configurado()).thenReturn(false);
		String admin = token(ADMIN);
		configurar(admin, true).andExpect(status().isConflict())
				.andExpect(jsonPath("$.message")
						.value("Falta configurar GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET en el servidor."));
		configurar(admin, false).andExpect(status().isOk());

		// Si alguien quita las llaves con Google ya activado, el botón deja de mostrarse.
		habilitarGoogle(true);
		mvc.perform(get("/auth/metodos")).andExpect(jsonPath("$.google").value(false));
		entrarConGoogle("codigo").andExpect(status().isForbidden());
	}

	@Test
	void conElMismoCorreoRegistradoEntraConLosPermisosDeSuRol() throws Exception {
		habilitarGoogle(true);
		googleDevuelve("codigo-ok", "Selene.Curo@GalilEdu.edu.pe", true);
		String respuesta = entrarConGoogle("codigo-ok").andExpect(status().isOk())
				.andExpect(jsonPath("$.user.username").value(ADMIN))
				.andExpect(jsonPath("$.user.email").value(CORREO_ADMIN))
				.andReturn().getResponse().getContentAsString();
		String jwt = JsonPath.read(respuesta, "$.jwt");
		mvc.perform(conToken(get("/auth/me"), jwt)).andExpect(status().isOk())
				.andExpect(jsonPath("$.data.username").value(ADMIN));
		mvc.perform(conToken(get(RUTA_CONFIGURACION), jwt)).andExpect(status().isOk());
	}

	@Test
	void conGoogleDesactivadoNoEntraNiConsultaAGoogle() throws Exception {
		entrarConGoogle("codigo-ok").andExpect(status().isForbidden())
				.andExpect(jsonPath("$.message")
						.value("El inicio de sesión con Google no está habilitado en el colegio."));
		verify(google, never()).cuentaDe(anyString());
	}

	@Test
	void unCorreoQueNoEsDeNingunUsuarioNoEntra() throws Exception {
		habilitarGoogle(true);
		googleDevuelve("codigo-ajeno", "alguien.mas@gmail.com", true);
		entrarConGoogle("codigo-ajeno").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message")
						.value("No se encontró una cuenta de GalilEdu asociada a este correo de Google."));
	}

	@Test
	void unCorreoQueGoogleNoVerificoNoEntra() throws Exception {
		habilitarGoogle(true);
		googleDevuelve("codigo-sin-verificar", CORREO_ADMIN, false);
		entrarConGoogle("codigo-sin-verificar").andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Google no ha verificado el correo de esta cuenta."));
	}

	@Test
	void unaCuentaDesactivadaNoEntraConGoogle() throws Exception {
		habilitarGoogle(true);
		googleDevuelve("codigo-ok", CORREO_ADMIN, true);
		jdbc.sql("UPDATE seguridad.usuario SET estado = 'INACTIVO' WHERE id = :u::uuid").param("u", USUARIO_ADMIN)
				.update();
		entrarConGoogle("codigo-ok").andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message")
						.value("Tu cuenta está desactivada. Comunícate con el administrador del colegio."));
	}

	@Test
	void unaCuentaBloqueadaPorIntentosFallidosTampocoEntraConGoogle() throws Exception {
		habilitarGoogle(true);
		googleDevuelve("codigo-ok", CORREO_ADMIN, true);
		jdbc.sql("UPDATE seguridad.usuario SET bloqueado_hasta = now() + interval '10 minutes' WHERE id = :u::uuid")
				.param("u", USUARIO_ADMIN).update();
		entrarConGoogle("codigo-ok").andExpect(status().isLocked());
	}

	@Test
	void siGoogleRechazaElCodigoNoEntra() throws Exception {
		habilitarGoogle(true);
		when(google.cuentaDe("codigo-vencido")).thenThrow(new ExcepcionNegocio(HttpStatus.UNAUTHORIZED,
				"No se pudo verificar tu cuenta de Google. Inténtalo de nuevo."));
		entrarConGoogle("codigo-vencido").andExpect(status().isUnauthorized())
				.andExpect(
						jsonPath("$.message").value("No se pudo verificar tu cuenta de Google. Inténtalo de nuevo."));
	}

	@Test
	void sinCodigoResponde400() throws Exception {
		habilitarGoogle(true);
		entrarConGoogle("").andExpect(status().isBadRequest());
	}

	@Test
	void usuarioYContrasenaSiguenFuncionandoConGoogleActivado() throws Exception {
		habilitarGoogle(true);
		token(ADMIN);
	}
}
