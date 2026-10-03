package com.galiledu.asistencia.infraestructura.persistencia;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.galiledu.asistencia.aplicacion.puertos.RepositorioAsistencia;
import com.galiledu.asistencia.dominio.*;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
/** Adaptador JDBC basado en asistencia.sql. */
@Repository
public class RepositorioAsistenciaPostgres implements RepositorioAsistencia {
	private static final RowMapper<JornadaAsistencia> J=(r,n)->new JornadaAsistencia(r.getObject("id",UUID.class),r.getObject("carga_academica_id",UUID.class),r.getDate("fecha").toLocalDate(),r.getTimestamp("fecha_registro").toInstant(),r.getBoolean("activo"));
	private static final RowMapper<DetalleAsistencia> D=(r,n)->new DetalleAsistencia(r.getObject("id",UUID.class),r.getObject("jornada_asistencia_id",UUID.class),r.getObject("matricula_id",UUID.class),EstadoAsistencia.valueOf(r.getString("estado")),r.getString("condicion")==null?null:CondicionAsistencia.valueOf(r.getString("condicion")),r.getTime("hora_marcacion")==null?null:r.getTime("hora_marcacion").toLocalTime(),r.getString("observacion"),r.getBoolean("activo"));
	private static final RowMapper<AsistenciaDocente> A=(r,n)->new AsistenciaDocente(r.getObject("id",UUID.class),r.getObject("docente_id",UUID.class),r.getDate("fecha").toLocalDate(),r.getTime("hora_entrada")==null?null:r.getTime("hora_entrada").toLocalTime(),r.getTime("hora_salida")==null?null:r.getTime("hora_salida").toLocalTime(),r.getString("observacion"),r.getBoolean("activo"));
	private final JdbcOperations jdbc;
	public RepositorioAsistenciaPostgres(JdbcOperations jdbc){this.jdbc=jdbc;}
	public JornadaAsistencia crearJornada(JornadaAsistencia j){return jdbc.queryForObject("INSERT INTO asistencia.jornada_asistencia(id,carga_academica_id,fecha,fecha_registro,activo) VALUES(?,?,?,?,TRUE) RETURNING *",J,j.id(),j.cargaAcademicaId(),j.fecha(),Timestamp.from(j.fechaRegistro()));}
	public Optional<JornadaAsistencia> buscarJornada(UUID id){return jdbc.query("SELECT * FROM asistencia.jornada_asistencia WHERE id=? AND activo=TRUE",J,id).stream().findFirst();}
	public List<JornadaAsistencia> listarJornadas(){return jdbc.query("SELECT * FROM asistencia.jornada_asistencia WHERE activo=TRUE ORDER BY fecha DESC,carga_academica_id",J);}
	public boolean actualizarJornada(JornadaAsistencia j){return jdbc.update("UPDATE asistencia.jornada_asistencia SET carga_academica_id=?,fecha=? WHERE id=? AND activo=TRUE",j.cargaAcademicaId(),j.fecha(),j.id())==1;}
	public boolean desactivarJornada(UUID id){int n=jdbc.update("UPDATE asistencia.jornada_asistencia SET activo=FALSE WHERE id=? AND activo=TRUE",id); if(n==1)jdbc.update("UPDATE asistencia.detalle_asistencia SET activo=FALSE WHERE jornada_asistencia_id=? AND activo=TRUE",id); return n==1;}
	public DetalleAsistencia crearDetalle(DetalleAsistencia d){return jdbc.queryForObject("INSERT INTO asistencia.detalle_asistencia(id,jornada_asistencia_id,matricula_id,estado,condicion,hora_marcacion,observacion,activo) VALUES(?,?,?,?::asistencia.estado_asistencia,?::asistencia.condicion_asistencia,?,?,TRUE) RETURNING *",D,d.id(),d.jornadaAsistenciaId(),d.matriculaId(),d.estado().name(),d.condicion()==null?null:d.condicion().name(),d.horaMarcacion(),d.observacion());}
	public Optional<DetalleAsistencia> buscarDetalle(UUID id){return jdbc.query("SELECT d.* FROM asistencia.detalle_asistencia d JOIN asistencia.jornada_asistencia j ON j.id=d.jornada_asistencia_id AND j.activo=TRUE WHERE d.id=? AND d.activo=TRUE",D,id).stream().findFirst();}
	public List<DetalleAsistencia> listarDetalles(UUID id){return jdbc.query("SELECT * FROM asistencia.detalle_asistencia WHERE jornada_asistencia_id=? AND activo=TRUE ORDER BY matricula_id",D,id);}
	public boolean actualizarDetalle(DetalleAsistencia d){return jdbc.update("UPDATE asistencia.detalle_asistencia SET jornada_asistencia_id=?,matricula_id=?,estado=?::asistencia.estado_asistencia,condicion=?::asistencia.condicion_asistencia,hora_marcacion=?,observacion=? WHERE id=? AND activo=TRUE",d.jornadaAsistenciaId(),d.matriculaId(),d.estado().name(),d.condicion()==null?null:d.condicion().name(),d.horaMarcacion(),d.observacion(),d.id())==1;}
	public boolean desactivarDetalle(UUID id){return jdbc.update("UPDATE asistencia.detalle_asistencia SET activo=FALSE WHERE id=? AND activo=TRUE",id)==1;}
	public AsistenciaDocente crearAsistenciaDocente(AsistenciaDocente a){return jdbc.queryForObject("INSERT INTO asistencia.asistencia_docente(id,docente_id,fecha,hora_entrada,hora_salida,observacion,activo) VALUES(?,?,?,?,?,?,TRUE) RETURNING *",A,a.id(),a.docenteId(),a.fecha(),a.horaEntrada(),a.horaSalida(),a.observacion());}
	public Optional<AsistenciaDocente> buscarAsistenciaDocente(UUID id){return jdbc.query("SELECT * FROM asistencia.asistencia_docente WHERE id=? AND activo=TRUE",A,id).stream().findFirst();}
	public List<AsistenciaDocente> listarAsistenciasDocentes(){return jdbc.query("SELECT * FROM asistencia.asistencia_docente WHERE activo=TRUE ORDER BY fecha DESC,docente_id",A);}
	public boolean actualizarAsistenciaDocente(AsistenciaDocente a){return jdbc.update("UPDATE asistencia.asistencia_docente SET docente_id=?,fecha=?,hora_entrada=?,hora_salida=?,observacion=? WHERE id=? AND activo=TRUE",a.docenteId(),a.fecha(),a.horaEntrada(),a.horaSalida(),a.observacion(),a.id())==1;}
	public boolean desactivarAsistenciaDocente(UUID id){return jdbc.update("UPDATE asistencia.asistencia_docente SET activo=FALSE WHERE id=? AND activo=TRUE",id)==1;}
}
