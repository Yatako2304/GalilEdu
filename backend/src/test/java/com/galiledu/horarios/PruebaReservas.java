package com.galiledu.horarios;

import java.util.UUID;

/**
 * Base de las pruebas de reservas (HU-48): agrega a {@link PruebaHorarios} lo que solo ellas necesitan (espacios
 * especiales, usuarios con roles y reservas insertadas por SQL). Todo se deshace al final de cada prueba.
 */
abstract class PruebaReservas extends PruebaHorarios {
	protected static final UUID ANIO_2026 = UUID.fromString("30000000-0000-0000-0000-000000002026");

	/** Un espacio especial con el aforo indicado. */
	protected UUID espacio(String codigo, String estado, int aforo) {
		return jdbc.sql("""
				INSERT INTO configuracion.ambiente (codigo, nombre, tipo, aforo_maximo, estado)
				VALUES (:codigo, :nombre, 'ESPACIO_ESPECIAL', :aforo, :estado) RETURNING id""")
				.param("codigo", codigo).param("nombre", "Espacio " + codigo).param("aforo", aforo)
				.param("estado", estado).query(UUID.class).single();
	}

	protected UUID usuarioPorNombre(String nombreUsuario) {
		return jdbc.sql("SELECT id FROM seguridad.usuario WHERE nombre_usuario = :u").param("u", nombreUsuario)
				.query(UUID.class).single();
	}

	protected UUID coordinador() {
		return usuarioPorNombre(COORDINADOR);
	}

	/** Una sección de 2026 (el año ACTIVO de los datos de prueba), por grado y nombre. */
	protected UUID seccion2026(String grado, String nombre) {
		return jdbc.sql("""
				SELECT id FROM configuracion.seccion
				WHERE anio_escolar_id = :anio AND grado_codigo = :grado AND nombre = :nombre""")
				.param("anio", ANIO_2026).param("grado", grado).param("nombre", nombre).query(UUID.class).single();
	}

	/** Un usuario nuevo con los roles indicados. {@code documento} y {@code nombreUsuario} no deben repetirse. */
	protected UUID usuario(String documento, String nombreUsuario, String estado, boolean eliminado,
			String... roles) {
		UUID persona = jdbc.sql("""
				INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, apellido_materno,
				                              nombres, fecha_nacimiento, nacionalidad, sexo)
				VALUES ('DNI', :documento, 'Prueba', 'Reserva', :nombres, '1980-01-01', 'Peruana', 'MASCULINO')
				RETURNING id""").param("documento", documento).param("nombres", "Usuario " + nombreUsuario)
				.query(UUID.class).single();
		UUID usuario = jdbc.sql("""
				INSERT INTO seguridad.usuario (persona_id, nombre_usuario, contrasena_hash, estado, eliminado)
				VALUES (:persona, :nombreUsuario, 'sin-acceso', :estado, :eliminado) RETURNING id""")
				.param("persona", persona).param("nombreUsuario", nombreUsuario).param("estado", estado)
				.param("eliminado", eliminado).query(UUID.class).single();
		for (String rol : roles) {
			jdbc.sql("INSERT INTO seguridad.usuario_rol (usuario_id, rol_codigo) VALUES (:u, :r)")
					.param("u", usuario).param("r", rol).update();
		}
		return usuario;
	}

	/** Inserta una reserva por SQL, con el estado indicado (ACTIVA o CANCELADA). */
	protected UUID reserva(UUID ambienteId, UUID seccionId, UUID responsableId, String fecha, String inicio,
			String fin, String estado) {
		return jdbc
				.sql("""
						INSERT INTO horarios.reserva_ambiente
						    (ambiente_id, seccion_id, responsable_id, actividad, fecha, hora_inicio, hora_fin, estado, creado_por)
						VALUES (:ambiente, :seccion, :responsable, 'Experimento', CAST(:fecha AS date), CAST(:inicio AS time),
						        CAST(:fin AS time), :estado, :creadoPor)
						RETURNING id""")
				.param("ambiente", ambienteId).param("seccion", seccionId)
				.param("responsable", responsableId).param("fecha", fecha).param("inicio", inicio).param("fin", fin)
				.param("estado", estado).param("creadoPor", coordinador()).query(UUID.class).single();
	}

