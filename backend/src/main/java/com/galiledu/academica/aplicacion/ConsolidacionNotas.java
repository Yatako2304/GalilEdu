package com.galiledu.academica.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.CargaAcademica;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.CompetenciaConfigurada;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.PeriodoAcademico;
import com.galiledu.academica.aplicacion.puertos.RepositorioCalificaciones;
import com.galiledu.academica.aplicacion.puertos.RepositorioCalificaciones.NotaDeMatricula;
import com.galiledu.academica.aplicacion.puertos.RepositorioNotasConsolidadas;
import com.galiledu.academica.dominio.CalculadoraNotas;
import com.galiledu.academica.dominio.CalculadoraNotas.NotaItem;
import com.galiledu.academica.dominio.CalculadoraNotas.NotaPeriodo;
import com.galiledu.academica.dominio.CalculoNotaIndeterminadoException;
import com.galiledu.academica.dominio.Calificacion;
import com.galiledu.academica.dominio.ConfiguracionCompetencia;
import com.galiledu.academica.dominio.EstadoPeriodo;
import com.galiledu.academica.dominio.TablaConversion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Componente Consolidación de Notas: calcula la nota de periodo por competencia y recalcula la
 * nota final con los periodos disponibles, aplicando las reglas y la tabla de conversión vigente.
 */
@Service
public class ConsolidacionNotas {
	private final RepositorioCalificaciones calificaciones;
	private final RepositorioNotasConsolidadas notas;
	private final ConsultaContextoAcademico contexto;
	private final VerificacionesAcademicas verificaciones;
	private final Clock reloj;

	public ConsolidacionNotas(RepositorioCalificaciones calificaciones, RepositorioNotasConsolidadas notas,
		ConsultaContextoAcademico contexto, Clock reloj) {
		this.calificaciones = Objects.requireNonNull(calificaciones);
		this.notas = Objects.requireNonNull(notas);
		this.contexto = Objects.requireNonNull(contexto);
		this.verificaciones = new VerificacionesAcademicas(contexto);
		this.reloj = Objects.requireNonNull(reloj);
	}

	@Transactional
	public ResultadoConsolidacion consolidarPeriodo(String nombreUsuario, UUID cargaAcademicaId,
		UUID configuracionCompetenciaId, UUID periodoAcademicoId) {
		ActorAcademico actor = verificaciones.actor(nombreUsuario);
		CargaAcademica carga = verificaciones.cargaActiva(cargaAcademicaId);
		verificaciones.exigirDocenteOAdministrador(actor, carga);
		CompetenciaConfigurada competencia = verificaciones.competenciaDeCarga(configuracionCompetenciaId, carga);
		PeriodoAcademico periodo = verificaciones.periodoDeCarga(periodoAcademicoId, carga);
		if (periodo.estado() == EstadoPeriodo.PLANIFICADO) {
			throw new SolicitudAcademicaInvalidaException("El periodo aún no ha iniciado");
		}
		ConfiguracionCompetencia configuracion = competencia.configuracion();
		if (!configuracion.admiteCalculo()) {
			throw new SolicitudAcademicaInvalidaException(
				"La escala cualitativa no admite reglas de promedio sin una equivalencia aprobada");
		}
		TablaConversion tabla = contexto.buscarTablaConversionVigente().orElse(null);
		Map<UUID, Integer> ordenes = contexto.listarPeriodosDelAnio(carga.anioId()).stream()
			.collect(Collectors.toMap(PeriodoAcademico::id, PeriodoAcademico::orden));
		Map<UUID, List<NotaItem>> porMatricula = calificaciones
			.listarParaConsolidar(carga.id(), competencia.id(), periodo.id()).stream()
			.collect(Collectors.groupingBy(NotaDeMatricula::matriculaId,
				Collectors.mapping(NotaDeMatricula::nota, Collectors.toList())));

		Instant ahora = reloj.instant();
		int calculadas = 0;
		List<UUID> sinNotas = new ArrayList<>();
		Map<UUID, String> pendientes = new LinkedHashMap<>();
		for (UUID matriculaId : contexto.listarMatriculasVigentes(carga.seccionId(), carga.anioId())) {
			List<NotaItem> notasItems = porMatricula.getOrDefault(matriculaId, List.of());
			if (notasItems.isEmpty()) {
				sinNotas.add(matriculaId);
				continue;
			}
			try {
				Calificacion notaPeriodo = CalculadoraNotas.agregarItems(configuracion, notasItems);
				List<NotaPeriodo> periodos = new ArrayList<>();
				periodos.add(new NotaPeriodo(periodo.orden(), notaPeriodo));
				notas.listarPeriodos(matriculaId, competencia.id()).stream()
					.filter(guardada -> !guardada.periodoAcademicoId().equals(periodo.id()))
					.forEach(guardada -> periodos.add(new NotaPeriodo(
						ordenDe(ordenes, guardada.periodoAcademicoId()), guardada.nota().valor())));
				Calificacion notaFinal = CalculadoraNotas.consolidarPeriodos(configuracion, periodos);
				UUID finalId = notas.guardarFinal(matriculaId, competencia.id(),
					CalculadoraNotas.conEquivalente(notaFinal, tabla), ahora);
				notas.guardarPeriodo(finalId, matriculaId, periodo.id(),
					CalculadoraNotas.conEquivalente(notaPeriodo, tabla), ahora);
				calculadas++;
			} catch (CalculoNotaIndeterminadoException e) {
				pendientes.put(matriculaId, e.getMessage());
			}
		}
		return new ResultadoConsolidacion(calculadas, List.copyOf(sinNotas), Map.copyOf(pendientes));
	}

	private static int ordenDe(Map<UUID, Integer> ordenes, UUID periodoId) {
		Integer orden = ordenes.get(periodoId);
		if (orden == null) {
			throw new IllegalStateException("La nota guardada referencia un periodo de otro año escolar");
		}
		return orden;
	}

	/** Matrículas calculadas, sin notas en el periodo o con una regla que no produjo resultado. */
	public record ResultadoConsolidacion(int calculadas, List<UUID> sinNotas, Map<UUID, String> pendientes) {}
}
