package com.galiledu.matricula.infraestructura.web;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.UUID;

import com.galiledu.matricula.aplicacion.GestionReservasMatricula;
import com.galiledu.matricula.aplicacion.puertos.RepositorioReservasMatricula;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matriculas/reservas")
public class ReservasMatriculaController {
	private final GestionReservasMatricula gestion;

	public ReservasMatriculaController(GestionReservasMatricula gestion) {
		this.gestion = gestion;
	}

	@PostMapping
	public ResponseEntity<RepositorioReservasMatricula.Reserva> reservar(@RequestBody ReservaRequest solicitud) {
		var reserva = gestion.reservar(solicitud.estudianteId(), solicitud.anioEscolarId(),
			solicitud.seccionId());
		return ResponseEntity.created(URI.create("/api/matriculas/reservas/" + reserva.id())).body(reserva);
	}

	@GetMapping("/{id}")
	public RepositorioReservasMatricula.Reserva consultar(@PathVariable UUID id) {
		return gestion.consultar(id);
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorRespuesta> noEncontrado(NoSuchElementException error) {
		return ResponseEntity.status(404).body(new ErrorRespuesta(error.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorRespuesta> invalido(IllegalArgumentException error) {
		return ResponseEntity.badRequest().body(new ErrorRespuesta(error.getMessage()));
	}

	@ExceptionHandler({IllegalStateException.class, DataIntegrityViolationException.class})
	public ResponseEntity<ErrorRespuesta> conflicto(RuntimeException error) {
		return ResponseEntity.status(409).body(new ErrorRespuesta(
			error instanceof DataIntegrityViolationException
				? "La matrícula entra en conflicto con datos existentes" : error.getMessage()));
	}

	public record ReservaRequest(UUID estudianteId, UUID anioEscolarId, UUID seccionId) {}
	public record ErrorRespuesta(String mensaje) {}
}
