package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

/** Stub del contrato de la Fase 0. TODO (Luciana, HU-46): implementar la detección de cruces. */
@Component
public class ValidadorCrucesStub implements ValidadorCruces {
	@Override
	public List<CruceDetectado> buscarCruces(UUID anioId, UUID seccionId, List<BloqueSolicitado> solicitados) {
		return List.of();
	}

	@Override
	public List<CruceDetectado> compararCruces(List<BloqueSolicitado> solicitados, List<BloqueOcupado> ocupados) {
		return List.of();
	}
}