	/** Un usuario solo con el rol TUTOR que puede iniciar sesión (contraseña de los datos de prueba). */
	protected String tutorConAcceso() {
		UUID persona = jdbc.sql("""
				INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, apellido_materno,
				                              nombres, fecha_nacimiento, nacionalidad, sexo)
				VALUES ('DNI', '79999981', 'Tutor', 'Solo', 'Tomás', '1980-01-01', 'Peruana', 'MASCULINO')
				RETURNING id""").query(UUID.class).single();
		UUID id = jdbc.sql("""
				INSERT INTO seguridad.usuario (persona_id, nombre_usuario, contrasena_hash, estado)
				VALUES (:p, 'U20269981', crypt(:clave, gen_salt('bf', 10)), 'ACTIVO') RETURNING id""")
				.param("p", persona).param("clave", CONTRASENA).query(UUID.class).single();
		jdbc.sql("INSERT INTO seguridad.usuario_rol (usuario_id, rol_codigo) VALUES (:u, 'TUTOR')").param("u", id)
				.update();
		return "U20269981";
	}

	/** Matricula {@code cuantos} alumnos nuevos (estado SECCION_RESERVADA, que ocupa vacante) en la sección de 2027. */
	protected void matricular(UUID seccionId, String grado, int cuantos) {
		for (int i = 0; i < cuantos; i++) {
			UUID persona = jdbc.sql("""
					INSERT INTO personas.persona (tipo_documento, numero_documento, apellido_paterno, apellido_materno,
					                              nombres, fecha_nacimiento, nacionalidad, sexo)
					VALUES ('DNI', :documento, 'Alumno', 'Reserva', 'Prueba', '2013-01-01', 'Peruana', 'FEMENINO')
					RETURNING id""").param("documento", "7888000" + i).query(UUID.class).single();
			UUID estudiante = jdbc.sql("""
					INSERT INTO estudiantes.estudiante (persona_id, pais_nacimiento, lengua_materna, anio_academico,
					                                    grado_codigo, forma_ingreso, modo_salida, estado)
					VALUES (:persona, 'Perú', 'Castellano', 2027, :grado, 'Edad normativa', 'SOLO', 'ACTIVO')
					RETURNING id""").param("persona", persona).param("grado", grado).query(UUID.class).single();
			jdbc.sql("""
					INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, seccion_id,
					                                 tipo_estudiante, estado, creado_por)
					VALUES (:estudiante, :anio, :grado, :seccion, 'NUEVO', 'SECCION_RESERVADA', :creador)""")
					.param("estudiante", estudiante).param("anio", ANIO_2027).param("grado", grado)
					.param("seccion", seccionId).param("creador", coordinador()).update();
		}
	}

	/** Cuerpo del POST: las horas «HH:mm» y la fecha «AAAA-MM-DD». */
	protected static String cuerpoReserva(UUID ambiente, UUID seccion, UUID responsable, String actividad,
			String fecha, String inicio, String fin) {
		return """
				{"ambienteId":"%s","seccionId":"%s","responsableId":"%s","actividad":"%s",
				"fecha":"%s","horaInicio":"%s","horaFin":"%s"}""".formatted(ambiente, seccion, responsable,
				actividad, fecha, inicio, fin);
	}

	/** Cuerpo del PUT: lo mismo, con la versión que devolvió la lectura. */
	protected static String cuerpoEdicion(long version, UUID ambiente, UUID seccion, UUID responsable,
			String actividad, String fecha, String inicio, String fin) {
		String cuerpo = cuerpoReserva(ambiente, seccion, responsable, actividad, fecha, inicio, fin);
		return "{\"version\":" + version + "," + cuerpo.substring(1);
	}
}
