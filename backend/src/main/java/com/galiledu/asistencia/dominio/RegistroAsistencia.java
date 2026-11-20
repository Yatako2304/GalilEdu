package com.galiledu.asistencia.dominio;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * La asistencia de una sección en un día (asistencia.registro_diario, V25), con el estado de cada estudiante. Se
 * registra una sola vez y luego solo se corrige: cada cambio deja una {@link CorreccionAsistencia}.
 */
@Entity
@Table(schema = "asistencia", name = "registro_diario")
public class RegistroAsistencia {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(name = "seccion_id", nullable = false)
	private UUID seccionId;

	@Column(nullable = false)
	private LocalDate fecha;

	@Column(name = "registrado_por", nullable = false)
	private UUID registradoPor;

	@Column(name = "registrado_en", nullable = false)
	private OffsetDateTime registradoEn;

	@Column(name = "corregido_por")
	private UUID corregidoPor;

	@Column(name = "corregido_en")
	private OffsetDateTime corregidoEn;

	@Version
	private int version;

	@OneToMany(mappedBy = "registro", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<AsistenciaEstudiante> asistencias = new ArrayList<>();

	protected RegistroAsistencia() {
	}

	/** AS-04: guarda la asistencia del día con todos los estudiantes ya marcados. */
	public static RegistroAsistencia registrar(UUID seccionId, LocalDate fecha, List<MarcaAsistencia> marcas,
			UUID usuarioId, OffsetDateTime cuando) {
		RegistroAsistencia registro = new RegistroAsistencia();
		registro.seccionId = seccionId;
		registro.fecha = fecha;
		registro.registradoPor = usuarioId;
		registro.registradoEn = cuando;
		for (MarcaAsistencia marca : marcas) {
			registro.asistencias.add(new AsistenciaEstudiante(registro, marca));
		}
		return registro;
	}

	/**
	 * AS-06: aplica solo las marcas que cambian y devuelve el historial de esos cambios. Las marcas de estudiantes
	 * que no estaban en el registro se ignoran (el servicio ya exige la lista completa de la sección).
	 */
	public List<CorreccionAsistencia> corregir(List<MarcaAsistencia> marcas, UUID usuarioId, OffsetDateTime cuando) {
		Map<UUID, AsistenciaEstudiante> porMatricula = asistencias.stream()
				.collect(Collectors.toMap(AsistenciaEstudiante::getMatriculaId, Function.identity()));
		List<CorreccionAsistencia> cambios = new ArrayList<>();
		for (MarcaAsistencia marca : marcas) {
			AsistenciaEstudiante actual = porMatricula.get(marca.matriculaId());
			if (actual != null && !marca.igualA(actual.getEstado(), actual.getHoraLlegada())) {
				cambios.add(new CorreccionAsistencia(actual, marca, usuarioId, cuando));
				actual.cambiar(marca);
			}
		}
		if (!cambios.isEmpty()) {
			corregidoPor = usuarioId;
			corregidoEn = cuando;
		}
		return cambios;
	}

	/** Agrega a un estudiante que se matriculó en la sección después de registrar la asistencia del día. */
	public void agregar(MarcaAsistencia marca) {
		asistencias.add(new AsistenciaEstudiante(this, marca));
	}

	public UUID getId() {
		return id;
	}

	public UUID getSeccionId() {
		return seccionId;
	}

	public LocalDate getFecha() {
		return fecha;
	}

	public UUID getRegistradoPor() {
		return registradoPor;
	}

	public OffsetDateTime getRegistradoEn() {
		return registradoEn;
	}

	public UUID getCorregidoPor() {
		return corregidoPor;
	}

	public OffsetDateTime getCorregidoEn() {
		return corregidoEn;
	}

	public int getVersion() {
		return version;
	}

	public List<AsistenciaEstudiante> getAsistencias() {
		return asistencias;
	}
}
