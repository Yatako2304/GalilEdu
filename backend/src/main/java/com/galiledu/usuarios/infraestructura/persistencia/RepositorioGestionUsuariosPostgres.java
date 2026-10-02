package com.galiledu.usuarios.infraestructura.persistencia;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioGestionUsuarios;
import com.galiledu.usuarios.aplicacion.SolicitudInvalidaException;
import com.galiledu.usuarios.dominio.DatosPersonales;
import com.galiledu.usuarios.dominio.TipoDocumento;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioGestionUsuariosPostgres implements RepositorioGestionUsuarios {
	private final JdbcOperations jdbc;

	public RepositorioGestionUsuariosPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public UUID crearPersona(DatosPersonales datos, Set<String> perfiles, List<UUID> apoderados) {
		verificarDuplicados(null, datos);
		UUID id = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO personas.persona
			(id, tipo_documento, numero_documento, nombres, primer_apellido, segundo_apellido, correo, telefono)
			VALUES (?, ?::personas.tipo_documento, ?, ?, ?, ?, ?, ?)
			""", id, datos.tipoDocumento().name(), datos.numeroDocumento(), datos.nombres(),
			datos.primerApellido(), datos.segundoApellido(), datos.correoElectronico(), datos.telefono());
		for (String perfil : perfiles) {
			switch (perfil) {
				case "ESTUDIANTE" -> jdbc.update("INSERT INTO personas.estudiante (id) VALUES (?)", id);
				case "DOCENTE" -> jdbc.update("INSERT INTO personas.docente (id) VALUES (?)", id);
				case "APODERADO" -> jdbc.update("INSERT INTO personas.apoderado (id) VALUES (?)", id);
				default -> throw new IllegalArgumentException("Perfil no reconocido");
			}
		}
		for (UUID apoderadoId : apoderados) vincularApoderado(id, apoderadoId, null);
		return id;
	}

	@Override
	public String crearCuenta(UUID personaId, String hashContrasena, Set<String> roles) {
		int anio = LocalDate.now(ZoneId.of("America/Lima")).getYear();
		Integer correlativo = jdbc.queryForObject("""
			INSERT INTO seguridad.correlativo_usuario_anual (anio, ultimo_correlativo)
			VALUES (?, 1)
			ON CONFLICT (anio) DO UPDATE
			SET ultimo_correlativo = seguridad.correlativo_usuario_anual.ultimo_correlativo + 1
			RETURNING ultimo_correlativo
			""", Integer.class, anio);
		if (correlativo == null || correlativo > 9999) {
			throw new IllegalStateException("Se agotó el correlativo anual de usuarios");
		}
		String username = "U%d%04d".formatted(anio, correlativo);
		UUID usuarioId = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO seguridad.usuario (id, persona_id, username, password_hash)
			VALUES (?, ?, ?, ?)
			""", usuarioId, personaId, username, hashContrasena);
		for (String rol : roles) {
			List<UUID> ids = jdbc.query("""
				SELECT id FROM seguridad.rol WHERE UPPER(nombre) = ? AND activo = TRUE
				""", (rs, row) -> rs.getObject("id", UUID.class), rol);
			if (ids.size() != 1) throw new SolicitudInvalidaException("Rol inexistente o inactivo: " + rol);
			jdbc.update("INSERT INTO seguridad.usuario_rol (usuario_id, rol_id) VALUES (?, ?)", usuarioId, ids.getFirst());
		}
		return username;
	}

	@Override
	public Optional<PersonaRegistrada> buscarPersona(UUID id) {
		List<DatosPersonaPersistidos> filas = jdbc.query("""
			SELECT tipo_documento, numero_documento, nombres, primer_apellido,
			       segundo_apellido, correo, telefono, activo
			FROM personas.persona WHERE id = ?
			""", (rs, row) -> new DatosPersonaPersistidos(
			new DatosPersonales(rs.getString("nombres"), rs.getString("primer_apellido"),
				rs.getString("segundo_apellido"), TipoDocumento.valueOf(rs.getString("tipo_documento")),
				rs.getString("numero_documento"), rs.getString("correo"), rs.getString("telefono")),
			rs.getBoolean("activo")), id);
		if (filas.isEmpty()) return Optional.empty();
		Set<String> perfiles = new HashSet<>();
		if (existeEstudiante(id)) perfiles.add("ESTUDIANTE");
		if (existeDocente(id)) perfiles.add("DOCENTE");
		if (existeApoderado(id)) perfiles.add("APODERADO");
		List<UUID> apoderados = jdbc.query("""
			SELECT apoderado_id FROM personas.vinculo_apoderado WHERE estudiante_id = ? ORDER BY apoderado_id
			""", (rs, row) -> rs.getObject("apoderado_id", UUID.class), id);
		return Optional.of(new PersonaRegistrada(id, filas.getFirst().datos(), filas.getFirst().activa(),
			Set.copyOf(perfiles), apoderados));
	}

	@Override
	public boolean actualizarPersona(UUID id, DatosPersonales datos) {
		if (!existePersonaActiva(id)) return false;
		verificarDuplicados(id, datos);
		return jdbc.update("""
			UPDATE personas.persona SET tipo_documento = ?::personas.tipo_documento,
			    numero_documento = ?, nombres = ?, primer_apellido = ?, segundo_apellido = ?,
			    correo = ?, telefono = ? WHERE id = ? AND activo = TRUE
			""", datos.tipoDocumento().name(), datos.numeroDocumento(), datos.nombres(),
			datos.primerApellido(), datos.segundoApellido(), datos.correoElectronico(), datos.telefono(), id) == 1;
	}

	@Override
	public boolean desactivarPersona(UUID id) {
		int filas = jdbc.update("UPDATE personas.persona SET activo = FALSE WHERE id = ? AND activo = TRUE", id);
		if (filas == 1) jdbc.update("UPDATE seguridad.usuario SET estado = 'INACTIVO' WHERE persona_id = ?", id);
		return filas == 1;
	}

	@Override
	public boolean reactivarPersona(UUID id) {
		int filas = jdbc.update("UPDATE personas.persona SET activo = TRUE WHERE id = ? AND activo = FALSE", id);
		if (filas == 1) {
			int cuentas = jdbc.update("""
				UPDATE seguridad.usuario SET estado = 'ACTIVO', intentos_fallidos = 0,
				    bloqueado_hasta = NULL WHERE persona_id = ? AND estado = 'INACTIVO'
				""", id);
			if (cuentas != 1) throw new IllegalStateException("La persona inactiva no tiene una cuenta reactivable");
		}
		return filas == 1;
	}

	@Override
	public UUID vincularApoderado(UUID estudianteId, UUID apoderadoId, String parentesco) {
		if (!existeEstudianteActivo(estudianteId)) throw new NoSuchElementException("Estudiante no encontrado");
		if (!existeApoderadoActivo(apoderadoId)) throw new NoSuchElementException("Apoderado no encontrado");
		UUID id = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO personas.vinculo_apoderado (id, estudiante_id, apoderado_id, parentesco)
			VALUES (?, ?, ?, ?)
			""", id, estudianteId, apoderadoId, parentesco);
		return id;
	}

	private void verificarDuplicados(UUID id, DatosPersonales datos) {
		int documentos = jdbc.queryForObject("""
			SELECT count(*) FROM personas.persona
			WHERE tipo_documento = ?::personas.tipo_documento AND numero_documento = ?
			AND (?::uuid IS NULL OR id <> ?::uuid)
			""", Integer.class, datos.tipoDocumento().name(), datos.numeroDocumento(), id, id);
		if (documentos > 0) throw new SolicitudInvalidaException("Ya existe una persona con ese documento");
		int correos = jdbc.queryForObject("""
			SELECT count(*) FROM personas.persona WHERE LOWER(correo) = LOWER(?)
			AND (?::uuid IS NULL OR id <> ?::uuid)
			""", Integer.class, datos.correoElectronico(), id, id);
		if (correos > 0) throw new SolicitudInvalidaException("Ya existe una persona con ese correo");
	}

	private boolean existePersonaActiva(UUID id) {
		return Boolean.TRUE.equals(jdbc.queryForObject(
			"SELECT EXISTS (SELECT 1 FROM personas.persona WHERE id = ? AND activo = TRUE)", Boolean.class, id));
	}

	private boolean existeEstudiante(UUID id) {
		return Boolean.TRUE.equals(jdbc.queryForObject(
			"SELECT EXISTS (SELECT 1 FROM personas.estudiante WHERE id = ?)", Boolean.class, id));
	}

	private boolean existeDocente(UUID id) {
		return Boolean.TRUE.equals(jdbc.queryForObject(
			"SELECT EXISTS (SELECT 1 FROM personas.docente WHERE id = ?)", Boolean.class, id));
	}

	private boolean existeApoderado(UUID id) {
		return Boolean.TRUE.equals(jdbc.queryForObject(
			"SELECT EXISTS (SELECT 1 FROM personas.apoderado WHERE id = ?)", Boolean.class, id));
	}

	private boolean existeEstudianteActivo(UUID id) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT EXISTS (SELECT 1 FROM personas.estudiante e JOIN personas.persona p ON p.id = e.id
			WHERE e.id = ? AND p.activo = TRUE)
			""", Boolean.class, id));
	}

	private boolean existeApoderadoActivo(UUID id) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT EXISTS (SELECT 1 FROM personas.apoderado a JOIN personas.persona p ON p.id = a.id
			WHERE a.id = ? AND p.activo = TRUE)
			""", Boolean.class, id));
	}

	private record DatosPersonaPersistidos(DatosPersonales datos, boolean activa) {}
}
