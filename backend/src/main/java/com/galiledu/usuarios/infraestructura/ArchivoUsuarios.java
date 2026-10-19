package com.galiledu.usuarios.infraestructura;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import com.galiledu.compartido.web.ExcepcionNegocio;

/**
 * Lee el archivo de la carga masiva (HU-8) y genera su plantilla. Formatos: .xlsx, .xls y .csv (separado por coma o
 * punto y coma, en UTF-8). Las columnas se reconocen por su encabezado, sin importar mayúsculas, tildes ni el orden.
 */
@Component
public class ArchivoUsuarios {
	/** Columnas de Figma > "Carga masiva de usuarios" > Columnas requeridas, en orden. */
	public static final List<Columna> COLUMNAS = List.of(
			new Columna("tipoDocumento", "Tipo de documento", "DNI"),
			new Columna("numeroDocumento", "N.° de documento", "41234567"),
			new Columna("apellidoPaterno", "Apellido paterno", "Quispe"),
			new Columna("apellidoMaterno", "Apellido materno", "Huamán"),
			new Columna("nombres", "Nombres", "Jorge Luis"),
			new Columna("fechaNacimiento", "Fecha de nacimiento", "03/08/1988"),
			new Columna("nacionalidad", "Nacionalidad", "Peruana"),
			new Columna("lugarNacimiento", "Lugar de nacimiento", "Lima"),
			new Columna("sexo", "Sexo", "Masculino"),
			new Columna("estadoCivil", "Estado civil", "Casado(a)"),
			new Columna("gradoInstruccion", "Grado de instrucción", "Superior universitaria"),
			new Columna("celular", "Celular", "987111222"),
			new Columna("correo", "Correo electrónico", "jorge.quispe@gmail.com"),
			new Columna("telefonoCasa", "Teléfono de casa", "014523344"),
			new Columna("profesion", "Profesión u ocupación", "Contador"),
			new Columna("centroTrabajo", "Centro de trabajo", "Grupo Andino S.A.C."),
			new Columna("cargo", "Cargo", "Analista contable"),
			new Columna("distrito", "Distrito", "San Miguel"),
			new Columna("direccion", "Dirección", "Av. La Marina 1234"),
			new Columna("esExalumno", "Exalumno (Sí / No)", "No"),
			new Columna("roles", "Rol", "Docente"));

	public static final int MAXIMO_FILAS = 1000;

	/** Encabezados alternativos que también se aceptan (ya normalizados). */
	private static final Map<String, String> ALIAS = Map.of(
			"numerodedocumento", "numeroDocumento",
			"nrodedocumento", "numeroDocumento",
			"documento", "numeroDocumento",
			"correo", "correo",
			"exalumno", "esExalumno",
			"roles", "roles",
			"profesion", "profesion");

	public record Columna(String clave, String encabezado, String ejemplo) {
	}

	/** Una fila del archivo con su número tal como lo ve el usuario en Excel (el encabezado es la fila 1). */
	public record Fila(int numero, Map<String, String> valores) {
		public String valor(String clave) {
			String v = valores.get(clave);
			return v == null || v.isBlank() ? null : v.strip();
		}
	}

