package com.galiledu.asistencia.dominio;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Un cambio del historial de correcciones (asistencia.correccion, V19): antes, después, quién y cuándo. */
@Entity
@Table(schema = "asistencia", name = "correccion")
public class CorreccionAsistencia {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "asistencia_estudiante_id", nullable = false)
	private UUID asistenciaEstudianteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_anterior", nullable = false)
	private EstadoAsistencia estadoAnterior;

	@Column(name = "hora_anterior")
	private LocalTime horaAnterior;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado_nuevo", nullable = false)
	private EstadoAsistencia estadoNuevo;

	@Column(name = "hora_nueva")
	private LocalTime horaNueva;

	@Column(name = "corregido_por", nullable = false)
	private UUID corregidoPor;

	@Column(name = "corregido_en", nullable = false)
	private OffsetDateTime corregidoEn;

	protected CorreccionAsistencia() {
	}

	CorreccionAsistencia(AsistenciaEstudiante asistencia, MarcaAsistencia nueva, UUID usuarioId,
			OffsetDateTime cuando) {
		this.asistenciaEstudianteId = asistencia.getId();
		this.estadoAnterior = asistencia.getEstado();
		this.horaAnterior = asistencia.getHoraLlegada();
		this.estadoNuevo = nueva.estado();
		this.horaNueva = nueva.horaLlegada();
		this.corregidoPor = usuarioId;
		this.corregidoEn = cuando;
	}

	public EstadoAsistencia getEstadoAnterior() {
		return estadoAnterior;
	}

	public EstadoAsistencia getEstadoNuevo() {
		return estadoNuevo;
	}

	public LocalTime getHoraNueva() {
		return horaNueva;
	}
}
