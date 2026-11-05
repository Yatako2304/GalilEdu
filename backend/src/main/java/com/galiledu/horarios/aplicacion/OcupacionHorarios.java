package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Lo que ya ocupan las demás secciones de un año. DUEÑO: Jose (HU-45). Hoy responde {@code OcupacionHorariosStub};
 * después tendrá una implementación SQL de solo lectura.
 */
public interface OcupacionHorarios {
	/**
	 * Los bloques guardados en el año, sin los de {@code excluirSeccionId}. Las horas se calculan con la estructura
	 * horaria de CADA sección, no con la de la sección que se edita.
	 */
	List<BloqueOcupado> bloquesOcupados(UUID anioId, UUID excluirSeccionId);
}
