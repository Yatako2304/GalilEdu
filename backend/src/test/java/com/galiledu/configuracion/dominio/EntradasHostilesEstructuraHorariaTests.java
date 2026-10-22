package com.galiledu.configuracion.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.galiledu.configuracion.dominio.CalculadoraJornada.FilaJornada;
import com.galiledu.configuracion.dominio.CalculadoraJornada.Jornada;

/**
 * HU-44, entradas equivocadas y maliciosas contra el dominio puro (sin Spring ni BD): código, descripción, días,
 * recesos, {@code normalizada()} y {@code detectarErrores}. El objetivo es encontrar fallos, no sumar cobertura:
 * un comentario "HUECO" marca algo que se acepta y es dudoso pero no rompe nada.
 */
class EntradasHostilesEstructuraHorariaTests {
	private static final UUID ANIO = UUID.randomUUID();
	private static final List<String> LUN_A_VIE = List.of("LUN", "MAR", "MIE", "JUE", "VIE");

	private static DefinicionEstructura conCodigo(String codigo) {
		return new DefinicionEstructura(codigo, "Prueba", LUN_A_VIE, LocalTime.of(8, 0), 6, 45, List.of());
	}

	private static DefinicionEstructura conDescripcion(String descripcion) {
		return new DefinicionEstructura("EH-1", descripcion, LUN_A_VIE, LocalTime.of(8, 0), 6, 45, List.of());
	}

	private static DefinicionEstructura conDias(List<String> dias) {
		return new DefinicionEstructura("EH-1", null, dias, LocalTime.of(8, 0), 6, 45, List.of());
	}

	private static DefinicionEstructura conRecesos(List<DatosReceso> recesos) {
		return new DefinicionEstructura("EH-1", null, LUN_A_VIE, LocalTime.of(8, 0), 6, 45, recesos);
	}

	// ---------- 1. Texto: código y descripción ----------

	private static final String CODIGO_INVALIDO = "El código solo admite letras, números y guion.";
	private static final String TEXTO_CON_CONTROLES = "El texto contiene caracteres no válidos.";

