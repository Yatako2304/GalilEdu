package com.galiledu.usuarios.aplicacion;

import static com.galiledu.compartido.web.ExcepcionNegocio.exigir;

import java.time.Year;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.compartido.web.Pagina;
import com.galiledu.personas.aplicacion.DatosPersona;
import com.galiledu.personas.aplicacion.ServicioPersonas;
import com.galiledu.personas.dominio.Persona;
import com.galiledu.usuarios.aplicacion.Dtos.AccesoReciente;
import com.galiledu.usuarios.aplicacion.Dtos.ResumenUsuarios;
import com.galiledu.usuarios.aplicacion.Dtos.SolicitudUsuario;
import com.galiledu.usuarios.aplicacion.Dtos.UsuarioCreado;
import com.galiledu.usuarios.aplicacion.Dtos.UsuarioDetalle;
import com.galiledu.usuarios.aplicacion.Dtos.UsuarioResumen;
import com.galiledu.usuarios.aplicacion.Dtos.UsuariosPorRol;
import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.GeneradorCredenciales;
import com.galiledu.usuarios.dominio.Rol;
import com.galiledu.usuarios.dominio.Usuario;
import com.galiledu.usuarios.infraestructura.CorrelativoUsuarios;
import com.galiledu.usuarios.infraestructura.NotificadorCredenciales;
import com.galiledu.usuarios.infraestructura.UsuarioRepositorio;

/** Gestión de cuentas por el administrador: HU-6 (registro), HU-9 (edición), HU-12 (desactivación). */
@Service
public class ServicioUsuarios {
	private static final ZoneId LIMA = ZoneId.of("America/Lima");
	/** Roles que no se asignan desde el formulario de usuario, con el motivo que se muestra. */
	private static final Map<Rol, String> ROLES_AUTOMATICOS = Map.of(
			Rol.APODERADO, "se asigna al inscribir a un estudiante",
			Rol.TUTOR, "se asigna al designarlo tutor de una sección",
			Rol.ESTUDIANTE, "no se asigna en esta iteración");

	private final UsuarioRepositorio usuarios;
	private final ServicioPersonas personas;
	private final CorrelativoUsuarios correlativo;
	private final PasswordEncoder contrasenas;
	private final NotificadorCredenciales notificador;
	private final VinculosUsuario vinculos;

	public ServicioUsuarios(UsuarioRepositorio usuarios, ServicioPersonas personas, CorrelativoUsuarios correlativo,
			PasswordEncoder contrasenas, NotificadorCredenciales notificador, VinculosUsuario vinculos) {
		this.usuarios = usuarios;
		this.personas = personas;
		this.correlativo = correlativo;
		this.contrasenas = contrasenas;
		this.notificador = notificador;
		this.vinculos = vinculos;
	}

	@Transactional(readOnly = true)
	public Pagina<UsuarioResumen> listar(String texto, Rol rol, EstadoUsuario estado, int pagina, int tamano) {
		Specification<Usuario> filtro = (raiz, consulta, cb) -> {
			List<Predicate> condiciones = new ArrayList<>();
			condiciones.add(cb.isFalse(raiz.get("eliminado")));
			if (texto != null && !texto.isBlank()) {
				var persona = raiz.join("persona");
				Expression<String> nombre = cb.lower(cb.concat(cb.concat(cb.concat(persona.get("nombres"), " "),
						cb.concat(persona.get("apellidoPaterno"), " ")),
						cb.coalesce(persona.<String>get("apellidoMaterno"), "")));
				String patron = "%" + texto.strip().toLowerCase() + "%";
				condiciones.add(cb.or(
						cb.like(nombre, patron),
						cb.like(cb.lower(cb.coalesce(persona.<String>get("correo"), "")), patron),
						cb.like(cb.lower(raiz.get("nombreUsuario")), patron)));
			}
			if (rol != null) {
				Join<Usuario, Rol> roles = raiz.join("roles");
				condiciones.add(cb.equal(roles, rol));
				consulta.distinct(true);
			}
			if (estado != null) {
				condiciones.add(cb.equal(raiz.get("estado"), estado));
			}
			return cb.and(condiciones.toArray(Predicate[]::new));
		};
		var orden = Sort.by("creadoEn").ascending().and(Sort.by("nombreUsuario"));
		var resultado = usuarios.findAll(filtro,
				PageRequest.of(Math.max(0, pagina - 1), Math.clamp(tamano, 1, 100), orden));
		return Pagina.de(resultado, UsuarioResumen::de);
	}

	@Transactional(readOnly = true)
	public UsuarioDetalle detalle(UUID id) {
		return detalleDe(obtener(id));
	}

