package com.galiledu.pagos.infraestructura;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifica la cabecera Stripe-Signature de un webhook (https://docs.stripe.com/webhooks#verify-manually):
 * {@code t=<segundos>,v1=<firma>}, donde la firma es HMAC-SHA256 de {@code t + "." + cuerpo} con el secreto del
 * endpoint (whsec_…). Rechaza firmas de más de 5 minutos para evitar que se reenvíe una notificación vieja.
 */
public final class FirmaStripe {
	static final long TOLERANCIA_SEGUNDOS = 300;

	private FirmaStripe() {
	}

	public static boolean esValida(String cuerpo, String cabecera, String secreto, Clock reloj) {
		if (cabecera == null || secreto == null || secreto.isBlank()) {
			return false;
		}
		String marca = null;
		List<String> firmas = new ArrayList<>();
		for (String parte : cabecera.split(",")) {
			String[] kv = parte.trim().split("=", 2);
			if (kv.length != 2) {
				continue;
			}
			if ("t".equals(kv[0])) {
				marca = kv[1];
			} else if ("v1".equals(kv[0])) {
				firmas.add(kv[1]);
			}
		}
		if (marca == null || firmas.isEmpty()) {
			return false;
		}
		long segundos;
		try {
			segundos = Long.parseLong(marca);
		} catch (NumberFormatException e) {
			return false;
		}
		if (Math.abs(Instant.now(reloj).getEpochSecond() - segundos) > TOLERANCIA_SEGUNDOS) {
			return false;
		}
		byte[] esperada = HexFormat.of().formatHex(hmac(secreto, marca + "." + cuerpo))
				.getBytes(StandardCharsets.UTF_8);
		return firmas.stream().anyMatch(f -> MessageDigest.isEqual(esperada, f.getBytes(StandardCharsets.UTF_8)));
	}

	static byte[] hmac(String secreto, String datos) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			return mac.doFinal(datos.getBytes(StandardCharsets.UTF_8));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}
}
