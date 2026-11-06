package com.galiledu.configuracion.aplicacion;

import java.util.Map;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;

/**
 * Lo que Horarios ya usa de una estructura horaria: antes de reducir sus bloques por día o quitarle un día,
 * Configuración debe saber hasta qué bloque hay cursos asignados. Es de solo lectura. DUEÑA: Valeria (HU-45). Hoy
 * responde
 * {@code UsoDeEstructuraStub}; después tendrá una implementación SQL sin importar nada de {@code horarios}.
 */
public interface UsoDeEstructura {
	/**
	 * Por cada día con cursos asignados, el mayor número de bloque usado en los horarios que usan la estructura. Vacío
	 * si ninguno tiene bloques; los días sin bloques no aparecen. Va en el orden de la semana.
	 */
	Map<DiaSemana, Integer> usoPorDia(UUID estructuraId);

	/**
	 * Si algún horario (cabecera de horarios.horario_seccion) usa la estructura, aunque todavía no tenga bloques: la
	 * clave foránea también impide eliminarla en ese caso.
	 */
	boolean tieneHorarios(UUID estructuraId);
}
