package com.galiledu.horarios.infraestructura;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.galiledu.horarios.dominio.ReservaAmbiente;

/**
 * Las consultas de superposición solo miran reservas ACTIVAS de la fecha y comparan estrictamente
 * ({@code inicio < otro.fin && otro.inicio < fin}): dos reservas contiguas no se cruzan. {@code excluirId} deja fuera
 * la reserva que se está editando; null no excluye nada.
 */
public interface ReservaAmbienteRepositorio extends JpaRepository<ReservaAmbiente, UUID> {
	/** Con un parámetro null Postgres no infiere el tipo del uuid: se usa este id que ninguna fila tiene. */
	UUID SIN_EXCLUIR = new UUID(0, 0);

	@Query("""
			SELECT r FROM ReservaAmbiente r
			WHERE r.ambienteId = :clave AND r.fecha = :fecha AND r.estado = com.galiledu.horarios.dominio.EstadoReserva.ACTIVA
			  AND r.horaInicio < :fin AND r.horaFin > :inicio AND r.id <> :excluirId
			ORDER BY r.horaInicio""")
	List<ReservaAmbiente> consultarPorAmbiente(@Param("clave") UUID ambienteId, @Param("fecha") LocalDate fecha,
			@Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin, @Param("excluirId") UUID excluirId);

	@Query("""
			SELECT r FROM ReservaAmbiente r
			WHERE r.seccionId = :clave AND r.fecha = :fecha AND r.estado = com.galiledu.horarios.dominio.EstadoReserva.ACTIVA
			  AND r.horaInicio < :fin AND r.horaFin > :inicio AND r.id <> :excluirId
			ORDER BY r.horaInicio""")
	List<ReservaAmbiente> consultarPorSeccion(@Param("clave") UUID seccionId, @Param("fecha") LocalDate fecha,
			@Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin, @Param("excluirId") UUID excluirId);

	@Query("""
			SELECT r FROM ReservaAmbiente r
			WHERE r.responsableId = :clave AND r.fecha = :fecha AND r.estado = com.galiledu.horarios.dominio.EstadoReserva.ACTIVA
			  AND r.horaInicio < :fin AND r.horaFin > :inicio AND r.id <> :excluirId
			ORDER BY r.horaInicio""")
	List<ReservaAmbiente> consultarPorResponsable(@Param("clave") UUID responsableId, @Param("fecha") LocalDate fecha,
			@Param("inicio") LocalTime inicio, @Param("fin") LocalTime fin, @Param("excluirId") UUID excluirId);

	default List<ReservaAmbiente> superpuestasPorAmbiente(UUID ambienteId, LocalDate fecha, LocalTime inicio,
			LocalTime fin, UUID excluirId) {
		return consultarPorAmbiente(ambienteId, fecha, inicio, fin, excluirId == null ? SIN_EXCLUIR : excluirId);
	}

	default List<ReservaAmbiente> superpuestasPorSeccion(UUID seccionId, LocalDate fecha, LocalTime inicio,
			LocalTime fin, UUID excluirId) {
		return consultarPorSeccion(seccionId, fecha, inicio, fin, excluirId == null ? SIN_EXCLUIR : excluirId);
	}

	default List<ReservaAmbiente> superpuestasPorResponsable(UUID responsableId, LocalDate fecha, LocalTime inicio,
			LocalTime fin, UUID excluirId) {
		return consultarPorResponsable(responsableId, fecha, inicio, fin, excluirId == null ? SIN_EXCLUIR : excluirId);
	}
}
