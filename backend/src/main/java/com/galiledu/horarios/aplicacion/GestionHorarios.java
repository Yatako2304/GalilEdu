package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario;
import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario.BloqueReferencia;
import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario.CargaAcademicaReferencia;
import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario.PeriodoReferencia;
import com.galiledu.horarios.aplicacion.puertos.ConsultaReferenciasHorario.SeccionReferencia;
import com.galiledu.horarios.aplicacion.puertos.RepositorioHorarios;
import com.galiledu.horarios.dominio.AsignacionHoraria;
import com.galiledu.horarios.dominio.DiaSemana;
import com.galiledu.horarios.dominio.HorarioSeccion;
import com.galiledu.horarios.dominio.OcupacionHoraria;
import com.galiledu.horarios.dominio.Subgrupo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de escritura de Horarios; cada método es una transacción completa. */
@Service
public class GestionHorarios {
	private final RepositorioHorarios repositorio;
	private final ConsultaReferenciasHorario referencias;
	private final VerificarDisponibilidadHorario disponibilidad;

	public GestionHorarios(RepositorioHorarios repositorio, ConsultaReferenciasHorario referencias,
		VerificarDisponibilidadHorario disponibilidad) {
		this.repositorio = repositorio;
		this.referencias = referencias;
		this.disponibilidad = disponibilidad;
	}

	@Transactional
	public HorarioSeccion crearBorrador(UUID seccionId) {
		seccionActiva(seccionId);
		return repositorio.crearBorrador(seccionId);
	}

	@Transactional(readOnly = true)
	public HorarioSeccion buscarHorario(UUID id) {
		return repositorio.buscarHorario(id).filter(HorarioSeccion::activo)
			.orElseThrow(() -> new NoSuchElementException("Horario no encontrado"));
	}

	@Transactional(readOnly = true)
	public List<HorarioSeccion> listarHorarios(UUID seccionId) {
		return repositorio.listarHorarios(seccionId);
	}

	@Transactional
	public void desactivarHorario(UUID id) {
		if (!repositorio.desactivarHorario(id)) {
			throw new NoSuchElementException("Horario no encontrado");
		}
	}

	/** Revisa una asignación sin guardarla; sirve para avisar antes de confirmar. */
	@Transactional(readOnly = true)
	public VerificarDisponibilidadHorario.Resultado verificarAsignacion(UUID horarioSeccionId,
		SolicitudAsignacion solicitud) {
		return disponibilidad.ejecutar(resolver(UUID.randomUUID(), horarioSeccionId, solicitud).ocupacion());
	}

	@Transactional
	public AsignacionHoraria registrarAsignacion(UUID horarioSeccionId, SolicitudAsignacion solicitud) {
		Resolucion resolucion = resolver(UUID.randomUUID(), horarioSeccionId, solicitud);
		validarSinCruces(resolucion);
		repositorio.guardarAsignacion(resolucion.asignacion());
		return resolucion.asignacion();
	}

	/** Reemplaza los datos de una asignación; no la mueve a otro horario. */
	@Transactional
	public AsignacionHoraria modificarAsignacion(UUID id, SolicitudAsignacion solicitud) {
		AsignacionHoraria actual = asignacionActiva(id);
		Resolucion resolucion = resolver(id, actual.horarioSeccionId(), solicitud);
		validarSinCruces(resolucion);
		if (!repositorio.actualizarAsignacion(resolucion.asignacion())) {
			throw new NoSuchElementException("Asignación no encontrada");
		}
		return resolucion.asignacion();
	}

	@Transactional
	public void retirarAsignacion(UUID id) {
		if (!repositorio.desactivarAsignacion(id)) {
			throw new NoSuchElementException("Asignación no encontrada");
		}
	}

	@Transactional(readOnly = true)
	public List<AsignacionHoraria> listarAsignaciones(UUID horarioSeccionId) {
		buscarHorario(horarioSeccionId);
		return repositorio.listarAsignaciones(horarioSeccionId);
	}

