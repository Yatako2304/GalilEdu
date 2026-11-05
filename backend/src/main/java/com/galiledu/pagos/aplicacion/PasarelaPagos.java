package com.galiledu.pagos.aplicacion;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Puerto hacia la pasarela de pagos externa (sección 3.8). El sistema no guarda datos de tarjetas: la pasarela los
 * captura en su propia página. Implementaciones: Stripe y una simulada para desarrollo y pruebas
 * ({@code galiledu.pagos.pasarela}).
 */
public interface PasarelaPagos {

	/** Concepto que se muestra en la página de pago (una línea por cuota, con capital y recargo). */
	record Concepto(String descripcion, BigDecimal monto) {
	}

	record Cobro(String codigoOrden, BigDecimal total, List<Concepto> conceptos, OffsetDateTime expiraEn,
			String urlExito, String urlCancelacion) {
	}

	/** {@code referencia}: id de la transacción en la pasarela; {@code url}: página a la que se redirige. */
	record Sesion(String referencia, String url) {
	}

	enum Estado {
		/** El apoderado aún no paga y la sesión sigue abierta. */
		PENDIENTE, APROBADO, RECHAZADO, EXPIRADO
	}

	record Resultado(Estado estado, BigDecimal montoConfirmado, String motivo) {
		public static Resultado pendiente() {
			return new Resultado(Estado.PENDIENTE, null, null);
		}
	}

	Sesion crearSesion(Cobro cobro);

	/** Consulta directa del estado, para cuando la notificación no llega (sección 3.8). */
	Resultado consultar(String referencia);

	/** Cierra la sesión si el apoderado canceló, para que ya no se pueda pagar con ella. */
	void cancelar(String referencia);
}
