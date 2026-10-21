package com.galiledu.configuracion.infraestructura;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.galiledu.configuracion.dominio.EstadoAnioEscolar;
import com.galiledu.configuracion.dominio.Regimen;

/**
 * Lecturas de solo consulta de la configuración del año escolar (HU-74): el listado de años con sus periodos contados.
 */
@Repository
public class ConsultasConfiguracion {
	private final JdbcClient jdbc;

	public ConsultasConfiguracion(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	public record AnioListado(UUID id, int anio, LocalDate fechaInicio, LocalDate fechaFin, Regimen regimen,
			EstadoAnioEscolar estado, int cantidadPeriodos) {
	}

	/** Del más reciente al más antiguo, como el listado "CI-01 · Año escolar" de Figma. */
	public List<AnioListado> anios() {
		return jdbc.sql("""
				SELECT a.id, a.anio, a.fecha_inicio, a.fecha_fin, a.regimen, a.estado,
				       (SELECT count(*) FROM configuracion.periodo_academico p WHERE p.anio_escolar_id = a.id)
				FROM configuracion.anio_escolar a
				ORDER BY a.anio DESC""")
				.query((rs, i) -> new AnioListado(rs.getObject(1, UUID.class), rs.getInt(2),
						rs.getObject(3, LocalDate.class), rs.getObject(4, LocalDate.class),
						Regimen.valueOf(rs.getString(5)), EstadoAnioEscolar.valueOf(rs.getString(6)), rs.getInt(7)))
				.list();
	}
}
