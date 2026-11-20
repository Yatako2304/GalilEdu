package com.galiledu.usuarios.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import com.galiledu.personas.aplicacion.DatosPersona;
import com.galiledu.usuarios.aplicacion.VinculosUsuario.EstudianteACargo;
import com.galiledu.usuarios.aplicacion.VinculosUsuario.SeccionTutoria;
import com.galiledu.usuarios.dominio.EstadoUsuario;
import com.galiledu.usuarios.dominio.Rol;
import com.galiledu.usuarios.dominio.Usuario;

/** Contratos JSON del módulo de usuarios. Deben coincidir con frontend/src/types/api.ts. */
public final class Dtos {
	private Dtos() {
	}

	// ---------- Autenticación (HU-1) ----------

	public record SolicitudLogin(
			@NotBlank(message = "Debe ingresar su usuario.") String username,
			@NotBlank(message = "Debe ingresar su contraseña.") String password) {
	}

	// ---------- Recuperación de contraseña (HU-4) ----------

	public record SolicitudRecuperacion(
			@NotBlank(message = "Ingrese su correo") @Email(message = "Ingrese un correo válido") String email) {
	}

	public record SolicitudVerificarCodigo(
			@NotBlank(message = "Ingrese su correo") String email,
			@NotBlank(message = "Ingrese el código de verificación.") String codigo) {
	}

	public record SolicitudRestablecer(
			@NotBlank(message = "Ingrese su correo") String email,
			@NotBlank(message = "Ingrese el código de verificación.") String codigo,
			@NotBlank(message = "Ingrese su nueva contraseña.") String contrasena) {
	}

	// ---------- Cambio de contraseña (HU-5) ----------

	public record SolicitudVerificarContrasena(
			@NotBlank(message = "Ingrese su contraseña actual.") String contrasenaActual) {
	}

	public record SolicitudCambioContrasena(
			@NotBlank(message = "Ingrese su contraseña actual.") String contrasenaActual,
			@NotBlank(message = "Ingrese su nueva contraseña.") String contrasenaNueva) {
	}

	/** Respuesta con un texto para la pantalla. */
	/** HU-3: código de autorización que entrega la ventana de Google. */
	public record SolicitudGoogle(@NotBlank(message = "Falta el código de Google.") String codigo) {
	}

	/** Métodos de acceso disponibles en la pantalla de inicio de sesión (público). */
	public record MetodosAcceso(boolean google, String googleIdCliente) {
	}

	/** HU-2: estado del interruptor de Google para el administrador. */
	public record ConfiguracionAccesoDto(boolean googleHabilitado, boolean googleConfigurado) {
	}

	public record SolicitudConfiguracionAcceso(
			@NotNull(message = "Indica si el inicio de sesión con Google queda activado.") Boolean googleHabilitado) {
	}

	public record Mensaje(String message) {
	}

	/** Usuario en sesión, con la forma que espera el frontend (types/api.ts → User). */
	public record UsuarioSesion(UUID id, String username, String firstName, String lastName, String email,
			List<Rol> roles, long createdAt) {
		public static UsuarioSesion de(Usuario u) {
			var p = u.getPersona();
			String apellidos = (p.getApellidoPaterno() + " "
					+ (p.getApellidoMaterno() == null ? "" : p.getApellidoMaterno())).strip();
			return new UsuarioSesion(u.getId(), u.getNombreUsuario(), p.getNombres(), apellidos, p.getCorreo(),
					ordenar(u.getRoles()), u.getCreadoEn().toEpochMilli());
		}
	}

	public record RespuestaLogin(String jwt, UsuarioSesion user) {
	}

	// ---------- Apoderado › «Datos de la Familia» (de solo vista) ----------

	/**
	 * Familia de uno o más estudiantes a cargo del apoderado: los hermanos con el mismo padre, madre y apoderado forman
	 * una sola familia. {@code nombre}: apellidos del estudiante ("Paredes Rojas"). {@code apoderado} es null si no se
	 * registró (el estudiante tiene padre o madre presentes).
	 */
	public record FamiliaApoderado(String nombre, List<EstudianteFamilia> estudiantes, IntegranteFamiliar padre,
			IntegranteFamiliar madre, IntegranteFamiliar apoderado, List<ContactoFamilia> contactosEmergencia) {
	}

