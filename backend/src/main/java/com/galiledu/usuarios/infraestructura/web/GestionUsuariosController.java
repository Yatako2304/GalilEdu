package com.galiledu.usuarios.infraestructura.web;

import java.net.URI;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;

import com.galiledu.usuarios.aplicacion.GestionUsuarios;
import com.galiledu.usuarios.aplicacion.SolicitudInvalidaException;
import com.galiledu.usuarios.aplicacion.puertos.RepositorioGestionUsuarios;
import com.galiledu.usuarios.dominio.DatosPersonales;
import com.galiledu.usuarios.dominio.TipoDocumento;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GestionUsuariosController {
	private final GestionUsuarios gestion;

	public GestionUsuariosController(GestionUsuarios gestion) {
		this.gestion = gestion;
	}

	@PostMapping("/usuarios")
	public ResponseEntity<GestionUsuarios.CuentaCreada> crear(@RequestBody CrearCuentaRequest solicitud) {
		if (solicitud == null || solicitud.persona() == null) {
			throw new IllegalArgumentException("Los datos de la persona son obligatorios");
		}
		var resultado = gestion.crearCuenta(solicitud.persona().aDominio(), solicitud.perfiles(),
			solicitud.apoderados(), solicitud.roles());
		return ResponseEntity.created(URI.create("/api/personas/" + resultado.personaId()))
			.cacheControl(CacheControl.noStore()).body(resultado);
	}

	@GetMapping("/personas/{id}")
	public RepositorioGestionUsuarios.PersonaRegistrada consultar(@PathVariable UUID id) {
		return gestion.buscarPersona(id);
	}

	@PutMapping("/personas/{id}")
	public RepositorioGestionUsuarios.PersonaRegistrada actualizar(@PathVariable UUID id,
		@RequestBody PersonaRequest solicitud) {
		if (solicitud == null) throw new IllegalArgumentException("Los datos de la persona son obligatorios");
		return gestion.actualizarPersona(id, solicitud.aDominio());
	}

	@DeleteMapping("/personas/{id}")
	public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
		gestion.desactivarPersona(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/personas/{id}/reactivacion")
	public ResponseEntity<Void> reactivar(@PathVariable UUID id) {
		gestion.reactivarPersona(id);
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/estudiantes/{estudianteId}/apoderados")
	public ResponseEntity<VinculoCreado> vincular(@PathVariable UUID estudianteId,
		@RequestBody VincularApoderadoRequest solicitud) {
		if (solicitud == null || solicitud.apoderadoId() == null) {
			throw new IllegalArgumentException("El apoderado es obligatorio");
		}
		UUID id = gestion.vincularApoderado(estudianteId, solicitud.apoderadoId(), solicitud.parentesco());
		return ResponseEntity.status(HttpStatus.CREATED).body(new VinculoCreado(id));
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorRespuesta> noEncontrado(NoSuchElementException error) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta(error.getMessage()));
	}

	@ExceptionHandler({IllegalArgumentException.class, SolicitudInvalidaException.class})
	public ResponseEntity<ErrorRespuesta> solicitudInvalida(RuntimeException error) {
		return ResponseEntity.badRequest().body(new ErrorRespuesta(error.getMessage()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ErrorRespuesta> conflicto(DataIntegrityViolationException error) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
			.body(new ErrorRespuesta("El registro entra en conflicto con datos existentes"));
	}

	public record CrearCuentaRequest(PersonaRequest persona, Set<String> perfiles,
		List<UUID> apoderados, Set<String> roles) {}

	public record PersonaRequest(String nombres, String primerApellido, String segundoApellido,
		TipoDocumento tipoDocumento, String numeroDocumento, String correoElectronico, String telefono) {
		DatosPersonales aDominio() {
			if (tipoDocumento == null) throw new IllegalArgumentException("El tipo de documento es obligatorio");
			return new DatosPersonales(nombres, primerApellido, segundoApellido, tipoDocumento,
				numeroDocumento, correoElectronico, telefono);
		}
	}

	public record VincularApoderadoRequest(UUID apoderadoId, String parentesco) {}
	public record VinculoCreado(UUID id) {}
	public record ErrorRespuesta(String mensaje) {}
}
