package com.galiledu.matricula.aplicacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.galiledu.compartido.web.ExcepcionNegocio;
import com.galiledu.configuracion.aplicacion.DtosSecciones.SeccionConVacantes;
import com.galiledu.matricula.aplicacion.DtosMatricula.ArchivoMatricula;
import com.galiledu.matricula.aplicacion.DtosMatricula.SeccionOpcion;
import com.galiledu.matricula.infraestructura.ConsultasMatricula.FichaMedicaFila;

/** Textos y reglas que comparten las pantallas de matrícula del Personal administrativo. */
final class PresentacionMatricula {
	private static final Set<String> TIPOS_ARCHIVO = Set.of("application/pdf", "image/jpeg", "image/png");
	private static final int TAMANO_MAXIMO = 10 * 1024 * 1024;

	private PresentacionMatricula() {
	}

	/** "SEC-3" + "B" → "3.° B". */
	static String etiqueta(SeccionConVacantes s) {
		return s.gradoCodigo().substring(s.gradoCodigo().indexOf('-') + 1) + ".° " + s.nombre();
	}

	static SeccionOpcion opcion(SeccionConVacantes s) {
		return new SeccionOpcion(s.id(), etiqueta(s), s.tutor(), s.capacidadMaxima(), s.alumnosOcupanVacante());
	}

	static String unir(String... partes) {
		return Stream.of(partes).filter(p -> p != null && !p.isBlank()).collect(Collectors.joining(" · "));
	}

	static String resumenMedico(Optional<FichaMedicaFila> ficha) {
		return ficha.map(f -> {
			List<String> partes = new ArrayList<>();
			if (f.tieneAlergias()) {
				partes.add("Alergias");
			}
			if (f.tieneEnfermedad()) {
				partes.add(f.enfermedadDetalle() == null || f.enfermedadDetalle().isBlank()
						? "Enfermedad"
						: f.enfermedadDetalle());
			}
			if (f.tomaMedicamento()) {
				partes.add("Toma medicamento");
			}
			return partes.isEmpty() ? "Sin observaciones" : String.join(", ", partes);
		}).orElse("Sin ficha médica");
	}

	/** PDF, JPG o PNG de hasta 10 MB y no vacío (RF-62). */
	static void validarArchivo(ArchivoMatricula archivo) {
		if (archivo.contenido().length == 0) {
			throw ExcepcionNegocio.invalido("El archivo \"" + archivo.nombre() + "\" está vacío.");
		}
		if (!TIPOS_ARCHIVO.contains(archivo.tipoMime())) {
			throw ExcepcionNegocio.invalido("El archivo \"" + archivo.nombre() + "\" debe ser PDF, JPG o PNG.");
		}
		if (archivo.contenido().length > TAMANO_MAXIMO) {
			throw ExcepcionNegocio.invalido("El archivo \"" + archivo.nombre() + "\" no puede pesar más de 10 MB.");
		}
	}
}
