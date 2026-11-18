package com.galiledu.horarios.aplicacion;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.dominio.DiaSemana;
import com.galiledu.horarios.aplicacion.AmbientesActivos.AmbienteDisponible;
import com.galiledu.horarios.aplicacion.CalendarioDelAnio.AnioConPeriodos;
import com.galiledu.horarios.aplicacion.JornadaDeSeccion.JornadaEstructura;
import com.galiledu.horarios.dominio.ReservaAmbiente;

/**
 * Las reglas de una reserva de espacio especial (HU-48, RF-122), sin Spring ni acceso a datos: quien las usa carga los
 * datos y esta clase decide. Cada regla de forma o de estado lanza {@link ExcepcionNegocio}; los cruces se devuelven
 * como lista para que el servicio responda con el primero.
 * <ul>
 * <li>400, forma: ambiente (1), aforo (2), fecha y hora (3).</li>
 * <li>409, estado: año cerrado (2), reserva finalizada o cancelada (6) y los cruces (4, 5, 7 y 8).</li>
 * </ul>
 * Los cruces comparan horas reales con {@code inicio < otro.fin && otro.inicio < fin}: dos franjas contiguas no se
 * cruzan.
 */
@Component
public class ReglasDeReserva {
	static final String ESPACIO_ESPECIAL = "ESPACIO_ESPECIAL";
	private static final String TITULO_AMBIENTE = "Ambiente no disponible";
	private static final String TITULO_RESPONSABLE = "Responsable no disponible";
	private static final String TITULO_SECCION = "Sección no disponible";

	/** Lo que se quiere reservar. */
	public record DatosDeReserva(UUID ambienteId, UUID seccionId, UUID responsableId, LocalDate fecha,
			LocalTime horaInicio, LocalTime horaFin) {
	}

	/**
	 * Una reserva ACTIVA ya guardada, con los nombres legibles para los mensajes ({@code seccion} es «1.° A»). La
	 * lista que recibe {@link #buscarCruces} puede traer reservas de otras fechas: aquí solo cuentan las de la misma.
	 */
	public record ReservaVigente(UUID id, UUID ambienteId, UUID seccionId, UUID responsableId, LocalDate fecha,
			LocalTime horaInicio, LocalTime horaFin, String seccion, String ambiente, String responsable,
			String actividad) {
	}

	/** Qué regla se incumplió. {@code titulo} es el de la alerta; null si el mensaje se muestra sin título. */
	public enum TipoCruceReserva {
		/** Regla 4: otra reserva activa del mismo ambiente. */
		AMBIENTE_RESERVADO(HttpStatus.CONFLICT, TITULO_AMBIENTE),
		/** Regla 5a: otra sección tiene ese ambiente en su horario semanal. */
		AMBIENTE_EN_HORARIO_DE_OTRA(HttpStatus.CONFLICT, TITULO_AMBIENTE),
		/** Regla 5b: la propia sección ya tiene ese mismo ambiente en su horario (no necesita reserva). */
		MISMO_AMBIENTE_EN_SU_HORARIO(HttpStatus.BAD_REQUEST, null),
		/** Regla 5b: la propia sección tiene clase en otro espacio especial a esa hora. */
		OTRO_ESPACIO_EN_SU_HORARIO(HttpStatus.CONFLICT, null),
		/** Regla 7a: el responsable dicta clase a otra sección a esa hora. */
		RESPONSABLE_CON_CLASE(HttpStatus.CONFLICT, TITULO_RESPONSABLE),
		/** Regla 7b: el responsable ya lo es de otra reserva activa superpuesta. */
		RESPONSABLE_CON_RESERVA(HttpStatus.CONFLICT, TITULO_RESPONSABLE),
		/** Regla 8: la sección ya tiene otra reserva activa superpuesta. */
		SECCION_CON_RESERVA(HttpStatus.CONFLICT, TITULO_SECCION);

		private final HttpStatus estado;
		private final String titulo;

		TipoCruceReserva(HttpStatus estado, String titulo) {
			this.estado = estado;
			this.titulo = titulo;
		}

		public HttpStatus estado() {
			return estado;
		}

		public String titulo() {
			return titulo;
		}
	}

	/** Un cruce. {@code mensaje} no lleva título: {@link #aExcepcion()} lo antepone cuando corresponde. */
	public record CruceDeReserva(TipoCruceReserva tipo, String mensaje) {
		public ExcepcionNegocio aExcepcion() {
			return new ExcepcionNegocio(tipo.estado(), MensajesReserva.conTitulo(tipo.titulo(), mensaje));
		}
	}

