package com.galiledu.horarios.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.horarios.dominio.AsignacionHoraria;
import com.galiledu.horarios.dominio.DiaSemana;
import com.galiledu.horarios.dominio.HorarioSeccion;
import com.galiledu.horarios.dominio.Subgrupo;

/** Escritura y lectura de lo que el módulo Horarios posee: versiones, asignaciones y subgrupos. */
public interface RepositorioHorarios {
	/** Crea la siguiente versión de la sección en estado borrador, sin repetir número. */
	HorarioSeccion crearBorrador(UUID seccionId);

	Optional<HorarioSeccion> buscarHorario(UUID id);

	/** Versiones activas de la sección, de la más reciente a la más antigua. */
	List<HorarioSeccion> listarHorarios(UUID seccionId);

	boolean desactivarHorario(UUID id);

	/**
	 * Serializa, hasta el fin de la transacción, a quienes validan y registran en el mismo bloque
	 * y día, para que dos registros simultáneos no pasen la validación a la vez.
	 */
	void serializarBloque(UUID bloqueHorarioId, DiaSemana dia);

	void guardarAsignacion(AsignacionHoraria asignacion);

	/** Reemplaza los datos de una asignación activa; falso si no existe o está inactiva. */
	boolean actualizarAsignacion(AsignacionHoraria asignacion);

	Optional<AsignacionHoraria> buscarAsignacion(UUID id);

	List<AsignacionHoraria> listarAsignaciones(UUID horarioSeccionId);

	boolean desactivarAsignacion(UUID id);

	void guardarSubgrupo(Subgrupo subgrupo);

	/** Incluye subgrupos inactivos: el nombre es único por sección aunque se haya desactivado. */
	boolean existeSubgrupo(UUID seccionId, String nombre);

	Optional<Subgrupo> buscarSubgrupo(UUID id);

	List<Subgrupo> listarSubgrupos(UUID seccionId);

	boolean desactivarSubgrupo(UUID id);

	boolean tieneAsignacionesActivas(UUID subgrupoId);

	/** Agrega la matrícula o la reincorpora si había sido retirada. */
	void agregarMiembro(UUID subgrupoId, UUID matriculaId);

	boolean retirarMiembro(UUID subgrupoId, UUID matriculaId);

	/** Matrículas activas del subgrupo. */
	List<UUID> listarMiembros(UUID subgrupoId);
}
