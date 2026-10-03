package com.galiledu.academica.aplicacion;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico;
import com.galiledu.academica.aplicacion.puertos.ConsultaContextoAcademico.CargaAcademica;
import com.galiledu.academica.aplicacion.puertos.RepositorioCalificaciones;
import com.galiledu.academica.aplicacion.puertos.RepositorioCalificaciones.CalificacionDetallada;
import com.galiledu.academica.aplicacion.puertos.RepositorioItemsEvaluacion;
import com.galiledu.academica.aplicacion.puertos.RepositorioNotasConsolidadas;
import com.galiledu.academica.aplicacion.puertos.RepositorioNotasConsolidadas.NotaFinalGuardada;
import com.galiledu.academica.aplicacion.puertos.RepositorioNotasConsolidadas.NotaPeriodoGuardada;
import com.galiledu.academica.dominio.CalificacionEstudiante;
import com.galiledu.academica.dominio.ItemEvaluacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Componente Consulta de Notas: lecturas por ítem (docente/coordinador) y por matrícula
 * (estudiante, apoderado vinculado o tutor). La exportación de reportes aún no está implementada.
 */
@Service
@Transactional(readOnly = true)
public class ConsultaNotas {
	private final RepositorioItemsEvaluacion items;
	private final RepositorioCalificaciones calificaciones;
	private final RepositorioNotasConsolidadas notas;
	private final ConsultaContextoAcademico contexto;
	private final VerificacionesAcademicas verificaciones;

	public ConsultaNotas(RepositorioItemsEvaluacion items, RepositorioCalificaciones calificaciones,
		RepositorioNotasConsolidadas notas, ConsultaContextoAcademico contexto) {
		this.items = Objects.requireNonNull(items);
		this.calificaciones = Objects.requireNonNull(calificaciones);
		this.notas = Objects.requireNonNull(notas);
		this.contexto = Objects.requireNonNull(contexto);
		this.verificaciones = new VerificacionesAcademicas(contexto);
	}

	public List<CalificacionEstudiante> calificacionesDeItem(String nombreUsuario, UUID itemId) {
		ActorAcademico actor = verificaciones.actor(nombreUsuario);
		ItemEvaluacion item = items.buscar(Objects.requireNonNull(itemId, "El ítem es obligatorio"))
			.orElseThrow(() -> new NoSuchElementException("Ítem de evaluación no encontrado"));
		CargaAcademica carga = verificaciones.cargaActiva(item.cargaAcademicaId());
		verificaciones.exigirLecturaDeCarga(actor, carga);
		return calificaciones.listarPorItem(item.id());
	}

	public NotasDeMatricula notasDeMatricula(String nombreUsuario, UUID matriculaId) {
		ActorAcademico actor = verificaciones.actor(nombreUsuario);
		Objects.requireNonNull(matriculaId, "La matrícula es obligatoria");
		if (contexto.buscarMatricula(matriculaId).isEmpty()) {
			throw new NoSuchElementException("Matrícula no encontrada");
		}
		if (!actor.esAdministrador() && !contexto.puedeConsultarMatricula(actor.personaId(), matriculaId)) {
			throw new AccesoAcademicoDenegadoException("La cuenta no tiene acceso a las notas de esta matrícula");
		}
		return new NotasDeMatricula(matriculaId, calificaciones.listarPorMatricula(matriculaId),
			notas.listarPeriodosDeMatricula(matriculaId), notas.listarFinales(matriculaId));
	}

	public record NotasDeMatricula(UUID matriculaId, List<CalificacionDetallada> calificaciones,
		List<NotaPeriodoGuardada> notasPeriodo, List<NotaFinalGuardada> notasFinales) {}
}
