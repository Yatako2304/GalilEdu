package com.galiledu.horarios.aplicacion;

import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;

/** Una celda del horario tal como llega del PUT o del endpoint de cruces: aún sin horas ni docente. */
public record CeldaHorario(DiaSemana dia, int numeroBloque, UUID planCursoId, UUID ambienteId) {
}
