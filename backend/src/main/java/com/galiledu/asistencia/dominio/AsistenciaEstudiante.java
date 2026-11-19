package com.galiledu.asistencia.dominio;

import java.time.LocalTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Estado de un estudiante en un registro diario (asistencia.asistencia_estudiante, V19). */
@Entity
@Table(schema = "asistencia", name = "asistencia_estudiante")
public class AsistenciaEstudiante {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "registro_id")
	private RegistroAsistencia registro;

	@Column(name = "matricula_id", nullable = false)
	private UUID matriculaId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoAsistencia estado;

	@Column(name = "hora_llegada")
	private LocalTime horaLlegada;

	protected AsistenciaEstudiante() {
	}

	AsistenciaEstudiante(RegistroAsistencia registro, MarcaAsistencia marca) {
		this.registro = registro;
		this.matriculaId = marca.matriculaId();
		this.estado = marca.estado();
		this.horaLlegada = marca.horaLlegada();
	}

	void cambiar(MarcaAsistencia marca) {
		this.estado = marca.estado();
		this.horaLlegada = marca.horaLlegada();
	}

	public UUID getId() {
		return id;
	}

	public UUID getMatriculaId() {
		return matriculaId;
	}

	public EstadoAsistencia getEstado() {
		return estado;
	}

	public LocalTime getHoraLlegada() {
		return horaLlegada;
	}
}
