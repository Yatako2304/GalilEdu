package com.galiledu.configuracion.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.configuracion.dominio.DatosAreaCurricular;
import com.galiledu.configuracion.dominio.DatosCompetencia;

public interface RepositorioCatalogoCurricular {
	UUID crearArea(DatosAreaCurricular datos);
	Optional<AreaCurricular> buscarArea(UUID id);
	List<AreaCurricular> listarAreas();
	boolean actualizarArea(UUID id, DatosAreaCurricular datos);
	boolean cambiarEstadoArea(UUID id, boolean activo);

	UUID crearCompetencia(DatosCompetencia datos);
	Optional<Competencia> buscarCompetencia(UUID id);
	List<Competencia> listarCompetencias();
	boolean actualizarCompetencia(UUID id, DatosCompetencia datos);
	boolean cambiarEstadoCompetencia(UUID id, boolean activo);

	record AreaCurricular(UUID id, String nombre, String descripcion, boolean activo) {}
	record Competencia(UUID id, UUID areaCurricularId, String nombre, String descripcion, boolean activo) {}
}
