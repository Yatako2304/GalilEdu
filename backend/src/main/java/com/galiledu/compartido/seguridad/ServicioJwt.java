package com.galiledu.compartido.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite el token del login. El "subject" es el id del usuario y "roles" sus códigos de rol. */
@Service
public class ServicioJwt {
	private final JwtEncoder codificador;
	private final Duration duracion;

	public ServicioJwt(JwtEncoder codificador, @Value("${galiledu.jwt.duracion-horas}") long horas) {
		this.codificador = codificador;
		this.duracion = Duration.ofHours(horas);
	}

	public String emitir(UUID usuarioId, String nombreUsuario, Collection<String> roles) {
		Instant ahora = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer("galiledu")
				.issuedAt(ahora)
				.expiresAt(ahora.plus(duracion))
				.subject(usuarioId.toString())
				.claim("username", nombreUsuario)
				.claim("roles", roles)
				.build();
		JwsHeader cabecera = JwsHeader.with(MacAlgorithm.HS256).build();
		return codificador.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
	}

	/** Id del usuario autenticado en la petición actual. */
	public static UUID usuarioActual(Authentication autenticacion) {
		return UUID.fromString(autenticacion.getName());
	}
}