	/**
	 * HU-6: crea persona + cuenta, genera usuario y contraseña y los envía por correo (RF-15, RF-16, RF-17). Exige los
	 * datos del formulario "Nuevo usuario" (CAHU6) y solo los roles que se asignan desde ahí.
	 */
	@Transactional
	public UsuarioCreado crear(SolicitudUsuario solicitud) {
		solicitud.persona().exigirCompletos();
		return alta(solicitud, rolesDelFormulario(EnumSet.noneOf(Rol.class), solicitud.roles()));
	}

	/**
	 * Alta de la carga masiva (HU-8): solo exige correo y celular, sin las reglas de {@link #crear}. La carga masiva
	 * está oculta y no se contempla en esta iteración.
	 */
	@Transactional
	UsuarioCreado crearDesdeCarga(SolicitudUsuario solicitud) {
		exigirContacto(solicitud);
		return alta(solicitud, EnumSet.copyOf(solicitud.roles()));
	}

	private UsuarioCreado alta(SolicitudUsuario solicitud, Set<Rol> roles) {
		Optional<Usuario> eliminado = personas.buscarPorDocumento(solicitud.persona().numeroDocumento())
				.flatMap(p -> usuarios.findByPersonaId(p.getId()))
				.filter(Usuario::isEliminado);
		if (eliminado.isPresent()) {
			return restaurar(eliminado.get(), solicitud.persona(), roles);
		}
		Persona persona = personas.crear(solicitud.persona());
		var creado = crearCuenta(persona, roles);
		return new UsuarioCreado(creado.getId(), creado.getNombreUsuario(), persona.getCorreo(), creado.getEstado());
	}

	/**
	 * Crea la cuenta de una persona ya registrada (también lo usa la inscripción para el responsable de matrícula).
	 * Envía las credenciales al correo de la persona.
	 */
	@Transactional
	public Usuario crearCuenta(Persona persona, Set<Rol> roles) {
		if (usuarios.existsByPersonaId(persona.getId())) {
			throw ExcepcionNegocio.conflicto("La persona ya tiene una cuenta de usuario");
		}
		if (persona.getCorreo() == null) {
			throw ExcepcionNegocio.invalido("La persona necesita un correo para recibir sus credenciales");
		}
		int anio = Year.now(LIMA).getValue();
		String nombreUsuario = GeneradorCredenciales.nombreUsuario(anio, correlativo.siguiente(anio));
		String contrasena = GeneradorCredenciales.contrasena();
		Usuario usuario = usuarios.save(new Usuario(persona, nombreUsuario, contrasenas.encode(contrasena), roles));
		notificador.enviar(persona.getCorreo(), persona.getNombres(), nombreUsuario, contrasena);
		return usuario;
	}

	/** Agrega un rol a la cuenta de la persona; si no tiene cuenta, la crea. Devuelve la cuenta. */
	@Transactional
	public Usuario asegurarRol(Persona persona, Rol rol) {
		return usuarios.findByPersonaId(persona.getId())
				.map(u -> {
					u.agregarRol(rol);
					return u;
				})
				.orElseGet(() -> crearCuenta(persona, EnumSet.of(rol)));
	}

	@Transactional(readOnly = true)
	public boolean tieneCuenta(UUID personaId) {
		return usuarios.existsByPersonaId(personaId);
	}

	/** Si la persona tiene una cuenta eliminada (al registrar de nuevo su documento, se reactiva). */
	public boolean cuentaEliminada(UUID personaId) {
		return usuarios.findByPersonaId(personaId).map(Usuario::isEliminado).orElse(false);
	}

	/** HU-9: edición de datos personales y roles (RF-22, RF-23). */
	@Transactional
	public UsuarioDetalle actualizar(UUID id, SolicitudUsuario solicitud) {
		solicitud.persona().exigirCompletos();
		Usuario usuario = obtener(id);
		Set<Rol> roles = rolesDelFormulario(usuario.getRoles(), solicitud.roles());
		personas.actualizar(usuario.getPersona(), solicitud.persona());
		usuario.reemplazarRoles(roles);
		return detalleDe(usuario);
	}

	/** HU-12: el usuario ya no puede iniciar sesión, pero se conserva su información. */
	@Transactional
	public UsuarioDetalle desactivar(UUID id, UUID solicitante) {
		evitarSobreSiMismo(id, solicitante, "desactivar");
		Usuario usuario = obtener(id);
		usuario.desactivar();
		return detalleDe(usuario);
	}

	@Transactional
	public UsuarioDetalle reactivar(UUID id) {
		Usuario usuario = obtener(id);
		usuario.reactivar();
		return detalleDe(usuario);
	}

