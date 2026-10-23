package com.galiledu.configuracion;

import static com.galiledu.configuracion.AyudasEntradasHostiles.arreglo;
import static com.galiledu.configuracion.AyudasEntradasHostiles.cuerpoDeCrear;
import static com.galiledu.configuracion.AyudasEntradasHostiles.cuerpoDeEditar;
import static com.galiledu.configuracion.AyudasEntradasHostiles.existeLaTabla;
import static com.galiledu.configuracion.AyudasEntradasHostiles.fotoDeEstructuras;
import static com.galiledu.configuracion.AyudasEntradasHostiles.rastrosEn;
import static com.galiledu.configuracion.AyudasEntradasHostiles.texto;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.galiledu.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;

/**
 * HU-44, entradas equivocadas y maliciosas contra la API. El objetivo es encontrar fallos reales: cada prueba exige
 * (a) que una entrada inválida nunca dé 5xx, (b) un cuerpo de error {message, errores} sin rastros internos y (c) que
 * la BD quede igual. Cuando algo se acepta aunque sea dudoso (HUECO) la prueba documenta la respuesta real; las que
 * hoy fallan por un BUG se dejan fallando a propósito, con el comentario "BUG".
 * <p>
 * 2026 es el año ACTIVO con EH-PRI-26 y EH-SEC-26; 2027 está PLANIFICADO y vacío, y es donde se crea.
 */
class EstructuraHorariaEntradasHostilesIT extends PruebaIntegracion {
	private static final String ANIO_2026 = "30000000-0000-0000-0000-000000002026";
	private static final String ANIO_2027 = "30000000-0000-0000-0000-000000002027";
	private static final String EH_PRI_26 = "31000000-0000-0000-0000-000000000001";
	private static final String UUID_NULO = "00000000-0000-0000-0000-000000000000";
	private static final String PENDIENTE_COMPARTIDO = "Pendiente compartido: ManejadorErrores devuelve 500; se activa cuando se corrija";
	private static final String CODIGO_INVALIDO = "El código solo admite letras, números y guion.";
	private static final String TEXTO_CON_CONTROLES = "El texto contiene caracteres no válidos.";
	private static final String DIA_EN_BLANCO = "Elige solo días válidos: de LUN a SAB.";
	private static final String MENSAJE_FORMATO = "La solicitud tiene un formato inválido";
	private static final String ANIO_NO_PLANIFICADO = "Solo se puede modificar la estructura horaria de un año planificado.";
	private static final String APODERADO = "U20260003";
	private static final String ESTUDIANTE = "U20260006";

	@Autowired
	private JdbcClient jdbc;
	@Autowired
	private EntityManager entityManager;

	private String personalAdministrativo;

	@BeforeEach
	void iniciarSesion() throws Exception {
		personalAdministrativo = token(PERSONAL_ADMINISTRATIVO);
	}

	// ---------- Ayudas ----------

	private static String rutaDelAnio(String anioId) {
		return "/configuracion/anios-escolares/" + anioId + "/estructuras-horarias";
	}

	private static String rutaEstructura(String estructuraId) {
		return "/configuracion/estructuras-horarias/" + estructuraId;
	}

	private MvcResult crear(String cuerpo) throws Exception {
		return mvc.perform(conToken(post(rutaDelAnio(ANIO_2027)), personalAdministrativo, cuerpo)).andReturn();
	}

	private MvcResult vistaPrevia(String cuerpo) throws Exception {
		return mvc.perform(conToken(post(rutaDelAnio(ANIO_2027) + "/vista-previa"), personalAdministrativo, cuerpo))
				.andReturn();
	}

	private MvcResult editar(String estructuraId, String cuerpo) throws Exception {
		return mvc.perform(conToken(put(rutaEstructura(estructuraId)), personalAdministrativo, cuerpo)).andReturn();
	}

	private MvcResult detalle(String estructuraId) throws Exception {
		return mvc.perform(conToken(get(rutaEstructura(estructuraId)), personalAdministrativo)).andReturn();
	}

