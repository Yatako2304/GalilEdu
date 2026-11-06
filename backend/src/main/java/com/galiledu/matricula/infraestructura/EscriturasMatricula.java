package com.galiledu.matricula.infraestructura;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoMatricula;

/** Escrituras de la matrícula del Personal administrativo (HU-24, HU-72). SQL directo, igual que las consultas. */
@Repository
public class EscriturasMatricula {
	private final JdbcClient jdbc;

	public EscriturasMatricula(JdbcClient jdbc) {
		this.jdbc = jdbc;
	}

	/** Bloquea las secciones del grado para que dos matrículas a la vez no pasen del cupo. */
	public void bloquearSeccionesDelGrado(UUID anioEscolarId, String gradoCodigo) {
		jdbc.sql("SELECT id FROM configuracion.seccion WHERE anio_escolar_id = ? AND grado_codigo = ? FOR UPDATE")
				.params(anioEscolarId, gradoCodigo).query(UUID.class).list();
	}

	public UUID insertarMatricula(UUID estudianteId, UUID anioEscolarId, String gradoCodigo, UUID seccionId,
			String tipo, UUID creadoPor) {
		return jdbc.sql("""
				INSERT INTO matricula.matricula (estudiante_id, anio_escolar_id, grado_codigo, seccion_id,
				                                 tipo_estudiante, estado, creado_por)
				VALUES (?, ?, ?, ?, ?, 'SECCION_RESERVADA', ?) RETURNING id""")
				.params(estudianteId, anioEscolarId, gradoCodigo, seccionId, tipo, creadoPor)
				.query(UUID.class).single();
	}

	public void insertarDocumento(UUID matriculaId, UUID documentoRequeridoId, ArchivoMatricula archivo,
			UUID subidoPor) {
		jdbc.sql("""
				INSERT INTO matricula.documento_matricula (matricula_id, documento_requerido_id, nombre_archivo,
				                                           tipo_mime, tamano_bytes, contenido, subido_por)
				VALUES (?, ?, ?, ?, ?, ?, ?)""")
				.params(matriculaId, documentoRequeridoId, archivo.nombre(), archivo.tipoMime(),
						archivo.contenido().length, archivo.contenido(), subidoPor)
				.update();
	}

	/**
	 * Sube o reemplaza el archivo de un documento: queda EN_REVISION, sin observación ni revisor, porque el colegio
	 * debe volver a revisarlo.
	 */
	public void guardarDocumento(UUID matriculaId, UUID documentoRequeridoId, ArchivoMatricula archivo,
			UUID subidoPor) {
		jdbc.sql("""
				INSERT INTO matricula.documento_matricula (matricula_id, documento_requerido_id, nombre_archivo,
				                                           tipo_mime, tamano_bytes, contenido, subido_por)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				ON CONFLICT (matricula_id, documento_requerido_id) DO UPDATE SET
				    nombre_archivo = EXCLUDED.nombre_archivo, tipo_mime = EXCLUDED.tipo_mime,
				    tamano_bytes = EXCLUDED.tamano_bytes, contenido = EXCLUDED.contenido,
				    estado = 'EN_REVISION', observacion = NULL, subido_por = EXCLUDED.subido_por,
				    subido_en = now(), revisado_por = NULL, revisado_en = NULL""")
				.params(matriculaId, documentoRequeridoId, archivo.nombre(), archivo.tipoMime(),
						archivo.contenido().length, archivo.contenido(), subidoPor)
				.update();
	}

	/** Pasa de SECCION_RESERVADA a EN_VALIDACION: los documentos quedan completos y el colegio debe revisarlos. */
	public boolean marcarEnValidacion(UUID matriculaId) {
		return jdbc.sql("""
				UPDATE matricula.matricula SET estado = 'EN_VALIDACION', actualizado_en = now()
				WHERE id = ? AND estado = 'SECCION_RESERVADA'""").param(matriculaId).update() > 0;
	}

	/** Mueve la matrícula a otra sección y deja el rastro en el historial de cambios (HU-26). */
	public void cambiarSeccion(UUID matriculaId, UUID origenId, UUID destinoId, UUID realizadoPor) {
		jdbc.sql("UPDATE matricula.matricula SET seccion_id = ?, actualizado_en = now() WHERE id = ?")
				.params(destinoId, matriculaId).update();
		if (origenId != null) {
			jdbc.sql("""
					INSERT INTO matricula.cambio_seccion (matricula_id, seccion_origen_id, seccion_destino_id,
					                                      realizado_por)
					VALUES (?, ?, ?, ?)""")
					.params(matriculaId, origenId, destinoId, realizadoPor).update();
		}
	}
}
