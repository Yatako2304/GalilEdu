package com.galiledu.estudiantes.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.galiledu.personas.dominio.Persona;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/** Estudiante inscrito (tabla estudiantes.estudiante, V2). No tiene usuario propio. */
@Entity
@Table(schema = "estudiantes", name = "estudiante")
public class Estudiante {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "persona_id", nullable = false, unique = true)
	private Persona persona;

	@Column(name = "pais_nacimiento") private String paisNacimiento;
	@Column(name = "departamento_nacimiento") private String departamentoNacimiento;
	@Column(name = "provincia_nacimiento") private String provinciaNacimiento;
	@Column(name = "distrito_nacimiento") private String distritoNacimiento;
	@Column(name = "referencia_domicilio") private String referenciaDomicilio;
	@Column(name = "tiene_internet") private Boolean tieneInternet;
	@Column(name = "lengua_materna") private String lenguaMaterna;
	@Column(name = "segunda_lengua") private String segundaLengua;
	@Column(name = "autoidentificacion_etnica") private String autoidentificacionEtnica;
	@Column(name = "tiene_discapacidad", nullable = false) private boolean tieneDiscapacidad;
	@Column(name = "discapacidad_detalle") private String discapacidadDetalle;
	@Column(name = "colegio_procedencia") private String colegioProcedencia;
	@Column(name = "gestion_procedencia") private String gestionProcedencia;
	@Column(name = "anio_academico", nullable = false) private short anioAcademico;
	@Enumerated(EnumType.STRING) @Column(nullable = false) private Nivel nivel;
	@Column(nullable = false) private String grado;
	@Column(name = "forma_ingreso") private String formaIngreso;
	@Enumerated(EnumType.STRING) @Column(name = "modo_salida") private ModoSalida modoSalida;
	@Enumerated(EnumType.STRING) @Column(nullable = false) private EstadoEstudiante estado = EstadoEstudiante.INSCRITO;
	@Column(nullable = false) private boolean eliminado;
	@Column(name = "creado_en", nullable = false, updatable = false) private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false) private Instant actualizadoEn;

	@OneToOne(mappedBy = "estudiante", cascade = CascadeType.ALL, orphanRemoval = true)
	private FichaMedica fichaMedica;

	@OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<VinculoFamiliar> vinculos = new ArrayList<>();

	@OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orden")
	private List<ContactoEmergencia> contactos = new ArrayList<>();

	@OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<AutorizadoRecojo> autorizados = new ArrayList<>();

	protected Estudiante() {
	}

	public Estudiante(Persona persona) {
		this.persona = persona;
	}

	@PrePersist
	void alCrear() {
		creadoEn = Instant.now();
		actualizadoEn = creadoEn;
	}

	@PreUpdate
	void alActualizar() {
		actualizadoEn = Instant.now();
	}

	public void asignarFichaMedica(FichaMedica ficha) {
		ficha.asociar(this);
		this.fichaMedica = ficha;
	}

	public void agregarVinculo(VinculoFamiliar vinculo) {
		vinculo.asociar(this);
		vinculos.add(vinculo);
	}

	public void agregarContacto(ContactoEmergencia contacto) {
		contacto.asociar(this);
		contactos.add(contacto);
	}

	public void agregarAutorizado(AutorizadoRecojo autorizado) {
		autorizado.asociar(this);
		autorizados.add(autorizado);
	}

	/** "1.° grado de primaria", "Inicial 5 años"… */
	public String gradoDescriptivo() {
		return nivel == Nivel.INICIAL ? grado : grado + " " + nivel.name().toLowerCase();
	}

	public void datosAcademicos(short anioAcademico, Nivel nivel, String grado, String formaIngreso) {
		this.anioAcademico = anioAcademico;
		this.nivel = nivel;
		this.grado = grado;
		this.formaIngreso = formaIngreso;
	}

	public void lugarNacimiento(String pais, String departamento, String provincia, String distrito) {
		this.paisNacimiento = pais;
		this.departamentoNacimiento = departamento;
		this.provinciaNacimiento = provincia;
		this.distritoNacimiento = distrito;
	}

	public void datosSocioculturales(String referenciaDomicilio, Boolean tieneInternet, String lenguaMaterna,
		String segundaLengua, String autoidentificacionEtnica, boolean tieneDiscapacidad, String discapacidadDetalle) {
		this.referenciaDomicilio = referenciaDomicilio;
		this.tieneInternet = tieneInternet;
		this.lenguaMaterna = lenguaMaterna;
		this.segundaLengua = segundaLengua;
		this.autoidentificacionEtnica = autoidentificacionEtnica;
		this.tieneDiscapacidad = tieneDiscapacidad;
		this.discapacidadDetalle = discapacidadDetalle;
	}

	public void procedencia(String colegio, String gestion) {
		this.colegioProcedencia = colegio;
		this.gestionProcedencia = gestion;
	}

	public void setModoSalida(ModoSalida modoSalida) {
		this.modoSalida = modoSalida;
	}

	public UUID getId() { return id; }
	public Persona getPersona() { return persona; }
	public String getPaisNacimiento() { return paisNacimiento; }
	public String getDepartamentoNacimiento() { return departamentoNacimiento; }
	public String getProvinciaNacimiento() { return provinciaNacimiento; }
	public String getDistritoNacimiento() { return distritoNacimiento; }
	public String getReferenciaDomicilio() { return referenciaDomicilio; }
	public Boolean getTieneInternet() { return tieneInternet; }
	public String getLenguaMaterna() { return lenguaMaterna; }
	public String getSegundaLengua() { return segundaLengua; }
	public String getAutoidentificacionEtnica() { return autoidentificacionEtnica; }
	public boolean isTieneDiscapacidad() { return tieneDiscapacidad; }
	public String getDiscapacidadDetalle() { return discapacidadDetalle; }
	public String getColegioProcedencia() { return colegioProcedencia; }
	public String getGestionProcedencia() { return gestionProcedencia; }
	public short getAnioAcademico() { return anioAcademico; }
	public Nivel getNivel() { return nivel; }
	public String getGrado() { return grado; }
	public String getFormaIngreso() { return formaIngreso; }
	public ModoSalida getModoSalida() { return modoSalida; }
	public EstadoEstudiante getEstado() { return estado; }
	public boolean isEliminado() { return eliminado; }
	public Instant getCreadoEn() { return creadoEn; }
	public FichaMedica getFichaMedica() { return fichaMedica; }
	public List<VinculoFamiliar> getVinculos() { return vinculos; }
	public List<ContactoEmergencia> getContactos() { return contactos; }
	public List<AutorizadoRecojo> getAutorizados() { return autorizados; }
}