	@Transactional
	public Subgrupo crearSubgrupo(UUID seccionId, String nombre) {
		seccionActiva(seccionId);
		Subgrupo subgrupo = Subgrupo.nuevo(seccionId, nombre);
		if (repositorio.existeSubgrupo(seccionId, subgrupo.nombre())) {
			throw new HorarioInvalidoException("Ya existe un subgrupo con ese nombre en la sección");
		}
		repositorio.guardarSubgrupo(subgrupo);
		return subgrupo;
	}

	@Transactional(readOnly = true)
	public List<Subgrupo> listarSubgrupos(UUID seccionId) {
		return repositorio.listarSubgrupos(seccionId);
	}

	@Transactional
	public void desactivarSubgrupo(UUID id) {
		subgrupoActivo(id);
		if (repositorio.tieneAsignacionesActivas(id)) {
			throw new HorarioInvalidoException("El subgrupo tiene asignaciones activas");
		}
		repositorio.desactivarSubgrupo(id);
	}

	@Transactional
	public void agregarMiembro(UUID subgrupoId, UUID matriculaId) {
		Subgrupo subgrupo = subgrupoActivo(subgrupoId);
		var matricula = referencias.buscarMatricula(matriculaId).filter(m -> m.activa())
			.orElseThrow(() -> new HorarioInvalidoException("La matrícula no existe o está inactiva"));
		if (!matricula.seccionId().equals(subgrupo.seccionId())) {
			throw new HorarioInvalidoException("La matrícula pertenece a otra sección");
		}
		repositorio.agregarMiembro(subgrupoId, matriculaId);
	}

	@Transactional
	public void retirarMiembro(UUID subgrupoId, UUID matriculaId) {
		if (!repositorio.retirarMiembro(subgrupoId, matriculaId)) {
			throw new NoSuchElementException("La matrícula no es miembro activo del subgrupo");
		}
	}

	@Transactional(readOnly = true)
	public List<UUID> listarMiembros(UUID subgrupoId) {
		subgrupoActivo(subgrupoId);
		return repositorio.listarMiembros(subgrupoId);
	}

	private void validarSinCruces(Resolucion resolucion) {
		AsignacionHoraria asignacion = resolucion.asignacion();
		repositorio.serializarBloque(asignacion.bloqueHorarioId(), asignacion.dia());
		var resultado = disponibilidad.ejecutar(resolucion.ocupacion());
		if (!resultado.disponible()) {
			throw new CruceHorarioException(resultado.cruces());
		}
	}

