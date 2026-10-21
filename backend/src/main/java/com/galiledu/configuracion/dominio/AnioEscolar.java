package com.galiledu.configuracion.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Año escolar y sus periodos (tabla configuracion.anio_escolar, V4). HU-74. */
@Entity
@Table(schema = "configuracion", name = "anio_escolar")
public class AnioEscolar {
	/** Cada periodo necesita al menos 2 días para cumplir fecha_fin > fecha_inicio (ck_periodo_academico_fechas). */
	private static final int DIAS_MINIMOS_POR_PERIODO = 2;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, unique = true)
	private short anio;
	@Column(name = "fecha_inicio", nullable = false)
	private LocalDate fechaInicio;
	@Column(name = "fecha_fin", nullable = false)
	private LocalDate fechaFin;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Regimen regimen;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoAnioEscolar estado = EstadoAnioEscolar.PLANIFICADO;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@OneToMany(mappedBy = "anioEscolar", cascade = CascadeType.PERSIST)
	@OrderBy("numero")
	private List<PeriodoAcademico> periodos = new ArrayList<>();

	protected AnioEscolar() {
	}

	/**
	 * Año PLANIFICADO con sus periodos ya repartidos. El servicio valida antes que fechaFin > fechaInicio y que el año
	 * alcance para el régimen (ver {@link #diasMinimos(Regimen)}).
	 */
	public static AnioEscolar planificar(int anio, LocalDate fechaInicio, LocalDate fechaFin, Regimen regimen) {
		AnioEscolar a = new AnioEscolar();
		a.anio = (short) anio;
		a.fechaInicio = fechaInicio;
		a.fechaFin = fechaFin;
		a.regimen = regimen;
		a.generarPeriodos();
		return a;
	}

	public static long diasMinimos(Regimen regimen) {
		return (long) regimen.cantidadPeriodos() * DIAS_MINIMOS_POR_PERIODO;
	}

	/**
	 * Reparte los días del año en partes iguales entre los periodos; los días que sobran van a los primeros. Quedan
	 * seguidos (cada uno empieza el día después de que acaba el anterior) y el último termina en fechaFin, así que no
	 * se cruzan ni se salen del año. Son fechas sugeridas: luego el colegio las ajusta (descansos entre periodos).
	 */
	private void generarPeriodos() {
		int cantidad = regimen.cantidadPeriodos();
		long totalDias = ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
		long base = totalDias / cantidad;
		long sobrantes = totalDias % cantidad;
		LocalDate inicio = fechaInicio;
		for (int numero = 1; numero <= cantidad; numero++) {
			long dias = base + (numero <= sobrantes ? 1 : 0);
			LocalDate fin = inicio.plusDays(dias - 1);
			periodos.add(new PeriodoAcademico(this, numero, regimen.nombrePeriodo(numero), inicio, fin));
			inicio = fin.plusDays(1);
		}
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
	public int getAnio() {
		return anio;
	}
	public LocalDate getFechaInicio() {
		return fechaInicio;
	}
	public LocalDate getFechaFin() {
		return fechaFin;
	}
	public Regimen getRegimen() {
		return regimen;
	}
	public EstadoAnioEscolar getEstado() {
		return estado;
	}
	public List<PeriodoAcademico> getPeriodos() {
		return periodos;
	}
}
