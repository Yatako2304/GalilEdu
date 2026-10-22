package com.galiledu.configuracion.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
	private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

	@OneToMany(mappedBy = "anioEscolar", cascade = CascadeType.ALL, orphanRemoval = true)
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
	 * Quita todos los periodos (solo PLANIFICADO). Quien regenere debe hacer flush antes: Hibernate inserta antes de
	 * borrar y chocaría con uq_periodo_academico_numero.
	 */
	public void quitarPeriodos() {
		exigirPlanificado();
		periodos.clear();
	}

	/** Cambia las fechas del año y regenera sus periodos. El servicio valida el rango (ver {@link #diasMinimos}). */
	public void editarFechas(LocalDate fechaInicio, LocalDate fechaFin) {
		exigirPlanificado();
		this.fechaInicio = fechaInicio;
		this.fechaFin = fechaFin;
		periodos.clear();
		generarPeriodos();
	}

	/** Cambia el régimen y regenera sus periodos con fechas sugeridas. */
	public void cambiarRegimen(Regimen regimen) {
		exigirPlanificado();
		this.regimen = regimen;
		periodos.clear();
		generarPeriodos();
	}

	private void exigirPlanificado() {
		if (estado != EstadoAnioEscolar.PLANIFICADO) {
			throw new IllegalStateException("Solo se puede modificar un año planificado.");
		}
	}

	/** El periodo cuyas fechas incluyen a {@code hoy}; vacío si el año no está ACTIVO. */
	public Optional<PeriodoAcademico> periodoEnCurso(LocalDate hoy) {
		return periodos.stream().filter(p -> p.estadoEn(hoy, estado) == EstadoPeriodo.EN_CURSO).findFirst();
	}

	/**
	 * El periodo YA TERMINADO (fechaFin anterior a hoy) cuyo registro de notas sigue abierto porque su fecha límite es
	 * vigente. Si hay varios, el más antiguo. El periodo en curso no cuenta: se muestra aparte. Vacío si el año no
	 * está ACTIVO.
	 */
	public Optional<PeriodoAcademico> periodoNotasAbiertas(LocalDate hoy) {
		return periodos.stream().filter(p -> p.getFechaFin().isBefore(hoy) && p.registroNotasAbierto(hoy, estado))
				.findFirst();
	}

	/** El primer periodo que aún no empieza; vacío si el año no está ACTIVO o ya no quedan periodos por empezar. */
	public Optional<PeriodoAcademico> proximoPeriodo(LocalDate hoy) {
		if (estado != EstadoAnioEscolar.ACTIVO) {
			return Optional.empty();
		}
		return periodos.stream().filter(p -> p.getFechaInicio().isAfter(hoy)).findFirst();
	}

	/** Porcentaje entero (0 a 100) de días del año ya transcurridos; incluye el día de hoy. */
	public int avance(LocalDate hoy) {
		if (hoy.isBefore(fechaInicio)) {
			return 0;
		}
		if (hoy.isAfter(fechaFin)) {
			return 100;
		}
		long totalDias = ChronoUnit.DAYS.between(fechaInicio, fechaFin) + 1;
		long transcurridos = ChronoUnit.DAYS.between(fechaInicio, hoy) + 1;
		return (int) (transcurridos * 100 / totalDias);
	}

	/**
	 * Edita un periodo. Si ya empezó, su fecha de inicio no cambia; si ya terminó, solo se amplía la fecha límite de
	 * notas. Un año CERRADO no se edita. Las fechas deben estar dentro del año, sin cruzarse con otro periodo, y la
	 * fecha límite puede caer dentro del periodo siguiente.
	 */
	public void editarPeriodo(UUID periodoId, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
			LocalDate fechaLimiteNotas, LocalDate hoy) {
		if (estado == EstadoAnioEscolar.CERRADO) {
			throw new IllegalStateException("El año escolar está cerrado: no se puede editar sus periodos.");
		}
		PeriodoAcademico periodo = periodos.stream().filter(p -> periodoId.equals(p.getId())).findFirst()
				.orElseThrow(() -> new IllegalArgumentException("El periodo no pertenece a este año escolar."));
		if (nombre == null || nombre.isBlank()) {
			throw new IllegalArgumentException("Ingresa el nombre del periodo.");
		}
		if (fechaInicio == null || fechaFin == null || fechaLimiteNotas == null) {
			throw new IllegalArgumentException("Ingresa las fechas del periodo y la fecha límite de notas.");
		}
		String nombreLimpio = nombre.strip();
		if (periodo.getFechaFin().isBefore(hoy)) {
			if (!nombreLimpio.equals(periodo.getNombre()) || !fechaInicio.equals(periodo.getFechaInicio())
					|| !fechaFin.equals(periodo.getFechaFin())) {
				throw new IllegalStateException(
						"El periodo ya terminó: solo se puede cambiar la fecha límite de notas.");
			}
		} else if (!periodo.getFechaInicio().isAfter(hoy) && !fechaInicio.equals(periodo.getFechaInicio())) {
			throw new IllegalStateException("El periodo ya empezó: no se puede cambiar.");
		}
		if (!fechaFin.isAfter(fechaInicio)) {
			throw new IllegalArgumentException("La fecha de fin debe ser posterior a la fecha de inicio.");
		}
		if (fechaInicio.isBefore(this.fechaInicio) || fechaFin.isAfter(this.fechaFin)) {
			throw new IllegalArgumentException("Las fechas del periodo deben estar dentro del año escolar ("
					+ FORMATO_FECHA.format(this.fechaInicio) + " al " + FORMATO_FECHA.format(this.fechaFin) + ").");
		}
		for (PeriodoAcademico otro : periodos) {
			if (otro != periodo && !fechaInicio.isAfter(otro.getFechaFin())
					&& !fechaFin.isBefore(otro.getFechaInicio())) {
				throw new IllegalArgumentException("Se cruza con el " + otro.getNombre() + ", que empieza el "
						+ FORMATO_FECHA.format(otro.getFechaInicio()) + ".");
			}
		}
		if (fechaLimiteNotas.isBefore(fechaFin)) {
			throw new IllegalArgumentException("La fecha límite de notas no puede ser anterior al fin del periodo.");
		}
		periodo.actualizar(nombreLimpio, fechaInicio, fechaFin, fechaLimiteNotas);
	}

	/** Pasa el año de PLANIFICADO a ACTIVO. Que no haya otro año activo y los requisitos los valida el servicio. */
	public void activar() {
		if (estado != EstadoAnioEscolar.PLANIFICADO) {
			throw new IllegalStateException("Solo se puede activar un año planificado.");
		}
		estado = EstadoAnioEscolar.ACTIVO;
	}

	/** Cierra un año ACTIVO una vez vencido el plazo de notas del último periodo. Un año CERRADO no se reabre. */
	public void cerrar(LocalDate hoy) {
		if (estado != EstadoAnioEscolar.ACTIVO) {
			throw new IllegalStateException("Solo se puede cerrar un año activo.");
		}
		LocalDate limite = periodos.getLast().getFechaLimiteNotas();
		if (!hoy.isAfter(limite)) {
			throw new IllegalStateException(
					"Los docentes pueden registrar notas hasta el " + FORMATO_FECHA.format(limite) + ".");
		}
		estado = EstadoAnioEscolar.CERRADO;
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
