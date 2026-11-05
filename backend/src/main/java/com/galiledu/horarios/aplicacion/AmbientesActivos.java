package com.galiledu.horarios.aplicacion;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Los ambientes físicos de Configuración. Es de solo lectura. Hoy lo resuelve {@code AmbientesActivosSql}. */
public interface AmbientesActivos {
	/** Un ambiente a elegir: {@code tipo} es AULA_REGULAR o ESPACIO_ESPECIAL. */
	record AmbienteDisponible(UUID id, String nombre, String tipo, int aforoMaximo) {
	}

	/** Los ambientes ACTIVOS, por nombre. */
	List<AmbienteDisponible> activos();

	/** El nombre de cada ambiente indicado, esté activo o no (un horario puede usar uno desactivado después). */
	Map<UUID, String> nombresPorId(Collection<UUID> ambienteIds);
}
