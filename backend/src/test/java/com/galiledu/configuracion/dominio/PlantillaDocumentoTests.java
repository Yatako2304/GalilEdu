package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

import com.galiledu.configuracion.dominio.PlantillaDocumento.Validada;

class PlantillaDocumentoTests {
	private static final byte[] XLSX = {'P', 'K', 3, 4, 20, 0, 0, 0};
	private static final byte[] XLS = {(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A,
			(byte) 0xE1, 0, 0};
	private static final byte[] CSV = "nombre,dni\nAna,12345678\n".getBytes(StandardCharsets.UTF_8);

	private static void rechaza(String mensaje, ThrowingCallable accion) {
		assertThatThrownBy(accion).isInstanceOf(IllegalArgumentException.class).hasMessage(mensaje);
	}

	@Test
	void aceptaUnXlsx() {
		Validada v = PlantillaDocumento.validar("Ficha.xlsx", XLSX);

		assertThat(v.nombreArchivo()).isEqualTo("Ficha.xlsx");
		assertThat(v.tipoMime()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		assertThat(v.contenido()).isEqualTo(XLSX);
	}

	@Test
	void aceptaUnXls() {
		assertThat(PlantillaDocumento.validar("ficha.xls", XLS).tipoMime()).isEqualTo("application/vnd.ms-excel");
	}

	@Test
	void aceptaUnCsv() {
		assertThat(PlantillaDocumento.validar("ficha.csv", CSV).tipoMime()).isEqualTo("text/csv");
	}

	@Test
	void laExtensionNoDistingueMayusculas() {
		assertThat(PlantillaDocumento.validar("FICHA.XLSX", XLSX).tipoMime()).contains("spreadsheetml");
	}

	@Test
	void rechazaOtrosTipos() {
		rechaza("La plantilla debe ser un archivo XLSX, XLS o CSV.",
				() -> PlantillaDocumento.validar("ficha.pdf", CSV));
		rechaza("La plantilla debe ser un archivo XLSX, XLS o CSV.",
				() -> PlantillaDocumento.validar("ficha.docx", XLSX));
		rechaza("La plantilla debe ser un archivo XLSX, XLS o CSV.", () -> PlantillaDocumento.validar("ficha", CSV));
	}

	@Test
	void rechazaUnArchivoCuyoContenidoNoCorrespondeALaExtension() {
		rechaza("El contenido del archivo no corresponde a un XLSX válido.",
				() -> PlantillaDocumento.validar("ficha.xlsx", CSV));
		rechaza("El contenido del archivo no corresponde a un XLS válido.",
				() -> PlantillaDocumento.validar("ficha.xls", XLSX));
		rechaza("El contenido del archivo no corresponde a un CSV válido.",
				() -> PlantillaDocumento.validar("ficha.csv", new byte[]{'a', 0, 'b'}));
		rechaza("El contenido del archivo no corresponde a un XLSX válido.",
				() -> PlantillaDocumento.validar("ficha.xlsx", new byte[]{'P', 'K'}));
	}

	@Test
	void rechazaUnArchivoVacioONulo() {
		rechaza("Selecciona un archivo para la plantilla.", () -> PlantillaDocumento.validar("ficha.csv", new byte[0]));
		rechaza("Selecciona un archivo para la plantilla.", () -> PlantillaDocumento.validar("ficha.csv", null));
	}

	@Test
	void admiteHasta10MbYRechazaMas() {
		byte[] justo = new byte[PlantillaDocumento.TAMANO_MAXIMO_BYTES];
		java.util.Arrays.fill(justo, (byte) 'a');
		assertThat(PlantillaDocumento.validar("grande.csv", justo).contenido()).hasSize(10 * 1024 * 1024);

		byte[] demasiado = new byte[PlantillaDocumento.TAMANO_MAXIMO_BYTES + 1];
		java.util.Arrays.fill(demasiado, (byte) 'a');
		rechaza("La plantilla no puede pesar más de 10 MB.", () -> PlantillaDocumento.validar("grande.csv", demasiado));
	}

	@Test
	void sinNombreDeArchivoSeRechaza() {
		rechaza("El archivo de la plantilla no tiene nombre.", () -> PlantillaDocumento.validar(null, CSV));
		rechaza("El archivo de la plantilla no tiene nombre.", () -> PlantillaDocumento.validar("  ", CSV));
	}

	@Test
	void guardaSoloElNombreDelArchivoSinLaRuta() {
		assertThat(PlantillaDocumento.validar("C:\\Users\\ana\\Documentos\\ficha.csv", CSV).nombreArchivo())
				.isEqualTo("ficha.csv");
		assertThat(PlantillaDocumento.validar("/tmp/ficha.csv", CSV).nombreArchivo()).isEqualTo("ficha.csv");
	}

	@Test
	void elNombreDelArchivoAdmiteHasta200Caracteres() {
		PlantillaDocumento.validar("a".repeat(196) + ".csv", CSV);
		rechaza("El nombre del archivo no puede pasar de 200 caracteres.",
				() -> PlantillaDocumento.validar("a".repeat(197) + ".csv", CSV));
	}
}