	// ---------- Reglas 1, 2, 3 y 6 ----------

	/**
	 * Regla 1: el ambiente debe existir, estar activo y ser un espacio especial. {@code ambiente} es null si no está.
	 */
	public void validarAmbiente(AmbienteDisponible ambiente) {
		if (ambiente == null || !ESPACIO_ESPECIAL.equals(ambiente.tipo())) {
			throw ExcepcionNegocio.invalido("Elige un espacio especial activo.");
		}
	}

	/** Regla 2: con el año cerrado no se reserva (409). */
	public void validarAnio(AnioConPeriodos anio) {
		if ("CERRADO".equals(anio.estado())) {
			throw ExcepcionNegocio.conflicto("No se puede reservar un espacio en un año cerrado.");
		}
	}

	/** Regla 2: los alumnos de la sección caben en el aforo del ambiente. */
	public void validarAforo(AmbienteDisponible ambiente, int matriculados) {
		if (matriculados > ambiente.aforoMaximo()) {
			throw ExcepcionNegocio.invalido("%s admite hasta %d personas y la sección tiene %d matriculados."
					.formatted(ambiente.nombre(), ambiente.aforoMaximo(), matriculados));
		}
	}

	/**
	 * Regla 3: las horas son válidas, la fecha y hora de inicio no son pasadas, la fecha cae en el año y en algún
	 * periodo, es un día lectivo de la estructura de la sección y las horas están dentro de su jornada.
	 */
	public void validarFechaYHora(DatosDeReserva datos, LocalDateTime ahora, AnioConPeriodos anio,
			JornadaEstructura jornada) {
		LocalDate fecha = datos.fecha();
		if (!datos.horaFin().isAfter(datos.horaInicio())) {
			throw ExcepcionNegocio.invalido("La hora de fin debe ser posterior a la de inicio.");
		}
		if (fecha.atTime(datos.horaInicio()).isBefore(ahora)) {
			throw ExcepcionNegocio.invalido("No se puede reservar en una fecha u hora que ya pasó.");
		}
		if (!anio.contiene(fecha)) {
			throw ExcepcionNegocio.invalido("La fecha debe estar dentro del año escolar %d.".formatted(anio.anio()));
		}
		if (anio.periodos().stream().noneMatch(p -> p.contiene(fecha))) {
			throw ExcepcionNegocio.invalido("La fecha no está dentro de ningún periodo académico del año.");
		}
		DiaSemana dia = diaDe(fecha);
		if (dia == null || !jornada.dias().contains(dia)) {
			throw ExcepcionNegocio.invalido("El %s no es un día de clases de esta sección."
					.formatted(MensajesReserva.nombreDelDia(fecha)));
		}
		if (jornada.bloques().isEmpty()) {
			throw ExcepcionNegocio.invalido("La sección no tiene una jornada configurada.");
		}
		LocalTime entrada = jornada.bloques().getFirst().inicio();
		LocalTime salida = jornada.bloques().getLast().fin();
		if (datos.horaInicio().isBefore(entrada) || datos.horaFin().isAfter(salida)) {
			throw ExcepcionNegocio.invalido("La reserva debe estar dentro de la jornada de la sección, de %s a %s."
					.formatted(MensajesReserva.hora(entrada), MensajesReserva.hora(salida)));
		}
	}

	/** Regla 6: una reserva cancelada o finalizada no se edita (409). */
	public void validarEditable(ReservaAmbiente reserva, LocalDateTime ahora) {
		if (reserva.estaCancelada()) {
			throw ExcepcionNegocio.conflicto("La reserva ya fue cancelada.");
		}
		if (reserva.estaFinalizada(ahora)) {
			throw ExcepcionNegocio.conflicto("La reserva ya finalizó y no se puede modificar.");
		}
	}

	// ---------- Reglas 4, 5, 7 y 8 ----------

