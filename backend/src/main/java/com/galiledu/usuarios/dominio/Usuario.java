package com.galiledu.usuarios.dominio;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.galiledu.personas.dominio.Persona;

/** Cuenta de acceso de una persona (tabla seguridad.usuario, V1). */
@Entity
@Table(schema = "seguridad", name = "usuario")
public class Usuario {
	/** RF-2: 5 intentos fallidos consecutivos bloquean la cuenta 15 minutos. */
	public static final int MAX_INTENTOS = 5;
	public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "persona_id", nullable = false, unique = true)
	private Persona persona;

	@Column(name = "nombre_usuario", nullable = false, unique = true, updatable = false)
	private String nombreUsuario;
	@Column(name = "contrasena_hash", nullable = false)
	private String contrasenaHash;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoUsuario estado = EstadoUsuario.ACTIVO;
	@Column(nullable = false)
	private boolean eliminado;
	@Column(name = "intentos_fallidos", nullable = false)
	private short intentosFallidos;
	@Column(name = "bloqueado_hasta")
	private Instant bloqueadoHasta;
	@Column(name = "ultimo_acceso")
	private Instant ultimoAcceso;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(schema = "seguridad", name = "usuario_rol", joinColumns = @JoinColumn(name = "usuario_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "rol_codigo")
	private Set<Rol> roles = new LinkedHashSet<>();

	protected Usuario() {
	}

	public Usuario(Persona persona, String nombreUsuario, String contrasenaHash, Set<Rol> roles) {
		this.persona = persona;
		this.nombreUsuario = nombreUsuario;
		this.contrasenaHash = contrasenaHash;
		this.roles = new LinkedHashSet<>(roles);
	}

	@PrePersist
	void alCrear() {
		creadoEn = Instant.now();
		actualizadoEn = creadoEn;
	}

	@PreUpdate
	void alActualizar() {
		actualizadoEn = Instant.now();
	}

	public boolean puedeIniciarSesion() {
		return estado == EstadoUsuario.ACTIVO && !eliminado;
	}

	public boolean bloqueadoEn(Instant ahora) {
		return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
	}

	/** Registra un intento fallido y bloquea al llegar al máximo. Devuelve true si quedó bloqueado. */
	public boolean registrarFallo(Instant ahora) {
		if (bloqueadoHasta != null && !bloqueadoHasta.isAfter(ahora)) {
			intentosFallidos = 0;
			bloqueadoHasta = null;
		}
		intentosFallidos++;
		if (intentosFallidos >= MAX_INTENTOS) {
			bloqueadoHasta = ahora.plus(DURACION_BLOQUEO);
			intentosFallidos = 0;
			return true;
		}
		return false;
	}

	public void registrarAcceso(Instant ahora) {
		intentosFallidos = 0;
		bloqueadoHasta = null;
		ultimoAcceso = ahora;
	}

	public void desactivar() {
		estado = EstadoUsuario.INACTIVO;
	}

	public void reactivar() {
		estado = EstadoUsuario.ACTIVO;
		intentosFallidos = 0;
		bloqueadoHasta = null;
	}

	public void eliminar() {
		eliminado = true;
		estado = EstadoUsuario.INACTIVO;
	}

	/**
	 * Vuelve a activar una cuenta eliminada cuando se registra otra vez a la misma persona (mismo documento):
	 * conserva su código de usuario e historial, con contraseña y roles nuevos.
	 */
	public void restaurar(String contrasenaHash, Set<Rol> nuevosRoles) {
		eliminado = false;
		estado = EstadoUsuario.ACTIVO;
		intentosFallidos = 0;
		bloqueadoHasta = null;
		this.contrasenaHash = contrasenaHash;
		reemplazarRoles(nuevosRoles);
	}

	public void agregarRol(Rol rol) {
		roles.add(rol);
	}

	public void reemplazarRoles(Set<Rol> nuevos) {
		roles.clear();
		roles.addAll(nuevos);
	}

	public UUID getId() {
		return id;
	}
	public Persona getPersona() {
		return persona;
	}
	public String getNombreUsuario() {
		return nombreUsuario;
	}
	public String getContrasenaHash() {
		return contrasenaHash;
	}
	public EstadoUsuario getEstado() {
		return estado;
	}
	public boolean isEliminado() {
		return eliminado;
	}
	public Instant getBloqueadoHasta() {
		return bloqueadoHasta;
	}
	public Instant getUltimoAcceso() {
		return ultimoAcceso;
	}
	public Instant getCreadoEn() {
		return creadoEn;
	}
	public Set<Rol> getRoles() {
		return roles;
	}
}
