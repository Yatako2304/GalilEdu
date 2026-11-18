package com.galiledu.academico.infraestructura;

import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Datos de otros módulos que los ambientes solo leen: las secciones que usan un aula (configuracion) y sus matrículas
 * (matricula). Mientras esos módulos no ofrezcan un servicio para esto, se leen con SQL de solo lectura; cuando lo
 * tengan, solo se reemplaza esta clase.
 */
@Component
public class ConsultasAmbientes {
	private final JdbcClient jdbc;

	public ConsultasAmbientes(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/**
	 * La sección con más matriculados entre las que usan el aula, para validar el aforo (RF-111): {@code grado} es el
	 * nombre del grado ("1.° de secundaria") y {@code seccion} el de la sección ("A").
	 */
	public record SeccionOcupada(String grado, String seccion, int matriculados) {
	}

	/**
	 * Solo cuentan las secciones ACTIVAS de años escolares que no están CERRADOS (PLANIFICADO y ACTIVO sí): una
	 * sección inactiva o de un año cerrado ya no ocupa el aula. Cuenta las matrículas que ocupan vacante, es decir,
	 * todas menos RETIRADO y ANULADA; una sección sin matrículas cuenta 0 y puede ser el resultado. Sin secciones que
	 * usen el aula, devuelve vacío. En un empate gana la sección del año más antiguo, el grado más bajo y el nombre
	 * menor, para que el mensaje al usuario sea siempre el mismo.
	 */
	public Optional<SeccionOcupada> seccionMasOcupada(UUID ambienteId) {
		// La condición de la matrícula (m.estado NOT IN ('RETIRADO', 'ANULADA')) repite
		// configuracion.dominio.Seccion.SQL_OCUPA_VACANTE: este módulo no puede importar esa clase. Si la regla de
		// "ocupa vacante" cambia allí, cambia también aquí.
		return jdbc.sql("""
				SELECT g.nombre AS grado, s.nombre AS seccion, count(m.id)::int AS matriculados
				FROM configuracion.seccion s
				JOIN configuracion.anio_escolar a ON a.id = s.anio_escolar_id
				JOIN configuracion.grado g ON g.codigo = s.grado_codigo
				LEFT JOIN matricula.matricula m ON m.seccion_id = s.id AND m.estado NOT IN ('RETIRADO', 'ANULADA')
				WHERE s.ambiente_id = :ambienteId AND s.estado = 'ACTIVA' AND a.estado <> 'CERRADO'
				GROUP BY s.id, s.nombre, g.nombre, g.orden, a.anio
				ORDER BY matriculados DESC, a.anio, g.orden, s.nombre
				LIMIT 1""")
				.param("ambienteId", ambienteId).query(SeccionOcupada.class).optional();
	}
}
