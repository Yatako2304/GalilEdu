package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.galiledu.configuracion.dominio.PlantillaDocumento.Validada;

class PlantillaDocumentoTests {
	private static final byte[] PDF = "%PDF-1.4\n%fin\n".getBytes(StandardCharsets.UTF_8);
	private static final byte[] DOCX = {'P', 'K', 3, 4, 20, 0, 0, 0};
	private static final byte[] JPG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0x10};
	private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
	private static final String FORMATO = "La plantilla debe ser un archivo PDF, DOCX, JPG o PNG.";

	private static void rechaza(String mensaje, ThrowingCallable accion) {
		assertThatThrownBy(accion).isInstanceOf(IllegalArgumentException.class).hasMessage(mensaje);
	}

	@DisplayName("Acepta un pdf")
	@Test
	void aceptaUnPdf() {
		Validada v = PlantillaDocumento.validar("Ficha.pdf", PDF);

		assertThat(v.nombreArchivo()).isEqualTo("Ficha.pdf");
		assertThat(v.tipoMime()).isEqualTo("application/pdf");
		assertThat(v.contenido()).isEqualTo(PDF);
	}

	@DisplayName("Acepta un docx")
	@Test
	void aceptaUnDocx() {
		assertThat(PlantillaDocumento.validar("ficha.docx", DOCX).tipoMime())
				.isEqualTo("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
	}

	@DisplayName("Acepta un jpg o jpeg")
	@Test
	void aceptaUnJpgOJpeg() {
		assertThat(PlantillaDocumento.validar("ficha.jpg", JPG).tipoMime()).isEqualTo("image/jpeg");
		assertThat(PlantillaDocumento.validar("ficha.jpeg", JPG).tipoMime()).isEqualTo("image/jpeg");
	}

	@DisplayName("Acepta un png")
	@Test
	void aceptaUnPng() {
		assertThat(PlantillaDocumento.validar("ficha.png", PNG).tipoMime()).isEqualTo("image/png");
	}

	@DisplayName("La extensión no distingue mayúsculas")
	@Test
	void laExtensionNoDistingueMayusculas() {
		assertThat(PlantillaDocumento.validar("FICHA.PDF", PDF).tipoMime()).isEqualTo("application/pdf");
	}

	@DisplayName("Rechaza otros tipos")
	@Test
	void rechazaOtrosTipos() {
		rechaza(FORMATO, () -> PlantillaDocumento.validar("ficha.xlsx", DOCX));
		rechaza(FORMATO, () -> PlantillaDocumento.validar("ficha.csv", PDF));
		rechaza(FORMATO, () -> PlantillaDocumento.validar("ficha", PDF));
	}

	@DisplayName("Rechaza un archivo cuyo contenido no corresponde a la extensión")
	@Test
	void rechazaUnArchivoCuyoContenidoNoCorrespondeALaExtension() {
		rechaza("El contenido del archivo no corresponde a un PDF válido.",
				() -> PlantillaDocumento.validar("ficha.pdf", PNG));
		rechaza("El contenido del archivo no corresponde a un DOCX válido.",
				() -> PlantillaDocumento.validar("ficha.docx", PDF));
		rechaza("El contenido del archivo no corresponde a un JPG válido.",
				() -> PlantillaDocumento.validar("ficha.jpg", PNG));
		rechaza("El contenido del archivo no corresponde a un PNG válido.",
				() -> PlantillaDocumento.validar("ficha.png", JPG));
		rechaza("El contenido del archivo no corresponde a un DOCX válido.",
				() -> PlantillaDocumento.validar("ficha.docx", new byte[]{'P', 'K'}));
	}

	@DisplayName("Rechaza un archivo vacio o nulo")
	@Test
	void rechazaUnArchivoVacioONulo() {
		rechaza("Selecciona un archivo para la plantilla.", () -> PlantillaDocumento.validar("ficha.pdf", new byte[0]));
		rechaza("Selecciona un archivo para la plantilla.", () -> PlantillaDocumento.validar("ficha.pdf", null));
	}

	@DisplayName("Admite hasta10 mb y rechaza más")
	@Test
	void admiteHasta10MbYRechazaMas() {
		byte[] justo = new byte[PlantillaDocumento.TAMANO_MAXIMO_BYTES];
		System.arraycopy(PDF, 0, justo, 0, PDF.length);
		assertThat(PlantillaDocumento.validar("grande.pdf", justo).contenido()).hasSize(10 * 1024 * 1024);

		byte[] demasiado = new byte[PlantillaDocumento.TAMANO_MAXIMO_BYTES + 1];
		System.arraycopy(PDF, 0, demasiado, 0, PDF.length);
		rechaza("La plantilla no puede pesar más de 10 MB.", () -> PlantillaDocumento.validar("grande.pdf", demasiado));
	}

	@DisplayName("Sin nombre de archivo se rechaza")
	@Test
	void sinNombreDeArchivoSeRechaza() {
		rechaza("El archivo de la plantilla no tiene nombre.", () -> PlantillaDocumento.validar(null, PDF));
		rechaza("El archivo de la plantilla no tiene nombre.", () -> PlantillaDocumento.validar("  ", PDF));
	}

	@DisplayName("Guarda solo el nombre del archivo sin la ruta")
	@Test
	void guardaSoloElNombreDelArchivoSinLaRuta() {
		assertThat(PlantillaDocumento.validar("C:\\Users\\ana\\Documentos\\ficha.pdf", PDF).nombreArchivo())
				.isEqualTo("ficha.pdf");
		assertThat(PlantillaDocumento.validar("/tmp/ficha.pdf", PDF).nombreArchivo()).isEqualTo("ficha.pdf");
	}

	@DisplayName("El nombre del archivo admite hasta200 caracteres")
	@Test
	void elNombreDelArchivoAdmiteHasta200Caracteres() {
		PlantillaDocumento.validar("a".repeat(196) + ".pdf", PDF);
		rechaza("El nombre del archivo no puede pasar de 200 caracteres.",
				() -> PlantillaDocumento.validar("a".repeat(197) + ".pdf", PDF));
	}
}
