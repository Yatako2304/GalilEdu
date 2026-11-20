package com.galiledu.usuarios.infraestructura;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.usuarios.aplicacion.CuentasGoogle;

/**
 * Inicio de sesión con Google por el flujo de código de autorización en ventana emergente
 * (https://developers.google.com/identity/oauth2/web/guides/use-code-model): el código se canjea en
 * oauth2.googleapis.com con la clave secreta y se valida el id_token (firma con las llaves públicas de Google, emisor,
 * audiencia y vencimiento). Sin GOOGLE_CLIENT_ID y GOOGLE_CLIENT_SECRET el inicio con Google no está disponible.
 */
@Component
public class CuentasGoogleOAuth implements CuentasGoogle {
	private static final Logger log = LoggerFactory.getLogger(CuentasGoogleOAuth.class);
	private static final String URL_TOKEN = "https://oauth2.googleapis.com/token";
	private static final String URL_LLAVES = "https://www.googleapis.com/oauth2/v3/certs";
	private static final List<String> EMISORES = List.of("https://accounts.google.com", "accounts.google.com");
	static final String NO_VERIFICADA = "No se pudo verificar tu cuenta de Google. Inténtalo de nuevo.";

	private final String idCliente;
	private final String claveSecreta;
	private final RestClient google = RestClient.builder().build();
	private JwtDecoder decodificador;

	public CuentasGoogleOAuth(@Value("${galiledu.google.id-cliente:}") String idCliente,
			@Value("${galiledu.google.clave-secreta:}") String claveSecreta) {
		this.idCliente = idCliente == null ? "" : idCliente.strip();
		this.claveSecreta = claveSecreta == null ? "" : claveSecreta.strip();
	}

	@Override
	public boolean configurado() {
		return !idCliente.isEmpty() && !claveSecreta.isEmpty();
	}

	@Override
	public String idCliente() {
		return idCliente;
	}

	@Override
	public CuentaGoogle cuentaDe(String codigo) {
		if (!configurado()) {
			throw new ExcepcionNegocio(HttpStatus.SERVICE_UNAVAILABLE,
					"El inicio de sesión con Google no está configurado en el servidor.");
		}
		try {
			MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
			form.add("code", codigo);
			form.add("client_id", idCliente);
			form.add("client_secret", claveSecreta);
			// El código viene de la ventana emergente de Google (ux_mode "popup"): no hay URL de retorno.
			form.add("redirect_uri", "postmessage");
			form.add("grant_type", "authorization_code");
			Map<?, ?> respuesta = google.post().uri(URL_TOKEN)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(form)
					.retrieve()
					.body(Map.class);
			Object idToken = respuesta == null ? null : respuesta.get("id_token");
			if (!(idToken instanceof String token)) {
				throw new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, NO_VERIFICADA);
			}
			Jwt jwt = decodificador().decode(token);
			String correo = jwt.getClaimAsString("email");
			if (correo == null || correo.isBlank()) {
				throw new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, NO_VERIFICADA);
			}
			return new CuentaGoogle(correo.strip(), "true".equalsIgnoreCase(jwt.getClaimAsString("email_verified")));
		} catch (RestClientException | JwtException e) {
			log.warn("Google rechazó el inicio de sesión: {}", e.getMessage());
			throw new ExcepcionNegocio(HttpStatus.UNAUTHORIZED, NO_VERIFICADA);
		}
	}

	private synchronized JwtDecoder decodificador() {
		if (decodificador == null) {
			NimbusJwtDecoder nimbus = NimbusJwtDecoder.withJwkSetUri(URL_LLAVES).build();
			nimbus.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(),
					jwt -> EMISORES.contains(jwt.getClaimAsString("iss"))
							? OAuth2TokenValidatorResult.success()
							: OAuth2TokenValidatorResult
									.failure(new OAuth2Error("invalid_token", "Emisor inválido", null)),
					jwt -> jwt.getAudience() != null && jwt.getAudience().contains(idCliente)
							? OAuth2TokenValidatorResult.success()
							: OAuth2TokenValidatorResult
									.failure(new OAuth2Error("invalid_token", "Audiencia inválida", null))));
			decodificador = nimbus;
		}
		return decodificador;
	}
}
