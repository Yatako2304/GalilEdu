package com.galiledu.pagos.web;

import java.time.Clock;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.pagos.aplicacion.ServicioPagoEnLinea;
import com.galiledu.pagos.infraestructura.FirmaStripe;

/**
 * Webhook de Stripe (sin sesión: lo llama Stripe, no el apoderado). Se acepta solo con una firma válida y, en vez de
 * creer el contenido de la notificación, el servicio vuelve a consultar la sesión en Stripe (sección 3.8).
 */
@RestController
@ConditionalOnProperty(name = "galiledu.pagos.pasarela", havingValue = "stripe")
public class NotificacionStripeController {
	public static final String RUTA = "/pagos/pasarela/stripe/notificaciones";
	private static final Logger LOG = LoggerFactory.getLogger(NotificacionStripeController.class);
	private static final Set<String> EVENTOS = Set.of("checkout.session.completed",
			"checkout.session.async_payment_succeeded", "checkout.session.async_payment_failed",
			"checkout.session.expired");

	private final ServicioPagoEnLinea servicio;
	private final String secreto;
	private final Clock reloj;

	public NotificacionStripeController(ServicioPagoEnLinea servicio,
			@Value("${galiledu.pagos.stripe.secreto-webhook:}") String secreto, Clock reloj) {
		this.servicio = servicio;
		this.secreto = secreto;
		this.reloj = reloj;
	}

	/** POST /api/pagos/pasarela/stripe/notificaciones */
	@PostMapping(RUTA)
	public ResponseEntity<Void> recibir(@RequestBody String cuerpo,
			@RequestHeader(name = "Stripe-Signature", required = false) String firma) {
		if (!FirmaStripe.esValida(cuerpo, firma, secreto, reloj)) {
			LOG.warn("Notificación de Stripe con firma inválida: se ignora.");
			return ResponseEntity.badRequest().build();
		}
		Map<String, Object> evento = JsonParserFactory.getJsonParser().parseMap(cuerpo);
		if (EVENTOS.contains(evento.get("type")) && evento.get("data") instanceof Map<?, ?> data
				&& data.get("object") instanceof Map<?, ?> sesion && sesion.get("id") instanceof String referencia) {
			servicio.notificar(referencia);
		}
		return ResponseEntity.ok().build();
	}
}
