package com.galiledu.academica.dominio;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Tabla vigente de equivalencias vigesimal → AD/A/B/C: un rango por nivel y sin solapes. */
public final class TablaConversion {
	private final List<RangoConversion> rangos;

	public TablaConversion(List<RangoConversion> rangos) {
		Objects.requireNonNull(rangos, "Los rangos son obligatorios");
		List<RangoConversion> ordenados = rangos.stream()
			.sorted(Comparator.comparing(RangoConversion::notaDesde)).toList();
		EnumSet<NivelCualitativo> niveles = EnumSet.noneOf(NivelCualitativo.class);
		for (int i = 0; i < ordenados.size(); i++) {
			if (!niveles.add(ordenados.get(i).valor())) {
				throw new IllegalArgumentException("Cada nivel cualitativo admite un solo rango");
			}
			if (i > 0 && ordenados.get(i).notaDesde().compareTo(ordenados.get(i - 1).notaHasta()) <= 0) {
				throw new IllegalArgumentException("Los rangos de conversión no pueden solaparse");
			}
		}
		if (niveles.size() != NivelCualitativo.values().length) {
			throw new IllegalArgumentException("La tabla debe definir un rango para AD, A, B y C");
		}
		this.rangos = ordenados;
	}

	/** Vacío si la nota cae entre dos rangos: no se inventa una equivalencia. */
	public Optional<NivelCualitativo> convertir(BigDecimal nota) {
		Objects.requireNonNull(nota, "La nota es obligatoria");
		return rangos.stream().filter(rango -> rango.contiene(nota)).map(RangoConversion::valor).findFirst();
	}

	public List<RangoConversion> rangos() {
		return rangos;
	}
}
