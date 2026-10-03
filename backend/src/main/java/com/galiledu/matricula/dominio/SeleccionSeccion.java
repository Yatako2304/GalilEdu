package com.galiledu.matricula.dominio;

import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

/** HU-24/RF-53: use the requested section when possible, otherwise first available by name. */
public final class SeleccionSeccion {
	private SeleccionSeccion() {}

	public static UUID elegir(UUID solicitadaId, List<Opcion> opciones) {
		Objects.requireNonNull(solicitadaId, "La sección solicitada es obligatoria");
		Objects.requireNonNull(opciones, "Las secciones son obligatorias");
		Opcion solicitada = opciones.stream().filter(s -> s.id().equals(solicitadaId))
			.findFirst().orElseThrow(() -> new NoSuchElementException("Sección no disponible para el año escolar"));
		if (solicitada.tieneCupo()) return solicitada.id();
		return opciones.stream().filter(s -> !s.id().equals(solicitadaId))
			.filter(s -> s.gradoId().equals(solicitada.gradoId()))
			.filter(Opcion::tieneCupo)
			.sorted(Comparator.comparing(Opcion::nombre).thenComparing(Opcion::id))
			.map(Opcion::id).findFirst()
			.orElseThrow(() -> new IllegalStateException("No hay cupos disponibles en el grado"));
	}

	public record Opcion(UUID id, UUID gradoId, String nombre, int capacidadMaxima, int ocupados) {
		public Opcion {
			Objects.requireNonNull(id);
			Objects.requireNonNull(gradoId);
			Objects.requireNonNull(nombre);
			if (capacidadMaxima <= 0 || ocupados < 0) {
				throw new IllegalArgumentException("Capacidad u ocupación inválida");
			}
		}

		public boolean tieneCupo() {
			return ocupados < capacidadMaxima;
		}
	}
}
