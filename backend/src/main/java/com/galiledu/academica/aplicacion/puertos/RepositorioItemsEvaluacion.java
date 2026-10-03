package com.galiledu.academica.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.academica.dominio.ItemEvaluacion;

public interface RepositorioItemsEvaluacion {
	void crear(ItemEvaluacion item);

	Optional<ItemEvaluacion> buscar(UUID id);

	boolean actualizar(ItemEvaluacion item);

	/** periodoAcademicoId nulo devuelve los ítems activos de todos los periodos. */
	List<ItemEvaluacion> listarActivos(UUID cargaAcademicaId, UUID periodoAcademicoId);

	boolean tieneCalificaciones(UUID itemId);
}
