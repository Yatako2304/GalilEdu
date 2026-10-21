package com.galiledu.configuracion.dominio;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Bimestre o trimestre de un año escolar (tabla configuracion.periodo_academico, V4). HU-74. Su estado no se guarda:
 * se calcula con las fechas (ver {@link #estadoEn}).
 */
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

	/**
	 * Estado del periodo. En un año PLANIFICADO todos sus periodos son PLANIFICADO y en uno CERRADO todos son CERRADO;
	 * solo en un año ACTIVO se decide por las fechas: PLANIFICADO antes del inicio, EN_CURSO entre el inicio y el fin,
	 * CERRADO después del fin. No se guarda porque nada lo actualizaría a tiempo.
	 */
	public EstadoPeriodo estadoEn(LocalDate hoy, EstadoAnioEscolar estadoAnio) {
		return switch (estadoAnio) {
			case PLANIFICADO -> EstadoPeriodo.PLANIFICADO;
			case CERRADO -> EstadoPeriodo.CERRADO;
			case ACTIVO -> hoy.isBefore(fechaInicio)
					? EstadoPeriodo.PLANIFICADO
					: hoy.isAfter(fechaFin) ? EstadoPeriodo.CERRADO : EstadoPeriodo.EN_CURSO;
		};
	}

	/**
	 * Los docentes registran notas solo en un año ACTIVO, desde que empieza el periodo hasta su fecha límite (que puede
	 * caer dentro del periodo siguiente). Para ampliar el plazo se edita el periodo.
	 */
	public boolean registroNotasAbierto(LocalDate hoy, EstadoAnioEscolar estadoAnio) {
		return estadoAnio == EstadoAnioEscolar.ACTIVO && !hoy.isBefore(fechaInicio) && !hoy.isAfter(fechaLimiteNotas);
	}

	/** Semanas redondeadas, como la columna "Duración" de Figma (02/03 al 08/05 son 68 días: "10 sem."). */
	public int duracionSemanas() {
		return (int) Math.round((ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1) / 7.0);
	}

	/** Solo la usa {@link AnioEscolar#editarPeriodo}, que valida las reglas. */
	void actualizar(String nombre, LocalDate fechaInicio, LocalDate fechaFin, LocalDate fechaLimiteNotas) {
		this.nombre = nombre;
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
		this.fechaLimiteNotas = fechaLimiteNotas;
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
}
