package com.galiledu.academica.infraestructura.persistencia;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

import com.galiledu.academica.dominio.Calificacion;
import com.galiledu.academica.dominio.EscalaCalificacion;
import com.galiledu.academica.dominio.NivelCualitativo;

/** Traducción entre Calificacion y el par valor_numerico / valor_cualitativo de las tablas. */
final class ColumnasCalificacion {
	private ColumnasCalificacion() {
	}

	static Calificacion leer(ResultSet rs, String numerico, String cualitativo) throws SQLException {
		BigDecimal vigesimal = rs.getBigDecimal(numerico);
		return vigesimal != null ? Calificacion.vigesimal(vigesimal)
			: Calificacion.cualitativa(NivelCualitativo.valueOf(rs.getString(cualitativo)));
	}

	static NivelCualitativo nivel(ResultSet rs, String columna) throws SQLException {
		String valor = rs.getString(columna);
		return valor == null ? null : NivelCualitativo.valueOf(valor);
	}

	static BigDecimal numerico(Calificacion valor) {
		return valor.escala() == EscalaCalificacion.VIGESIMAL ? valor.vigesimal() : null;
	}

	static String cualitativo(Calificacion valor) {
		return valor.escala() == EscalaCalificacion.CUALITATIVA ? valor.cualitativa().name() : null;
	}

	static Instant instante(ResultSet rs, String columna) throws SQLException {
		Timestamp valor = rs.getTimestamp(columna);
		return valor == null ? null : valor.toInstant();
	}

	static Timestamp timestamp(Instant valor) {
		return valor == null ? null : Timestamp.from(valor);
	}
}
