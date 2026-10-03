package com.galiledu.academica.aplicacion.puertos;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.galiledu.academica.dominio.NotaConsolidada;

public interface RepositorioNotasConsolidadas {
	/** Crea o recalcula la nota final de la competencia y devuelve su id. */
	UUID guardarFinal(UUID matriculaId, UUID configuracionCompetenciaId, NotaConsolidada nota, Instant ahora);

	/** Crea o recalcula la nota del periodo dentro de su nota final. */
	void guardarPeriodo(UUID notaFinalId, UUID matriculaId, UUID periodoAcademicoId, NotaConsolidada nota,
		Instant ahora);

	List<NotaPeriodoGuardada> listarPeriodos(UUID matriculaId, UUID configuracionCompetenciaId);

	List<NotaPeriodoGuardada> listarPeriodosDeMatricula(UUID matriculaId);

	List<NotaFinalGuardada> listarFinales(UUID matriculaId);

	record NotaPeriodoGuardada(UUID configuracionCompetenciaId, UUID periodoAcademicoId, NotaConsolidada nota,
		Instant fechaCalculo) {}

	record NotaFinalGuardada(UUID id, UUID configuracionCompetenciaId, NotaConsolidada nota, Instant fechaCalculo) {}
}
