package com.galiledu.asistencia.infraestructura.web;
import java.net.URI;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import com.galiledu.asistencia.aplicacion.GestionAsistencia;
import com.galiledu.asistencia.dominio.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/asistencia")
public class AsistenciaController {
	private final GestionAsistencia g;
	public AsistenciaController(GestionAsistencia g){this.g=g;}
	@PostMapping("/jornadas") public ResponseEntity<JornadaAsistencia> crearJornada(@RequestBody JornadaRequest r){var j=g.crearJornada(r.cargaAcademicaId(),r.fecha());return ResponseEntity.created(URI.create("/api/asistencia/jornadas/"+j.id())).body(j);}
	@GetMapping("/jornadas") public List<JornadaAsistencia> jornadas(){return g.listarJornadas();}
	@GetMapping("/jornadas/{id}") public JornadaAsistencia jornada(@PathVariable UUID id){return g.buscarJornada(id);}
	@PutMapping("/jornadas/{id}") public JornadaAsistencia editarJornada(@PathVariable UUID id,@RequestBody JornadaRequest r){return g.actualizarJornada(id,r.cargaAcademicaId(),r.fecha());}
	@DeleteMapping("/jornadas/{id}") public ResponseEntity<Void> borrarJornada(@PathVariable UUID id){g.desactivarJornada(id);return ResponseEntity.noContent().build();}
	@PostMapping("/jornadas/{jornadaId}/detalles") public ResponseEntity<DetalleAsistencia> crearDetalle(@PathVariable UUID jornadaId,@RequestBody DetalleRequest r){var d=g.crearDetalle(jornadaId,r.matriculaId(),r.estado(),r.condicion(),r.horaMarcacion(),r.observacion());return ResponseEntity.created(URI.create("/api/asistencia/detalles/"+d.id())).body(d);}
	@GetMapping("/jornadas/{jornadaId}/detalles") public List<DetalleAsistencia> detalles(@PathVariable UUID jornadaId){return g.listarDetalles(jornadaId);}
	@GetMapping("/detalles/{id}") public DetalleAsistencia detalle(@PathVariable UUID id){return g.buscarDetalle(id);}
	@PutMapping("/detalles/{id}") public DetalleAsistencia editarDetalle(@PathVariable UUID id,@RequestBody DetalleRequest r){return g.actualizarDetalle(id,r.jornadaAsistenciaId(),r.matriculaId(),r.estado(),r.condicion(),r.horaMarcacion(),r.observacion());}
	@DeleteMapping("/detalles/{id}") public ResponseEntity<Void> borrarDetalle(@PathVariable UUID id){g.desactivarDetalle(id);return ResponseEntity.noContent().build();}
	@PostMapping("/docentes") public ResponseEntity<AsistenciaDocente> crearDocente(@RequestBody DocenteRequest r){var a=g.crearAsistenciaDocente(r.docenteId(),r.fecha(),r.horaEntrada(),r.horaSalida(),r.observacion());return ResponseEntity.created(URI.create("/api/asistencia/docentes/"+a.id())).body(a);}
	@GetMapping("/docentes") public List<AsistenciaDocente> docentes(){return g.listarAsistenciasDocentes();}
	@GetMapping("/docentes/{id}") public AsistenciaDocente docente(@PathVariable UUID id){return g.buscarAsistenciaDocente(id);}
	@PutMapping("/docentes/{id}") public AsistenciaDocente editarDocente(@PathVariable UUID id,@RequestBody DocenteRequest r){return g.actualizarAsistenciaDocente(id,r.docenteId(),r.fecha(),r.horaEntrada(),r.horaSalida(),r.observacion());}
	@DeleteMapping("/docentes/{id}") public ResponseEntity<Void> borrarDocente(@PathVariable UUID id){g.desactivarAsistenciaDocente(id);return ResponseEntity.noContent().build();}
	@ExceptionHandler(NoSuchElementException.class) public ResponseEntity<ErrorRespuesta> noEncontrado(NoSuchElementException e){return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorRespuesta(e.getMessage()));}
	@ExceptionHandler(IllegalArgumentException.class) public ResponseEntity<ErrorRespuesta> invalido(IllegalArgumentException e){return ResponseEntity.badRequest().body(new ErrorRespuesta(e.getMessage()));}
	@ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<ErrorRespuesta> conflicto(DataIntegrityViolationException e){return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorRespuesta("Duplicado o referencia inexistente"));}
	public record JornadaRequest(UUID cargaAcademicaId,LocalDate fecha){}
	public record DetalleRequest(UUID jornadaAsistenciaId,UUID matriculaId,EstadoAsistencia estado,CondicionAsistencia condicion,LocalTime horaMarcacion,String observacion){}
	public record DocenteRequest(UUID docenteId,LocalDate fecha,LocalTime horaEntrada,LocalTime horaSalida,String observacion){}
	public record ErrorRespuesta(String mensaje){}
}