	/** Comprueba lo que las claves foráneas no garantizan: año, sección, día lectivo y periodos. */
	private Resolucion resolver(UUID id, UUID horarioSeccionId, SolicitudAsignacion solicitud) {
		Objects.requireNonNull(solicitud, "La solicitud es obligatoria");
		HorarioSeccion horario = buscarHorario(horarioSeccionId);
		SeccionReferencia seccion = seccionActiva(horario.seccionId());
		CargaAcademicaReferencia carga = referencias.buscarCargaAcademica(solicitud.cargaAcademicaId())
			.filter(CargaAcademicaReferencia::activa)
			.orElseThrow(() -> new HorarioInvalidoException("La carga académica no existe o está inactiva"));
		if (!carga.seccionId().equals(horario.seccionId())) {
			throw new HorarioInvalidoException("La carga académica pertenece a otra sección");
		}
		BloqueReferencia bloque = referencias.buscarBloque(solicitud.bloqueHorarioId())
			.filter(BloqueReferencia::activo)
			.orElseThrow(() -> new HorarioInvalidoException("El bloque no existe o está inactivo"));
		if (!bloque.anioId().equals(seccion.anioId())) {
			throw new HorarioInvalidoException("El bloque pertenece a otro año escolar");
		}
		if (!bloque.diasLectivos().contains(solicitud.dia())) {
			throw new HorarioInvalidoException("El día no es lectivo en la estructura horaria del bloque");
		}
		if (solicitud.espacioFisicoId() != null && !referencias.existeEspacioActivo(solicitud.espacioFisicoId())) {
			throw new HorarioInvalidoException("El espacio físico no existe o está inactivo");
		}
		validarPeriodos(solicitud.periodos(), seccion);
		if (solicitud.subgrupoId() != null) {
			Subgrupo subgrupo = repositorio.buscarSubgrupo(solicitud.subgrupoId()).filter(Subgrupo::activo)
				.orElseThrow(() -> new HorarioInvalidoException("El subgrupo no existe o está inactivo"));
			if (!subgrupo.seccionId().equals(horario.seccionId())) {
				throw new HorarioInvalidoException("El subgrupo pertenece a otra sección");
			}
		}
		AsignacionHoraria asignacion = new AsignacionHoraria(id, horarioSeccionId, carga.id(),
			bloque.id(), solicitud.espacioFisicoId(), solicitud.subgrupoId(), solicitud.tipoSesion(),
			solicitud.dia(), solicitud.periodos(), true);
		return new Resolucion(asignacion, new OcupacionHoraria(id, horarioSeccionId, seccion.nombre(),
			carga.curso(), carga.docenteId(), solicitud.espacioFisicoId(), solicitud.subgrupoId(),
			bloque.id(), solicitud.dia(), solicitud.periodos()));
	}

	private void validarPeriodos(Set<UUID> pedidos, SeccionReferencia seccion) {
		List<PeriodoReferencia> existentes = referencias.buscarPeriodos(pedidos);
		if (existentes.size() != pedidos.size()) {
			throw new HorarioInvalidoException("Algún periodo no existe");
		}
		for (PeriodoReferencia periodo : existentes) {
			if (!periodo.activo()) {
				throw new HorarioInvalidoException("Algún periodo está inactivo");
			}
			if (!periodo.anioId().equals(seccion.anioId())) {
				throw new HorarioInvalidoException("Algún periodo pertenece a otro año escolar");
			}
		}
	}

	private SeccionReferencia seccionActiva(UUID seccionId) {
		return referencias.buscarSeccion(seccionId).filter(SeccionReferencia::activa)
			.orElseThrow(() -> new HorarioInvalidoException("La sección no existe o está inactiva"));
	}

	private AsignacionHoraria asignacionActiva(UUID id) {
		return repositorio.buscarAsignacion(id).filter(AsignacionHoraria::activa)
			.orElseThrow(() -> new NoSuchElementException("Asignación no encontrada"));
	}

	private Subgrupo subgrupoActivo(UUID id) {
		return repositorio.buscarSubgrupo(id).filter(Subgrupo::activo)
			.orElseThrow(() -> new NoSuchElementException("Subgrupo no encontrado"));
	}

	/**
	 * Datos de una asignación; el docente sale de la carga académica y el año, del bloque. Los
	 * periodos son obligatorios: la base de datos admite cero, pero sin periodo no habría
	 * validación de cruces posible.
	 */
	public record SolicitudAsignacion(UUID cargaAcademicaId, UUID bloqueHorarioId,
		UUID espacioFisicoId, UUID subgrupoId, String tipoSesion, DiaSemana dia, Set<UUID> periodos) {
		public SolicitudAsignacion {
			Objects.requireNonNull(cargaAcademicaId, "La carga académica es obligatoria");
			Objects.requireNonNull(bloqueHorarioId, "El bloque es obligatorio");
			Objects.requireNonNull(dia, "El día es obligatorio");
			if (periodos == null || periodos.isEmpty()) {
				throw new IllegalArgumentException("La asignación debe regir en al menos un periodo");
			}
			periodos = Set.copyOf(periodos);
		}
	}

	private record Resolucion(AsignacionHoraria asignacion, OcupacionHoraria ocupacion) {}
}