	public List<Fila> leer(String nombreArchivo, InputStream contenido) {
		String nombre = nombreArchivo == null ? "" : nombreArchivo.toLowerCase(Locale.ROOT);
		try {
			List<List<String>> celdas;
			if (nombre.endsWith(".csv")) {
				celdas = leerCsv(new String(contenido.readAllBytes(), StandardCharsets.UTF_8));
			} else if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
				celdas = leerExcel(contenido);
			} else {
				throw ExcepcionNegocio.invalido("El archivo debe ser .xlsx, .xls o .csv");
			}
			return aFilas(celdas);
		} catch (IOException | RuntimeException e) {
			if (e instanceof ExcepcionNegocio negocio) {
				throw negocio;
			}
			throw ExcepcionNegocio
					.invalido("No se pudo leer el archivo. Verifica que use la plantilla y no esté dañado.");
		}
	}

	/** Plantilla .xlsx con los encabezados y una fila de ejemplo. */
	public byte[] plantilla() {
		try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
			Sheet hoja = libro.createSheet("Usuarios");
			CellStyle negrita = libro.createCellStyle();
			Font fuente = libro.createFont();
			fuente.setBold(true);
			negrita.setFont(fuente);
			CellStyle texto = libro.createCellStyle();
			texto.setDataFormat(libro.createDataFormat().getFormat("@"));
			Row encabezado = hoja.createRow(0);
			Row ejemplo = hoja.createRow(1);
			for (int i = 0; i < COLUMNAS.size(); i++) {
				Cell titulo = encabezado.createCell(i);
				titulo.setCellValue(COLUMNAS.get(i).encabezado());
				titulo.setCellStyle(negrita);
				Cell valor = ejemplo.createCell(i);
				valor.setCellValue(COLUMNAS.get(i).ejemplo());
				valor.setCellStyle(texto);
				// Columnas como texto: así Excel no quita los ceros iniciales del DNI ni cambia las fechas.
				hoja.setDefaultColumnStyle(i, texto);
				hoja.setColumnWidth(i, 22 * 256);
			}
			hoja.createFreezePane(0, 1);
			libro.write(salida);
			return salida.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo generar la plantilla", e);
		}
	}

	private List<Fila> aFilas(List<List<String>> celdas) {
		if (celdas.isEmpty()) {
			throw ExcepcionNegocio.invalido("El archivo está vacío");
		}
		List<String> claves = celdas.get(0).stream().map(ArchivoUsuarios::clave).toList();
		for (String requerida : List.of("tipoDocumento", "numeroDocumento", "apellidoPaterno", "nombres", "celular",
				"correo", "roles")) {
			if (!claves.contains(requerida)) {
				String encabezado = COLUMNAS.stream().filter(c -> c.clave().equals(requerida)).findFirst()
						.orElseThrow().encabezado();
				throw ExcepcionNegocio.invalido("Falta la columna \"" + encabezado + "\". Usa la plantilla.");
			}
		}
		List<Fila> filas = new ArrayList<>();
		for (int i = 1; i < celdas.size(); i++) {
			List<String> fila = celdas.get(i);
			if (fila.stream().allMatch(v -> v == null || v.isBlank())) {
				continue;
			}
			Map<String, String> valores = new LinkedHashMap<>();
			for (int j = 0; j < claves.size() && j < fila.size(); j++) {
				if (claves.get(j) != null) {
					valores.put(claves.get(j), fila.get(j));
				}
			}
			filas.add(new Fila(i + 1, valores));
		}
		if (filas.isEmpty()) {
			throw ExcepcionNegocio.invalido("El archivo no tiene registros debajo del encabezado");
		}
		if (filas.size() > MAXIMO_FILAS) {
			throw ExcepcionNegocio
					.invalido("El archivo tiene más de " + MAXIMO_FILAS + " registros. Divídelo en partes.");
		}
		return filas;
	}

	/** "N.° de documento" → numeroDocumento. Devuelve null si el encabezado no es una columna conocida. */
	static String clave(String encabezado) {
		String n = normalizar(encabezado);
		for (Columna c : COLUMNAS) {
			if (normalizar(c.encabezado()).equals(n)) {
				return c.clave();
			}
		}
		return ALIAS.get(n);
	}

	static String normalizar(String texto) {
		if (texto == null) {
			return "";
		}
		String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
		return sinTildes.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
	}

	private static List<List<String>> leerExcel(InputStream contenido) throws IOException {
		DataFormatter formato = new DataFormatter(Locale.ROOT);
		DateTimeFormatter fecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
		List<List<String>> filas = new ArrayList<>();
		try (Workbook libro = WorkbookFactory.create(contenido)) {
			Sheet hoja = libro.getSheetAt(0);
			for (Row fila : hoja) {
				List<String> valores = new ArrayList<>();
				for (int i = 0; i < fila.getLastCellNum(); i++) {
					Cell celda = fila.getCell(i);
					if (celda == null) {
						valores.add("");
					} else if (celda.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(celda)) {
						valores.add(celda.getLocalDateTimeCellValue().toLocalDate().format(fecha));
					} else {
						valores.add(formato.formatCellValue(celda));
					}
				}
				while (filas.size() < fila.getRowNum()) {
					filas.add(List.of());
				}
				filas.add(valores);
			}
		}
		return filas;
	}

	/** CSV simple con comillas dobles; el separador (coma o punto y coma) se detecta en el encabezado. */
	static List<List<String>> leerCsv(String texto) {
		if (texto.startsWith("\uFEFF")) {
			texto = texto.substring(1);
		}
		String primeraLinea = texto.lines().findFirst().orElse("");
		char separador = primeraLinea.chars().filter(c -> c == ';').count() > primeraLinea.chars()
				.filter(c -> c == ',').count() ? ';' : ',';
		List<List<String>> filas = new ArrayList<>();
		List<String> actual = new ArrayList<>();
		StringBuilder campo = new StringBuilder();
		boolean entreComillas = false;
		for (int i = 0; i < texto.length(); i++) {
			char c = texto.charAt(i);
			if (entreComillas) {
				if (c == '"' && i + 1 < texto.length() && texto.charAt(i + 1) == '"') {
					campo.append('"');
					i++;
				} else if (c == '"') {
					entreComillas = false;
				} else {
					campo.append(c);
				}
			} else if (c == '"') {
				entreComillas = true;
			} else if (c == separador) {
				actual.add(campo.toString());
				campo.setLength(0);
			} else if (c == '\n' || c == '\r') {
				if (c == '\r' && i + 1 < texto.length() && texto.charAt(i + 1) == '\n') {
					i++;
				}
				actual.add(campo.toString());
				campo.setLength(0);
				filas.add(actual);
				actual = new ArrayList<>();
			} else {
				campo.append(c);
			}
		}
		if (campo.length() > 0 || !actual.isEmpty()) {
			actual.add(campo.toString());
			filas.add(actual);
		}
		return filas;
	}
}