	private static String cuerpoDe(MvcResult resultado) throws Exception {
		return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	private static String abreviar(String texto) {
		return texto.length() > 300 ? texto.substring(0, 300) + "…(" + texto.length() + " caracteres)" : texto;
	}

	/** La foto de la BD, con los cambios de JPA ya enviados: así una escritura a medias no pasa desapercibida. */
	private String foto() {
		entityManager.flush();
		return fotoDeEstructuras(jdbc, true);
	}

	private String fotoSinFechaDeActualizacion() {
		entityManager.flush();
		return fotoDeEstructuras(jdbc, false);
	}

	/**
	 * Criterios a) y b): estado 4xx y cuerpo {message, errores} sin rastros internos. Con {@code permiteCuerpoVacio}
	 * se acepta un cuerpo vacío (lo que devuelve el firewall o el filtro de seguridad antes de llegar a la API).
	 */
	private static void exigeRechazoLimpio(MvcResult resultado, boolean permiteCuerpoVacio) throws Exception {
		int estado = resultado.getResponse().getStatus();
		String cuerpo = cuerpoDe(resultado);
		assertThat(estado).as("estado HTTP (cuerpo: %s)", abreviar(cuerpo)).isBetween(400, 499);
		assertThat(rastrosEn(cuerpo)).as("rastros internos en: %s", abreviar(cuerpo)).isEmpty();
		if (cuerpo.isBlank() && permiteCuerpoVacio) {
			return;
		}
		assertThat(cuerpo).as("cuerpo de error vacío (estado %d)", estado).isNotBlank();
		Object mensaje = JsonPath.read(cuerpo, "$.message");
		Object errores = JsonPath.read(cuerpo, "$.errores");
		assertThat(mensaje).as("message en %s", abreviar(cuerpo)).isInstanceOf(String.class);
		assertThat((String) mensaje).isNotBlank();
		assertThat(errores).as("errores en %s", abreviar(cuerpo)).isInstanceOf(Map.class);
	}

	private static void exigeRechazoLimpio(MvcResult resultado) throws Exception {
		exigeRechazoLimpio(resultado, false);
	}

	private static void exigeRechazoConMensaje(MvcResult resultado, int estado, String mensaje) throws Exception {
		exigeRechazoLimpio(resultado);
		assertThat(resultado.getResponse().getStatus()).isEqualTo(estado);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(mensaje);
	}

	/** Crear con ese cuerpo debe rechazarse con 4xx limpio y sin tocar la BD (criterio c). */
	private MvcResult exigeCrearRechazadoSinEscribir(String cuerpo) throws Exception {
		String antes = foto();
		MvcResult resultado = crear(cuerpo);
		exigeRechazoLimpio(resultado);
		assertThat(foto()).as("la BD cambió pese al rechazo").isEqualTo(antes);
		return resultado;
	}

	/** Crear debe responder 201 y devolver el id de la estructura. */
	private String exigeCreada(MvcResult resultado) throws Exception {
		assertThat(resultado.getResponse().getStatus()).as("estado (cuerpo: %s)", abreviar(cuerpoDe(resultado)))
				.isEqualTo(201);
		return JsonPath.read(cuerpoDe(resultado), "$.estructura.id");
	}

	private String leerDelDetalle(String estructuraId, String rutaJson) throws Exception {
		return JsonPath.read(cuerpoDe(detalle(estructuraId)), rutaJson);
	}

	private String crearValida(String... clavesYLiterales) throws Exception {
		return exigeCreada(crear(cuerpoDeCrear(clavesYLiterales)));
	}

	// ---------- 1. Texto: código y descripción ----------

	static Stream<Arguments> codigosSinContenidoUtil() {
		return Stream.of(
				Arguments.of("vacío", ""),
				Arguments.of("solo espacios", "     "),
				Arguments.of("tabulaciones", "\t\t"),
				Arguments.of("saltos de línea", "\n\n"),
				Arguments.of("retorno y salto", "\r\n"),
				Arguments.of("mezcla de blancos", " \t\r\n "),
				Arguments.of("controles al borde", "\u0001\u001B"),
				Arguments.of("NUL solo", "\u0000"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("codigosSinContenidoUtil")
	void unCodigoSinContenidoUtilSeRechazaConSuMensajeSinEscribirNada(String nombre, String codigo) throws Exception {
		// Respuesta real: 400 "Ingresa el código de la estructura."
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("codigo", texto(codigo)));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message"))
				.isEqualTo("Ingresa el código de la estructura.");
	}

	static Stream<Arguments> codigosDeSoloCaracteresInvisibles() {
		return Stream.of(
				Arguments.of("espacio de ancho cero U+200B", Character.toString(0x200B)),
				Arguments.of("espacio duro U+00A0", Character.toString(0x00A0)),
				Arguments.of("espacio em U+2003", Character.toString(0x2003)),
				Arguments.of("marca de orden de bytes U+FEFF", Character.toString(0xFEFF)));
	}

	/**
	 * Corregido: trim() no recorta estos caracteres, pero el código solo admite letras A-Z, números y guion. Respuesta
	 * real: 400 "El código solo admite letras, números y guion."
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("codigosDeSoloCaracteresInvisibles")
	void unCodigoDeSoloCaracteresInvisiblesSeRechazaSinEscribirNada(String nombre, String codigo) throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("codigo", texto(codigo)));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(CODIGO_INVALIDO);
	}

	static Stream<Arguments> largosDelCodigo() {
		return Stream.of(
				Arguments.of("20 caracteres", "A".repeat(20), true),
				Arguments.of("20 caracteres con espacios al borde", "  " + "A".repeat(20) + "  ", true),
				Arguments.of("21 caracteres", "A".repeat(21), false),
				Arguments.of("100 000 caracteres", "A".repeat(100_000), false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("largosDelCodigo")
	void elCodigoEnElLimiteSeAceptaYUnoMasSeRechaza(String nombre, String codigo, boolean seAcepta) throws Exception {
		if (seAcepta) {
			String id = crearValida("codigo", texto(codigo));
			assertThat(leerDelDetalle(id, "$.codigo")).isEqualTo("A".repeat(20));
		} else {
			MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("codigo", texto(codigo)));
			assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message"))
					.isEqualTo("El código admite hasta 20 caracteres.");
		}
	}

	static Stream<Arguments> largosDeLaDescripcion() {
		return Stream.of(
				Arguments.of("100 caracteres", "D".repeat(100), true),
				Arguments.of("101 caracteres", "D".repeat(101), false),
				Arguments.of("100 000 caracteres", "D".repeat(100_000), false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("largosDeLaDescripcion")
	void laDescripcionEnElLimiteSeAceptaYUnaMasSeRechaza(String nombre, String descripcion, boolean seAcepta)
			throws Exception {
		if (seAcepta) {
			String id = crearValida("descripcion", texto(descripcion));
			assertThat(leerDelDetalle(id, "$.descripcion")).isEqualTo(descripcion);
		} else {
			MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("descripcion", texto(descripcion)));
			assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message"))
					.isEqualTo("La descripción admite hasta 100 caracteres.");
		}
	}

	@Test
	void unaDescripcionVaciaONulaOSoloDeBlancosSeGuardaComoSinDescripcion() throws Exception {
		int numero = 0;
		for (String descripcion : new String[]{"\"\"", "null", texto("   " + "\t" + "\n"), null}) {
			String id = crearValida("codigo", texto("EH-VACIA-" + numero++), "descripcion", descripcion);

			assertThat(cuerpoDe(detalle(id))).contains("\"descripcion\":null");
		}
	}

	static Stream<Arguments> codigosEspecialesAceptados() {
		return Stream.of(
				Arguments.of("mayúsculas y minúsculas mezcladas", "eH-pRi-1", "EH-PRI-1"),
				Arguments.of("espacios al borde", " eh-1 ", "EH-1"),
				Arguments.of("los códigos de Figma en minúsculas", "eh-tar-26", "EH-TAR-26"),
				Arguments.of("diez eszett, que valen 20 al pasar a SS", "ß".repeat(10), "SS".repeat(10)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("codigosEspecialesAceptados")
	void losCodigosConLetrasNumerosYGuionSeGuardanNormalizados(String nombre, String codigo, String esperado)
			throws Exception {
		String id = crearValida("codigo", texto(codigo));

		assertThat(leerDelDetalle(id, "$.codigo")).isEqualTo(esperado);
	}

	static Stream<Arguments> codigosFueraDeLaRegla() {
		return Stream.of(
				Arguments.of("ñ y tildes", "ñandú-áé"),
				Arguments.of("emoji", "\uD83D\uDE00"),
				Arguments.of("diez emojis (20 unidades)", "\uD83D\uDE00".repeat(10)),
				Arguments.of("cirílico", "\u0435\u043D-1"),
				Arguments.of("griego", "\u03B1\u03B2\u03B3-1"),
				Arguments.of("ancho completo", "\uFF25\uFF28-\uFF11"),
				Arguments.of("ruta relativa", "../x"),
				Arguments.of("espacio interno", "EH 1"),
				Arguments.of("barras", "a/b\\c"),
				Arguments.of("guion bajo y punto", "EH_1.2"),
				Arguments.of("signos", "EH-1!"));
	}

	/**
	 * Corregido: todo lo que no sea A-Z, número o guion (tras normalizar) se rechaza. Cubre cirílico, invisibles y
	 * rutas.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("codigosFueraDeLaRegla")
	void losCodigosConOtrosCaracteresSeRechazanConSuMensaje(String nombre, String codigo) throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("codigo", texto(codigo)));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(CODIGO_INVALIDO);
	}

	static Stream<Arguments> codigosQueSeAlarganAlNormalizar() {
		return Stream.of(
				Arguments.of("once eszett (22 al pasar a SS)", "ß".repeat(11)),
				Arguments.of("once emojis (22 unidades)", "\uD83D\uDE00".repeat(11)));
	}

	/**
	 * HUECO menor: el largo se mide ya normalizado y en unidades UTF-16, así que 11 caracteres escritos pueden
	 * rechazarse por largo. Es más estricto que la columna, no peligroso.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("codigosQueSeAlarganAlNormalizar")
	void losCodigosQueSeAlarganAlNormalizarSeRechazanPorLargo(String nombre, String codigo) throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("codigo", texto(codigo)));

		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message"))
				.isEqualTo("El código admite hasta 20 caracteres.");
	}

	@Test
	void elMismoCodigoConYSinEspaciosYEnOtraCapitalizacionEsDuplicado() throws Exception {
		crearValida("codigo", texto("eh-1"));

		for (String variante : new String[]{" EH-1 ", "Eh-1", "\tEH-1\n", "eH-1  "}) {
			MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("codigo", texto(variante)));

			assertThat(resultado.getResponse().getStatus()).as(variante).isEqualTo(409);
			assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message"))
					.isEqualTo("Ya existe una estructura con el código EH-1 en este año.");
		}
	}

	static Stream<Arguments> textosConNul() {
		return Stream.of(
				Arguments.of("NUL en el código", "codigo", "A\u0000B"),
				Arguments.of("NUL en la descripción", "descripcion", "x\u0000y"));
	}

	/**
	 * Corregido: antes el NUL en el código daba 500 y en la descripción un 409 engañoso ("Otra persona acaba de
	 * cambiar…"). Respuesta real: 400 "El texto contiene caracteres no válidos." en ambos.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("textosConNul")
	void unNulDentroDelTextoSeRechazaConSuMensajeSinEscribirNada(String nombre, String campo, String valor)
			throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear(campo, texto(valor)));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(TEXTO_CON_CONTROLES);
	}

	/** Corregido: la vista previa con NUL responde 200 con el error en la lista, ya no 500. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("textosConNul")
	void unNulDentroDelTextoEnLaVistaPreviaListaElError(String nombre, String campo, String valor) throws Exception {
		MvcResult resultado = vistaPrevia(cuerpoDeCrear(campo, texto(valor)));

		assertThat(resultado.getResponse().getStatus()).as("cuerpo: %s", abreviar(cuerpoDe(resultado))).isEqualTo(200);
		assertThat((Boolean) JsonPath.read(cuerpoDe(resultado), "$.valida")).isFalse();
		assertThat((List<String>) JsonPath.read(cuerpoDe(resultado), "$.errores")).contains(TEXTO_CON_CONTROLES);
		assertThat(rastrosEn(cuerpoDe(resultado))).isEmpty();
	}

	static Stream<Arguments> controlesQueNoSonNul() {
		List<Arguments> casos = new ArrayList<>();
		for (String campo : new String[]{"codigo", "descripcion"}) {
			casos.add(Arguments.of("\\u0001 dentro de " + campo, campo, "A\u0001B"));
			casos.add(Arguments.of("escape \\u001B dentro de " + campo, campo, "A\u001BB"));
			casos.add(Arguments.of("CRLF dentro de " + campo, campo, "A\r\nB"));
			casos.add(Arguments.of("tabulación dentro de " + campo, campo, "A\tB"));
		}
		return casos.stream();
	}

	/** Corregido: los caracteres de control en medio del código o la descripción se rechazan (400). */
	@ParameterizedTest(name = "{0}")
	@MethodSource("controlesQueNoSonNul")
	void losCaracteresDeControlEnMedioSeRechazanSinEscribirNada(String nombre, String campo, String valor)
			throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear(campo, texto(valor)));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(TEXTO_CON_CONTROLES);
	}

	static Stream<String> textosDeInyeccion() {
		return Stream.of("'; DROP TABLE configuracion.receso; --", "' OR '1'='1", "<script>alert(1)</script>",
				"${jndi:ldap://x}", "%s%n%x", "{{7*7}}");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("textosDeInyeccion")
	void lasCadenasDeInyeccionEnLaDescripcionSeGuardanLiteralesYLasTablasSiguen(String texto) throws Exception {
		String id = crearValida("descripcion", texto(texto));

		assertThat(leerDelDetalle(id, "$.descripcion")).isEqualTo(texto);
		assertThat(existeLaTabla(jdbc, "configuracion.receso")).isTrue();
		assertThat(existeLaTabla(jdbc, "configuracion.estructura_horaria")).isTrue();
		assertThat(jdbc.sql("SELECT count(*) FROM configuracion.estructura_horaria").query(Integer.class).single())
				.isEqualTo(3);
	}

	/** Como código, ninguna cadena de inyección cumple la regla: se rechazan por largo o por caracteres no válidos. */
	@ParameterizedTest(name = "{0}")
	@MethodSource("textosDeInyeccion")
	void lasCadenasDeInyeccionComoCodigoSeRechazanSinEscribirNadaYLasTablasSiguen(String texto) throws Exception {
		String normalizado = texto.trim().toUpperCase(Locale.ROOT);
		String esperado = normalizado.length() > 20 ? "El código admite hasta 20 caracteres." : CODIGO_INVALIDO;

		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("codigo", texto(texto)));

		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(esperado);
		assertThat(existeLaTabla(jdbc, "configuracion.receso")).isTrue();
	}

	// ---------- 2. Días lectivos ----------

	static Stream<Arguments> diasInvalidos() {
		return Stream.of(
				Arguments.of("arreglo vacío", "[]", "Elige al menos un día lectivo."),
				Arguments.of("clave ausente", null, "Elige al menos un día lectivo."),
				Arguments.of("null", "null", "Elige al menos un día lectivo."),
				Arguments.of("[null]", "[null]", DIA_EN_BLANCO),
				Arguments.of("[\"\"]", "[\"\"]", DIA_EN_BLANCO),
				Arguments.of("texto en vez de arreglo", "\"LUN\"", MENSAJE_FORMATO),
				Arguments.of("LUNES", "[\"LUNES\"]", "Día lectivo no válido: LUNES."),
				Arguments.of("DOM", "[\"DOM\"]", "Día lectivo no válido: DOM."),
				Arguments.of("repetidos", "[\"LUN\",\"LUN\"]", "No repitas los días lectivos."),
				Arguments.of("válidos e inválidos", "[\"LUN\",\"XYZ\",\"MAR\"]", "Día lectivo no válido: XYZ."),
				Arguments.of("mil días repetidos", arreglo("\"LUN\"", 1000), "No repitas los días lectivos."),
				Arguments.of("números", "[1,2]", null),
				Arguments.of("objetos", "[{}]", null));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("diasInvalidos")
	void losDiasInvalidosSeRechazanSinEscribirNada(String nombre, String literal, String mensaje) throws Exception {
		// Corregido: "[null]" y [""] dan "Elige solo días válidos: de LUN a SAB." en vez de un mensaje con el nombre vacío.
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("diasLectivos", literal));

		if (mensaje != null) {
			assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(mensaje);
		}
	}

	static Stream<Arguments> diasValidos() {
		return Stream.of(
				Arguments.of("minúsculas", "[\"lun\",\" mar \"]", List.of("LUN", "MAR")),
				Arguments.of("desordenados", "[\"VIE\",\"LUN\",\"MIE\"]", List.of("LUN", "MIE", "VIE")),
				Arguments.of("los seis días", "[\"SAB\",\"MIE\",\"LUN\",\"VIE\",\"MAR\",\"JUE\"]",
						List.of("LUN", "MAR", "MIE", "JUE", "VIE", "SAB")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("diasValidos")
	void losDiasValidosSeGuardanEnOrdenDeSemana(String nombre, String literal, List<String> esperados)
			throws Exception {
		String id = crearValida("diasLectivos", literal);

		assertThat((List<String>) JsonPath.read(cuerpoDe(detalle(id)), "$.diasLectivos")).isEqualTo(esperados);
	}

	/**
	 * HUECO (sin corregir): mil días inválidos se rechazan con 400 y el primero de los mil mensajes ("D0"); la vista
	 * previa devolvería los mil. Se documenta el comportamiento actual.
	 */
	@Test
	void milDiasInvalidosSeRechazanSinFallarNiFiltrarNada() throws Exception {
		StringBuilder dias = new StringBuilder("[");
		for (int numero = 0; numero < 1000; numero++) {
			dias.append(numero > 0 ? "," : "").append("\"D").append(numero).append('"');
		}

		MvcResult resultado = exigeCrearRechazadoSinEscribir(
				cuerpoDeCrear("diasLectivos", dias.append(']').toString()));

		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo("Día lectivo no válido: D0.");
	}

	// ---------- 3. Números ----------

	static Stream<Arguments> bloquesPorDiaRechazados() {
		return Stream.of(
				Arguments.of("0", "0", "Los bloques por día van de 1 a 12."),
				Arguments.of("-1", "-1", "Los bloques por día van de 1 a 12."),
				Arguments.of("13", "13", "Los bloques por día van de 1 a 12."),
				Arguments.of("20", "20", "Los bloques por día van de 1 a 12."),
				Arguments.of("120", "120", "Los bloques por día van de 1 a 12."),
				Arguments.of("121", "121", "Los bloques por día van de 1 a 12."),
				Arguments.of("Integer.MAX_VALUE", "2147483647", "Los bloques por día van de 1 a 12."),
				Arguments.of("Integer.MIN_VALUE", "-2147483648", "Los bloques por día van de 1 a 12."),
				Arguments.of("fuera de int", "99999999999", MENSAJE_FORMATO),
				Arguments.of("texto no numérico", "\"seis\"", MENSAJE_FORMATO),
				Arguments.of("null", "null", "Los bloques por día van de 1 a 12."),
				Arguments.of("clave ausente", null, "Los bloques por día van de 1 a 12."),
				Arguments.of("1e2 (100)", "1e2", "Los bloques por día van de 1 a 12."),
				Arguments.of("arreglo", "[6]", MENSAJE_FORMATO),
				Arguments.of("objeto", "{}", MENSAJE_FORMATO));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("bloquesPorDiaRechazados")
	void losBloquesPorDiaFueraDeRangoOMalEscritosSeRechazan(String nombre, String literal, String mensaje)
			throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("bloquesPorDia", literal));

		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(mensaje);
	}

	static Stream<Arguments> bloquesPorDiaAceptados() {
		return Stream.of(
				Arguments.of("1", "1", 1),
				Arguments.of("12", "12", 12),
				// HUECO: un decimal se trunca en silencio (5.5 → 5) y un texto numérico se convierte.
				Arguments.of("5.5 se trunca a 5", "5.5", 5),
				Arguments.of("\"6\" como texto se convierte a 6", "\"6\"", 6));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("bloquesPorDiaAceptados")
	void losBloquesPorDiaValidosOConvertiblesSeGuardan(String nombre, String literal, int esperado) throws Exception {
		String id = crearValida("bloquesPorDia", literal);

		assertThat(JsonPath.<Integer>read(cuerpoDe(detalle(id)), "$.bloquesPorDia")).isEqualTo(esperado);
	}

	@Test
	void unBooleanComoBloquesPorDiaSeRechazaSinFallar() throws Exception {
		// Observado al correr: ver el informe. El criterio es 4xx limpio.
		exigeCrearRechazadoSinEscribir(cuerpoDeCrear("bloquesPorDia", "true"));
	}

	static Stream<Arguments> duracionRechazada() {
		return Stream.of(
				Arguments.of("0", "0", "La duración del bloque va de 20 a 120 minutos."),
				Arguments.of("-1", "-1", "La duración del bloque va de 20 a 120 minutos."),
				Arguments.of("1", "1", "La duración del bloque va de 20 a 120 minutos."),
				Arguments.of("19", "19", "La duración del bloque va de 20 a 120 minutos."),
				Arguments.of("121", "121", "La duración del bloque va de 20 a 120 minutos."),
				Arguments.of("Integer.MAX_VALUE", "2147483647", "La duración del bloque va de 20 a 120 minutos."),
				Arguments.of("Integer.MIN_VALUE", "-2147483648", "La duración del bloque va de 20 a 120 minutos."),
				Arguments.of("fuera de int", "99999999999", MENSAJE_FORMATO),
				Arguments.of("texto no numérico", "\"cuarenta\"", MENSAJE_FORMATO),
				Arguments.of("null", "null", "La duración del bloque va de 20 a 120 minutos."),
				Arguments.of("clave ausente", null, "La duración del bloque va de 20 a 120 minutos."));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("duracionRechazada")
	void laDuracionDelBloqueFueraDeRangoOMalEscritaSeRechaza(String nombre, String literal, String mensaje)
			throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("bloquesPorDia", "2",
				"duracionBloqueMin", literal));

		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(mensaje);
	}

	static Stream<Arguments> duracionAceptada() {
		return Stream.of(
				Arguments.of("20", "20", 20),
				Arguments.of("120", "120", 120),
				Arguments.of("45.5 se trunca a 45", "45.5", 45),
				Arguments.of("\"45\" como texto se convierte a 45", "\"45\"", 45),
				Arguments.of("1e2 se convierte a 100", "1e2", 100));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("duracionAceptada")
	void laDuracionValidaOConvertibleSeGuarda(String nombre, String literal, int esperado) throws Exception {
		// Con 2 bloques de hasta 120 min la jornada termina antes de las 18:00.
		String id = crearValida("bloquesPorDia", "2", "duracionBloqueMin", literal);

		assertThat(JsonPath.<Integer>read(cuerpoDe(detalle(id)), "$.duracionBloqueMin")).isEqualTo(esperado);
	}

	@Test
	void unBooleanComoDuracionSeRechazaSinFallar() throws Exception {
		exigeCrearRechazadoSinEscribir(cuerpoDeCrear("duracionBloqueMin", "true"));
	}

	private static String receso(String despues, String duracion) {
		return "[{\"despuesDeBloque\":" + despues + ",\"duracionMin\":" + duracion + "}]";
	}

	static Stream<Arguments> recesosNumericosRechazados() {
		return Stream.of(
				Arguments.of("después del bloque 0", "0", "15", "Un receso va después de un bloque entre 1 y 5."),
				Arguments.of("después del bloque -1", "-1", "15", "Un receso va después de un bloque entre 1 y 5."),
				Arguments.of("después del último bloque", "6", "15", "Un receso va después de un bloque entre 1 y 5."),
				Arguments.of("después del bloque bloques+1", "7", "15",
						"Un receso va después de un bloque entre 1 y 5."),
				Arguments.of("después del bloque MAX", "2147483647", "15",
						"Un receso va después de un bloque entre 1 y 5."),
				Arguments.of("duración 4", "2", "4", "El receso dura de 5 a 60 minutos."),
				Arguments.of("duración 61", "2", "61", "El receso dura de 5 a 60 minutos."),
				Arguments.of("duración 0", "2", "0", "El receso dura de 5 a 60 minutos."),
				Arguments.of("duración negativa", "2", "-10", "El receso dura de 5 a 60 minutos."),
				Arguments.of("duración MAX", "2", "2147483647", "El receso dura de 5 a 60 minutos."),
				Arguments.of("duración fuera de int", "2", "99999999999", MENSAJE_FORMATO));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("recesosNumericosRechazados")
	void losRecesosFueraDeRangoSeRechazan(String nombre, String despues, String duracion, String mensaje)
			throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("recesos", receso(despues, duracion)));

		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(mensaje);
	}

	static Stream<Arguments> recesosNumericosAceptados() {
		return Stream.of(
				Arguments.of("después del bloque bloques-1", "5", "15"),
				Arguments.of("duración 5", "2", "5"),
				Arguments.of("duración 60", "2", "60"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("recesosNumericosAceptados")
	void losRecesosEnElBordeSeAceptan(String nombre, String despues, String duracion) throws Exception {
		String id = crearValida("recesos", receso(despues, duracion));

		assertThat((List<?>) JsonPath.read(cuerpoDe(detalle(id)), "$.recesos")).hasSize(1);
	}

	// ---------- 4. Hora de entrada y jornada máxima ----------

	static Stream<Arguments> horasMalEscritas() {
		return Stream.of(
				Arguments.of("25:99", "\"25:99\""),
				Arguments.of("24:00", "\"24:00\""),
				Arguments.of("7:45 sin cero inicial", "\"7:45\""),
				Arguments.of("07-45", "\"07-45\""),
				Arguments.of("07:45 PM", "\"07:45 PM\""),
				Arguments.of("un número", "745"),
				Arguments.of("un arreglo de enteros fuera de rango", "[25,99]"),
				Arguments.of("un objeto", "{}"),
				Arguments.of("un booleano", "true"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("horasMalEscritas")
	void unaHoraDeEntradaMalEscritaSeRechazaSinEscribirNada(String nombre, String literal) throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("horaEntrada", literal));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(MENSAJE_FORMATO);
	}

	@Test
	void laHoraDeEntradaVaciaONulaOAusenteSeRechazaConSuMensaje() throws Exception {
		for (String literal : new String[]{"\"\"", "null", null}) {
			MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("horaEntrada", literal));

			assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message"))
					.as(String.valueOf(literal)).isEqualTo("Ingresa la hora de entrada.");
		}
	}

	/**
	 * Corregido: la hora con segundos se trunca a minutos al normalizar, así lo guardado, lo mostrado y lo calculado
	 * coinciden (07:45). Respuesta real: 201, la BD guarda 07:45:00.
	 */
	@Test
	void laHoraConSegundosSeTruncaAMinutosYSeGuardaIgualQueSeMuestra() throws Exception {
		String id = crearValida("horaEntrada", "\"07:45:30\"", "bloquesPorDia", "2");

		assertThat(leerDelDetalle(id, "$.horaEntrada")).isEqualTo("07:45");
		assertThat(leerDelDetalle(id, "$.jornada[0].inicio")).isEqualTo("07:45");
		String guardada = jdbc.sql("SELECT hora_entrada::text FROM configuracion.estructura_horaria WHERE id = :id")
				.param("id", UUID.fromString(id)).query(String.class).single();
		assertThat(guardada).isEqualTo("07:45:00");
	}

	@Test
	void lasHorasLimiteConLaReglaDeLas1800() throws Exception {
		// 00:00 con 6 bloques de 45 min termina a las 04:30: válida.
		crearValida("codigo", texto("EH-0000"), "horaEntrada", "\"00:00\"");
		// 23:59 supera las 18:00.
		MvcResult tarde = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("horaEntrada", "\"23:59\""));

		assertThat((String) JsonPath.read(cuerpoDe(tarde), "$.message"))
				.isEqualTo("La jornada debe terminar como máximo a las 18:00.");
	}

	@Test
	void laJornadaMaximaDesdeLas2359NoDesbordaNiDaHorasNegativas() throws Exception {
		String once = "[" + String.join(",", java.util.stream.IntStream.rangeClosed(1, 11)
				.mapToObj(despues -> "{\"despuesDeBloque\":" + despues + ",\"duracionMin\":60}").toList()) + "]";
		String cuerpo = cuerpoDeCrear("horaEntrada", "\"23:59\"", "bloquesPorDia", "12", "duracionBloqueMin", "120",
				"recesos", once);

		MvcResult previa = vistaPrevia(cuerpo);
		MvcResult creacion = exigeCrearRechazadoSinEscribir(cuerpo);

		// HUECO: la vista previa muestra horas pasada la medianoche como "58:59" (no es una hora válida).
		assertThat(previa.getResponse().getStatus()).isEqualTo(200);
		assertThat((String) JsonPath.read(cuerpoDe(previa), "$.jornada.horaSalida")).isEqualTo("58:59");
		assertThat((Boolean) JsonPath.read(cuerpoDe(previa), "$.valida")).isFalse();
		assertThat((List<?>) JsonPath.read(cuerpoDe(previa), "$.jornada.filas")).hasSize(23);
		assertThat(cuerpoDe(previa)).doesNotContain("\"inicio\":\"-");
		assertThat((String) JsonPath.read(cuerpoDe(creacion), "$.message"))
				.isEqualTo("La jornada debe terminar como máximo a las 18:00.");
	}

	// ---------- 5. Recesos con forma rara ----------

	static Stream<Arguments> recesosAceptadosConFormaRara() {
		return Stream.of(
				Arguments.of("null", "null", 0),
				Arguments.of("clave ausente", null, 0),
				Arguments.of("arreglo vacío", "[]", 0),
				// HUECO: un null dentro del arreglo se descarta en silencio y la petición se acepta.
				Arguments.of("[null]", "[null]", 0),
				Arguments.of("desordenados", "[{\"despuesDeBloque\":4,\"duracionMin\":10},{\"despuesDeBloque\":2,"
						+ "\"duracionMin\":20}]", 2),
				Arguments.of("con campos de más", "[{\"despuesDeBloque\":2,\"duracionMin\":15,\"id\":\""
						+ UUID_NULO + "\",\"foo\":\"bar\"}]", 1));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("recesosAceptadosConFormaRara")
	void losRecesosNulosVaciosDesordenadosOConCamposDeMasSeAceptan(String nombre, String literal, int cantidad)
			throws Exception {
		String id = crearValida("recesos", literal);

		List<Integer> bloques = JsonPath.read(cuerpoDe(detalle(id)), "$.recesos[*].despuesDeBloque");
		assertThat(bloques).hasSize(cantidad).isSorted();
	}

	static Stream<Arguments> recesosRechazados() {
		return Stream.of(
				Arguments.of("[{}]", "[{}]", null),
				Arguments.of("duplicados", "[{\"despuesDeBloque\":2,\"duracionMin\":10},{\"despuesDeBloque\":2,"
						+ "\"duracionMin\":10}]", "No puede haber dos recesos después del mismo bloque."),
				Arguments.of("falta la duración", "[{\"despuesDeBloque\":2}]",
						"El receso dura de 5 a 60 minutos."),
				Arguments.of("falta el bloque", "[{\"duracionMin\":15}]",
						"Un receso va después de un bloque entre 1 y 5."),
				Arguments.of("cinco mil elementos", arreglo("{\"despuesDeBloque\":2,\"duracionMin\":10}", 5000),
						"No puede haber dos recesos después del mismo bloque."),
				Arguments.of("un objeto en vez de arreglo", "{\"a\":1}", MENSAJE_FORMATO),
				Arguments.of("un texto en vez de arreglo", "\"x\"", MENSAJE_FORMATO));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("recesosRechazados")
	void losRecesosMalFormadosSeRechazanSinEscribirNada(String nombre, String literal, String mensaje)
			throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("recesos", literal));

		if (mensaje != null) {
			assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(mensaje);
		}
	}

	// ---------- 6. Identificadores y rutas ----------

	static Stream<Arguments> segmentosConUuidMalFormado() {
		return Stream.of(
				Arguments.of("UUID mal escrito", "no-es-un-uuid"),
				Arguments.of("con llaves", "%7B31000000-0000-0000-0000-000000000001%7D"),
				Arguments.of("UUID incompleto", "31000000-0000-0000-0000"),
				Arguments.of("id larguísimo", "a".repeat(5000)));
	}

	static Stream<Arguments> segmentosQueRechazaElFirewall() {
		return Stream.of(
				Arguments.of("punto punto", ".."),
				Arguments.of("punto punto codificado", "%2e%2e"),
				Arguments.of("NUL codificado", "%00"),
				Arguments.of("barra codificada", "a%2Fb"));
	}

	/**
	 * Un identificador mal formado en la ruta debe dar 4xx limpio en detalle, editar y eliminar, sin tocar la BD.
	 * Cuando {@code permiteCuerpoVacio} es true se admite el cuerpo vacío del firewall.
	 */
	private void exigeRutaMalFormadaRechazada(String segmento, boolean permiteCuerpoVacio) throws Exception {
		URI uri = URI.create("/configuracion/estructuras-horarias/" + segmento);
		String antes = foto();
		List<String> problemas = new ArrayList<>();
		for (String metodo : List.of("GET", "PUT", "DELETE")) {
			MockHttpServletRequestBuilder peticion = switch (metodo) {
				case "GET" -> get(uri);
				case "PUT" -> put(uri).contentType(MediaType.APPLICATION_JSON).content(cuerpoDeEditar());
				default -> delete(uri);
			};
			peticion.header("Authorization", "Bearer " + personalAdministrativo);
			MvcResult resultado = mvc.perform(peticion).andReturn();
			try {
				exigeRechazoLimpio(resultado, permiteCuerpoVacio);
			} catch (AssertionError fallo) {
				problemas.add(metodo + " -> estado " + resultado.getResponse().getStatus() + ": "
						+ abreviar(cuerpoDe(resultado)));
			}
		}

		assertThat(problemas).as("endpoints con entrada de ruta inválida mal resuelta").isEmpty();
		assertThat(foto()).isEqualTo(antes);
	}

	/**
	 * Pendiente compartido (BUG 4): un UUID mal escrito en la ruta da 500 porque ManejadorErrores no trata
	 * MethodArgumentTypeMismatchException. Debe ser 400.
	 */
	@Disabled(PENDIENTE_COMPARTIDO)
	@ParameterizedTest(name = "{0}")
	@MethodSource("segmentosConUuidMalFormado")
	void unUuidMalFormadoEnLaRutaDebeDar4xxLimpioEnLosTresEndpoints(String nombre, String segmento) throws Exception {
		exigeRutaMalFormadaRechazada(segmento, false);
	}

	/**
	 * HUECO (sin corregir): ".." y los códigos codificados los rechaza el firewall de Spring Security antes de llegar a
	 * la API. Respuesta real: 400 con el cuerpo vacío (no {message, errores}); no es un 5xx ni filtra nada.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("segmentosQueRechazaElFirewall")
	void unaRutaQueRechazaElFirewallDa400ConCuerpoVacio(String nombre, String segmento) throws Exception {
		exigeRutaMalFormadaRechazada(segmento, true);
	}

	/** Pendiente compartido (BUG 4): la ruta con barra final no coincide con nada y da 500; debe ser 404. */
	@Disabled(PENDIENTE_COMPARTIDO)
	@Test
	void unIdentificadorVacioEnLaRutaDa4xx() throws Exception {
		// "/estructuras-horarias/" no coincide con ninguna ruta. Si el manejador compartido lo trata como error
		// genérico (NoResourceFoundException), dará 500 en lugar de 404.
		MvcResult resultado = mvc.perform(conToken(get("/configuracion/estructuras-horarias/"), personalAdministrativo))
				.andReturn();

		exigeRechazoLimpio(resultado, true);
	}

	@Test
	void elUuidNuloYUnoAleatorioDan404ConSuMensaje() throws Exception {
		for (String id : new String[]{UUID_NULO, UUID.randomUUID().toString()}) {
			exigeRechazoConMensaje(detalle(id), 404, "La estructura horaria no existe.");
			exigeRechazoConMensaje(editar(id, cuerpoDeEditar()), 404, "La estructura horaria no existe.");
			exigeRechazoConMensaje(mvc.perform(conToken(delete(rutaEstructura(id)), personalAdministrativo))
					.andReturn(), 404, "La estructura horaria no existe.");
		}
	}

	@Test
	void unUuidEnMayusculasEsValidoYEncuentraLaEstructura() throws Exception {
		MvcResult resultado = detalle(EH_PRI_26.toUpperCase(Locale.ROOT));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.codigo")).isEqualTo("EH-PRI-26");
	}

	@Test
	void elIdDeUnAnioComoIdDeEstructuraYViceversaDan404() throws Exception {
		exigeRechazoConMensaje(detalle(ANIO_2026), 404, "La estructura horaria no existe.");
		exigeRechazoConMensaje(mvc.perform(conToken(get(rutaDelAnio(EH_PRI_26)), personalAdministrativo))
				.andReturn(), 404, "El año escolar no existe.");
	}

	private String crearAnioCerradoConEstructura() {
		UUID anioId = jdbc.sql("""
				INSERT INTO configuracion.anio_escolar (anio, fecha_inicio, fecha_fin, regimen, estado)
				VALUES (2025, '2025-03-03', '2025-12-19', 'BIMESTRAL', 'CERRADO') RETURNING id""")
				.query(UUID.class).single();
		UUID estructuraId = jdbc.sql("""
				INSERT INTO configuracion.estructura_horaria
				    (anio_escolar_id, codigo, dias_lectivos, hora_entrada, bloques_por_dia, duracion_bloque_min, nivel)
				VALUES (:anioId, 'EH-25', ARRAY['LUN', 'MAR'], '08:00', 6, 45, 'PRIMARIA') RETURNING id""")
				.param("anioId", anioId).query(UUID.class).single();
		entityManager.clear();
		return estructuraId.toString();
	}

	@Test
	void unaEstructuraDeUnAnioActivoOCerradoRechazaEditarYEliminarSinCambiarNada() throws Exception {
		String cerrada = crearAnioCerradoConEstructura();
		String antes = foto();

		for (String id : new String[]{EH_PRI_26, cerrada}) {
			exigeRechazoConMensaje(editar(id, cuerpoDeEditar("esPrincipal", "true")), 409, ANIO_NO_PLANIFICADO);
			exigeRechazoConMensaje(mvc.perform(conToken(delete(rutaEstructura(id)), personalAdministrativo))
					.andReturn(), 409, ANIO_NO_PLANIFICADO);
		}

		assertThat(foto()).isEqualTo(antes);
	}

	@Test
	void unAnioInexistenteDa404EnListarCrearYVistaPrevia() throws Exception {
		String cuerpo = cuerpoDeCrear();
		String antes = foto();

		exigeRechazoConMensaje(mvc.perform(conToken(get(rutaDelAnio(UUID_NULO)), personalAdministrativo)).andReturn(),
				404, "El año escolar no existe.");
		exigeRechazoConMensaje(mvc.perform(conToken(post(rutaDelAnio(UUID_NULO)), personalAdministrativo, cuerpo))
				.andReturn(), 404, "El año escolar no existe.");
		exigeRechazoConMensaje(mvc.perform(conToken(post(rutaDelAnio(UUID_NULO) + "/vista-previa"),
				personalAdministrativo, cuerpo)).andReturn(), 404, "El año escolar no existe.");

		assertThat(foto()).isEqualTo(antes);
	}

	/**
	 * Qué error gana cuando la ruta y el cuerpo son inválidos a la vez. Respuesta real: el JSON ilegible gana siempre
	 * (400, lo resuelve Spring antes de llegar a la API); luego gana el año o la estructura inexistente (404), luego el
	 * año que no admite cambios (409) y al final las reglas del cuerpo (400).
	 */
	@Test
	void cuandoLaRutaYElCuerpoSonInvalidosGanaPrimeroLoQueSeEvaluaAntes() throws Exception {
		String cuerpoInvalido = cuerpoDeCrear("bloquesPorDia", "13");

		exigeRechazoConMensaje(mvc.perform(conToken(post(rutaDelAnio(UUID_NULO)), personalAdministrativo,
				cuerpoInvalido)).andReturn(), 404, "El año escolar no existe.");
		exigeRechazoConMensaje(mvc.perform(conToken(post(rutaDelAnio(ANIO_2026)), personalAdministrativo,
				cuerpoInvalido)).andReturn(), 409, ANIO_NO_PLANIFICADO);
		exigeRechazoConMensaje(editar(UUID_NULO, cuerpoDeEditar("bloquesPorDia", "13")), 404,
				"La estructura horaria no existe.");
		exigeRechazoConMensaje(editar(EH_PRI_26, cuerpoDeEditar("bloquesPorDia", "13")), 409, ANIO_NO_PLANIFICADO);
		exigeRechazoConMensaje(mvc.perform(conToken(post(rutaDelAnio(UUID_NULO)), personalAdministrativo,
				"{\"codigo\":")).andReturn(), 400, MENSAJE_FORMATO);
	}

	// ---------- 7. Forma de la petición ----------

	static Stream<Arguments> cuerposIlegibles() {
		return Stream.of(
				Arguments.of("vacío", ""),
				Arguments.of("null", "null"),
				Arguments.of("arreglo", "[]"),
				Arguments.of("texto JSON suelto", "\"texto\""),
				Arguments.of("texto sin comillas", "texto suelto"),
				Arguments.of("JSON roto", "{\"codigo\":"),
				Arguments.of("llave sin cerrar", "{"),
				Arguments.of("número", "42"),
				Arguments.of("anidado muy profundo", "{\"a\":".repeat(5000) + "1" + "}".repeat(5000)));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("cuerposIlegibles")
	void unCuerpoIlegibleSeRechazaSinEscribirNada(String nombre, String cuerpo) throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpo);

		assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
	}

	/**
	 * OK: Jackson rechaza el texto que sobra después del JSON. Respuesta real: 400 "La solicitud tiene un formato
	 * inválido".
	 */
	@Test
	void laBasuraAlFinalDelJsonSeRechazaSinEscribirNada() throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(
				cuerpoDeCrear("codigo", texto("EH-BASURA")) + " basura al final");

		assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).isEqualTo(MENSAJE_FORMATO);
	}

	/**
	 * HUECO: las claves duplicadas se aceptan y gana la última aparición. Respuesta real: 201 con el código de la
	 * última clave. Propuesta: activar STRICT_DUPLICATE_DETECTION en Jackson (configuración compartida).
	 */
	@Test
	void lasClavesDuplicadasSeAceptanYGanaLaUltima() throws Exception {
		String conDuplicadas = "{\"codigo\":\"EH-PRIMERO\",\"codigo\":\"EH-SEGUNDO\"," + cuerpoDeCrear().substring(1);

		String id = exigeCreada(crear(conDuplicadas));

		assertThat(leerDelDetalle(id, "$.codigo")).isEqualTo("EH-HOSTIL");
	}

	/**
	 * HUECO: no hay límite de tamaño del cuerpo JSON. Un código de 10 MB llega hasta el dominio y se rechaza por largo
	 * (400), pero antes se leyó entero en memoria. Propuesta: limitar el tamaño de la petición en el servidor.
	 */
	@Test
	void unCuerpoDe10MbSeRechazaPorElLargoDelCodigoSinEscribirNada() throws Exception {
		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("codigo", texto("A".repeat(10_000_000))));

		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message"))
				.isEqualTo("El código admite hasta 20 caracteres.");
	}

	static Stream<Arguments> tiposDeContenidoIncorrectos() {
		return Stream.of(
				Arguments.of("text/plain", MediaType.TEXT_PLAIN),
				Arguments.of("application/x-www-form-urlencoded", MediaType.APPLICATION_FORM_URLENCODED),
				Arguments.of("application/xml", MediaType.APPLICATION_XML),
				Arguments.of("sin cabecera", null));
	}

	/**
	 * Un Content-Type que no es JSON debe dar 4xx (415). Es probable que el manejador compartido lo convierta en 500
	 * al atrapar HttpMediaTypeNotSupportedException con su handler de Exception: si falla, es BUG de archivo
	 * compartido.
	 */
	@Disabled(PENDIENTE_COMPARTIDO)
	@ParameterizedTest(name = "{0}")
	@MethodSource("tiposDeContenidoIncorrectos")
	void unTipoDeContenidoQueNoEsJsonDebeDar4xx(String nombre, MediaType tipo) throws Exception {
		MockHttpServletRequestBuilder peticion = post(rutaDelAnio(ANIO_2027))
				.header("Authorization", "Bearer " + personalAdministrativo).content(cuerpoDeCrear());
		if (tipo != null) {
			peticion.contentType(tipo);
		}
		String antes = foto();

		MvcResult resultado = mvc.perform(peticion).andReturn();

		exigeRechazoLimpio(resultado);
		assertThat(foto()).isEqualTo(antes);
	}

	static Stream<Arguments> metodosNoPermitidos() {
		return Stream.of(
				Arguments.of("PATCH sobre una estructura", "PATCH", "/configuracion/estructuras-horarias/" + EH_PRI_26),
				Arguments.of("POST sobre una estructura", "POST", "/configuracion/estructuras-horarias/" + EH_PRI_26),
				Arguments.of("PUT sobre la colección", "PUT", "/configuracion/anios-escolares/" + ANIO_2027
						+ "/estructuras-horarias"),
				Arguments.of("DELETE sobre la colección", "DELETE", "/configuracion/anios-escolares/" + ANIO_2027
						+ "/estructuras-horarias"));
	}

	/**
	 * Un método no permitido debe dar 405. Es probable que el manejador compartido lo convierta en 500 al atrapar
	 * HttpRequestMethodNotSupportedException: si falla, es BUG de archivo compartido.
	 */
	@Disabled(PENDIENTE_COMPARTIDO)
	@ParameterizedTest(name = "{0}")
	@MethodSource("metodosNoPermitidos")
	void unMetodoNoPermitidoDebeDar4xxSinTocarLaBase(String nombre, String metodo, String ruta) throws Exception {
		String antes = foto();
		MockHttpServletRequestBuilder peticion = switch (metodo) {
			case "PATCH" -> patch(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpoDeEditar());
			case "POST" -> post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpoDeCrear());
			case "PUT" -> put(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpoDeEditar());
			default -> delete(ruta);
		};
		peticion.header("Authorization", "Bearer " + personalAdministrativo);

		MvcResult resultado = mvc.perform(peticion).andReturn();

		exigeRechazoLimpio(resultado);
		assertThat(foto()).isEqualTo(antes);
	}

