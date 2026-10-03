package com.galiledu.academica.dominio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Criterio de evaluación de una carga académica que mide una competencia en un periodo. */
public final class ItemEvaluacion {
	private static final BigDecimal PESO_MAXIMO = new BigDecimal("999.99");
	private final UUID id;
	private final UUID cargaAcademicaId;
	private final UUID configuracionCompetenciaId;
	private final UUID periodoAcademicoId;
	private final String nombre;
	private final TipoItemEvaluacion tipo;
	private final LocalDate fechaEvaluacion;
	private final BigDecimal peso;
	private final boolean activo;

	public ItemEvaluacion(UUID id, UUID cargaAcademicaId, UUID configuracionCompetenciaId,
		UUID periodoAcademicoId, String nombre, TipoItemEvaluacion tipo, LocalDate fechaEvaluacion,
		BigDecimal peso, boolean activo) {
		this.id = Objects.requireNonNull(id, "El id del ítem es obligatorio");
		this.cargaAcademicaId = Objects.requireNonNull(cargaAcademicaId, "La carga académica es obligatoria");
		this.configuracionCompetenciaId = Objects.requireNonNull(configuracionCompetenciaId,
			"La competencia es obligatoria");
		this.periodoAcademicoId = Objects.requireNonNull(periodoAcademicoId, "El periodo es obligatorio");
		if (nombre == null || nombre.isBlank() || nombre.strip().length() > 120) {
			throw new IllegalArgumentException("El nombre del ítem es obligatorio y admite hasta 120 caracteres");
		}
		this.nombre = nombre.strip();
		this.tipo = Objects.requireNonNull(tipo, "El tipo de ítem es obligatorio");
		this.fechaEvaluacion = fechaEvaluacion;
		if (peso != null && (peso.signum() < 0 || peso.compareTo(PESO_MAXIMO) > 0)) {
			throw new IllegalArgumentException("El peso debe estar entre 0 y 999.99");
		}
		this.peso = peso;
		this.activo = activo;
	}

	public static ItemEvaluacion nuevo(UUID cargaAcademicaId, UUID configuracionCompetenciaId,
		UUID periodoAcademicoId, String nombre, TipoItemEvaluacion tipo, LocalDate fechaEvaluacion,
		BigDecimal peso) {
		return new ItemEvaluacion(UUID.randomUUID(), cargaAcademicaId, configuracionCompetenciaId,
			periodoAcademicoId, nombre, tipo, fechaEvaluacion, peso, true);
	}

	/** La carga, la competencia y el periodo no cambian; para otro destino se crea otro ítem. */
	public ItemEvaluacion modificar(String nombre, TipoItemEvaluacion tipo, LocalDate fechaEvaluacion,
		BigDecimal peso) {
		return new ItemEvaluacion(id, cargaAcademicaId, configuracionCompetenciaId, periodoAcademicoId,
			nombre, tipo, fechaEvaluacion, peso, activo);
	}

	public ItemEvaluacion desactivar() {
		return new ItemEvaluacion(id, cargaAcademicaId, configuracionCompetenciaId, periodoAcademicoId,
			nombre, tipo, fechaEvaluacion, peso, false);
	}

	public UUID id() {
		return id;
	}

	public UUID cargaAcademicaId() {
		return cargaAcademicaId;
	}

	public UUID configuracionCompetenciaId() {
		return configuracionCompetenciaId;
	}

	public UUID periodoAcademicoId() {
		return periodoAcademicoId;
	}

	public String nombre() {
		return nombre;
	}

	public TipoItemEvaluacion tipo() {
		return tipo;
	}

	public LocalDate fechaEvaluacion() {
		return fechaEvaluacion;
	}

	public BigDecimal peso() {
		return peso;
	}

	public boolean activo() {
		return activo;
	}

	@Override
	public String toString() {
		return "ItemEvaluacion[id=" + id + ", nombre=" + nombre + ", tipo=" + tipo + ", activo=" + activo + "]";
	}
}
