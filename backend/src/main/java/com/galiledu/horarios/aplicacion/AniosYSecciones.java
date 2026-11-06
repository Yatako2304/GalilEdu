package com.galiledu.horarios.aplicacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Los años escolares y las secciones de cada grado que el Coordinador elige para armar un horario. Es de solo
 * lectura: Configuración los ofrece solo al Personal administrativo, así que Horarios los lee por su cuenta. Hoy lo
 * resuelve {@code AniosYSeccionesSql}; cuando Configuración ofrezca un servicio equivalente para otros roles, solo se
 * reemplaza esa clase.
 */
public interface AniosYSecciones {
	/** Un año escolar. {@code estado} es PLANIFICADO, ACTIVO o CERRADO. */
	record AnioLeido(UUID id, int anio, String estado) {
	}

	/** Una sección de un grado. {@code estado} es ACTIVA o INACTIVA. */
	record SeccionLeida(UUID id, String nombre, String estado) {
	}

	/** Un grado del catálogo con las secciones que tiene en el año (puede no tener ninguna). */
	record GradoLeido(String codigo, String nombre, String nivel, int numero, List<SeccionLeida> secciones) {
	}

	/** Todos los años, del más reciente al más antiguo. */
	List<AnioLeido> anios();

	/** Los 11 grados del catálogo, en su orden, con sus secciones del año; vacío si el año no existe. */
	Optional<List<GradoLeido>> gradosDelAnio(UUID anioEscolarId);
}