	@Test
	void unCuerpoEnUnGetSeIgnora() throws Exception {
		MvcResult resultado = mvc.perform(conToken(get(rutaDelAnio(ANIO_2026)), personalAdministrativo,
				cuerpoDeCrear())).andReturn();

		assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
	}

	/**
	 * Los campos que no son del contrato no se aplican: id, anioEscolarId, nivel, creadoEn, actualizadoEn y el id de
	 * un receso se ignoran. La BD conserva los valores originales (salvo la fecha de actualización, que sí cambia por
	 * ser una edición real).
	 */
	@Test
	void losCamposDeMasEnEditarNoSeAplican() throws Exception {
		String id = crearValida("nivel", texto("SECUNDARIA"), "recesos", receso("2", "15"));
		String antes = fotoSinFechaDeActualizacion();
		String recesoConId = "[{\"id\":\"" + UUID_NULO + "\",\"despuesDeBloque\":2,\"duracionMin\":15}]";
		String cuerpo = cuerpoDeEditar("esPrincipal", "true", "recesos", recesoConId).replaceFirst("\\{",
				"{\"id\":\"" + UUID_NULO + "\",\"anioEscolarId\":\"" + ANIO_2026
						+ "\",\"nivel\":\"PRIMARIA\",\"creadoEn\":\"2000-01-01T00:00:00Z\","
						+ "\"actualizadoEn\":\"2000-01-01T00:00:00Z\",");

		MvcResult resultado = editar(id, cuerpo);

		assertThat(resultado.getResponse().getStatus()).as(abreviar(cuerpoDe(resultado))).isEqualTo(200);
		assertThat(fotoSinFechaDeActualizacion()).isEqualTo(antes);
		assertThat(leerDelDetalle(id, "$.nivel")).isEqualTo("SECUNDARIA");
	}

