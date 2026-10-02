package com.galiledu.configuracion.aplicacion;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.puertos.RepositorioCatalogoCurricular;
import com.galiledu.configuracion.dominio.DatosAreaCurricular;
import com.galiledu.configuracion.dominio.DatosCompetencia;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionCatalogoCurricular {
	private final RepositorioCatalogoCurricular repositorio;

	public GestionCatalogoCurricular(RepositorioCatalogoCurricular repositorio) {
		this.repositorio = repositorio;
	}

	@Transactional
	public RepositorioCatalogoCurricular.AreaCurricular crearArea(DatosAreaCurricular datos) {
		return consultarArea(repositorio.crearArea(datos));
	}

	@Transactional(readOnly = true)
	public RepositorioCatalogoCurricular.AreaCurricular consultarArea(UUID id) {
		return repositorio.buscarArea(id).orElseThrow(() -> new NoSuchElementException("Área curricular no encontrada"));
	}

	@Transactional(readOnly = true)
	public List<RepositorioCatalogoCurricular.AreaCurricular> listarAreas() {
		return repositorio.listarAreas();
	}

	@Transactional
	public RepositorioCatalogoCurricular.AreaCurricular actualizarArea(UUID id, DatosAreaCurricular datos) {
		if (!repositorio.actualizarArea(id, datos)) throw new NoSuchElementException("Área curricular no encontrada");
		return consultarArea(id);
	}

	@Transactional
	public void cambiarEstadoArea(UUID id, boolean activo) {
		if (!repositorio.cambiarEstadoArea(id, activo)) throw new NoSuchElementException("Área curricular no encontrada");
	}

	@Transactional
	public RepositorioCatalogoCurricular.Competencia crearCompetencia(DatosCompetencia datos) {
		return consultarCompetencia(repositorio.crearCompetencia(datos));
	}

	@Transactional(readOnly = true)
	public RepositorioCatalogoCurricular.Competencia consultarCompetencia(UUID id) {
		return repositorio.buscarCompetencia(id).orElseThrow(() -> new NoSuchElementException("Competencia no encontrada"));
	}

	@Transactional(readOnly = true)
	public List<RepositorioCatalogoCurricular.Competencia> listarCompetencias() {
		return repositorio.listarCompetencias();
	}

	@Transactional
	public RepositorioCatalogoCurricular.Competencia actualizarCompetencia(UUID id, DatosCompetencia datos) {
		if (!repositorio.actualizarCompetencia(id, datos)) throw new NoSuchElementException("Competencia no encontrada");
		return consultarCompetencia(id);
	}

	@Transactional
	public void cambiarEstadoCompetencia(UUID id, boolean activo) {
		if (!repositorio.cambiarEstadoCompetencia(id, activo)) throw new NoSuchElementException("Competencia no encontrada");
	}
}
