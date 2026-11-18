package com.galiledu.horarios.aplicacion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** Los filtros de fecha del historial de reservas (Figma: «Esta semana», «Este mes», «Este año»). */
public enum FiltroDeReservas {
	/** De lunes a domingo de la semana de hoy. */
	SEMANA,
	/** El mes calendario de hoy. */
	MES,
	/** El año calendario de hoy. */
	ANIO;

	/** Primer día del rango (inclusive) respecto de {@code hoy}. */
	public LocalDate desde(LocalDate hoy) {
		return switch (this) {
			case SEMANA -> hoy.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
			case MES -> hoy.withDayOfMonth(1);
			case ANIO -> hoy.withDayOfYear(1);
		};
	}

	/** Último día del rango (inclusive) respecto de {@code hoy}. */
	public LocalDate hasta(LocalDate hoy) {
		return switch (this) {
			case SEMANA -> hoy.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
			case MES -> hoy.with(TemporalAdjusters.lastDayOfMonth());
			case ANIO -> hoy.with(TemporalAdjusters.lastDayOfYear());
		};
	}
}
