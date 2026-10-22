package com.galiledu.configuracion.dominio;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Calcula la jornada de un día (HU-44) desde la hora de entrada, los bloques por día, la duración del bloque y los
 * recesos. Los horarios no se guardan: se calculan cada vez. Clase pura, sin Spring ni base de datos.
 * <p>
 * Trabaja en minutos desde las 00:00 y no con {@link LocalTime}: sumar pasada la medianoche daría la vuelta (23:00 más
 * 4 horas serían las 03:00) y una jornada imposible parecería terminar temprano.
 * <p>
 * Supone datos ya validados (bloques de 1 a 12, recesos después de un bloque existente y sin repetirse):
 * {@link EstructuraHoraria#detectarErrores} es quien los valida antes de calcular.
 */
public final class CalculadoraJornada {
	private CalculadoraJornada() {
	}

	public enum TipoFila {
		BLOQUE, RECESO
	}

	/** Una fila de la jornada. {@code numero} es el del bloque (desde 1) y es null en los recesos. */
	public record FilaJornada(TipoFila tipo, Integer numero, int inicioMinutos, int finMinutos) {
		public int duracionMinutos() {
			return finMinutos - inicioMinutos;
		}
	}

	/**
	 * Las filas de un día en orden y {@code salidaMinutos}: el minuto en que termina la jornada, contando un receso
	 * final si lo hubiera (con datos válidos coincide con el fin del último bloque). Se cuenta desde las 00:00 y puede
	 * pasar de 1440.
	 */
	public record Jornada(List<FilaJornada> filas, int salidaMinutos) {
		public Jornada {
			filas = List.copyOf(filas);
		}
	}

	public static Jornada calcular(LocalTime horaEntrada, int bloquesPorDia, int duracionBloqueMin,
			List<DatosReceso> recesos) {
		List<FilaJornada> filas = new ArrayList<>();
		int minutoActual = horaEntrada.getHour() * 60 + horaEntrada.getMinute();
		for (int numeroBloque = 1; numeroBloque <= bloquesPorDia; numeroBloque++) {
			int finDelBloque = minutoActual + duracionBloqueMin;
			filas.add(new FilaJornada(TipoFila.BLOQUE, numeroBloque, minutoActual, finDelBloque));
			minutoActual = finDelBloque;
			for (DatosReceso receso : recesos) {
				if (receso.despuesDeBloque() == numeroBloque) {
					int finDelReceso = minutoActual + receso.duracionMin();
					filas.add(new FilaJornada(TipoFila.RECESO, null, minutoActual, finDelReceso));
					minutoActual = finDelReceso;
				}
			}
		}
		return new Jornada(filas, minutoActual);
	}

	/** "07:45". Pasada la medianoche no da la vuelta: 1560 minutos son "26:00". */
	public static String formatoHora(int minutosDesdeMedianoche) {
		return String.format(Locale.ROOT, "%02d:%02d", minutosDesdeMedianoche / 60, minutosDesdeMedianoche % 60);
	}
}
