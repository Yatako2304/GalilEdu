package com.galiledu.pagos.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Política de morosidad del tarifario (HU-34) y cálculo del recargo (sección 3.10). Solo las pensiones generan mora:
 * vencida la pensión y pasados los días de gracia, el recargo es la tasa por cada 30 días de atraso (desde el
 * vencimiento) sobre el capital pendiente, sin pasar del tope. No se capitaliza.
 */
public record Morosidad(int diasGracia, BigDecimal recargoPorcentaje, BigDecimal topeRecargoPorcentaje) {
	private static final BigDecimal CIEN = BigDecimal.valueOf(100);

	public BigDecimal recargo(Cuota cuota, LocalDate hoy) {
		BigDecimal saldo = cuota.saldo();
		if (!cuota.esPension() || saldo.signum() <= 0 || !hoy.isAfter(cuota.getFechaVencimiento().plusDays(diasGracia))
				|| recargoPorcentaje == null || recargoPorcentaje.signum() <= 0) {
			return BigDecimal.ZERO.setScale(2);
		}
		long diasAtraso = ChronoUnit.DAYS.between(cuota.getFechaVencimiento(), hoy);
		long periodos = (diasAtraso + 29) / 30;
		BigDecimal porcentaje = recargoPorcentaje.multiply(BigDecimal.valueOf(periodos));
		if (topeRecargoPorcentaje != null && porcentaje.compareTo(topeRecargoPorcentaje) > 0) {
			porcentaje = topeRecargoPorcentaje;
		}
		return saldo.multiply(porcentaje).divide(CIEN, 2, RoundingMode.HALF_UP);
	}
}
