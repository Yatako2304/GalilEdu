package com.galiledu.configuracion.infraestructura.web;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.GestionCatalogoCurricular;
import com.galiledu.configuracion.aplicacion.puertos.RepositorioCatalogoCurricular;
import com.galiledu.configuracion.dominio.DatosAreaCurricular;
import com.galiledu.configuracion.dominio.DatosCompetencia;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/configuracion")
public class CatalogoCurricularController {
	private final GestionCatalogoCurricular gestion;

	public CatalogoCurricularController(GestionCatalogoCurricular gestion) {
		this.gestion = gestion;
	}

	@PostMapping("/areas-curriculares")
	public ResponseEntity<RepositorioCatalogoCurricular.AreaCurricular> crearArea(@RequestBody AreaRequest solicitud) {
		var creada = gestion.crearArea(solicitud.aDominio());
		return ResponseEntity.created(URI.create("/api/configuracion/areas-curriculares/" + creada.id())).body(creada);
	}

	@GetMapping("/areas-curriculares")
	public List<RepositorioCatalogoCurricular.AreaCurricular> listarAreas() {
		return gestion.listarAreas();
	}

	@GetMapping("/areas-curriculares/{id}")
	public RepositorioCatalogoCurricular.AreaCurricular consultarArea(@PathVariable UUID id) {
		return gestion.consultarArea(id);
	}

	@PutMapping("/areas-curriculares/{id}")
	public RepositorioCatalogoCurricular.AreaCurricular actualizarArea(@PathVariable UUID id,
		@RequestBody AreaRequest solicitud) {
		return gestion.actualizarArea(id, solicitud.aDominio());
	}

	@DeleteMapping("/areas-curriculares/{id}")
	public ResponseEntity<Void> desactivarArea(@PathVariable UUID id) {
		gestion.cambiarEstadoArea(id, false);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/areas-curriculares/{id}/reactivacion")
	public ResponseEntity<Void> reactivarArea(@PathVariable UUID id) {
		gestion.cambiarEstadoArea(id, true);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/competencias")
	public ResponseEntity<RepositorioCatalogoCurricular.Competencia> crearCompetencia(@RequestBody CompetenciaRequest solicitud) {
		var creada = gestion.crearCompetencia(solicitud.aDominio());
		return ResponseEntity.created(URI.create("/api/configuracion/competencias/" + creada.id())).body(creada);
	}

	@GetMapping("/competencias")
	public List<RepositorioCatalogoCurricular.Competencia> listarCompetencias() {
		return gestion.listarCompetencias();
	}

	@GetMapping("/competencias/{id}")
	public RepositorioCatalogoCurricular.Competencia consultarCompetencia(@PathVariable UUID id) {
		return gestion.consultarCompetencia(id);
	}

	@PutMapping("/competencias/{id}")
	public RepositorioCatalogoCurricular.Competencia actualizarCompetencia(@PathVariable UUID id,
		@RequestBody CompetenciaRequest solicitud) {
		return gestion.actualizarCompetencia(id, solicitud.aDominio());
	}

	@DeleteMapping("/competencias/{id}")
	public ResponseEntity<Void> desactivarCompetencia(@PathVariable UUID id) {
		gestion.cambiarEstadoCompetencia(id, false);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/competencias/{id}/reactivacion")
	public ResponseEntity<Void> reactivarCompetencia(@PathVariable UUID id) {
		gestion.cambiarEstadoCompetencia(id, true);
		return ResponseEntity.noContent().build();
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorRespuesta> noEncontrado(NoSuchElementException error) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta(error.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorRespuesta> solicitudInvalida(IllegalArgumentException error) {
		return ResponseEntity.badRequest().body(new ErrorRespuesta(error.getMessage()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorRespuesta> conflicto(DataIntegrityViolationException error) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(new ErrorRespuesta("El registro entra en conflicto con datos existentes"));
	}

	public record AreaRequest(String nombre, String descripcion) {
		DatosAreaCurricular aDominio() {
			return new DatosAreaCurricular(nombre, descripcion);
		}
	}

	public record CompetenciaRequest(UUID areaCurricularId, String nombre, String descripcion) {
		DatosCompetencia aDominio() {
			return new DatosCompetencia(areaCurricularId, nombre, descripcion);
		}
	}

	public record ErrorRespuesta(String mensaje) {}
}
