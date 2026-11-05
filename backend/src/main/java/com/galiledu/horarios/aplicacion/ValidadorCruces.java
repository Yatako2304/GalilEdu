package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

/**
 * Detecta los cruces de docente y de ambiente entre secciones del mismo año (HU-46).
 * <p>
 * DUEÑA: Luciana. Hoy responde {@code ValidadorCrucesStub}, que no detecta nada; al implementar, se reemplaza esa
 * clase (o se borra y la nueva se anota con {@code @Service}).
 */
public interface ValidadorCruces {
	/**
	 * Cruces de los bloques solicitados para la sección con lo ya guardado en las demás secciones del año. Pide los
	 * ocupados a {@link OcupacionHorarios} y delega en {@link #compararCruces}.
	 */
	List<CruceDetectado> buscarCruces(UUID anioId, UUID seccionId, List<BloqueSolicitado> solicitados);

	/** Lógica pura, sin acceso a datos: compara los solicitados con los ocupados. */
	List<CruceDetectado> compararCruces(List<BloqueSolicitado> solicitados, List<BloqueOcupado> ocupados);
}
