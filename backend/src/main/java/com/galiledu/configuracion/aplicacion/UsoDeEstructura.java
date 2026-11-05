package com.galiledu.configuracion.aplicacion;

import java.util.Map;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Lo que Horarios ya usa de una estructura horaria: antes de reducir sus bloques por día o quitarle un día,
 * Configuración debe saber hasta qué bloque hay cursos asignados. Es de solo lectura. DUEÑO: Jose (HU-45). Hoy responde
 * {@code UsoDeEstructuraStub}; después tendrá una implementación SQL sin importar nada de {@code horarios}.
 */
public interface UsoDeEstructura {
	/** Por cada día con cursos asignados, el mayor número de bloque usado. Vacío si la estructura no se usa. */
	Map<DiaSemana, Integer> usoPorDia(UUID estructuraId);
}
