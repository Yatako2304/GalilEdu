package com.galiledu.configuracion.infraestructura.persistencia;

import java.util.List;
import java.util.UUID;

import com.galiledu.configuracion.aplicacion.puertos.SeccionesParaMatricula;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class SeccionesParaMatriculaPostgres implements SeccionesParaMatricula {
	private final JdbcOperations jdbc;

	public SeccionesParaMatriculaPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public List<Seccion> bloquearOpciones(UUID anioEscolarId, UUID seccionSolicitadaId) {
		List<UUID> grados = jdbc.query("""
			SELECT s.grado_id FROM configuracion.seccion s
			JOIN configuracion.anio_escolar a ON a.id = s.anio_id
			JOIN configuracion.grado g ON g.id = s.grado_id
			WHERE s.id = ? AND s.anio_id = ? AND s.activo = TRUE
			  AND s.estado = 'ACTIVA'::configuracion.estado_seccion
			  AND a.activo = TRUE AND g.activo = TRUE
			""", (rs, row) -> rs.getObject("grado_id", UUID.class),
			seccionSolicitadaId, anioEscolarId);
		if (grados.isEmpty()) return List.of();
		return jdbc.query("""
			SELECT s.id, s.grado_id, s.nombre, s.capacidad_maxima
			FROM configuracion.seccion s
			WHERE s.anio_id = ? AND s.grado_id = ? AND s.activo = TRUE
			  AND s.estado = 'ACTIVA'::configuracion.estado_seccion
			ORDER BY s.nombre, s.id
			FOR UPDATE OF s
			""", (rs, row) -> new Seccion(rs.getObject("id", UUID.class),
			rs.getObject("grado_id", UUID.class), rs.getString("nombre"),
			rs.getInt("capacidad_maxima")), anioEscolarId, grados.getFirst());
	}
}
