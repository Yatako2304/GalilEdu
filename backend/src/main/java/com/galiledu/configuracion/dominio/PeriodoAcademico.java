package com.galiledu.configuracion.dominio;

import java.time.LocalDate;
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

/** Bimestre o trimestre de un año escolar (tabla configuracion.periodo_academico, V4). HU-74. */
@Entity
@Table(schema = "configuracion", name = "periodo_academico")
public class PeriodoAcademico {
	/**
	 * Días entre el fin del periodo y la fecha límite para registrar notas. Valor por defecto pendiente de confirmar
	 * con el colegio; la BD solo exige fecha_limite_notas >= fecha_fin.
	 */
	static final int DIAS_LIMITE_NOTAS = 5;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "anio_escolar_id", nullable = false, updatable = false)
	private AnioEscolar anioEscolar;

	@Column(nullable = false)
	private short numero;
	@Column(nullable = false)
	private String nombre;
	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;
	@Column(name = "fecha_fin", nullable = false)
	private LocalDate fechaFin;
	@Column(name = "fecha_limite_notas", nullable = false)
	private LocalDate fechaLimiteNotas;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoPeriodo estado = EstadoPeriodo.PLANIFICADO;

	protected PeriodoAcademico() {
	}

	/** Solo la crea {@link AnioEscolar}, que es quien reparte las fechas. */
	PeriodoAcademico(AnioEscolar anioEscolar, int numero, String nombre, LocalDate fechaInicio, LocalDate fechaFin) {
		this.anioEscolar = anioEscolar;
		this.numero = (short) numero;
		this.nombre = nombre;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
		this.fechaLimiteNotas = fechaFin.plusDays(DIAS_LIMITE_NOTAS);
	}

	public UUID getId() {
		return id;
	}
	public int getNumero() {
		return numero;
	}
	public String getNombre() {
		return nombre;
	}
	public LocalDate getFechaInicio() {
		return fechaInicio;
	}
	public LocalDate getFechaFin() {
		return fechaFin;
	}
	public LocalDate getFechaLimiteNotas() {
		return fechaLimiteNotas;
	}
	public EstadoPeriodo getEstado() {
		return estado;
	}
}
