package com.galiledu.configuracion.dominio;

import java.math.BigDecimal;

/**
 * Lo que el Personal administrativo escribe en P19: los tres montos y la política de morosidad. El calendario y el
 * vencimiento de la matrícula no se escriben: salen de las fechas del año. La cuota de ingreso es opcional (solo
 * alumnos nuevos).
 */
public record DatosTarifario(BigDecimal montoCuotaIngreso, BigDecimal montoMatricula, BigDecimal montoPension,
		Integer diasGracia, BigDecimal recargoPorcentaje, BigDecimal topeRecargoPorcentaje) {
}
