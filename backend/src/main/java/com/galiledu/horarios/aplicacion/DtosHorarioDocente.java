package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.DtosHorarios.JornadaDelDia;

/**
 * Contratos JSON del horario de dictado del docente y del tutor (HU-49). Son de solo lectura. Las horas viajan como
 * texto "HH:mm" y los días como LUN…SAB.
 */
public final class DtosHorarioDocente {
	private DtosHorarioDocente() {
	}

	/** Un año escolar. {@code estado} es PLANIFICADO, ACTIVO o CERRADO. */
	public record AnioDelDocente(UUID id, int anio, String estado) {
	}

	/** Un bloque que el docente dicta: curso, sección ("1.° A") y nombre del ambiente. */
	public record ClaseDictada(DiaSemana dia, int numeroBloque, String curso, String seccion, String ambiente) {
	}

	/**
	 * Las clases del docente que siguen una misma estructura horaria, con la jornada de esa estructura. {@code nivel}
	 * es PRIMARIA o SECUNDARIA y {@code titulo} "Primaria" o "Secundaria" (con el código de la estructura si el docente
	 * tiene dos del mismo nivel). Dos clases pueden caer en la misma celda si un cruce quedó guardado: se devuelven las
	 * dos.
	 */
	public record HorarioPorEstructura(String nivel, String titulo, JornadaDelDia jornada, List<ClaseDictada> clases) {
	}

	/**
	 * GET /horarios/docente. {@code anio} es null si el docente no tiene clases en ningún año; {@code horarios} está
	 * vacío si no tiene clases en el año consultado. Primaria va antes que secundaria.
	 */
	public record HorarioDocente(AnioDelDocente anio, List<HorarioPorEstructura> horarios) {
	}
}
