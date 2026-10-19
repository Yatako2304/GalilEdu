package com.galiledu.estudiantes.aplicacion;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.estudiantes.aplicacion.DtosEstudiantes.DocumentoAdjunto;
import com.galiledu.estudiantes.dominio.DocumentoVinculo;
import com.galiledu.estudiantes.dominio.Parentesco;
import com.galiledu.estudiantes.dominio.VinculoFamiliar;
import com.galiledu.estudiantes.infraestructura.DocumentoVinculoRepositorio;
import com.galiledu.estudiantes.infraestructura.EstudianteRepositorio;

/** HU-7, paso 3: documento que acredita al apoderado (carta poder, resolución de tutela…). */
@Service
public class ServicioDocumentosApoderado {
	private final EstudianteRepositorio estudiantes;
	private final DocumentoVinculoRepositorio documentos;

	public ServicioDocumentosApoderado(EstudianteRepositorio estudiantes, DocumentoVinculoRepositorio documentos) {
		this.estudiantes = estudiantes;
		this.documentos = documentos;
	}

	/** Guarda o reemplaza el documento del apoderado del estudiante. */
	@Transactional
	public DocumentoAdjunto subir(UUID estudianteId, String tipo, MultipartFile archivo) {
		if (tipo == null || tipo.isBlank()) {
			throw ExcepcionNegocio.invalido("Indica qué documento acredita al apoderado");
		}
		if (archivo == null || archivo.isEmpty()) {
			throw ExcepcionNegocio.invalido("Adjunta el archivo del documento");
		}
		if (!DocumentoVinculo.TIPOS_PERMITIDOS.contains(archivo.getContentType())) {
			throw ExcepcionNegocio.invalido("El documento debe ser PDF, JPG o PNG");
		}
		if (archivo.getSize() > DocumentoVinculo.TAMANO_MAXIMO) {
			throw ExcepcionNegocio.invalido("El documento no puede pesar más de 5 MB");
		}
		VinculoFamiliar apoderado = apoderado(estudianteId);
		DocumentoVinculo documento = documentos.findById(apoderado.getId())
				.orElseGet(() -> new DocumentoVinculo(apoderado.getId()));
		documento.reemplazar(tipo.strip(), nombreSeguro(archivo.getOriginalFilename()), archivo.getContentType(),
				bytes(archivo));
		documentos.save(documento);
		return new DocumentoAdjunto(documento.getTipo(), documento.getNombreArchivo(), documento.getSubidoEn());
	}

	@Transactional(readOnly = true)
	public DocumentoVinculo descargar(UUID estudianteId) {
		return documentos.findById(apoderado(estudianteId).getId())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El apoderado no tiene documento adjunto"));
	}

	private VinculoFamiliar apoderado(UUID estudianteId) {
		return estudiantes.findById(estudianteId)
				.filter(e -> !e.isEliminado())
				.orElseThrow(() -> ExcepcionNegocio.noEncontrado("El estudiante no existe"))
				.getVinculos().stream()
				.filter(v -> v.getParentesco() == Parentesco.APODERADO && v.getPersona() != null)
				.findFirst()
				.orElseThrow(() -> ExcepcionNegocio.invalido("El estudiante no tiene apoderado registrado"));
	}

	/** Evita rutas o caracteres raros en el nombre que se guarda y se devuelve al descargar. */
	private static String nombreSeguro(String original) {
		String nombre = original == null ? "documento" : original.replaceAll("^.*[\\\\/]", "");
		nombre = nombre.replaceAll("[^\\p{L}\\p{N}._ -]", "_").strip();
		return nombre.isEmpty() ? "documento" : nombre.substring(0, Math.min(nombre.length(), 200));
	}

	private static byte[] bytes(MultipartFile archivo) {
		try {
			return archivo.getBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}
