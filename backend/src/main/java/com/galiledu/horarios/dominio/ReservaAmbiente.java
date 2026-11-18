package com.galiledu.horarios.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Reserva de un espacio físico especial para una sección en una fecha y una franja horaria (tabla
 * horarios.reserva_ambiente, V17). HU-48.
 * <p>
 * El ambiente, la sección y los usuarios son de otros módulos: solo se guardan sus ids. Las reglas que dependen de
 * otros datos (cruces, aforo, calendario) las valida el servicio; aquí solo están las que se ven en la propia fila.
 * "Eliminar" es {@link #cancelar()}.
 */
@Entity
@Table(schema = "horarios", name = "reserva_ambiente")
public class ReservaAmbiente {
	public static final int LONGITUD_MAXIMA_ACTIVIDAD = 120;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "ambiente_id", nullable = false)
	private UUID ambienteId;
	@Column(name = "seccion_id", nullable = false)
	private UUID seccionId;
	@Column(name = "responsable_id", nullable = false)
	private UUID responsableId;
	@Column(nullable = false)
	private String actividad;
	@Column(nullable = false)
	private LocalDate fecha;
	@Column(name = "hora_inicio", nullable = false)
	private LocalTime horaInicio;
	@Column(name = "hora_fin", nullable = false)
	private LocalTime horaFin;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoReserva estado = EstadoReserva.ACTIVA;
	@Column(name = "creado_por", nullable = false, updatable = false)
	private UUID creadoPor;
	/** Bloqueo optimista: Hibernate la incrementa en cada cambio de la fila. */
	@Version
	@Column(nullable = false)
	private long version;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	protected ReservaAmbiente() {
	}

	/** Reserva ACTIVA. */
	public static ReservaAmbiente crear(UUID ambienteId, UUID seccionId, UUID responsableId, String actividad,
			LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, UUID creadoPor) {
		ReservaAmbiente r = new ReservaAmbiente();
		r.reprogramar(ambienteId, seccionId, responsableId, actividad, fecha, horaInicio, horaFin);
		if (creadoPor == null) {
			throw new IllegalArgumentException("Falta quién registra la reserva.");
		}
		r.creadoPor = creadoPor;
		return r;
	}

	/** Cambia lo que el Coordinador edita: ambiente, sección, responsable, actividad, fecha y horas. */
	public void reprogramar(UUID ambienteId, UUID seccionId, UUID responsableId, String actividad, LocalDate fecha,
			LocalTime horaInicio, LocalTime horaFin) {
		if (ambienteId == null || seccionId == null || responsableId == null || fecha == null || horaInicio == null
				|| horaFin == null) {
			throw new IllegalArgumentException("Faltan datos de la reserva.");
		}
		if (actividad == null || actividad.isBlank() || actividad.strip().length() > LONGITUD_MAXIMA_ACTIVIDAD) {
			throw new IllegalArgumentException("La actividad es obligatoria y admite hasta "
					+ LONGITUD_MAXIMA_ACTIVIDAD + " caracteres.");
		}
		if (!horaFin.isAfter(horaInicio)) {
			throw new IllegalArgumentException("La hora de fin debe ser posterior a la de inicio.");
		}
		this.ambienteId = ambienteId;
		this.seccionId = seccionId;
		this.responsableId = responsableId;
		this.actividad = actividad.strip();
		this.fecha = fecha;
		this.horaInicio = horaInicio;
		this.horaFin = horaFin;
	}

	/** "Eliminar" de la pantalla: la fila se conserva como CANCELADA y deja de contar para los cruces. */
	public void cancelar() {
		this.estado = EstadoReserva.CANCELADA;
	}

	public boolean estaCancelada() {
		return estado == EstadoReserva.CANCELADA;
	}

	/** Finalizada cuando su hora de fin ya llegó. Una reserva cancelada no está finalizada: está cancelada. */
	public boolean estaFinalizada(LocalDateTime ahora) {
		return estado == EstadoReserva.ACTIVA && !fecha.atTime(horaFin).isAfter(ahora);
	}

	@PrePersist
	void alCrear() {
		creadoEn = Instant.now();
		actualizadoEn = creadoEn;
	}

	@PreUpdate
	void alActualizar() {
		actualizadoEn = Instant.now();
	}

	public UUID getId() {
		return id;
	}
	public UUID getAmbienteId() {
		return ambienteId;
	}
	public UUID getSeccionId() {
		return seccionId;
	}
	public UUID getResponsableId() {
		return responsableId;
	}
	public String getActividad() {
		return actividad;
	}
	public LocalDate getFecha() {
		return fecha;
	}
	public LocalTime getHoraInicio() {
		return horaInicio;
	}
	public LocalTime getHoraFin() {
		return horaFin;
	}
	public EstadoReserva getEstado() {
		return estado;
	}
	public UUID getCreadoPor() {
		return creadoPor;
	}
	public long getVersion() {
		return version;
	}
	public Instant getCreadoEn() {
		return creadoEn;
	}
	public Instant getActualizadoEn() {
		return actualizadoEn;
	}
}
