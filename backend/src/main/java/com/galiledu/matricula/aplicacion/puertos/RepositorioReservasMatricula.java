package com.galiledu.matricula.aplicacion.puertos;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface RepositorioReservasMatricula {
	boolean existe(UUID estudianteId, UUID anioEscolarId);
	int ocupados(UUID seccionId);
	UUID reservar(UUID estudianteId, UUID anioEscolarId, UUID seccionId, LocalDate fechaRegistro);
	Optional<Reserva> buscar(UUID reservaId);

	record Reserva(UUID id, UUID estudianteId, UUID anioEscolarId, UUID seccionId,
		String estado, String tipo, LocalDate fechaRegistro) {}
}