	/**
	 * Corregido: omitir "esPrincipal" en el PUT significa "no cambiar". Editar la principal sin ese campo da 200 y
	 * sigue siendo principal; editar una que no lo es sin ese campo la deja como estaba. Desmarcar la principal exige
	 * un
	 * false explícito (409, probado en EstructuraHorariaIT).
	 */
	@Test
	void editarSinEnviarEsPrincipalConservaLaMarcaQueYaTiene() throws Exception {
		String principal = crearValida("codigo", texto("EH-A"));
		String otra = crearValida("codigo", texto("EH-B"));

		MvcResult sobrePrincipal = editar(principal, cuerpoDeEditar("codigo", texto("EH-A"), "esPrincipal", null));
		MvcResult sobreOtra = editar(otra, cuerpoDeEditar("codigo", texto("EH-B"), "esPrincipal", null));

		assertThat(sobrePrincipal.getResponse().getStatus()).as(abreviar(cuerpoDe(sobrePrincipal))).isEqualTo(200);
		assertThat(sobreOtra.getResponse().getStatus()).as(abreviar(cuerpoDe(sobreOtra))).isEqualTo(200);
		assertThat((Boolean) JsonPath.read(cuerpoDe(sobrePrincipal), "$.estructura.esPrincipal")).isTrue();
		assertThat((Boolean) JsonPath.read(cuerpoDe(sobreOtra), "$.estructura.esPrincipal")).isFalse();
		assertThat((List<?>) JsonPath.read(cuerpoDe(sobrePrincipal), "$.advertencias")).isEmpty();
	}

