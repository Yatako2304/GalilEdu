package com.galiledu.horarios.infraestructura.persistencia;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario;
import com.galiledu.horarios.dominio.DiaSemana;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

/** Solo lectura de configuracion.* y matricula.matricula; Horarios no escribe en esas tablas. */
@Repository
public class ConsultaReferenciasHorarioPostgres implements ConsultaReferenciasHorario {
	private final JdbcOperations jdbc;

	public ConsultaReferenciasHorarioPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public Optional<SeccionReferencia> buscarSeccion(UUID id) {
		return jdbc.query("""
			SELECT s.id, s.anio_id, g.nombre || ' ' || s.nombre AS nombre,
			       (s.activo AND s.estado = 'ACTIVA') AS activa
			FROM configuracion.seccion s
			JOIN configuracion.grado g ON g.id = s.grado_id
			WHERE s.id = ?
			""", (rs, fila) -> new SeccionReferencia(rs.getObject("id", UUID.class),
			rs.getObject("anio_id", UUID.class), rs.getString("nombre"), rs.getBoolean("activa")),
			id).stream().findFirst();
	}

	@Override
	public Optional<CargaAcademicaReferencia> buscarCargaAcademica(UUID id) {
		return jdbc.query("""
			SELECT ca.id, ca.seccion_id, ca.docente_id, cu.nombre AS curso,
			       (ca.activo AND oc.activo) AS activa
			FROM configuracion.carga_academica ca
			JOIN configuracion.oferta_curso oc ON oc.id = ca.oferta_curso_id
			JOIN configuracion.curso cu ON cu.id = oc.curso_id
			WHERE ca.id = ?
			""", (rs, fila) -> new CargaAcademicaReferencia(rs.getObject("id", UUID.class),
			rs.getObject("seccion_id", UUID.class), rs.getObject("docente_id", UUID.class),
			rs.getString("curso"), rs.getBoolean("activa")), id).stream().findFirst();
	}

	@Override
	public Optional<BloqueReferencia> buscarBloque(UUID id) {
		return jdbc.query("""
			SELECT b.id, eh.anio_id, array_to_string(eh.dias_lectivos, ',') AS dias,
			       (b.activo AND eh.activo) AS activo
			FROM configuracion.bloque_horario b
			JOIN configuracion.estructura_horaria eh ON eh.id = b.estructura_horaria_id
			WHERE b.id = ?
			""", (rs, fila) -> new BloqueReferencia(rs.getObject("id", UUID.class),
			rs.getObject("anio_id", UUID.class), dias(rs.getString("dias")), rs.getBoolean("activo")),
			id).stream().findFirst();
	}

	@Override
	public List<PeriodoReferencia> buscarPeriodos(Collection<UUID> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		String marcadores = String.join(",", Collections.nCopies(ids.size(), "?"));
		return jdbc.query("SELECT id, anio_id, activo FROM configuracion.periodo_academico WHERE id IN ("
			+ marcadores + ")", (rs, fila) -> new PeriodoReferencia(rs.getObject("id", UUID.class),
			rs.getObject("anio_id", UUID.class), rs.getBoolean("activo")), ids.toArray());
	}

	@Override
	public boolean existeEspacioActivo(UUID id) {
		return Boolean.TRUE.equals(jdbc.queryForObject("""
			SELECT EXISTS (SELECT 1 FROM configuracion.espacio_fisico WHERE id = ? AND activo = TRUE)
			""", Boolean.class, id));
	}

	@Override
	public Optional<MatriculaReferencia> buscarMatricula(UUID id) {
		return jdbc.query("SELECT id, seccion_id, activo FROM matricula.matricula WHERE id = ?",
			(rs, fila) -> new MatriculaReferencia(rs.getObject("id", UUID.class),
				rs.getObject("seccion_id", UUID.class), rs.getBoolean("activo")), id)
			.stream().findFirst();
	}

	private static Set<DiaSemana> dias(String lista) {
		if (lista == null || lista.isBlank()) {
			return Set.of();
		}
		return Arrays.stream(lista.split(",")).map(DiaSemana::valueOf).collect(Collectors.toUnmodifiableSet());
	}
}
