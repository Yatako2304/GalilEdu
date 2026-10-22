package com.galiledu.configuracion.aplicacion;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.galiledu.configuracion.dominio.CalculadoraJornada;
import com.galiledu.configuracion.dominio.CalculadoraJornada.FilaJornada;
import com.galiledu.configuracion.dominio.CalculadoraJornada.Jornada;
import com.galiledu.configuracion.dominio.DatosReceso;
import com.galiledu.configuracion.dominio.DefinicionEstructura;
import com.galiledu.configuracion.dominio.EstructuraHoraria;
import com.galiledu.configuracion.dominio.Receso;

/**
 * Contratos JSON de las estructuras horarias (HU-44). Deben coincidir con frontend/src/types/api.ts. Las horas viajan
 * como texto "HH:mm".
 * <p>
 * Las solicitudes no llevan anotaciones de validación a propósito: la vista previa se usa con el formulario a medio
 * llenar y tiene que responder con la lista de errores, no con un 400. Las reglas viven en {@link EstructuraHoraria}.
 */
public final class DtosEstructurasHorarias {
	private DtosEstructurasHorarias() {
	}

	// ---------- Solicitudes ----------

	/** Un receso del formulario: después de qué bloque va y cuánto dura. Lo que falte se toma como 0 y se rechaza. */
	public record SolicitudReceso(Integer despuesDeBloque, Integer duracionMin) {
		DatosReceso aDatos() {
			return new DatosReceso(valorOCero(despuesDeBloque), valorOCero(duracionMin));
		}
	}

	/**
	 * Cuerpo de crear y de la vista previa ("Modal - Nueva estructura horaria"). El nivel y {@code esPrincipal} no
	 * están en Figma pero la BD los exige. La primera estructura de un nivel nace principal aunque la casilla venga en
	 * false. El año va en la ruta.
	 */
	public record SolicitudEstructuraHoraria(String codigo, String descripcion, String nivel, Boolean esPrincipal,
			List<String> diasLectivos, LocalTime horaEntrada, Integer bloquesPorDia, Integer duracionBloqueMin,
			List<SolicitudReceso> recesos) {
		DefinicionEstructura definicion() {
			return definicionDe(codigo, descripcion, diasLectivos, horaEntrada, bloquesPorDia, duracionBloqueMin,
					recesos);
		}

		boolean pideSerPrincipal() {
			return Boolean.TRUE.equals(esPrincipal);
		}
	}

	/**
	 * Cuerpo de editar: lo mismo que crear, sin {@code nivel} porque no se cambia (para otro nivel se elimina y se crea
	 * de nuevo). Los recesos reemplazan a los anteriores. {@code esPrincipal} es opcional: ausente conserva la marca
	 * que ya tiene, true marca esta como principal (la anterior baja) y false sobre la principal se rechaza (para
	 * cambiar de principal se marca la otra).
	 */
	public record SolicitudEdicionEstructuraHoraria(String codigo, String descripcion, Boolean esPrincipal,
			List<String> diasLectivos, LocalTime horaEntrada, Integer bloquesPorDia, Integer duracionBloqueMin,
			List<SolicitudReceso> recesos) {
		DefinicionEstructura definicion() {
			return definicionDe(codigo, descripcion, diasLectivos, horaEntrada, bloquesPorDia, duracionBloqueMin,
					recesos);
		}
	}

	// ---------- Respuestas ----------

	/** Un receso: después de qué bloque va y cuánto dura. */
	public record RecesoResumen(int despuesDeBloque, int duracionMin) {
		static RecesoResumen de(Receso receso) {
			return new RecesoResumen(receso.getDespuesDeBloque(), receso.getDuracionMin());
		}
	}

	/**
	 * Tarjeta de la lista de "CI-12" y sus indicadores: días lectivos, hora de entrada y de salida, bloques por día y
	 * duración, recesos y bloques por semana. {@code esPrincipal} marca la que se usa para su nivel.
	 */
	public record EstructuraHorariaResumen(UUID id, String codigo, String descripcion, String nivel,
			boolean esPrincipal, List<String> diasLectivos, String horaEntrada, String horaSalida, int bloquesPorDia,
			int duracionBloqueMin, List<RecesoResumen> recesos, int bloquesPorSemana) {
		static EstructuraHorariaResumen de(EstructuraHoraria e) {
			return new EstructuraHorariaResumen(e.getId(), e.getCodigo(), e.getDescripcion(), e.getNivel(),
					e.esPrincipal(), e.getDiasLectivos(), horaDe(e.getHoraEntrada()), horaSalidaDe(e.jornada()),
					e.getBloquesPorDia(), e.getDuracionBloqueMin(), recesosDe(e), e.bloquesPorSemana());
		}
	}

