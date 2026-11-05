package com.galiledu.pagos.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Evento que Pagos publica cuando la pasarela confirma una orden (HU-41). Otros módulos lo escuchan sin depender de
 * Pagos: por ejemplo, Matrícula matricula al estudiante cuando la cuota pagada es la de matrícula (HU-31).
 *
 * @param cuotaIds
 *            cuotas que quedaron pagadas por completo
 * @param matriculasPagadas
 *            matrículas cuya cuota de matrícula quedó pagada por completo con esta orden (HU-31)
 */
public record PagoEnLineaAprobado(UUID ordenId, String codigoOrden, UUID usuarioId, List<UUID> cuotaIds,
		List<UUID> matriculasPagadas) {
}
