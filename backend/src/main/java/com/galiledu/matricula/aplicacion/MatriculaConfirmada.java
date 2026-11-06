package com.galiledu.matricula.aplicacion;

import java.util.UUID;

/**
 * Evento que Matrícula publica cuando un estudiante queda MATRICULADO (HU-31). Pagos lo escucha para generar el
 * cronograma de pensiones sin que Matrícula dependa de él.
 */
public record MatriculaConfirmada(UUID matriculaId, UUID usuarioId) {
}
