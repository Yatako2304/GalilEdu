package com.galiledu.academica.aplicacion;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico;
import com.galiledu.academica.aplicacion.puertos.RegistroAuditoriaAcademica;
import com.galiledu.academica.aplicacion.puertos.RepositorioCalificaciones;
import com.galiledu.academica.aplicacion.puertos.RepositorioItemsEvaluacion;
import com.galiledu.academica.aplicacion.puertos.RepositorioNotasConsolidadas;
import com.galiledu.academica.dominio.CalculadoraNotas;
import com.galiledu.academica.dominio.CalificacionEstudiante;
import com.galiledu.academica.dominio.ConfiguracionCompetencia;
import com.galiledu.academica.dominio.EscalaCalificacion;
import com.galiledu.academica.dominio.EstadoPeriodo;
import com.galiledu.academica.dominio.EvidenciaPedagogica;
import com.galiledu.academica.dominio.ItemEvaluacion;
import com.galiledu.academica.dominio.NotaConsolidada;
import com.galiledu.academica.dominio.ReglaAgregacionItems;
import com.galiledu.academica.dominio.ReglaConsolidacionPeriodos;
import com.galiledu.academica.dominio.TablaConversion;

/** Implementa todos los puertos en memoria con una carga, sección y competencia de ejemplo. */
class AcademicoEnMemoria implements ConsultaContextoAcademico, RepositorioItemsEvaluacion,
	RepositorioNotasConsolidadas, RegistroAuditoriaAcademica {
	final UUID anio = UUID.randomUUID();
	final UUID seccion = UUID.randomUUID();
	final UUID oferta = UUID.randomUUID();
	final UUID docente = UUID.randomUUID();
	final UUID carga = UUID.randomUUID();
	final UUID competencia = UUID.randomUUID();
	final UUID periodo1 = UUID.randomUUID();
	final UUID periodo2 = UUID.randomUUID();
	final UUID matricula1 = UUID.randomUUID();
	final UUID matricula2 = UUID.randomUUID();
	final Map<String, ActorAcademico> actores = new HashMap<>();
	final Map<UUID, PeriodoAcademico> periodos = new HashMap<>();
	final Map<UUID, ItemEvaluacion> items = new HashMap<>();
	final Map<UUID, CalificacionEstudiante> calificaciones = new HashMap<>();
	final Map<UUID, EvidenciaPedagogica> evidencias = new HashMap<>();
	final Map<String, UUID> finales = new HashMap<>();
	final Map<UUID, NotaFinalGuardada> notasFinales = new HashMap<>();
	final Map<String, NotaPeriodoGuardada> notasPeriodo = new HashMap<>();
	final List<EventoAuditoria> eventos = new ArrayList<>();
	final Set<UUID> coordinadores = new HashSet<>();
	final Map<UUID, Set<UUID>> lectoresMatricula = new HashMap<>();
	ConfiguracionCompetencia configuracion = new ConfiguracionCompetencia(EscalaCalificacion.VIGESIMAL,
		ReglaAgregacionItems.PROMEDIO_SIMPLE, ReglaConsolidacionPeriodos.PROMEDIO_PERIODOS);
	TablaConversion tabla;
	final RepositorioCalificaciones repositorioCalificaciones = new Calificaciones();

	AcademicoEnMemoria() {
		actores.put("docente", new ActorAcademico(UUID.randomUUID(), docente, Set.of("DOCENTE")));
		actores.put("otro", new ActorAcademico(UUID.randomUUID(), UUID.randomUUID(), Set.of("DOCENTE")));
		actores.put("admin", new ActorAcademico(UUID.randomUUID(), UUID.randomUUID(), Set.of("ADMINISTRADOR")));
		periodo(periodo1, 1, EstadoPeriodo.EN_CURSO, LocalDateTime.of(2026, 6, 30, 23, 59));
		periodo(periodo2, 2, EstadoPeriodo.PLANIFICADO, LocalDateTime.of(2026, 9, 30, 23, 59));
	}

	void periodo(UUID id, int orden, EstadoPeriodo estado, LocalDateTime limite) {
		periodos.put(id, new PeriodoAcademico(id, anio, orden, estado, limite, true));
	}

	@Override public Optional<ActorAcademico> buscarActor(String nombreUsuario) {
		return Optional.ofNullable(actores.get(nombreUsuario));
	}

	@Override public Optional<CargaAcademica> buscarCarga(UUID id) {
		return id.equals(carga) ? Optional.of(new CargaAcademica(carga, oferta, seccion, docente, anio, true))
			: Optional.empty();
	}

	@Override public Optional<CompetenciaConfigurada> buscarCompetencia(UUID id) {
		return id.equals(competencia)
			? Optional.of(new CompetenciaConfigurada(competencia, oferta, UUID.randomUUID(), configuracion, true))
			: Optional.empty();
	}

	@Override public Optional<PeriodoAcademico> buscarPeriodo(UUID id) {
		return Optional.ofNullable(periodos.get(id));
	}

	@Override public List<PeriodoAcademico> listarPeriodosDelAnio(UUID anioId) {
		return List.copyOf(periodos.values());
	}

	@Override public Optional<MatriculaAcademica> buscarMatricula(UUID id) {
		return id.equals(matricula1) || id.equals(matricula2)
			? Optional.of(new MatriculaAcademica(id, seccion, anio, true)) : Optional.empty();
	}

	@Override public List<UUID> listarMatriculasVigentes(UUID seccionId, UUID anioId) {
		return List.of(matricula1, matricula2);
	}

	@Override public Optional<TablaConversion> buscarTablaConversionVigente() {
		return Optional.ofNullable(tabla);
	}

	@Override public boolean coordinaOferta(UUID docenteId, UUID ofertaCursoId) {
		return coordinadores.contains(docenteId);
	}

	@Override public boolean puedeConsultarMatricula(UUID personaId, UUID matriculaId) {
		return lectoresMatricula.getOrDefault(matriculaId, Set.of()).contains(personaId);
	}

	@Override public void crear(ItemEvaluacion item) {
		items.put(item.id(), item);
	}

	@Override public Optional<ItemEvaluacion> buscar(UUID id) {
		return Optional.ofNullable(items.get(id));
	}

	@Override public boolean actualizar(ItemEvaluacion item) {
		return items.replace(item.id(), item) != null;
	}

	@Override public List<ItemEvaluacion> listarActivos(UUID cargaId, UUID periodoId) {
		return items.values().stream().filter(ItemEvaluacion::activo)
			.filter(item -> periodoId == null || item.periodoAcademicoId().equals(periodoId)).toList();
	}

	@Override public boolean tieneCalificaciones(UUID itemId) {
		return calificaciones.values().stream().anyMatch(c -> c.itemEvaluacionId().equals(itemId));
	}

	@Override public UUID guardarFinal(UUID matriculaId, UUID competenciaId, NotaConsolidada nota, Instant ahora) {
		UUID id = finales.computeIfAbsent(matriculaId + "/" + competenciaId, clave -> UUID.randomUUID());
		notasFinales.put(id, new NotaFinalGuardada(id, competenciaId, nota, ahora));
		return id;
	}

	@Override public void guardarPeriodo(UUID finalId, UUID matriculaId, UUID periodoId, NotaConsolidada nota,
		Instant ahora) {
		notasPeriodo.put(matriculaId + "/" + periodoId,
			new NotaPeriodoGuardada(notasFinales.get(finalId).configuracionCompetenciaId(), periodoId, nota, ahora));
	}

	@Override public List<NotaPeriodoGuardada> listarPeriodos(UUID matriculaId, UUID competenciaId) {
		return listarPeriodosDeMatricula(matriculaId).stream()
			.filter(n -> n.configuracionCompetenciaId().equals(competenciaId)).toList();
	}

	@Override public List<NotaPeriodoGuardada> listarPeriodosDeMatricula(UUID matriculaId) {
		return notasPeriodo.entrySet().stream().filter(e -> e.getKey().startsWith(matriculaId + "/"))
			.map(Map.Entry::getValue).toList();
	}

	@Override public List<NotaFinalGuardada> listarFinales(UUID matriculaId) {
		return finales.entrySet().stream().filter(e -> e.getKey().startsWith(matriculaId + "/"))
			.map(e -> notasFinales.get(e.getValue())).toList();
	}

	@Override public void registrar(EventoAuditoria evento) {
		eventos.add(evento);
	}

	private class Calificaciones implements RepositorioCalificaciones {
		@Override public Optional<CalificacionEstudiante> buscarParaActualizar(UUID itemId, UUID matriculaId) {
			return calificaciones.values().stream()
				.filter(c -> c.itemEvaluacionId().equals(itemId) && c.matriculaId().equals(matriculaId)).findFirst();
		}

		@Override public void crear(CalificacionEstudiante calificacion) {
			calificaciones.put(calificacion.id(), calificacion);
		}

		@Override public boolean actualizar(CalificacionEstudiante calificacion) {
			return calificaciones.replace(calificacion.id(), calificacion) != null;
		}

		@Override public void guardarEvidencia(UUID calificacionId, EvidenciaPedagogica evidencia) {
			evidencias.put(calificacionId, evidencia);
		}

		@Override public List<CalificacionEstudiante> listarPorItem(UUID itemId) {
			return calificaciones.values().stream().filter(c -> c.itemEvaluacionId().equals(itemId)).toList();
		}

		@Override public List<NotaDeMatricula> listarParaConsolidar(UUID cargaId, UUID competenciaId, UUID periodoId) {
			return calificaciones.values().stream().filter(c -> {
				ItemEvaluacion item = items.get(c.itemEvaluacionId());
				return item.activo() && item.periodoAcademicoId().equals(periodoId);
			}).map(c -> {
				ItemEvaluacion item = items.get(c.itemEvaluacionId());
				return new NotaDeMatricula(c.matriculaId(), new CalculadoraNotas.NotaItem(c.valor(), item.peso(),
					item.fechaEvaluacion(), c.fechaRegistro()));
			}).toList();
		}

		@Override public List<CalificacionDetallada> listarPorMatricula(UUID matriculaId) {
			return calificaciones.values().stream().filter(c -> c.matriculaId().equals(matriculaId)).map(c -> {
				ItemEvaluacion item = items.get(c.itemEvaluacionId());
				return new CalificacionDetallada(c, item.nombre(), item.tipo(), item.cargaAcademicaId(),
					item.configuracionCompetenciaId(), item.periodoAcademicoId(), evidencias.get(c.id()));
			}).toList();
		}

		@Override public Optional<CalificacionEstudiante> buscar(UUID id) {
			return Optional.ofNullable(calificaciones.get(id));
		}
	}
}
