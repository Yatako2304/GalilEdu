package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

import com.galiledu.horarios.aplicacion.DtosHorarios.Referencia;

/**
 * Los usuarios que pueden ser responsables de una reserva (HU-48): activos, no eliminados y con rol DOCENTE o TUTOR.
 * Es de solo lectura. Hoy lo resuelve {@code ResponsablesElegiblesSql}; cuando el módulo de usuarios ofrezca un
 * servicio equivalente, solo se reemplaza esa clase.
 */
public interface ResponsablesElegibles {
	/** Los elegibles, por nombre. */
	List<Referencia> elegibles();

	/** Si el usuario es elegible. */
	boolean existe(UUID usuarioId);
}
