package com.galiledu.pagos.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/** Verificación de la firma de los webhooks de Stripe y conversión de soles a céntimos. */
class FirmaStripeTests {
	private static final String SECRETO = "whsec_prueba";
	private static final String CUERPO = "{\"type\":\"checkout.session.completed\"}";
	private static final long AHORA = 1_793_000_000L;
	private final Clock reloj = Clock.fixed(Instant.ofEpochSecond(AHORA), ZoneOffset.UTC);

	private static String firma(long marca, String secreto, String cuerpo) {
		return "t=" + marca + ",v1=" + HexFormat.of().formatHex(FirmaStripe.hmac(secreto, marca + "." + cuerpo));
	}

	@Test
	void aceptaUnaFirmaValida() {
		assertThat(FirmaStripe.esValida(CUERPO, firma(AHORA, SECRETO, CUERPO), SECRETO, reloj)).isTrue();
	}

	@Test
	void rechazaOtroSecretoOtroCuerpoYFirmaVieja() {
		assertThat(FirmaStripe.esValida(CUERPO, firma(AHORA, "whsec_otro", CUERPO), SECRETO, reloj)).isFalse();
		assertThat(FirmaStripe.esValida("{}", firma(AHORA, SECRETO, CUERPO), SECRETO, reloj)).isFalse();
		assertThat(FirmaStripe.esValida(CUERPO, firma(AHORA - 600, SECRETO, CUERPO), SECRETO, reloj)).isFalse();
		assertThat(FirmaStripe.esValida(CUERPO, null, SECRETO, reloj)).isFalse();
		assertThat(FirmaStripe.esValida(CUERPO, firma(AHORA, SECRETO, CUERPO), "", reloj)).isFalse();
	}

	@Test
	void convierteSolesACentimos() {
		assertThat(PasarelaStripe.centimos(new BigDecimal("612.00"))).isEqualTo(61_200L);
		assertThat(PasarelaStripe.centimos(new BigDecimal("0.5"))).isEqualTo(50L);
	}
}
