package com.galiledu.usuarios.web;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.galiledu.compartido.seguridad.ServicioJwt;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.usuarios.aplicacion.Dtos.ResumenUsuarios;
import com.galiledu.usuarios.aplicacion.Dtos.RolCatalogo;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudUsuario;
import com.galiledu.usuarios.aplicacion.Dtos.UsuarioCreado;
import com.galiledu.usuarios.aplicacion.Dtos.UsuarioDetalle;
import com.galiledu.usuarios.aplicacion.Dtos.UsuarioResumen;
import com.galiledu.usuarios.aplicacion.ServicioUsuarios;
import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.Rol;

import jakarta.validation.Valid;

/** Figma > Administrador > Usuarios (HU-6, HU-9, HU-12). Solo el administrador. */
@RestController
@RequestMapping("/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {
	private final ServicioUsuarios servicio;

	public UsuarioController(ServicioUsuarios servicio) {
		this.servicio = servicio;
	}

	/** GET /api/usuarios?q=&rol=&estado=&page=1&size=8 */
	@GetMapping
	public Pagina<UsuarioResumen> listar(
		@RequestParam(name = "q", required = false) String texto,
		@RequestParam(required = false) Rol rol,
		@RequestParam(required = false) EstadoUsuario estado,
		@RequestParam(defaultValue = "1") int page,
		@RequestParam(defaultValue = "8") int size) {
		return servicio.listar(texto, rol, estado, page, size);
	}

	@GetMapping("/resumen")
	public ResumenUsuarios resumen() {
		return servicio.resumen();
	}

	/** Roles que el administrador puede asignar en el formulario (Figma no incluye Estudiante). */
	@GetMapping("/roles")
	public List<RolCatalogo> roles() {
		return Arrays.stream(Rol.values())
			.filter(r -> r != Rol.ESTUDIANTE)
			.map(r -> new RolCatalogo(r, r.nombre()))
			.toList();
	}

	@GetMapping("/{id}")
	public UsuarioDetalle detalle(@PathVariable UUID id) {
		return servicio.detalle(id);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UsuarioCreado crear(@Valid @RequestBody SolicitudUsuario solicitud) {
		return servicio.crear(solicitud);
	}

	@PutMapping("/{id}")
	public UsuarioDetalle actualizar(@PathVariable UUID id, @Valid @RequestBody SolicitudUsuario solicitud) {
		return servicio.actualizar(id, solicitud);
	}

	@PostMapping("/{id}/desactivar")
	public UsuarioDetalle desactivar(@PathVariable UUID id, Authentication autenticacion) {
		return servicio.desactivar(id, ServicioJwt.usuarioActual(autenticacion));
	}

	@PostMapping("/{id}/reactivar")
	public UsuarioDetalle reactivar(@PathVariable UUID id) {
		return servicio.reactivar(id);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void eliminar(@PathVariable UUID id, Authentication autenticacion) {
		servicio.eliminar(id, ServicioJwt.usuarioActual(autenticacion));
	}
}
