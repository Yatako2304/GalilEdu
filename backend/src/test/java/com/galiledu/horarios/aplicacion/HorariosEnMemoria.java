package com.galiledu.horarios.aplicacion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.galiledu.horarios.aplicacion.puertos.ConsultaOcupacionesHorario;
import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario;
import com.galiledu.horarios.aplicacion.puertos.RepositorioHorarios;
import com.galiledu.horarios.dominio.AsignacionHoraria;
import com.galiledu.horarios.dominio.DiaSemana;
import com.galiledu.horarios.dominio.HorarioSeccion;
import com.galiledu.horarios.dominio.OcupacionHoraria;
import com.galiledu.horarios.dominio.Subgrupo;

/** Implementa los tres puertos de Horarios sobre mapas, con datos de configuración controlables. */
final class HorariosEnMemoria implements RepositorioHorarios, ConsultaOcupacionesHorario,
	ConsultaReferenciasHorario {
	final Map<UUID, HorarioSeccion> horarios = new LinkedHashMap<>();
	final Map<UUID, AsignacionHoraria> asignaciones = new LinkedHashMap<>();
	final Map<UUID, Subgrupo> subgrupos = new LinkedHashMap<>();
	final Map<UUID, Set<UUID>> miembros = new HashMap<>();
	final List<String> serializados = new ArrayList<>();

	final Map<UUID, SeccionReferencia> secciones = new HashMap<>();
	final Map<UUID, CargaAcademicaReferencia> cargas = new HashMap<>();
	final Map<UUID, BloqueReferencia> bloques = new HashMap<>();
	final Map<UUID, PeriodoReferencia> periodos = new HashMap<>();
	final Map<UUID, MatriculaReferencia> matriculas = new HashMap<>();
	final Set<UUID> espaciosActivos = new HashSet<>();

	@Override
	public HorarioSeccion crearBorrador(UUID seccionId) {
		int version = horarios.values().stream().filter(h -> h.seccionId().equals(seccionId))
			.mapToInt(HorarioSeccion::version).max().orElse(0) + 1;
		HorarioSeccion horario = HorarioSeccion.nuevoBorrador(seccionId, version);
		horarios.put(horario.id(), horario);
		return horario;
	}

	@Override
	public Optional<HorarioSeccion> buscarHorario(UUID id) {
		return Optional.ofNullable(horarios.get(id));
	}

	@Override
	public List<HorarioSeccion> listarHorarios(UUID seccionId) {
		return horarios.values().stream().filter(h -> h.seccionId().equals(seccionId) && h.activo())
			.sorted(Comparator.comparingInt(HorarioSeccion::version).reversed()).toList();
	}

	@Override
	public boolean desactivarHorario(UUID id) {
		HorarioSeccion horario = horarios.get(id);
		if (horario == null || !horario.activo()) {
			return false;
		}
		horarios.put(id, horario.desactivar());
		return true;
	}

	@Override
	public void serializarBloque(UUID bloqueHorarioId, DiaSemana dia) {
		serializados.add(bloqueHorarioId + ":" + dia);
	}

	@Override
	public void guardarAsignacion(AsignacionHoraria asignacion) {
		asignaciones.put(asignacion.id(), asignacion);
	}

	@Override
	public boolean actualizarAsignacion(AsignacionHoraria asignacion) {
		AsignacionHoraria actual = asignaciones.get(asignacion.id());
		if (actual == null || !actual.activa()) {
			return false;
		}
		asignaciones.put(asignacion.id(), asignacion);
		return true;
	}

	@Override
	public Optional<AsignacionHoraria> buscarAsignacion(UUID id) {
		return Optional.ofNullable(asignaciones.get(id));
	}

	@Override
	public List<AsignacionHoraria> listarAsignaciones(UUID horarioSeccionId) {
		return asignaciones.values().stream()
			.filter(a -> a.horarioSeccionId().equals(horarioSeccionId) && a.activa()).toList();
	}

	@Override
	public boolean desactivarAsignacion(UUID id) {
		AsignacionHoraria actual = asignaciones.get(id);
		if (actual == null || !actual.activa()) {
			return false;
		}
		asignaciones.put(id, actual.desactivar());
		return true;
	}

	@Override
	public void guardarSubgrupo(Subgrupo subgrupo) {
		subgrupos.put(subgrupo.id(), subgrupo);
	}

	@Override
	public boolean existeSubgrupo(UUID seccionId, String nombre) {
		return subgrupos.values().stream()
			.anyMatch(s -> s.seccionId().equals(seccionId) && s.nombre().equals(nombre));
	}

	@Override
	public Optional<Subgrupo> buscarSubgrupo(UUID id) {
		return Optional.ofNullable(subgrupos.get(id));
	}

	@Override
	public List<Subgrupo> listarSubgrupos(UUID seccionId) {
		return subgrupos.values().stream()
			.filter(s -> s.seccionId().equals(seccionId) && s.activo()).toList();
	}

	@Override
	public boolean desactivarSubgrupo(UUID id) {
		Subgrupo actual = subgrupos.get(id);
		if (actual == null || !actual.activo()) {
			return false;
		}
		subgrupos.put(id, actual.desactivar());
		return true;
	}

	@Override
	public boolean tieneAsignacionesActivas(UUID subgrupoId) {
		return asignaciones.values().stream()
			.anyMatch(a -> a.activa() && subgrupoId.equals(a.subgrupoId()));
	}

	@Override
	public void agregarMiembro(UUID subgrupoId, UUID matriculaId) {
		miembros.computeIfAbsent(subgrupoId, clave -> new HashSet<>()).add(matriculaId);
	}

	@Override
	public boolean retirarMiembro(UUID subgrupoId, UUID matriculaId) {
		return miembros.getOrDefault(subgrupoId, new HashSet<>()).remove(matriculaId);
	}

	@Override
	public List<UUID> listarMiembros(UUID subgrupoId) {
		return List.copyOf(miembros.getOrDefault(subgrupoId, Set.of()));
	}

	@Override
	public List<OcupacionHoraria> buscarOcupaciones(UUID horarioSeccionId, UUID bloqueHorarioId,
		DiaSemana dia, Set<UUID> periodosConsultados) {
		List<OcupacionHoraria> resultado = new ArrayList<>();
		for (AsignacionHoraria a : asignaciones.values()) {
			HorarioSeccion horario = horarios.get(a.horarioSeccionId());
			Set<UUID> comunes = new HashSet<>(a.periodos());
			comunes.retainAll(periodosConsultados);
			if (!a.activa() || !horario.activo() || !a.bloqueHorarioId().equals(bloqueHorarioId)
				|| a.dia() != dia || comunes.isEmpty()) {
				continue;
			}
			CargaAcademicaReferencia carga = cargas.get(a.cargaAcademicaId());
			resultado.add(new OcupacionHoraria(a.id(), a.horarioSeccionId(),
				secciones.get(horario.seccionId()).nombre(), carga.curso(), carga.docenteId(),
				a.espacioFisicoId(), a.subgrupoId(), a.bloqueHorarioId(), a.dia(), comunes));
		}
		return resultado;
	}

	@Override
	public Optional<SeccionReferencia> buscarSeccion(UUID id) {
		return Optional.ofNullable(secciones.get(id));
	}

	@Override
	public Optional<CargaAcademicaReferencia> buscarCargaAcademica(UUID id) {
		return Optional.ofNullable(cargas.get(id));
	}

	@Override
	public Optional<BloqueReferencia> buscarBloque(UUID id) {
		return Optional.ofNullable(bloques.get(id));
	}

	@Override
	public List<PeriodoReferencia> buscarPeriodos(Collection<UUID> ids) {
		return ids.stream().map(periodos::get).filter(p -> p != null).toList();
	}

	@Override
	public boolean existeEspacioActivo(UUID id) {
		return espaciosActivos.contains(id);
	}

	@Override
	public Optional<MatriculaReferencia> buscarMatricula(UUID id) {
		return Optional.ofNullable(matriculas.get(id));
	}
}