	@Test
	void unFalseExplicitoSobreLaPrincipalSigueDando409YNoCambiaNada() throws Exception {
		String principal = crearValida();
		String antes = foto();

		MvcResult resultado = editar(principal, cuerpoDeEditar("esPrincipal", "false"));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
		assertThat((String) JsonPath.read(cuerpoDe(resultado), "$.message")).startsWith("No se puede quitar la marca");
		assertThat(foto()).isEqualTo(antes);
	}

	@Test
	void unTrueExplicitoMarcaComoPrincipalYBajaLaAnterior() throws Exception {
		crearValida("codigo", texto("EH-A"));
		String otra = crearValida("codigo", texto("EH-B"));

		MvcResult resultado = editar(otra, cuerpoDeEditar("codigo", texto("EH-B"), "esPrincipal", "true"));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
		assertThat((Boolean) JsonPath.read(cuerpoDe(resultado), "$.estructura.esPrincipal")).isTrue();
		assertThat((List<?>) JsonPath.read(cuerpoDe(resultado), "$.advertencias")).hasSize(1);
	}

	// ---------- 8. Seguridad y roles ----------

	private List<MockHttpServletRequestBuilder> losSeisEndpoints(String estructuraId) {
		return List.of(
				get(rutaDelAnio(ANIO_2026)),
				get(rutaEstructura(estructuraId)),
				post(rutaDelAnio(ANIO_2027)).contentType(MediaType.APPLICATION_JSON).content(cuerpoDeCrear()),
				post(rutaDelAnio(ANIO_2027) + "/vista-previa").contentType(MediaType.APPLICATION_JSON)
						.content(cuerpoDeCrear()),
				put(rutaEstructura(estructuraId)).contentType(MediaType.APPLICATION_JSON).content(cuerpoDeEditar()),
				delete(rutaEstructura(estructuraId)));
	}