	/** "Eliminar" de Figma: borrado lógico. La cuenta deja de listarse y no puede iniciar sesión. */
	@Transactional
	public void eliminar(UUID id, UUID solicitante) {
		evitarSobreSiMismo(id, solicitante, "eliminar");
		Usuario usuario = obtener(id);
		usuario.eliminar();
		// Libera el correo para que pueda usarse en otro registro (el correo es único entre personas).
		usuario.getPersona().setCorreo(null);
	}

	/**
	 * Registrar de nuevo el documento de un usuario eliminado: se actualizan sus datos y se reactiva la misma cuenta
	 * (mismo código de usuario) con roles y contraseña nuevos. Para quien registra es un alta normal.
	 */
	private UsuarioCreado restaurar(Usuario usuario, DatosPersona datos, Set<Rol> roles) {
		Persona persona = personas.actualizar(usuario.getPersona(), datos);
		String contrasena = GeneradorCredenciales.contrasena();
		usuario.restaurar(contrasenas.encode(contrasena), roles);
		notificador.enviar(persona.getCorreo(), persona.getNombres(), usuario.getNombreUsuario(), contrasena);
		return new UsuarioCreado(usuario.getId(), usuario.getNombreUsuario(), persona.getCorreo(), usuario.getEstado());
	}

	@Transactional(readOnly = true)
	public ResumenUsuarios resumen() {
		List<Usuario> todos = usuarios.findAll().stream().filter(u -> !u.isEliminado()).toList();
		long activos = todos.stream().filter(u -> u.getEstado() == EstadoUsuario.ACTIVO).count();
		AccesoReciente ultimo = todos.stream()
				.filter(u -> u.getUltimoAcceso() != null)
				.max(Comparator.comparing(Usuario::getUltimoAcceso))
				.map(u -> new AccesoReciente(u.getPersona().nombreCorto(), u.getUltimoAcceso()))
				.orElse(null);
		List<UsuariosPorRol> porRol = new ArrayList<>();
		for (Rol rol : Rol.values()) {
			List<String> nombres = todos.stream()
					.filter(u -> u.getRoles().contains(rol))
					.map(u -> u.getPersona().nombreCorto()
							+ (u.getEstado() == EstadoUsuario.INACTIVO ? " (inactivo)" : ""))
					.toList();
			if (!nombres.isEmpty()) {
				porRol.add(new UsuariosPorRol(rol, rol.nombre(), nombres.size(), nombres));
			}
		}
		List<UsuarioResumen> inactivos = todos.stream()
				.filter(u -> u.getEstado() == EstadoUsuario.INACTIVO)
				.map(UsuarioResumen::de)
				.toList();
		return new ResumenUsuarios(todos.size(), activos, todos.size() - activos, ultimo, porRol, inactivos);
	}

	/** Detalle con los estudiantes a cargo (si es Apoderado) y las secciones de tutoría (si es Tutor). */
	private UsuarioDetalle detalleDe(Usuario u) {
		return UsuarioDetalle.de(u,
				u.getRoles().contains(Rol.APODERADO) ? vinculos.estudiantesACargo(u.getPersona().getId()) : List.of(),
				u.getRoles().contains(Rol.TUTOR) ? vinculos.tutorDe(u.getId()) : List.of());
	}

	private Usuario obtener(UUID id) {
		return usuarios.findById(id)
				.filter(u -> !u.isEliminado())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El usuario no existe"));
	}

	private static void exigirContacto(SolicitudUsuario solicitud) {
		exigir(solicitud.persona().correo(), "Ingresa el correo electrónico");
		exigir(solicitud.persona().celular(), "Ingresa el celular");
	}

	/**
	 * Roles que se guardan desde el formulario. Apoderado, Tutor y Estudiante no se asignan ahí (CAHU6-33 y 34): no se
	 * pueden agregar, y los que el usuario ya tiene se conservan aunque no vengan en la solicitud.
	 */
	private static Set<Rol> rolesDelFormulario(Set<Rol> actuales, Set<Rol> pedidos) {
		Set<Rol> roles = EnumSet.noneOf(Rol.class);
		for (Rol rol : pedidos) {
			if (ROLES_AUTOMATICOS.containsKey(rol) && !actuales.contains(rol)) {
				throw ExcepcionNegocio.invalido("El rol " + rol.nombre() + " " + ROLES_AUTOMATICOS.get(rol));
			}
			roles.add(rol);
		}
		actuales.stream().filter(ROLES_AUTOMATICOS::containsKey).forEach(roles::add);
		return roles;
	}

	private static void evitarSobreSiMismo(UUID id, UUID solicitante, String accion) {
		if (Objects.equals(id, solicitante)) {
			throw ExcepcionNegocio.invalido("No puedes " + accion + " tu propia cuenta");
		}
	}
}
