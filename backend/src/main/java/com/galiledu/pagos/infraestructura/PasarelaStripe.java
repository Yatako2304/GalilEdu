package com.galiledu.pagos.infraestructura;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.galiledu.pagos.aplicacion.PasarelaPagos;

/**
 * Stripe Checkout (galiledu.pagos.pasarela=stripe). El apoderado paga en la página de Stripe; aquí solo se crea la
 * sesión y se consulta su estado con la API REST (https://docs.stripe.com/api/checkout/sessions), sin la librería de
 * Stripe. Montos en soles (PEN) y en céntimos.
 */
@Component
@ConditionalOnProperty(name = "galiledu.pagos.pasarela", havingValue = "stripe")
public class PasarelaStripe implements PasarelaPagos {
	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	private final RestClient stripe;

	public PasarelaStripe(@Value("${galiledu.pagos.stripe.clave-secreta}") String claveSecreta) {
		if (claveSecreta == null || claveSecreta.isBlank()) {
			throw new IllegalStateException("Falta STRIPE_SECRET_KEY para usar la pasarela de Stripe.");
		}
		this.stripe = RestClient.builder()
				.baseUrl("https://api.stripe.com/v1")
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + claveSecreta)
				.build();
	}

	@Override
	public Sesion crearSesion(Cobro cobro) {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("mode", "payment");
		form.add("success_url", cobro.urlExito());
		form.add("cancel_url", cobro.urlCancelacion());
		form.add("client_reference_id", cobro.codigoOrden());
		form.add("metadata[orden]", cobro.codigoOrden());
		form.add("expires_at", String.valueOf(cobro.expiraEn().toEpochSecond()));
		for (int i = 0; i < cobro.conceptos().size(); i++) {
			Concepto c = cobro.conceptos().get(i);
			String linea = "line_items[" + i + "]";
			form.add(linea + "[quantity]", "1");
			form.add(linea + "[price_data][currency]", "pen");
			form.add(linea + "[price_data][unit_amount]", String.valueOf(centimos(c.monto())));
			form.add(linea + "[price_data][product_data][name]", c.descripcion());
		}
		Map<?, ?> sesion = stripe.post().uri("/checkout/sessions")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(Map.class);
		return new Sesion((String) sesion.get("id"), (String) sesion.get("url"));
	}

	/**
	 * status: open | complete | expired. payment_status: paid | unpaid | no_payment_required. Una sesión "complete"
	 * sin pagar es un pago diferido que falló.
	 */
	@Override
	public Resultado consultar(String referencia) {
		Map<?, ?> sesion = stripe.get().uri("/checkout/sessions/{id}", referencia).retrieve().body(Map.class);
		String estado = (String) sesion.get("status");
		String pago = (String) sesion.get("payment_status");
		if ("complete".equals(estado) && "paid".equals(pago)) {
			Number total = (Number) sesion.get("amount_total");
			return new Resultado(Estado.APROBADO, BigDecimal.valueOf(total.longValue()).divide(CIEN, 2,
					RoundingMode.UNNECESSARY), null);
		}
		if ("complete".equals(estado)) {
			return new Resultado(Estado.RECHAZADO, null, "La pasarela no pudo cobrar el pago.");
		}
		if ("expired".equals(estado)) {
			return new Resultado(Estado.EXPIRADO, null, null);
		}
		return Resultado.pendiente();
	}

	@Override
	public void cancelar(String referencia) {
		try {
			stripe.post().uri("/checkout/sessions/{id}/expire", referencia).retrieve().toBodilessEntity();
		} catch (RestClientResponseException e) {
			// Ya no estaba abierta (pagada o expirada): no hay nada que cerrar.
		}
	}

	static long centimos(BigDecimal monto) {
		return monto.multiply(CIEN).setScale(0, RoundingMode.HALF_UP).longValueExact();
	}
}