	/**
	 * La lista de "CI-12". {@code anioModificable} es true solo con el año PLANIFICADO: el frontend deshabilita
	 * "+ Nueva", Editar y Eliminar cuando es false.
	 */
	public record EstructurasHorariasDelAnio(boolean anioModificable, List<EstructuraHorariaResumen> estructuras) {
	}

	/** Una fila de la jornada: BLOQUE (con su número) o RECESO (sin número), con inicio, fin y duración. */
	public record FilaJornadaResumen(String tipo, Integer numero, String inicio, String fin, int duracionMin) {
		static FilaJornadaResumen de(FilaJornada fila) {
			return new FilaJornadaResumen(fila.tipo().name(), fila.numero(),
					CalculadoraJornada.formatoHora(fila.inicioMinutos()),
					CalculadoraJornada.formatoHora(fila.finMinutos()), fila.duracionMinutos());
		}
	}

	/**
	 * "CI-12", "Jornada semanal": lo mismo que la tarjeta más la jornada de un día, calculada (no se guarda), con los
	 * bloques y recesos en orden. {@code jornada} es igual cada día lectivo.
	 */
	public record DetalleEstructuraHoraria(UUID id, String codigo, String descripcion, String nivel,
			boolean esPrincipal, List<String> diasLectivos, String horaEntrada, String horaSalida, int bloquesPorDia,
			int duracionBloqueMin, List<RecesoResumen> recesos, int bloquesPorSemana, boolean anioModificable,
			List<FilaJornadaResumen> jornada) {
		static DetalleEstructuraHoraria de(EstructuraHoraria e, boolean anioModificable) {
			EstructuraHorariaResumen r = EstructuraHorariaResumen.de(e);
			return new DetalleEstructuraHoraria(r.id(), r.codigo(), r.descripcion(), r.nivel(), r.esPrincipal(),
					r.diasLectivos(), r.horaEntrada(), r.horaSalida(), r.bloquesPorDia(), r.duracionBloqueMin(),
					r.recesos(), r.bloquesPorSemana(), anioModificable, filasDe(e.jornada()));
		}
	}

	/**
	 * Respuesta de crear y editar: la estructura guardada y las advertencias para mostrar al usuario (por ejemplo, que
	 * cambia la principal o que la planificación actual hay que revisarla). Lista vacía si no hay nada que avisar.
	 */
	public record EstructuraGuardada(DetalleEstructuraHoraria estructura, List<String> advertencias) {
	}

	/** La jornada calculada de la vista previa: sus filas, la hora de salida y los bloques por semana. */
	public record JornadaCalculada(List<FilaJornadaResumen> filas, String horaSalida, int bloquesPorSemana) {
	}

	/**
	 * "Vista previa de la jornada" del modal. {@code errores} trae todos los problemas del formulario y {@code valida}
	 * es true si no hay ninguno. {@code jornada} viene aunque la jornada pase de las 18:00 (para mostrar la salida
	 * real) y es null si faltan datos para calcularla.
	 */
	public record VistaPreviaJornada(boolean valida, List<String> errores, JornadaCalculada jornada) {
	}

	// ---------- Apoyo ----------

	private static int valorOCero(Integer valor) {
		return valor == null ? 0 : valor;
	}

	private static DefinicionEstructura definicionDe(String codigo, String descripcion, List<String> diasLectivos,
			LocalTime horaEntrada, Integer bloquesPorDia, Integer duracionBloqueMin, List<SolicitudReceso> recesos) {
		List<DatosReceso> datosDeRecesos = new ArrayList<>();
		if (recesos != null) {
			for (SolicitudReceso receso : recesos) {
				if (receso != null) {
					datosDeRecesos.add(receso.aDatos());
				}
			}
		}
		return new DefinicionEstructura(codigo, descripcion, diasLectivos, horaEntrada, valorOCero(bloquesPorDia),
				valorOCero(duracionBloqueMin), datosDeRecesos);
	}

	static String horaDe(LocalTime hora) {
		return CalculadoraJornada.formatoHora(hora.getHour() * 60 + hora.getMinute());
	}

	static String horaSalidaDe(Jornada jornada) {
		return CalculadoraJornada.formatoHora(jornada.salidaMinutos());
	}

	static List<FilaJornadaResumen> filasDe(Jornada jornada) {
		List<FilaJornadaResumen> filas = new ArrayList<>();
		for (FilaJornada fila : jornada.filas()) {
			filas.add(FilaJornadaResumen.de(fila));
		}
		return filas;
	}

	private static List<RecesoResumen> recesosDe(EstructuraHoraria e) {
		List<RecesoResumen> recesos = new ArrayList<>();
		for (Receso receso : e.getRecesos()) {
			recesos.add(RecesoResumen.de(receso));
		}
		return recesos;
	}
}