	/**
	 * Los cruces de la reserva, en el orden de las reglas (4, 5, 7a, 7b, 8).
	 *
	 * @param excluirId
	 *            la reserva que se edita, que no cuenta contra sí misma en 4, 7b y 8; null al crear
	 * @param reservas
	 *            reservas ACTIVAS ya guardadas (solo cuentan las de la misma fecha)
	 * @param bloques
	 *            bloques guardados en los horarios semanales del año, de TODAS las secciones, con las horas de la
	 *            estructura de cada una (solo cuentan los del mismo día de la semana)
	 * @param tipoPorAmbiente
	 *            tipo (AULA_REGULAR o ESPACIO_ESPECIAL) de cada ambiente conocido. Un ambiente que falta (por ejemplo,
	 *            uno ya desactivado) no se trata como espacio especial: la clase de la propia sección en él se permite.
	 */
	public List<CruceDeReserva> buscarCruces(DatosDeReserva datos, UUID excluirId, List<ReservaVigente> reservas,
			List<BloqueOcupado> bloques, Map<UUID, String> tipoPorAmbiente) {
		List<ReservaVigente> delDia = reservas.stream().filter(r -> datos.fecha().equals(r.fecha())
				&& !Objects.equals(r.id(), excluirId) && seSuperponen(datos, r.horaInicio(), r.horaFin())).toList();
		DiaSemana dia = diaDe(datos.fecha());
		List<BloqueOcupado> delHorario = bloques.stream()
				.filter(b -> b.dia() == dia && seSuperponen(datos, b.inicio(), b.fin())).toList();

		List<CruceDeReserva> cruces = new ArrayList<>();
		// 4: otra reserva activa del mismo ambiente.
		delDia.stream().filter(r -> r.ambienteId().equals(datos.ambienteId()))
				.forEach(r -> cruces.add(new CruceDeReserva(TipoCruceReserva.AMBIENTE_RESERVADO,
						MensajesReserva.ambienteReservado(r))));
		// 5: el ambiente frente a los horarios semanales.
		for (BloqueOcupado b : delHorario) {
			boolean propia = b.seccionId().equals(datos.seccionId());
			boolean mismoAmbiente = datos.ambienteId().equals(b.ambienteId());
			if (!propia && mismoAmbiente) {
				cruces.add(new CruceDeReserva(TipoCruceReserva.AMBIENTE_EN_HORARIO_DE_OTRA,
						MensajesCruce.ambiente(b)));
			} else if (propia && mismoAmbiente) {
				cruces.add(new CruceDeReserva(TipoCruceReserva.MISMO_AMBIENTE_EN_SU_HORARIO,
						MensajesReserva.ambienteEnSuHorario(b)));
			} else if (propia && ESPACIO_ESPECIAL.equals(tipoPorAmbiente.get(b.ambienteId()))) {
				cruces.add(new CruceDeReserva(TipoCruceReserva.OTRO_ESPACIO_EN_SU_HORARIO,
						MensajesReserva.otroEspacioEnSuHorario(b)));
			}
			// La propia sección en un aula regular: la clase se traslada al espacio especial.
		}
		// 7a: el responsable dicta clase a OTRA sección (con la propia sí se puede).
		delHorario.stream()
				.filter(b -> !b.seccionId().equals(datos.seccionId()) && datos.responsableId().equals(b.docenteId()))
				.forEach(b -> cruces
						.add(new CruceDeReserva(TipoCruceReserva.RESPONSABLE_CON_CLASE, MensajesCruce.docente(b))));
		// 7b: el responsable ya lo es de otra reserva superpuesta.
		delDia.stream().filter(r -> r.responsableId().equals(datos.responsableId()))
				.forEach(r -> cruces.add(new CruceDeReserva(TipoCruceReserva.RESPONSABLE_CON_RESERVA,
						MensajesReserva.responsableConReserva(r))));
		// 8: la sección ya tiene otra reserva superpuesta.
		delDia.stream().filter(r -> r.seccionId().equals(datos.seccionId()))
				.forEach(r -> cruces.add(new CruceDeReserva(TipoCruceReserva.SECCION_CON_RESERVA,
						MensajesReserva.seccionConReserva(r))));
		return cruces;
	}

	/** El día de la semana de la fecha; null el domingo, que no puede ser lectivo. */
	static DiaSemana diaDe(LocalDate fecha) {
		DayOfWeek dia = fecha.getDayOfWeek();
		return dia == DayOfWeek.SUNDAY ? null : DiaSemana.values()[dia.getValue() - 1];
	}

	private static boolean seSuperponen(DatosDeReserva datos, LocalTime inicio, LocalTime fin) {
		return datos.horaInicio().isBefore(fin) && inicio.isBefore(datos.horaFin());
	}
}
