package com.galiledu.academica.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.academica.dominio.CalculadoraNotas;
import com.galiledu.academica.dominio.CalificacionEstudiante;
import com.galiledu.academica.dominio.EvidenciaPedagogica;
import com.galiledu.academica.dominio.TipoItemEvaluacion;

public interface RepositorioCalificaciones {
	/** Bloquea la fila existente para que dos correcciones simultáneas no se pisen. */
	Optional<CalificacionEstudiante> buscarParaActualizar(UUID itemEvaluacionId, UUID matriculaId);

	Optional<CalificacionEstudiante> buscar(UUID id);

	void crear(CalificacionEstudiante calificacion);

	boolean actualizar(CalificacionEstudiante calificacion);

	/** Reemplaza la evidencia anterior: la relación es 0..1 por calificación. */
	void guardarEvidencia(UUID calificacionId, EvidenciaPedagogica evidencia);

	List<CalificacionEstudiante> listarPorItem(UUID itemEvaluacionId);

	/** Calificaciones de ítems activos de una carga, competencia y periodo, para consolidar. */
	List<NotaDeMatricula> listarParaConsolidar(UUID cargaAcademicaId, UUID configuracionCompetenciaId,
		UUID periodoAcademicoId);

	List<CalificacionDetallada> listarPorMatricula(UUID matriculaId);

	record NotaDeMatricula(UUID matriculaId, CalculadoraNotas.NotaItem nota) {}

	record CalificacionDetallada(CalificacionEstudiante calificacion, String item, TipoItemEvaluacion tipo,
		UUID cargaAcademicaId, UUID configuracionCompetenciaId, UUID periodoAcademicoId,
		EvidenciaPedagogica evidencia) {}
}
