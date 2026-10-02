package com.galiledu.usuarios.aplicacion;

import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioGestionUsuarios;
import com.galiledu.usuarios.aplicacion.puertos.ServicioContrasenas;
import com.galiledu.usuarios.dominio.DatosPersonales;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GestionUsuarios {
	private static final String CARACTERES = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%";
	private static final SecureRandom ALEATORIO = new SecureRandom();
	private static final Set<String> PERFILES = Set.of("ESTUDIANTE", "DOCENTE", "APODERADO");
	private final RepositorioGestionUsuarios repositorio;
	private final ServicioContrasenas contrasenas;

	public GestionUsuarios(RepositorioGestionUsuarios repositorio, ServicioContrasenas contrasenas) {
		this.repositorio = repositorio;
		this.contrasenas = contrasenas;
	}

	@Transactional
	public CuentaCreada crearCuenta(DatosPersonales datos, Set<String> perfiles,
		List<UUID> apoderados, Set<String> roles) {
		Set<String> perfilesValidados = normalizar(perfiles);
		Set<String> rolesValidados = normalizar(roles);
		if (!PERFILES.containsAll(perfilesValidados)) {
			throw new IllegalArgumentException("Perfil de persona no reconocido");
		}
		if (rolesValidados.isEmpty()) {
			throw new IllegalArgumentException("Debe asignar al menos un rol existente");
		}
		List<UUID> vinculos = apoderados == null ? List.of() : List.copyOf(apoderados);
		if (perfilesValidados.contains("ESTUDIANTE") && vinculos.isEmpty()) {
			throw new IllegalArgumentException("El estudiante requiere al menos un apoderado registrado");
		}
		if (!perfilesValidados.contains("ESTUDIANTE") && !vinculos.isEmpty()) {
			throw new IllegalArgumentException("Solo un estudiante puede recibir apoderados");
		}
		String clave = generarContrasena();
		UUID personaId = repositorio.crearPersona(datos, perfilesValidados, vinculos);
		String username = repositorio.crearCuenta(personaId, contrasenas.codificar(clave), rolesValidados);
		return new CuentaCreada(personaId, username, rolesValidados, clave);
	}

	@Transactional(readOnly = true)
	public RepositorioGestionUsuarios.PersonaRegistrada buscarPersona(UUID id) {
		return repositorio.buscarPersona(id).orElseThrow(() -> new NoSuchElementException("Persona no encontrada"));
	}

	@Transactional
	public RepositorioGestionUsuarios.PersonaRegistrada actualizarPersona(UUID id, DatosPersonales datos) {
		if (!repositorio.actualizarPersona(id, datos)) {
			throw new NoSuchElementException("Persona no encontrada");
		}
		return buscarPersona(id);
	}

	@Transactional
	public void desactivarPersona(UUID id) {
		if (!repositorio.desactivarPersona(id)) {
			throw new NoSuchElementException("Persona no encontrada");
		}
	}

	@Transactional
	public void reactivarPersona(UUID id) {
		if (!repositorio.reactivarPersona(id)) {
			throw new NoSuchElementException("Persona inactiva no encontrada");
		}
	}

	@Transactional
	public UUID vincularApoderado(UUID estudianteId, UUID apoderadoId, String parentesco) {
		return repositorio.vincularApoderado(estudianteId, apoderadoId, parentesco);
	}

	private static Set<String> normalizar(Set<String> valores) {
		if (valores == null) return Set.of();
		return valores.stream().map(valor -> {
			if (valor == null || valor.isBlank()) throw new IllegalArgumentException("El rol o perfil no puede estar vacío");
			return valor.strip().toUpperCase(Locale.ROOT);
		}).collect(Collectors.toUnmodifiableSet());
	}

	private static String generarContrasena() {
		StringBuilder valor = new StringBuilder(20);
		for (int i = 0; i < 20; i++) valor.append(CARACTERES.charAt(ALEATORIO.nextInt(CARACTERES.length())));
		return valor.toString();
	}

	public record CuentaCreada(UUID personaId, String nombreUsuario, Set<String> roles, String contrasenaInicial) {}
}
