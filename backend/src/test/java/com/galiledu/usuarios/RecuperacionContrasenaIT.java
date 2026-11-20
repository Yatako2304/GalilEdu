package com.galiledu.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.ResultActions;

import com.galiledu.PruebaIntegracion;

/**
 * HU-4 (Figma › Autenticación › Restablecer contraseña): correo → código de 6 dígitos → contraseña nueva. Sin SMTP en
 * las pruebas el código solo va al log, así que cada prueba reemplaza su hash por el de un código conocido.
 */
class RecuperacionContrasenaIT extends PruebaIntegracion {
	private static final String CORREO = "selene.curo@galiledu.edu.pe";
	private static final String USUARIO_ID = "10000000-0000-0000-0000-000000000001";
	private static final String CODIGO = "482913";
	private static final String NUEVA = "Nueva#2026";

	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private PasswordEncoder hashes;

	private ResultActions pedir(String ruta, String json) throws Exception {
		return mvc.perform(post("/auth/recuperar-contrasena" + ruta).contentType(MediaType.APPLICATION_JSON)
				.content(json));
	}

	private ResultActions solicitar(String correo) throws Exception {
		return pedir("", """
				{"email":"%s"}""".formatted(correo));
	}

	private ResultActions verificar(String codigo) throws Exception {
		return pedir("/verificar", """
				{"email":"%s","codigo":"%s"}""".formatted(CORREO, codigo));
	}

	private ResultActions restablecer(String codigo, String contrasena) throws Exception {
		return pedir("/restablecer", """
				{"email":"%s","codigo":"%s","contrasena":"%s"}""".formatted(CORREO, codigo, contrasena));
	}

	private ResultActions login(String contrasena) throws Exception {
		return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
				{"username":"%s","password":"%s"}""".formatted(ADMIN, contrasena)));
	}

	/** Pide un código y fija su valor en {@link #CODIGO} (el real solo se ve en el log). */
	private void conCodigoConocido() throws Exception {
		solicitar(CORREO).andExpect(status().isOk());
		jdbc.sql("""
				UPDATE seguridad.codigo_recuperacion SET codigo_hash = :h
				WHERE usuario_id = :u::uuid AND usado_en IS NULL""")
				.param("h", hashes.encode(CODIGO)).param("u", USUARIO_ID).update();
	}

	private int codigosVigentes() {
		return jdbc.sql("""
				SELECT count(*) FROM seguridad.codigo_recuperacion WHERE usuario_id = :u::uuid AND usado_en IS NULL""")
				.param("u", USUARIO_ID).query(Integer.class).single();
	}

	@Test
	void conUnCorreoRegistradoEnviaUnCodigoQueVenceEnDiezMinutos() throws Exception {
		solicitar(CORREO).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Te enviamos un código de verificación a tu correo."));
		var minutos = jdbc.sql("""
				SELECT extract(epoch FROM expira_en - creado_en) / 60 FROM seguridad.codigo_recuperacion
				WHERE usuario_id = :u::uuid""").param("u", USUARIO_ID).query(Double.class).single();
		assertThat(minutos).isEqualTo(10.0);
	}

	@Test
	void elCorreoNoDistingueMayusculas() throws Exception {
		solicitar("Selene.Curo@GALILEDU.edu.pe").andExpect(status().isOk());
	}

	@Test
	void unCorreoSinCuentaDaElMensajeDeFigma() throws Exception {
		solicitar("nadie@galiledu.edu.pe").andExpect(status().isNotFound())
				.andExpect(jsonPath("$.message").value("El correo no está asociado a ninguna cuenta."));
	}

	@Test
	void unaCuentaDesactivadaNoPuedeRecuperarla() throws Exception {
		solicitar("leo.montoya@galiledu.edu.pe").andExpect(status().isConflict());
	}

	@Test
	void soloSePuedePedirOtroCodigoCadaSesentaSegundosYAnulaElAnterior() throws Exception {
		solicitar(CORREO).andExpect(status().isOk());
		solicitar(CORREO).andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Espera ")));

		jdbc.sql("UPDATE seguridad.codigo_recuperacion SET creado_en = creado_en - interval '61 seconds'").update();
		solicitar(CORREO).andExpect(status().isOk());
		assertThat(codigosVigentes()).isEqualTo(1);
	}

	@Test
	void conElCodigoCorrectoRestableceLaContrasenaYElCodigoNoSeReutiliza() throws Exception {
		conCodigoConocido();
		verificar(CODIGO).andExpect(status().isOk());
		restablecer(CODIGO, NUEVA).andExpect(status().isOk())
				.andExpect(jsonPath("$.message").value("Su contraseña ha sido actualizada correctamente."));

		login(NUEVA).andExpect(status().isOk());
		login(CONTRASENA).andExpect(status().isUnauthorized());
		restablecer(CODIGO, "Otra#2026x").andExpect(status().isBadRequest());
	}

	@Test
	void unCodigoIncorrectoDaElMensajeDeFigmaYAlQuintoIntentoSeAnula() throws Exception {
		conCodigoConocido();
		for (int i = 1; i <= 4; i++) {
			verificar("000000").andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.message").value("Código de verificación incorrecto."));
		}
		verificar("000000").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Superaste los intentos")));
		// Ya ni el código correcto sirve.
		verificar(CODIGO).andExpect(status().isBadRequest());
	}

	@Test
	void unCodigoVencidoNoSirve() throws Exception {
		conCodigoConocido();
		jdbc.sql("UPDATE seguridad.codigo_recuperacion SET expira_en = :t")
				.param("t", OffsetDateTime.now().minus(Duration.ofMinutes(1))).update();
		verificar(CODIGO).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("El código venció")));
	}

	@Test
	void sinHaberPedidoUnCodigoNoSeVerificaNada() throws Exception {
		verificar(CODIGO).andExpect(status().isBadRequest());
	}

	@Test
	void unaContrasenaQueNoCumpleLaReglaSeRechazaSinGastarElCodigo() throws Exception {
		conCodigoConocido();
		restablecer(CODIGO, "passw123").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.startsWith("Mínimo 8 caracteres")));
		restablecer(CODIGO, NUEVA).andExpect(status().isOk());
	}

	@Test
	void restablecerQuitaElBloqueoPorIntentosFallidos() throws Exception {
		for (int i = 0; i < 5; i++) {
			login("incorrecta");
		}
		login(CONTRASENA).andExpect(status().isLocked());
		conCodigoConocido();
		restablecer(CODIGO, NUEVA).andExpect(status().isOk());
		login(NUEVA).andExpect(status().isOk());
	}

	@Test
	void unCorreoMalEscritoSeRechazaAntesDeBuscar() throws Exception {
		solicitar("no-es-correo").andExpect(status().isBadRequest());
	}
}
