package com.galiledu.personas.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Cualquier persona del colegio. Tabla personas.persona (V1). */
@Entity
@Table(schema = "personas", name = "persona")
public class Persona {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo_documento", nullable = false)
	private TipoDocumento tipoDocumento;
	@Column(name = "numero_documento", nullable = false)
	private String numeroDocumento;
	@Column(name = "apellido_paterno", nullable = false)
	private String apellidoPaterno;
	@Column(name = "apellido_materno")
	private String apellidoMaterno;
	@Column(nullable = false)
	private String nombres;
	@Column(name = "fecha_nacimiento")
	private LocalDate fechaNacimiento;
	private String nacionalidad;
	@Column(name = "lugar_nacimiento")
	private String lugarNacimiento;
	@Enumerated(EnumType.STRING)
	private Sexo sexo;
	@Column(name = "estado_civil")
	private String estadoCivil;
	@Column(name = "grado_instruccion")
	private String gradoInstruccion;
	private String celular;
	private String correo;
	@Column(name = "telefono_casa")
	private String telefonoCasa;
	private String profesion;
	@Column(name = "centro_trabajo")
	private String centroTrabajo;
	private String cargo;
	private String distrito;
	private String direccion;
	@Column(name = "es_exalumno", nullable = false)
	private boolean esExalumno;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

	@PrePersist
	void alCrear() {
		creadoEn = Instant.now();
		actualizadoEn = creadoEn;
	}

	@PreUpdate
	void alActualizar() {
		actualizadoEn = Instant.now();
	}

	public String nombreCompleto() {
		return (nombres + " " + apellidoPaterno + (apellidoMaterno == null ? "" : " " + apellidoMaterno)).strip();
	}

	/** Nombre corto como en la tabla de Figma: "María Fernández". */
	public String nombreCorto() {
		return nombres.split(" ")[0] + " " + apellidoPaterno;
	}

	public UUID getId() { return id; }
	public TipoDocumento getTipoDocumento() { return tipoDocumento; }
	public void setTipoDocumento(TipoDocumento v) { tipoDocumento = v; }
	public String getNumeroDocumento() { return numeroDocumento; }
	public void setNumeroDocumento(String v) { numeroDocumento = v; }
	public String getApellidoPaterno() { return apellidoPaterno; }
	public void setApellidoPaterno(String v) { apellidoPaterno = v; }
	public String getApellidoMaterno() { return apellidoMaterno; }
	public void setApellidoMaterno(String v) { apellidoMaterno = v; }
	public String getNombres() { return nombres; }
	public void setNombres(String v) { nombres = v; }
	public LocalDate getFechaNacimiento() { return fechaNacimiento; }
	public void setFechaNacimiento(LocalDate v) { fechaNacimiento = v; }
	public String getNacionalidad() { return nacionalidad; }
	public void setNacionalidad(String v) { nacionalidad = v; }
	public String getLugarNacimiento() { return lugarNacimiento; }
	public void setLugarNacimiento(String v) { lugarNacimiento = v; }
	public Sexo getSexo() { return sexo; }
	public void setSexo(Sexo v) { sexo = v; }
	public String getEstadoCivil() { return estadoCivil; }
	public void setEstadoCivil(String v) { estadoCivil = v; }
	public String getGradoInstruccion() { return gradoInstruccion; }
	public void setGradoInstruccion(String v) { gradoInstruccion = v; }
	public String getCelular() { return celular; }
	public void setCelular(String v) { celular = v; }
	public String getCorreo() { return correo; }
	public void setCorreo(String v) { correo = v; }
	public String getTelefonoCasa() { return telefonoCasa; }
	public void setTelefonoCasa(String v) { telefonoCasa = v; }
	public String getProfesion() { return profesion; }
	public void setProfesion(String v) { profesion = v; }
	public String getCentroTrabajo() { return centroTrabajo; }
	public void setCentroTrabajo(String v) { centroTrabajo = v; }
	public String getCargo() { return cargo; }
	public void setCargo(String v) { cargo = v; }
	public String getDistrito() { return distrito; }
	public void setDistrito(String v) { distrito = v; }
	public String getDireccion() { return direccion; }
	public void setDireccion(String v) { direccion = v; }
	public boolean isEsExalumno() { return esExalumno; }
	public void setEsExalumno(boolean v) { esExalumno = v; }
	public Instant getCreadoEn() { return creadoEn; }
}
