package com.galiledu.configuracion.aplicacion.puertos;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Contract exposed by Institutional Configuration to modules that need a school year's dates/state. */
public interface ConsultaAnioEscolar {
	Optional<AnioEscolar> buscar(UUID id);

	record AnioEscolar(UUID id, LocalDate fechaInicio, LocalDate fechaFin, String estado) {}
}
