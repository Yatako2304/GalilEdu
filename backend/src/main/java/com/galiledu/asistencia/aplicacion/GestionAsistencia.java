package com.galiledu.asistencia.aplicacion;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import com.galiledu.asistencia.aplicacion.puertos.RepositorioAsistencia;
import com.galiledu.asistencia.dominio.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
/** Casos CRUD básicos para jornadas, matrículas y docentes. */
@Service
public class GestionAsistencia {
	private final RepositorioAsistencia repo;
	private final Clock reloj;
	public GestionAsistencia(RepositorioAsistencia repo, Clock reloj) { this.repo=Objects.requireNonNull(repo); this.reloj=Objects.requireNonNull(reloj); }
	@Transactional public JornadaAsistencia crearJornada(UUID carga, LocalDate fecha) { return repo.crearJornada(JornadaAsistencia.nueva(carga,fecha,reloj.instant())); }
	@Transactional(readOnly=true) public JornadaAsistencia buscarJornada(UUID id) { return repo.buscarJornada(id).orElseThrow(()->noEncontrado("Jornada")); }
	@Transactional(readOnly=true) public List<JornadaAsistencia> listarJornadas() { return List.copyOf(repo.listarJornadas()); }
	@Transactional public JornadaAsistencia actualizarJornada(UUID id,UUID carga,LocalDate fecha) {
		var a=buscarJornada(id).cambiar(carga,fecha); if(!repo.actualizarJornada(a)) throw noEncontrado("Jornada"); return buscarJornada(id);
	}
	@Transactional public void desactivarJornada(UUID id) { if(!repo.desactivarJornada(id)) throw noEncontrado("Jornada"); }
	@Transactional public DetalleAsistencia crearDetalle(UUID j,UUID m,EstadoAsistencia e,CondicionAsistencia c,LocalTime h,String o) {
		buscarJornada(j); return repo.crearDetalle(DetalleAsistencia.nuevo(j,m,e,c,h,o));
	}
	@Transactional(readOnly=true) public DetalleAsistencia buscarDetalle(UUID id) { return repo.buscarDetalle(id).orElseThrow(()->noEncontrado("Detalle")); }
	@Transactional(readOnly=true) public List<DetalleAsistencia> listarDetalles(UUID j) { buscarJornada(j); return List.copyOf(repo.listarDetalles(j)); }
	@Transactional public DetalleAsistencia actualizarDetalle(UUID id,UUID j,UUID m,EstadoAsistencia e,CondicionAsistencia c,LocalTime h,String o) {
		var a=buscarDetalle(id).cambiar(j,m,e,c,h,o); buscarJornada(j); if(!repo.actualizarDetalle(a)) throw noEncontrado("Detalle"); return buscarDetalle(id);
	}
	@Transactional public void desactivarDetalle(UUID id) { if(!repo.desactivarDetalle(id)) throw noEncontrado("Detalle"); }
	@Transactional public AsistenciaDocente crearAsistenciaDocente(UUID d,LocalDate f,LocalTime en,LocalTime sa,String o) {
		return repo.crearAsistenciaDocente(AsistenciaDocente.nueva(d,f,en,sa,o));
	}
	@Transactional(readOnly=true) public AsistenciaDocente buscarAsistenciaDocente(UUID id) { return repo.buscarAsistenciaDocente(id).orElseThrow(()->noEncontrado("Asistencia docente")); }
	@Transactional(readOnly=true) public List<AsistenciaDocente> listarAsistenciasDocentes() { return List.copyOf(repo.listarAsistenciasDocentes()); }
	@Transactional public AsistenciaDocente actualizarAsistenciaDocente(UUID id,UUID d,LocalDate f,LocalTime en,LocalTime sa,String o) {
		var a=buscarAsistenciaDocente(id).cambiar(d,f,en,sa,o); if(!repo.actualizarAsistenciaDocente(a)) throw noEncontrado("Asistencia docente"); return buscarAsistenciaDocente(id);
	}
	@Transactional public void desactivarAsistenciaDocente(UUID id) { if(!repo.desactivarAsistenciaDocente(id)) throw noEncontrado("Asistencia docente"); }
	private static NoSuchElementException noEncontrado(String e) { return new NoSuchElementException(e+" no encontrada"); }
}
