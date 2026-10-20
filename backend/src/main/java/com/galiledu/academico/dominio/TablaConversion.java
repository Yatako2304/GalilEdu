package com.galiledu.academico.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Versión de la tabla de conversión vigesimal → AD/A/B/C (tablas academico.tabla_conversion y
 * tabla_conversion_nivel, V6). HU-55. Las versiones no se editan: se agrega una nueva con su fecha de vigencia.
 */
@Entity
@Table(schema = "academico", name = "tabla_conversion")
public class TablaConversion {
	public static final int NOTA_MINIMA = 0;
	public static final int NOTA_MAXIMA = 20;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "vigente_desde", nullable = false, unique = true, updatable = false)
	private LocalDate vigenteDesde;
	@Column(name = "creado_por", updatable = false)
	private UUID creadoPor;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "academico", name = "tabla_conversion_nivel", joinColumns = @JoinColumn(name = "tabla_id"))
	private List<RangoNivel> niveles = new ArrayList<>();

	protected TablaConversion() {
	}

	/** Exige que los rangos cubran de 0 a 20 sin vacíos ni cruces, en orden C < B < A < AD. */
	public TablaConversion(LocalDate vigenteDesde, List<RangoNivel> niveles, UUID creadoPor) {
		validarRangos(niveles);
		this.vigenteDesde = vigenteDesde;
		this.niveles = new ArrayList<>(niveles);
		this.creadoPor = creadoPor;
	}

	@PrePersist
	void alCrear() {
		creadoEn = Instant.now();
	}

	/**
	 * Revisa los rangos y lanza IllegalArgumentException con un mensaje para el usuario si no son válidos. C empieza
	 * en 0, cada nivel empieza justo después del anterior y AD termina en 20.
	 */
	public static void validarRangos(List<RangoNivel> niveles) {
		var presentes = EnumSet.noneOf(NivelLogro.class);
		niveles.forEach(n -> presentes.add(n.getNivel()));
		if (niveles.size() != NivelLogro.values().length || !presentes.equals(EnumSet.allOf(NivelLogro.class))) {
			throw new IllegalArgumentException("Define un rango para cada nivel: AD, A, B y C.");
		}
		var ordenados = niveles.stream().sorted(Comparator.comparing(RangoNivel::getNivel)).toList();
		int esperado = NOTA_MINIMA;
		for (RangoNivel n : ordenados) {
			if (n.getNotaDesde() > n.getNotaHasta()) {
				throw new IllegalArgumentException(
						"En %s, la nota \"desde\" no puede ser mayor que \"hasta\".".formatted(n.getNivel()));
			}
			if (n.getNotaDesde() < esperado) {
				throw new IllegalArgumentException(
						"El rango de %s se cruza con el nivel anterior.".formatted(n.getNivel()));
			}
			if (n.getNotaDesde() > esperado) {
				throw new IllegalArgumentException("Falta cubrir de %d a %d antes de %s."
						.formatted(esperado, n.getNotaDesde() - 1, n.getNivel()));
			}
			esperado = n.getNotaHasta() + 1;
		}
		if (esperado - 1 != NOTA_MAXIMA) {
			throw new IllegalArgumentException("El nivel AD debe terminar en 20.");
		}
	}

	/** Niveles de mayor a menor, como la tabla de Figma (AD, A, B, C). */
	public List<RangoNivel> nivelesDeMayorAMenor() {
		return niveles.stream().sorted(Comparator.comparing(RangoNivel::getNivel).reversed()).toList();
	}

	public UUID getId() {
		return id;
	}
	public LocalDate getVigenteDesde() {
		return vigenteDesde;
	}
	public UUID getCreadoPor() {
		return creadoPor;
	}
	public Instant getCreadoEn() {
		return creadoEn;
	}
}
