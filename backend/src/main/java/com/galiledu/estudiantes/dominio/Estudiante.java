package com.galiledu.estudiantes.dominio;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

import org.hibernate.annotations.Formula;

import com.galiledu.personas.dominio.Persona;

/**
 * Estudiante (tabla estudiantes.estudiante, V2 y V7). Si tiene cuenta propia, es el usuario con la misma persona.
 */
@Entity
@Table(schema = "estudiantes", name = "estudiante")
public class Estudiante {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.EAGER, optional = false)
	@JoinColumn(name = "persona_id", nullable = false, unique = true)
	private Persona persona;

	@Column(name = "pais_nacimiento")
	private String paisNacimiento;
	@Column(name = "departamento_nacimiento")
	private String departamentoNacimiento;
	@Column(name = "provincia_nacimiento")
	private String provinciaNacimiento;
	@Column(name = "distrito_nacimiento")
	private String distritoNacimiento;
	@Column(name = "referencia_domicilio")
	private String referenciaDomicilio;
	@Column(name = "tiene_internet")
	private Boolean tieneInternet;
	@Column(name = "lengua_materna")
	private String lenguaMaterna;
	@Column(name = "segunda_lengua")
	private String segundaLengua;
	@Column(name = "autoidentificacion_etnica")
	private String autoidentificacionEtnica;
	@Column(name = "tiene_discapacidad", nullable = false)
	private boolean tieneDiscapacidad;
	@Column(name = "discapacidad_detalle")
	private String discapacidadDetalle;
	@Column(name = "colegio_procedencia")
	private String colegioProcedencia;
	@Column(name = "gestion_procedencia")
	private String gestionProcedencia;
	@Column(name = "anio_academico", nullable = false)
	private short anioAcademico;
	/** Código del catálogo configuracion.grado (SEC-1, PRI-3…). */
	@Column(name = "grado_codigo", nullable = false)
	private String gradoCodigo;
	@Formula("(SELECT g.nombre FROM configuracion.grado g WHERE g.codigo = grado_codigo)")
	private String gradoNombre;
	@Formula("""
			(CASE WHEN estado = 'RETIRADO' THEN 'RETIRADO'
			      WHEN EXISTS (SELECT 1 FROM matricula.matricula m
			                   JOIN configuracion.anio_escolar a ON a.id = m.anio_escolar_id
			                   WHERE m.estudiante_id = id AND a.anio = anio_academico AND m.estado = 'MATRICULADO')
			      THEN 'MATRICULADO'
			      ELSE 'INSCRITO' END)""")
	private String situacion;
	@Column(name = "forma_ingreso")
	private String formaIngreso;
	@Enumerated(EnumType.STRING)
	@Column(name = "modo_salida")
	private ModoSalida modoSalida;
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EstadoEstudiante estado = EstadoEstudiante.ACTIVO;
	@Column(nullable = false)
	private boolean eliminado;
	@Column(name = "creado_en", nullable = false, updatable = false)
	private Instant creadoEn;
	@Column(name = "actualizado_en", nullable = false)
	private Instant actualizadoEn;

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

	/** "1.° de secundaria", "3.° de primaria"… (nombre del catálogo de grados). */
	public String gradoDescriptivo() {
		if (gradoNombre != null) {
			return gradoNombre;
		}
		// Recién creado en esta misma transacción: la fórmula aún no se leyó de la BD.
		String nivel = gradoCodigo.startsWith("PRI") ? "primaria" : "secundaria";
		return gradoCodigo.substring(gradoCodigo.indexOf('-') + 1) + ".° de " + nivel;
	}

	/** Inscrito, matriculado en su año académico o retirado. */
	public SituacionEstudiante getSituacion() {
		return situacion == null ? SituacionEstudiante.INSCRITO : SituacionEstudiante.valueOf(situacion);
	}

	public void datosAcademicos(short anioAcademico, String gradoCodigo, String formaIngreso) {
		this.anioAcademico = anioAcademico;
		this.gradoCodigo = gradoCodigo;
		this.formaIngreso = formaIngreso;
	}

	public void lugarNacimiento(String pais, String departamento, String provincia, String distrito) {
		this.paisNacimiento = pais;
		this.departamentoNacimiento = departamento;
		this.provinciaNacimiento = provincia;
		this.distritoNacimiento = distrito;
	}

	public void datosSocioculturales(String referenciaDomicilio, Boolean tieneInternet, String lenguaMaterna,
			String segundaLengua, String autoidentificacionEtnica, boolean tieneDiscapacidad,
			String discapacidadDetalle) {
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

	public UUID getId() {
		return id;
	}
	public Persona getPersona() {
		return persona;
	}
	public String getPaisNacimiento() {
		return paisNacimiento;
	}
	public String getDepartamentoNacimiento() {
		return departamentoNacimiento;
	}
	public String getProvinciaNacimiento() {
		return provinciaNacimiento;
	}
	public String getDistritoNacimiento() {
		return distritoNacimiento;
	}
	public String getReferenciaDomicilio() {
		return referenciaDomicilio;
	}
	public Boolean getTieneInternet() {
		return tieneInternet;
	}
	public String getLenguaMaterna() {
		return lenguaMaterna;
	}
	public String getSegundaLengua() {
		return segundaLengua;
	}
	public String getAutoidentificacionEtnica() {
		return autoidentificacionEtnica;
	}
	public boolean isTieneDiscapacidad() {
		return tieneDiscapacidad;
	}
	public String getDiscapacidadDetalle() {
		return discapacidadDetalle;
	}
	public String getColegioProcedencia() {
		return colegioProcedencia;
	}
	public String getGestionProcedencia() {
		return gestionProcedencia;
	}
	public short getAnioAcademico() {
		return anioAcademico;
	}
	public String getGradoCodigo() {
		return gradoCodigo;
	}
	public String getFormaIngreso() {
		return formaIngreso;
	}
	public ModoSalida getModoSalida() {
		return modoSalida;
	}
	public EstadoEstudiante getEstado() {
		return estado;
	}
	public boolean isEliminado() {
		return eliminado;
	}
	public Instant getCreadoEn() {
		return creadoEn;
	}
	public FichaMedica getFichaMedica() {
		return fichaMedica;
	}
	public List<VinculoFamiliar> getVinculos() {
		return vinculos;
	}
	public List<ContactoEmergencia> getContactos() {
		return contactos;
	}
	public List<AutorizadoRecojo> getAutorizados() {
		return autorizados;
	}
}