	static Stream<Arguments> credencialesInvalidas() {
		return Stream.of(
				Arguments.of("sin cabecera de autorización", null),
				Arguments.of("token basura", "Bearer basura"),
				Arguments.of("Bearer vacío", "Bearer "),
				Arguments.of("esquema equivocado", "Basic dXNlcjpwYXNz"),
				Arguments.of("JWT con tres partes falsas", "Bearer aaa.bbb.ccc"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("credencialesInvalidas")
	void sinTokenOConTokenInvalidoNingunEndpointRespondeAlgoQueNoSea4xxLimpio(String nombre, String autorizacion)
			throws Exception {
		String antes = foto();
		for (MockHttpServletRequestBuilder peticion : losSeisEndpoints(EH_PRI_26)) {
			if (autorizacion != null) {
				peticion.header("Authorization", autorizacion);
			}
			MvcResult resultado = mvc.perform(peticion).andReturn();

			int estado = resultado.getResponse().getStatus();
			assertThat(estado).as("estado con %s (cuerpo: %s)", nombre, abreviar(cuerpoDe(resultado)))
					.isIn(401, 403);
			assertThat(rastrosEn(cuerpoDe(resultado))).isEmpty();
		}
		assertThat(foto()).isEqualTo(antes);
	}

	@Test
	void unTokenConLaFirmaAlteradaSeRechaza() throws Exception {
		String alterado = personalAdministrativo.substring(0, personalAdministrativo.length() - 2) + "xx";

		MvcResult resultado = mvc.perform(get(rutaDelAnio(ANIO_2026)).header("Authorization", "Bearer " + alterado))
				.andReturn();

		assertThat(resultado.getResponse().getStatus()).isIn(401, 403);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("rolesSinPermiso")
	void losRolesSinPermisoRecibenSiempre403SinImportarSiLaEstructuraExiste(String nombre, String usuario)
			throws Exception {
		String token = token(usuario);
		String antes = foto();
		for (String estructuraId : new String[]{EH_PRI_26, UUID_NULO}) {
			for (MockHttpServletRequestBuilder peticion : losSeisEndpoints(estructuraId)) {
				MvcResult resultado = mvc.perform(peticion.header("Authorization", "Bearer " + token)).andReturn();

				assertThat(resultado.getResponse().getStatus()).as("%s, estructura %s", nombre, estructuraId)
						.isEqualTo(403);
				assertThat(rastrosEn(cuerpoDe(resultado))).isEmpty();
			}
		}
		assertThat(foto()).isEqualTo(antes);
	}

	static Stream<Arguments> rolesSinPermiso() {
		return Stream.of(
				Arguments.of("DOCENTE", DOCENTE),
				Arguments.of("APODERADO", APODERADO),
				Arguments.of("ESTUDIANTE", ESTUDIANTE),
				Arguments.of("COORDINADOR", COORDINADOR),
				Arguments.of("ADMINISTRADOR", ADMIN));
	}

	/** El 403 de un rol sin permiso es idéntico exista o no la estructura: no revela si existe. */
	@Test
	void elRechazoPorRolEsIgualParaUnaEstructuraQueExisteYUnaQueNo() throws Exception {
		String token = token(DOCENTE);

		MvcResult existente = mvc.perform(get(rutaEstructura(EH_PRI_26)).header("Authorization", "Bearer " + token))
				.andReturn();
		MvcResult inexistente = mvc.perform(get(rutaEstructura(UUID_NULO)).header("Authorization", "Bearer " + token))
				.andReturn();

		assertThat(existente.getResponse().getStatus()).isEqualTo(inexistente.getResponse().getStatus());
		assertThat(cuerpoDe(existente)).isEqualTo(cuerpoDe(inexistente));
	}

	/**
	 * BUG menor de fuga: con un rol sin permiso y un UUID mal escrito la respuesta es 500 (el manejador compartido) y
	 * no 403: Spring resuelve el argumento antes de aplicar @PreAuthorize, así que se distingue "ruta con UUID
	 * inválido" de "sin permiso". Se espera 403.
	 */
	@Disabled(PENDIENTE_COMPARTIDO)
	@Test
	void unRolSinPermisoNoDebeVerUnErrorDistintoPorUnUuidMalEscrito() throws Exception {
		String token = token(DOCENTE);

		MvcResult resultado = mvc
				.perform(get(rutaEstructura("no-es-un-uuid")).header("Authorization", "Bearer " + token))
				.andReturn();

		assertThat(resultado.getResponse().getStatus()).as(abreviar(cuerpoDe(resultado))).isEqualTo(403);
	}

	// ---------- 9. Estado y reglas de negocio con entradas extrañas ----------

	@Test
	void eliminarDosVecesLaMismaEstructuraDa404EnLaSegunda() throws Exception {
		crearValida("codigo", texto("EH-A"));
		String otra = crearValida("codigo", texto("EH-B"));

		assertThat(mvc.perform(conToken(delete(rutaEstructura(otra)), personalAdministrativo)).andReturn()
				.getResponse().getStatus()).isEqualTo(204);
		String antes = foto();

		exigeRechazoConMensaje(mvc.perform(conToken(delete(rutaEstructura(otra)), personalAdministrativo)).andReturn(),
				404, "La estructura horaria no existe.");
		assertThat(foto()).isEqualTo(antes);
	}

	@Test
	void editarUnaEstructuraEliminadaDa404() throws Exception {
		crearValida("codigo", texto("EH-A"));
		String otra = crearValida("codigo", texto("EH-B"));
		mvc.perform(conToken(delete(rutaEstructura(otra)), personalAdministrativo));
		String antes = foto();

		exigeRechazoConMensaje(editar(otra, cuerpoDeEditar()), 404, "La estructura horaria no existe.");

		assertThat(foto()).isEqualTo(antes);
	}

	@Test
	void marcarComoPrincipalLaQueYaLoEsNoCambiaNadaNiAvisa() throws Exception {
		String principal = crearValida();

		MvcResult resultado = editar(principal, cuerpoDeEditar("esPrincipal", "true"));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
		assertThat((List<?>) JsonPath.read(cuerpoDe(resultado), "$.advertencias")).isEmpty();
	}

	@Test
	void crearDosVecesElMismoCodigoConDistintaCapitalizacionDa409LaSegunda() throws Exception {
		crearValida("codigo", texto("Eh-Dos"));

		MvcResult resultado = exigeCrearRechazadoSinEscribir(cuerpoDeCrear("codigo", texto("eH-dOS")));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(409);
	}

	@Test
	void cambiarElNivelPorUnCampoExtraNoTieneEfecto() throws Exception {
		String id = crearValida("nivel", texto("PRIMARIA"));

		MvcResult resultado = editar(id, cuerpoDeEditar("esPrincipal", "true", "nivel", texto("SECUNDARIA")));

		assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
		assertThat(leerDelDetalle(id, "$.nivel")).isEqualTo("PRIMARIA");
	}

	@Test
	void crearEliminarYVolverACrearElMismoCodigoEnElMismoAnioFunciona() throws Exception {
		crearValida("codigo", texto("EH-A"));
		for (int vuelta = 0; vuelta < 3; vuelta++) {
			String id = crearValida("codigo", texto("EH-CICLO"), "recesos", receso("2", "15"));
			assertThat(mvc.perform(conToken(delete(rutaEstructura(id)), personalAdministrativo)).andReturn()
					.getResponse().getStatus()).isEqualTo(204);
		}

		assertThat(jdbc.sql("SELECT count(*) FROM configuracion.estructura_horaria WHERE codigo = 'EH-CICLO'")
				.query(Integer.class).single()).isZero();
		assertThat(jdbc.sql("SELECT count(*) FROM configuracion.receso r WHERE NOT EXISTS (SELECT 1 FROM "
				+ "configuracion.estructura_horaria e WHERE e.id = r.estructura_horaria_id)").query(Integer.class)
				.single()).isZero();
	}

	private void planificar(String gradoCodigo, String curso, int horas) {
		UUID cursoId = jdbc.sql("SELECT id FROM academico.curso WHERE nombre = :nombre").param("nombre", curso)
				.query(UUID.class).single();
		jdbc.sql("""
				INSERT INTO academico.plan_curso (anio_escolar_id, grado_codigo, curso_id, horas_semanales)
				VALUES (:anioId, :gradoCodigo, :cursoId, :horas)""")
				.param("anioId", UUID.fromString(ANIO_2027)).param("gradoCodigo", gradoCodigo)
				.param("cursoId", cursoId).param("horas", horas).update();
		entityManager.clear();
	}

	@Test
	void conPlanesAlLimiteLosBloquesIgualesALasHorasSeAceptanYUnoMenosSeRechaza() throws Exception {
		String principal = crearValida("codigo", texto("EH-SEC"), "nivel", texto("SECUNDARIA"));
		planificar("SEC-1", "Álgebra", 10);
		planificar("SEC-1", "Geometría", 10);
		planificar("SEC-1", "Aritmética", 8);

		// 4 días × 7 bloques = 28 bloques por semana = 28 horas planificadas: se acepta.
		MvcResult igual = editar(principal, cuerpoDeEditar("codigo", texto("EH-SEC"), "esPrincipal", "true",
				"diasLectivos", "[\"LUN\",\"MAR\",\"MIE\",\"JUE\"]", "bloquesPorDia", "7"));
		assertThat(igual.getResponse().getStatus()).as(abreviar(cuerpoDe(igual))).isEqualTo(200);
		String antes = foto();

		// 3 días × 9 bloques = 27 < 28: se rechaza y no cambia nada.
		MvcResult unoMenos = editar(principal, cuerpoDeEditar("codigo", texto("EH-SEC"), "esPrincipal", "true",
				"diasLectivos", "[\"LUN\",\"MAR\",\"MIE\"]", "bloquesPorDia", "9"));

		exigeRechazoLimpio(unoMenos);
		assertThat(unoMenos.getResponse().getStatus()).isEqualTo(409);
		assertThat((String) JsonPath.read(cuerpoDe(unoMenos), "$.message")).startsWith(
				"No se puede reducir la jornada a 27 bloques por semana: 1.° de secundaria ya tiene 28 horas");
		assertThat(foto()).isEqualTo(antes);
	}

	// ---------- 11. Fugas de información ----------

	/**
	 * Recorre una batería de peticiones que fallan de formas distintas y revisa que ninguna filtre detalles internos.
	 */
	@Test
	void ningunaRespuestaDeErrorFiltraDetallesInternos() throws Exception {
		String cerrada = crearAnioCerradoConEstructura();
		List<MvcResult> respuestas = new ArrayList<>();
		respuestas.add(crear(cuerpoDeCrear("codigo", texto("A".repeat(21)))));
		respuestas.add(crear("{\"codigo\":"));
		respuestas.add(crear(cuerpoDeCrear("horaEntrada", "\"25:99\"")));
		respuestas.add(crear(cuerpoDeCrear("bloquesPorDia", "99999999999")));
		respuestas.add(crear(cuerpoDeCrear("diasLectivos", "\"LUN\"")));
		respuestas.add(crear(cuerpoDeCrear("recesos", "[{}]")));
		respuestas.add(detalle("no-es-un-uuid"));
		respuestas.add(detalle(UUID_NULO));
		respuestas.add(editar(EH_PRI_26, cuerpoDeEditar()));
		respuestas.add(editar(cerrada, cuerpoDeEditar()));
		respuestas.add(mvc.perform(conToken(delete(rutaEstructura(UUID_NULO)), personalAdministrativo)).andReturn());
		respuestas.add(mvc.perform(conToken(get(rutaDelAnio("no-es-un-uuid")), personalAdministrativo)).andReturn());
		respuestas.add(mvc.perform(get(rutaDelAnio(ANIO_2026))).andReturn());
		// Los NUL no van aquí: PostgreSQL los rechaza y deja abortada la transacción de la prueba. Se prueban en
		// EstructuraHorariaConcurrenciaIT, que usa transacciones reales.

		List<String> filtraciones = new ArrayList<>();
		for (MvcResult respuesta : respuestas) {
			List<String> rastros = rastrosEn(cuerpoDe(respuesta));
			if (!rastros.isEmpty()) {
				filtraciones.add("estado " + respuesta.getResponse().getStatus() + " " + rastros + ": "
						+ abreviar(cuerpoDe(respuesta)));
			}
		}

		assertThat(filtraciones).as("respuestas que filtran detalles internos").isEmpty();
	}
}
