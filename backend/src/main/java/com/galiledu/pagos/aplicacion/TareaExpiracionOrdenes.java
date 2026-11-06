package com.galiledu.pagos.aplicacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * CAHU41-8: cada minuto resuelve las órdenes cuyo plazo (30 min) ya pasó. Antes de expirarlas consulta a la pasarela,
 * por si el pago sí se completó y la notificación no llegó.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "galiledu.pagos.expiracion-automatica", havingValue = "true", matchIfMissing = true)
public class TareaExpiracionOrdenes {
	private static final Logger LOG = LoggerFactory.getLogger(TareaExpiracionOrdenes.class);

	private final ServicioPagoEnLinea servicio;

	public TareaExpiracionOrdenes(ServicioPagoEnLinea servicio) {
		this.servicio = servicio;
	}

	@Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
	public void expirarVencidas() {
		for (String codigo : servicio.ordenesVencidas()) {
			try {
				servicio.resolverSiVencio(codigo);
			} catch (RuntimeException e) {
				LOG.error("No se pudo resolver la orden vencida {}", codigo, e);
			}
		}
	}
}
