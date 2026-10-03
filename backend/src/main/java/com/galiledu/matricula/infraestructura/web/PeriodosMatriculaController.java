package com.galiledu.matricula.infraestructura.web;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.galiledu.matricula.aplicacion.ConsultarPeriodoMatricula;
import com.galiledu.matricula.aplicacion.GestionPeriodosMatricula;
import com.galiledu.matricula.dominio.PeriodoMatricula;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matriculas/periodos")
public class PeriodosMatriculaController {
	private final GestionPeriodosMatricula gestion;

	public PeriodosMatriculaController(GestionPeriodosMatricula gestion) {
		this.gestion = gestion;
	}

	@PutMapping("/{anioEscolarId}")
	public PeriodoMatricula guardar(@PathVariable UUID anioEscolarId, @RequestBody FechasRequest solicitud) {
		return gestion.guardar(new PeriodoMatricula(anioEscolarId, solicitud.inicio(), solicitud.fin()));
	}

	@GetMapping("/{anioEscolarId}")
	public ConsultarPeriodoMatricula.Resultado consultar(@PathVariable UUID anioEscolarId,
		@RequestParam(required = false) LocalDate fecha) {
		return gestion.consultar(anioEscolarId,
			fecha == null ? LocalDate.now(ZoneId.of("America/Lima")) : fecha);
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorRespuesta> noEncontrado(NoSuchElementException error) {
		return ResponseEntity.status(404).body(new ErrorRespuesta(error.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorRespuesta> invalido(IllegalArgumentException error) {
		return ResponseEntity.badRequest().body(new ErrorRespuesta(error.getMessage()));
	}

	public record FechasRequest(LocalDate inicio, LocalDate fin) {}
	public record ErrorRespuesta(String mensaje) {}
}
