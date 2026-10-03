package com.galiledu.asistencia.aplicacion.puertos;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.galiledu.asistencia.dominio.*;
/** Puerto de persistencia del módulo; no expone SQL ni JDBC. */
public interface RepositorioAsistencia {
	JornadaAsistencia crearJornada(JornadaAsistencia j);
	Optional<JornadaAsistencia> buscarJornada(UUID id);
	List<JornadaAsistencia> listarJornadas();
	boolean actualizarJornada(JornadaAsistencia j);
	boolean desactivarJornada(UUID id);
	DetalleAsistencia crearDetalle(DetalleAsistencia d);
	Optional<DetalleAsistencia> buscarDetalle(UUID id);
	List<DetalleAsistencia> listarDetalles(UUID jornadaId);
	boolean actualizarDetalle(DetalleAsistencia d);
	boolean desactivarDetalle(UUID id);
	AsistenciaDocente crearAsistenciaDocente(AsistenciaDocente a);
	Optional<AsistenciaDocente> buscarAsistenciaDocente(UUID id);
	List<AsistenciaDocente> listarAsistenciasDocentes();
	boolean actualizarAsistenciaDocente(AsistenciaDocente a);
	boolean desactivarAsistenciaDocente(UUID id);
}
