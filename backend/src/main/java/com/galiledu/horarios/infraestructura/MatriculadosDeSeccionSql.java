package com.galiledu.horarios.infraestructura;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import com.galiledu.horarios.aplicacion.MatriculadosDeSeccion;

/**
 * Cuenta matricula.matricula por SQL de solo lectura. La condición de vacante es la de
 * {@code Seccion.SQL_OCUPA_VACANTE}
 * (Configuración), copiada aquí porque {@code Seccion} es una entidad de otro módulo y no se importa; la prueba
 * {@code MatriculadosDeSeccionSqlIT} falla si las dos condiciones divergen. Cuando Matrícula ofrezca un servicio
 * equivalente, solo se reemplaza esta clase.
 */
@Component
public class MatriculadosDeSeccionSql implements MatriculadosDeSeccion {
	/** Misma condición que {@code Seccion.SQL_OCUPA_VACANTE}: todas las matrículas menos RETIRADO y ANULADA. */
	static final String SQL_OCUPA_VACANTE = "m.estado NOT IN ('RETIRADO', 'ANULADA')";

	private final JdbcClient jdbc;

	public MatriculadosDeSeccionSql(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public int de(UUID seccionId) {
		return jdbc.sql("SELECT count(*) FROM matricula.matricula m WHERE m.seccion_id = :seccionId AND "
				+ SQL_OCUPA_VACANTE).param("seccionId", seccionId).query(Integer.class).single();
	}
}
