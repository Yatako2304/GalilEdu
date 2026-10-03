package com.galiledu.academica.infraestructura.persistencia;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import com.galiledu.academica.aplicacion.puertos.RegistroAuditoriaAcademica;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

/** Escribe en seguridad.registro_auditoria dentro de la transacción del caso de uso académico. */
@Repository
public class RegistroAuditoriaAcademicaPostgres implements RegistroAuditoriaAcademica {
	private static final String MODULO = "GESTION_ACADEMICA";
	private final JdbcOperations jdbc;

	public RegistroAuditoriaAcademicaPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public void registrar(EventoAuditoria evento) {
		jdbc.update("""
			INSERT INTO seguridad.registro_auditoria
			(id, usuario_responsable_id, modulo, entidad, entidad_id, operacion,
			 referencias, valor_anterior, valor_nuevo)
			VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb, ?::jsonb)
			""", UUID.randomUUID(), evento.usuarioResponsableId(), MODULO, evento.entidad(), evento.entidadId(),
			evento.operacion(), json(evento.referencias()), json(evento.valorAnterior()), json(evento.valorNuevo()));
	}

	/** Objeto JSON plano de cadenas; el orden fijo facilita comparar eventos. */
	static String json(Map<String, String> valores) {
		if (valores == null) return null;
		StringBuilder json = new StringBuilder("{");
		new TreeMap<>(valores).forEach((clave, valor) -> {
			if (json.length() > 1) json.append(',');
			json.append(texto(clave)).append(':').append(valor == null ? "null" : texto(valor));
		});
		return json.append('}').toString();
	}

	private static String texto(String valor) {
		StringBuilder resultado = new StringBuilder("\"");
		for (char caracter : valor.toCharArray()) {
			switch (caracter) {
				case '"' -> resultado.append("\\\"");
				case '\\' -> resultado.append("\\\\");
				case '\n' -> resultado.append("\\n");
				case '\r' -> resultado.append("\\r");
				case '\t' -> resultado.append("\\t");
				default -> {
					if (caracter < 0x20) resultado.append("\\u%04x".formatted((int) caracter));
					else resultado.append(caracter);
				}
			}
		}
		return resultado.append('"').toString();
	}
}
