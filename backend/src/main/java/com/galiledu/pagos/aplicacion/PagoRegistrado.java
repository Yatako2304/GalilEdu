package com.galiledu.pagos.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Evento que Pagos publica al quedar registrado un pago: cuando la pasarela confirma una orden (HU-41) o cuando el
 * Personal administrativo registra un pago en caja. Otros módulos lo escuchan sin depender de Pagos: por ejemplo,
 * Matrícula matricula al estudiante cuando la cuota pagada es la de matrícula (HU-31).
 *
 * @param referencia
 *            código de la orden en línea (OP-2026-…) o número de constancia del pago en caja, para los registros
 * @param usuarioId
 *            quien pagó en línea o quien registró el pago en caja
 * @param cuotaIds
 *            cuotas que quedaron pagadas por completo
 * @param matriculasPagadas
 *            matrículas cuya cuota de matrícula quedó pagada por completo con este pago (HU-31)
 */
public record PagoRegistrado(String referencia, UUID usuarioId, List<UUID> cuotaIds, List<UUID> matriculasPagadas) {
}