	static Stream<Arguments> codigosSinContenidoQueTrimRecorta() {
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
	@MethodSource("codigosSinContenidoQueTrimRecorta")
	void unCodigoSinContenidoUtilSeRechazaComoVacio(String nombre, String codigo) {
		assertThat(EstructuraHoraria.detectarErrores(conCodigo(codigo)))
				.containsExactly("Ingresa el código de la estructura.");
	}

	static Stream<Arguments> codigosDeSoloCaracteresInvisibles() {
		return Stream.of(
				Arguments.of("espacio de ancho cero U+200B", Character.toString(0x200B)),
				Arguments.of("espacio duro U+00A0", Character.toString(0x00A0)),
				Arguments.of("espacio em U+2003", Character.toString(0x2003)),
				Arguments.of("marca de orden de bytes U+FEFF", Character.toString(0xFEFF)),
				Arguments.of("ancho cero y duro juntos", Character.toString(0x200B) + Character.toString(0x00A0)));
	}

	/**
	 * Corregido: String.trim() no recorta estos caracteres, pero ya no importa: tras normalizar, el código solo admite
	 * letras A-Z, números y guion, así que un código invisible se rechaza.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("codigosDeSoloCaracteresInvisibles")
	void unCodigoDeSoloCaracteresInvisiblesSeRechazaPorNoSerLetrasNumerosNiGuion(String nombre, String codigo) {
		assertThat(EstructuraHoraria.detectarErrores(conCodigo(codigo))).containsExactly(CODIGO_INVALIDO);
	}

	@Test
	void elCodigoEnElLimiteDe20SeAceptaYConUnoMasSeRechaza() {
		assertThat(EstructuraHoraria.detectarErrores(conCodigo("A".repeat(20)))).isEmpty();
		assertThat(EstructuraHoraria.detectarErrores(conCodigo("A".repeat(21))))
				.containsExactly("El código admite hasta 20 caracteres.");
		assertThat(EstructuraHoraria.detectarErrores(conCodigo("A".repeat(100_000))))
				.containsExactly("El código admite hasta 20 caracteres.");
	}

	@Test
	void laDescripcionEnElLimiteDe100SeAceptaYConUnaMasSeRechaza() {
		assertThat(EstructuraHoraria.detectarErrores(conDescripcion("D".repeat(100)))).isEmpty();
		assertThat(EstructuraHoraria.detectarErrores(conDescripcion("D".repeat(101))))
				.containsExactly("La descripción admite hasta 100 caracteres.");
		assertThat(EstructuraHoraria.detectarErrores(conDescripcion("D".repeat(100_000))))
				.containsExactly("La descripción admite hasta 100 caracteres.");
	}

	@Test
	void unaDescripcionSoloDeBlancosSeGuardaComoSinDescripcion() {
		assertThat(conDescripcion("   \t\n").normalizada().descripcion()).isNull();
		assertThat(conDescripcion("").normalizada().descripcion()).isNull();
		assertThat(conDescripcion(null).normalizada().descripcion()).isNull();
	}

	@Test
	void losCodigosDeLaHuYLosDelModalDeFigmaSonValidos() {
		for (String codigo : List.of("EH-PRI-26", "EH-SEC-26", "EH-TAR-26", "eh-tar-26", " EH-TAR-26 ", "A", "0-9")) {
			assertThat(EstructuraHoraria.detectarErrores(conCodigo(codigo))).as(codigo).isEmpty();
		}
	}

	static Stream<Arguments> codigosEspecialesYSuNormalizacion() {
		return Stream.of(
				Arguments.of("mayúsculas y minúsculas mezcladas", "eH-pRi-1", "EH-PRI-1", true),
				Arguments.of("espacios al borde", " eh-1 ", "EH-1", true),
				Arguments.of("ß se alarga a SS y sigue siendo A-Z", "ß", "SS", true),
				Arguments.of("ñ y tildes", "ñandú-áé", "ÑANDÚ-ÁÉ", false),
				Arguments.of("emoji", "\uD83D\uDE00", "\uD83D\uDE00", false),
				Arguments.of("cirílico minúscula", "\u0435\u043D-1", "\u0415\u041D-1", false),
				Arguments.of("griego", "\u03B1\u03B2\u03B3-1", "\u0391\u0392\u0393-1", false),
				Arguments.of("ancho completo", "\uFF25\uFF28-\uFF11", "\uFF25\uFF28-\uFF11", false));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("codigosEspecialesYSuNormalizacion")
	void losCodigosEspecialesSeNormalizanYSoloPasanSiQuedanEnAaZNumerosYGuion(String nombre, String entrada,
			String esperado, boolean esValido) {
		String normalizado = conCodigo(entrada).normalizada().codigo();

		// La normalización es toUpperCase(Locale.ROOT) tras recortar: el resultado debe ser exactamente ese.
		assertThat(normalizado).isEqualTo(esperado).isEqualTo(entrada.trim().toUpperCase(Locale.ROOT));
		assertThat(EstructuraHoraria.detectarErrores(conCodigo(entrada)).isEmpty()).isEqualTo(esValido);
	}

	@Test
	void laEszettQueSeAlargaASSHaceQueElLargoSeCuenteYaNormalizado() {
		// HUECO: 11 "ß" son 11 caracteres escritos pero 22 al guardarse ("SS" × 11): se rechaza por largo.
		String once = "ß".repeat(11);

		assertThat(conCodigo(once).normalizada().codigo()).hasSize(22);
		assertThat(EstructuraHoraria.detectarErrores(conCodigo(once)))
				.containsExactly("El código admite hasta 20 caracteres.");
		assertThat(EstructuraHoraria.detectarErrores(conCodigo("ß".repeat(10)))).isEmpty();
	}

	@Test
	void losEmojisYLasLetrasDeOtrosAlfabetosYaNoSeAceptanComoCodigo() {
		// Antes: 10 emojis cabían (20 unidades UTF-16) y las letras cirílicas se aceptaban como "otro" código.
		// Ahora ninguna pasa la regla de letras A-Z, números y guion; con 11 emojis gana el error de largo (22).
		String emojis = "\uD83D\uDE00";
		assertThat(EstructuraHoraria.detectarErrores(conCodigo(emojis.repeat(10)))).containsExactly(CODIGO_INVALIDO);
		assertThat(EstructuraHoraria.detectarErrores(conCodigo(emojis.repeat(11))))
				.containsExactly("El código admite hasta 20 caracteres.");
		assertThat(EstructuraHoraria.detectarErrores(conCodigo("\u0415\u041D-1"))).containsExactly(CODIGO_INVALIDO);
	}

	@Test
	void elMismoCodigoConYSinEspaciosYEnOtraCapitalizacionEsElMismoCodigo() {
		String normalizado = conCodigo("EH-1").normalizada().codigo();

		for (String variante : List.of("eh-1", " EH-1 ", "Eh-1", "\tEH-1\n", "eH-1  ")) {
			assertThat(conCodigo(variante).normalizada().codigo()).as(variante).isEqualTo(normalizado);
		}
	}

	@Test
	void normalizadaEsIdempotente() {
		DefinicionEstructura una = new DefinicionEstructura("  eh-1 ", "  d  ", List.of("vie", "lun"),
				LocalTime.of(8, 0, 30), 6, 45, List.of(new DatosReceso(4, 10), new DatosReceso(2, 10))).normalizada();

		assertThat(una.normalizada()).isEqualTo(una);
	}

	/**
	 * Corregido: el código y la descripción rechazan los caracteres de control que quedan en medio (NUL, CR, LF,
	 * tabulación, escape). Los que están al borde los recorta trim() y no cuentan. Además el código rechaza rutas,
	 * barras
	 * y espacios internos por la regla de letras A-Z, números y guion.
	 */
	@Test
	void elCodigoYLaDescripcionRechazanLosCaracteresDeControlEnMedio() {
		for (String texto : List.of("A\u0001B", "A\u001BB", "A\r\nB", "A\tB", "A\u0000B", "A\u0085B")) {
			assertThat(EstructuraHoraria.detectarErrores(conCodigo(texto))).as("código " + texto)
					.containsExactly(TEXTO_CON_CONTROLES);
			assertThat(EstructuraHoraria.detectarErrores(conDescripcion(texto))).as("descripción " + texto)
					.containsExactly(TEXTO_CON_CONTROLES);
		}
	}

	@Test
	void elMensajeDeControlNoSeRepiteSiElCodigoYLaDescripcionLoTienen() {
		DefinicionEstructura ambos = new DefinicionEstructura("A\u0000B", "x\u0000y", LUN_A_VIE, LocalTime.of(8, 0), 6,
				45, List.of());

		assertThat(EstructuraHoraria.detectarErrores(ambos)).containsExactly(TEXTO_CON_CONTROLES);
	}

	@Test
	void losControlesAlBordeDeLaDescripcionSeRecortanYNoSeRechazan() {
		assertThat(EstructuraHoraria.detectarErrores(conDescripcion("\r\nhola\t"))).isEmpty();
		assertThat(conDescripcion("\r\nhola\t").normalizada().descripcion()).isEqualTo("hola");
	}

	@Test
	void elCodigoRechazaRutasBarrasYEspaciosInternos() {
		for (String codigo : List.of("../x", "EH 1", "a/b\\c", "EH_1", "EH.1", "EH-1!")) {
			assertThat(EstructuraHoraria.detectarErrores(conCodigo(codigo))).as(codigo)
					.containsExactly(CODIGO_INVALIDO);
		}
	}

	static Stream<String> textosDeInyeccion() {
		return Stream.of("'; DROP TABLE configuracion.receso; --", "' OR '1'='1", "<script>alert(1)</script>",
				"${jndi:ldap://x}", "%s%n%x", "{{7*7}}");
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("textosDeInyeccion")
	void lasCadenasDeInyeccionSonDatosEnLaDescripcionYSeRechazanComoCodigo(String texto) {
		// Como descripción (hasta 100, sin controles) son válidas y se guardan tal cual: no se interpretan.
		assertThat(EstructuraHoraria.detectarErrores(conDescripcion(texto))).isEmpty();
		assertThat(conDescripcion(texto).normalizada().descripcion()).isEqualTo(texto.trim());
		// Como código ninguna cumple letras A-Z, números y guion: se rechazan por largo o por caracteres no válidos.
		List<String> errores = EstructuraHoraria.detectarErrores(conCodigo(texto));
		assertThat(errores).hasSize(1);
		assertThat(errores.get(0)).isIn("El código admite hasta 20 caracteres.", CODIGO_INVALIDO);
	}

	@Test
	void unCodigoQueValidaNuncaSuperaLaColumnaDe20NiTieneCaracteresFueraDeLaRegla() {
		// Mayúsculas que cambian de largo: ŉ → ʼN, ǰ → J̌, ΐ → Ϊ́. El largo se mide ya normalizado.
		for (String codigo : List.of("ŉ".repeat(10), "ǰ".repeat(10), "ΐ".repeat(7), "ß".repeat(10), "ß".repeat(11))) {
			String normalizado = conCodigo(codigo).normalizada().codigo();
			boolean valido = EstructuraHoraria.detectarErrores(conCodigo(codigo)).isEmpty();
			if (valido) {
				assertThat(normalizado).hasSizeLessThanOrEqualTo(20).matches("[A-Z0-9-]+");
			}
		}
	}

	// ---------- 2. Días lectivos ----------

	@Test
	void losDiasVaciosNulosONulosDentroDelArregloSeRechazan() {
		assertThat(EstructuraHoraria.detectarErrores(conDias(List.of())))
				.containsExactly("Elige al menos un día lectivo.");
		assertThat(EstructuraHoraria.detectarErrores(conDias(null))).containsExactly("Elige al menos un día lectivo.");
		// Un día null o en blanco da un mensaje claro, una sola vez aunque haya varios.
		String enBlanco = "Elige solo días válidos: de LUN a SAB.";
		assertThat(EstructuraHoraria.detectarErrores(conDias(Arrays.asList((String) null))))
				.containsExactly(enBlanco);
		assertThat(EstructuraHoraria.detectarErrores(conDias(List.of("")))).containsExactly(enBlanco);
		assertThat(EstructuraHoraria.detectarErrores(conDias(Arrays.asList(null, "  ", "LUN"))))
				.containsExactly(enBlanco);
		assertThat(EstructuraHoraria.detectarErrores(conDias(Arrays.asList(null, null)))).containsExactly(enBlanco);
	}

	@Test
	void losDiasEnMinusculasSeAceptanYLosNombresCompletosONoExistentesSeRechazan() {
		assertThat(EstructuraHoraria.detectarErrores(conDias(List.of("lun", " mar ")))).isEmpty();
		assertThat(EstructuraHoraria.detectarErrores(conDias(List.of("LUNES"))))
				.containsExactly("Día lectivo no válido: LUNES.");
		assertThat(EstructuraHoraria.detectarErrores(conDias(List.of("DOM"))))
				.containsExactly("Día lectivo no válido: DOM.");
		assertThat(EstructuraHoraria.detectarErrores(conDias(List.of("LUN", "XYZ", "MAR"))))
				.containsExactly("Día lectivo no válido: XYZ.");
	}

	@Test
	void losDiasRepetidosSeRechazanYLosSeisDiasSeAceptanEnOrdenDeSemana() {
		assertThat(EstructuraHoraria.detectarErrores(conDias(List.of("LUN", "LUN"))))
				.containsExactly("No repitas los días lectivos.");
		DefinicionEstructura seisDesordenados = conDias(List.of("SAB", "MIE", "LUN", "VIE", "MAR", "JUE"));

		assertThat(EstructuraHoraria.detectarErrores(seisDesordenados)).isEmpty();
		assertThat(seisDesordenados.normalizada().diasLectivos())
				.containsExactly("LUN", "MAR", "MIE", "JUE", "VIE", "SAB");
	}

	/**
	 * HUECO (sin corregir): mil días inválidos dan un error por cada uno (mil mensajes). Crear usa solo el primero; la
	 * vista previa los devolvería todos. Se documenta el comportamiento actual.
	 */
	@Test
	void milDiasRepetidosOMilInvalidosSeRechazanSinFallar() {
		List<String> milLunes = new ArrayList<>();
		List<String> milInvalidos = new ArrayList<>();
		for (int numero = 0; numero < 1000; numero++) {
			milLunes.add("LUN");
			milInvalidos.add("D" + numero);
		}

		assertThat(EstructuraHoraria.detectarErrores(conDias(milLunes)))
				.containsExactly("No repitas los días lectivos.");
		assertThat(EstructuraHoraria.detectarErrores(conDias(milInvalidos))).hasSize(1000);
	}

	// ---------- 3. Números y recesos ----------

	@Test
	void losBloquesYLaDuracionEnLosBordesYFueraDeEllos() {
		for (int bloques : new int[]{0, -1, 13, 20, 120, 121, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
			DefinicionEstructura definicion = new DefinicionEstructura("EH-1", null, LUN_A_VIE, LocalTime.of(8, 0),
					bloques, 45, List.of());
			assertThat(EstructuraHoraria.detectarErrores(definicion)).as("bloques " + bloques)
					.containsExactly("Los bloques por día van de 1 a 12.");
			assertThat(EstructuraHoraria.calcularJornada(definicion)).isEmpty();
		}
		for (int duracion : new int[]{0, -1, 1, 19, 121, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
			DefinicionEstructura definicion = new DefinicionEstructura("EH-1", null, LUN_A_VIE, LocalTime.of(8, 0), 2,
					duracion, List.of());
			assertThat(EstructuraHoraria.detectarErrores(definicion)).as("duración " + duracion)
					.containsExactly("La duración del bloque va de 20 a 120 minutos.");
		}
	}

	@Test
	void losRecesosEnLosBordesYFueraDeEllos() {
		for (int despues : new int[]{0, -1, 6, 7, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
			assertThat(EstructuraHoraria.detectarErrores(conRecesos(List.of(new DatosReceso(despues, 15)))))
					.as("después del bloque " + despues)
					.containsExactly("Un receso va después de un bloque entre 1 y 5.");
		}
		assertThat(EstructuraHoraria.detectarErrores(conRecesos(List.of(new DatosReceso(5, 15))))).isEmpty();
		for (int duracion : new int[]{4, 0, -10, 61, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
			assertThat(EstructuraHoraria.detectarErrores(conRecesos(List.of(new DatosReceso(2, duracion)))))
					.as("duración " + duracion).containsExactly("El receso dura de 5 a 60 minutos.");
		}
		assertThat(EstructuraHoraria.detectarErrores(conRecesos(List.of(new DatosReceso(2, 5))))).isEmpty();
		assertThat(EstructuraHoraria.detectarErrores(conRecesos(List.of(new DatosReceso(2, 60))))).isEmpty();
	}

	// ---------- 4. Hora de entrada y jornada máxima ----------

	@Test
	void laJornadaMaximaPosibleNoDesbordaNiDaHorasNegativas() {
		// 12 bloques de 120 min con 11 recesos de 60 min: 1 440 + 660 = 2 100 min, desde las 23:59.
		List<DatosReceso> once = new ArrayList<>();
		for (int despues = 1; despues <= 11; despues++) {
			once.add(new DatosReceso(despues, 60));
		}
		DefinicionEstructura maxima = new DefinicionEstructura("EH-MAX", null, LUN_A_VIE, LocalTime.of(23, 59), 12, 120,
				once);

		Jornada jornada = EstructuraHoraria.calcularJornada(maxima).orElseThrow();

		assertThat(jornada.salidaMinutos()).isEqualTo(23 * 60 + 59 + 2100);
		assertThat(jornada.filas()).hasSize(23);
		for (FilaJornada fila : jornada.filas()) {
			assertThat(fila.inicioMinutos()).isNotNegative();
			assertThat(fila.finMinutos()).isGreaterThan(fila.inicioMinutos());
		}
		assertThat(EstructuraHoraria.detectarErrores(maxima))
				.containsExactly("La jornada debe terminar como máximo a las 18:00.");
		// HUECO: pasada la medianoche las horas siguen subiendo ("58:59"), que no es una hora válida.
		assertThat(CalculadoraJornada.formatoHora(jornada.salidaMinutos())).isEqualTo("58:59");
	}

	@Test
	void lasHorasLimiteConLaReglaDeLas1800() {
		assertThat(EstructuraHoraria.detectarErrores(new DefinicionEstructura("EH-1", null, LUN_A_VIE,
				LocalTime.of(0, 0), 6, 45, List.of()))).isEmpty();
		assertThat(EstructuraHoraria.detectarErrores(new DefinicionEstructura("EH-1", null, LUN_A_VIE,
				LocalTime.of(23, 59), 1, 20, List.of())))
				.containsExactly("La jornada debe terminar como máximo a las 18:00.");
		assertThat(EstructuraHoraria.detectarErrores(new DefinicionEstructura("EH-1", null, LUN_A_VIE,
				LocalTime.of(17, 40), 1, 20, List.of()))).isEmpty();
		assertThat(EstructuraHoraria.detectarErrores(new DefinicionEstructura("EH-1", null, LUN_A_VIE,
				LocalTime.of(17, 41), 1, 20, List.of())))
				.containsExactly("La jornada debe terminar como máximo a las 18:00.");
	}

	/**
	 * Corregido: la hora con segundos se trunca a minutos al normalizar, así lo guardado (07:45) y lo calculado
	 * (07:45) coinciden.
	 */
	@Test
	void laHoraConSegundosSeTruncaAMinutosAlNormalizar() {
		DefinicionEstructura conSegundos = new DefinicionEstructura("EH-1", null, LUN_A_VIE, LocalTime.of(7, 45, 59), 2,
				45, List.of());

		EstructuraHoraria estructura = EstructuraHoraria.crear(ANIO, "PRIMARIA", true, conSegundos);

		assertThat(conSegundos.normalizada().horaEntrada()).isEqualTo(LocalTime.of(7, 45));
		assertThat(estructura.getHoraEntrada()).isEqualTo(LocalTime.of(7, 45));
		assertThat(estructura.jornada().filas().get(0).inicioMinutos()).isEqualTo(7 * 60 + 45);
	}

	// ---------- 5. Recesos con forma rara ----------

	@Test
	void losRecesosNulosVaciosODuplicadosNoFallanEnLaNormalizacion() {
		assertThatCode(() -> conRecesos(null).normalizada()).doesNotThrowAnyException();
		assertThat(EstructuraHoraria.detectarErrores(conRecesos(null))).isEmpty();
		assertThat(EstructuraHoraria.detectarErrores(conRecesos(List.of()))).isEmpty();
		assertThat(
				EstructuraHoraria.detectarErrores(conRecesos(List.of(new DatosReceso(2, 10), new DatosReceso(2, 10)))))
				.containsExactly("No puede haber dos recesos después del mismo bloque.");
	}

	/**
	 * BUG latente del dominio (no alcanzable por HTTP hoy): un null dentro de la lista de recesos lanza
	 * NullPointerException. Con un solo null, normalizada() no falla (ordenar un elemento no llama al comparador) pero
	 * detectarErrores sí, al leer receso.duracionMin(); con [null, x], falla ya la normalización. La capa de
	 * aplicación descarta los null al armar la definición, por eso la API no lo muestra; cualquier otro llamador
	 * daría un 500. La prueba exige el comportamiento correcto (no lanzar) y por eso hoy FALLA. Corrección mínima:
	 * ignorar los null en normalizada() (filter(Objects::nonNull)).
	 */
	@Test
	void unNullDentroDeLosRecesosNoDebeRevertarElDominio() {
		DefinicionEstructura soloNull = conRecesos(Arrays.asList((DatosReceso) null));
		DefinicionEstructura nullYUnReceso = conRecesos(Arrays.asList(null, new DatosReceso(2, 10)));

		assertThatCode(() -> EstructuraHoraria.detectarErrores(soloNull)).doesNotThrowAnyException();
		assertThatCode(() -> EstructuraHoraria.detectarErrores(nullYUnReceso)).doesNotThrowAnyException();
		assertThatCode(nullYUnReceso::normalizada).doesNotThrowAnyException();
	}

	@Test
	void cincoMilRecesosSeRechazanSinFallar() {
		List<DatosReceso> cincoMil = new ArrayList<>();
		for (int numero = 0; numero < 5000; numero++) {
			cincoMil.add(new DatosReceso(numero % 5 + 1, 10));
		}

		assertThat(EstructuraHoraria.detectarErrores(conRecesos(cincoMil)))
				.containsExactly("No puede haber dos recesos después del mismo bloque.");
	}

	@Test
	void losRecesosDesordenadosSeGuardanEnOrdenDeBloque() {
		EstructuraHoraria estructura = EstructuraHoraria.crear(ANIO, "PRIMARIA", true,
				conRecesos(List.of(new DatosReceso(4, 10), new DatosReceso(2, 20), new DatosReceso(3, 15))));

		assertThat(estructura.getRecesos()).extracting(Receso::getDespuesDeBloque).containsExactly(2, 3, 4);
	}

	@Test
	void crearConDatosQueNoValidanNoDejaLaEstructuraACedias() {
		assertThatThrownBy(() -> EstructuraHoraria.crear(ANIO, "PRIMARIA", true,
				conRecesos(List.of(new DatosReceso(99, 99))))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> EstructuraHoraria.crear(null, "PRIMARIA", true, conCodigo("EH-1")))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> EstructuraHoraria.crear(ANIO, null, true, conCodigo("EH-1")))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