	/** {@code estado}: INSCRITO, MATRICULADO o RETIRADO. */
	public record EstudianteFamilia(UUID id, String nombreCompleto, String grado, int anioAcademico, String estado) {
	}

	/**
	 * Padre, madre o apoderado. {@code persona} es null si «no registra» (con {@code motivoNoRegistra});
	 * {@code esUsted} marca a quien consulta.
	 */
	public record IntegranteFamiliar(DatosPersona persona, boolean fallecido, String motivoNoRegistra,
			String relacion, boolean esUsted) {
	}

	public record ContactoFamilia(String nombreCompleto, String parentesco, String celular, String telefonoFijo) {
	}

	// ---------- Gestión de usuarios (HU-6, HU-9, HU-12) ----------

	public record SolicitudUsuario(
			@NotNull(message = "Completa los datos personales") @Valid DatosPersona persona,
			@NotEmpty(message = "Asigna al menos un rol") Set<Rol> roles) {
	}

	public record UsuarioResumen(UUID id, String username, String nombre, String nombreCompleto, String correo,
			List<Rol> roles, EstadoUsuario estado, Instant ultimoAcceso) {
		public static UsuarioResumen de(Usuario u) {
			var p = u.getPersona();
			return new UsuarioResumen(u.getId(), u.getNombreUsuario(), p.nombreCorto(), p.nombreCompleto(),
					p.getCorreo(), ordenar(u.getRoles()), u.getEstado(), u.getUltimoAcceso());
		}
	}

	/**
	 * {@code estudiantesACargo} solo si tiene el rol Apoderado y {@code tutorDe} solo si tiene el rol Tutor; si no, van
	 * vacíos.
	 */
	public record UsuarioDetalle(UUID id, String username, EstadoUsuario estado, Instant creadoEn,
			Instant ultimoAcceso, List<Rol> roles, UUID personaId, DatosPersona persona,
			List<EstudianteACargo> estudiantesACargo, List<SeccionTutoria> tutorDe) {
		public static UsuarioDetalle de(Usuario u, List<EstudianteACargo> estudiantesACargo,
				List<SeccionTutoria> tutorDe) {
			return new UsuarioDetalle(u.getId(), u.getNombreUsuario(), u.getEstado(), u.getCreadoEn(),
					u.getUltimoAcceso(), ordenar(u.getRoles()), u.getPersona().getId(),
					DatosPersona.de(u.getPersona()), estudiantesACargo, tutorDe);
		}
	}

	/** Lo que muestra el modal "Usuario registrado" de Figma. La contraseña solo viaja por correo. */
	public record UsuarioCreado(UUID id, String username, String correo, EstadoUsuario estado) {
	}

	// ---------- Carga masiva (HU-8) ----------

	/** Error de una fila del archivo. La fila es la de Excel (el encabezado es la fila 1). */
	public record ErrorFila(int fila, String mensaje) {
	}

	/** Resultado de validar (paso 2) o cargar (paso 3) el archivo. En la validación, creados es 0. */
	public record ResultadoCargaMasiva(String nombreArchivo, int total, int validos, int creados,
			List<ErrorFila> errores) {
	}

	public record RolCatalogo(Rol codigo, String nombre) {
	}

	// ---------- Inicio del administrador ----------

	public record ResumenUsuarios(long total, long activos, long inactivos, AccesoReciente ultimoAcceso,
			List<UsuariosPorRol> porRol, List<UsuarioResumen> cuentasInactivas) {
	}

	public record AccesoReciente(String nombre, Instant fecha) {
	}

	public record UsuariosPorRol(Rol rol, String nombre, long cantidad, List<String> usuarios) {
	}

	static List<Rol> ordenar(Set<Rol> roles) {
		return roles.stream().sorted().toList();
	}
}
