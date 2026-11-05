package com.galiledu.pagos.infraestructura;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.galiledu.pagos.aplicacion.PasarelaPagos;

/**
 * Pasarela de prueba (galiledu.pagos.pasarela=simulada, por defecto): no cobra nada. La "página de pago" es una
 * pantalla del frontend donde se elige aprobar o rechazar; sirve para desarrollar, probar y demostrar sin una cuenta
 * de Stripe. El resultado vive en memoria: si el backend se reinicia, las órdenes abiertas terminan expirando.
 */
@Component
@ConditionalOnProperty(name = "galiledu.pagos.pasarela", havingValue = "simulada", matchIfMissing = true)
public class PasarelaSimulada implements PasarelaPagos {
	static final String RUTA_SIMULADOR = "/app/apoderado/pensiones/pasarela-simulada";

	private final String urlFrontend;
	private final Map<String, Resultado> resultados = new ConcurrentHashMap<>();
	private final Map<String, BigDecimal> montos = new ConcurrentHashMap<>();

	public PasarelaSimulada(@Value("${galiledu.pagos.url-frontend}") String urlFrontend) {
		this.urlFrontend = urlFrontend.replaceAll("/+$", "");
	}

	@Override
	public Sesion crearSesion(Cobro cobro) {
		String referencia = "SIM-" + cobro.codigoOrden();
		resultados.put(referencia, Resultado.pendiente());
		montos.put(referencia, cobro.total());
		return new Sesion(referencia, urlFrontend + RUTA_SIMULADOR + "?orden=" + cobro.codigoOrden());
	}

	@Override
	public Resultado consultar(String referencia) {
		return resultados.getOrDefault(referencia, Resultado.pendiente());
	}

	@Override
	public void cancelar(String referencia) {
		resultados.computeIfPresent(referencia, (r, actual) -> actual.estado() == Estado.PENDIENTE
				? new Resultado(Estado.EXPIRADO, null, null)
				: actual);
	}

	/** Lo que haría la pasarela real al terminar el pago: aprobarlo por el monto cobrado o rechazarlo. */
	public void decidir(String referencia, boolean aprobar) {
		if (aprobar) {
			aprobarPor(referencia, montos.get(referencia));
		} else {
			resultados.put(referencia, new Resultado(Estado.RECHAZADO, null, "Tarjeta rechazada (simulación)."));
		}
	}

	/** Aprueba por un monto dado; distinto al de la orden, simula un cobro que no coincide (RF-105). */
	public void aprobarPor(String referencia, BigDecimal monto) {
		resultados.put(referencia, new Resultado(Estado.APROBADO, monto, null));
	}
}
