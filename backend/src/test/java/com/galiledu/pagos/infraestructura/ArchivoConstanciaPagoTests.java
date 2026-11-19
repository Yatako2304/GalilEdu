package com.galiledu.pagos.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import com.galiledu.pagos.aplicacion.DtosPagos.ConstanciaPago;
import com.galiledu.pagos.aplicacion.DtosPagos.CuotaConstancia;

/** HU-42 · RF-107: contenido y paginación del PDF de la constancia. */
class ArchivoConstanciaPagoTests {
	/** 25/09/2026 10:00 en Lima (UTC-5). */
	private static final OffsetDateTime PAGO = OffsetDateTime.of(2026, 9, 25, 15, 0, 0, 0, ZoneOffset.UTC);
	private static final OffsetDateTime GENERADO = OffsetDateTime.of(2026, 11, 19, 17, 30, 0, 0, ZoneOffset.UTC);

	@Test
	void incluyeLosDatosDelPagoLasCuotasElRecargoYElTotal() throws Exception {
		List<CuotaConstancia> cuotas = List.of(
				new CuotaConstancia("Pensión", "Pensión de septiembre", LocalDate.of(2026, 9, 30),
						new BigDecimal("600.00"), new BigDecimal("12.00")),
				new CuotaConstancia("Pensión", "Pensión de octubre", LocalDate.of(2026, 10, 31),
						new BigDecimal("600.00"), BigDecimal.ZERO));
		byte[] archivo = ArchivoConstanciaPago.pdf(constancia("CONFIRMADO", null, cuotas), GENERADO);

		assertThat(new String(archivo, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
		try (PDDocument pdf = Loader.loadPDF(archivo)) {
			assertThat(pdf.getNumberOfPages()).isEqualTo(1);
			String texto = new PDFTextStripper().getText(pdf);
			assertThat(texto).contains("Constancia de pago", "N.° C-2026-00008", "Sofía Paredes Rojas",
					"DNI 74125896", "2026 · 4.° de secundaria A", "25/09/2026 10:00", "TX848617", "Confirmado",
					"Pensión de septiembre", "30/09/2026", "S/ 612.00", "Pensión de octubre", "Monto de cuotas",
					"S/ 1,200.00", "Recargo por mora", "S/ 12.00", "Total pagado", "S/ 1,212.00",
					"Generada el 19/11/2026 12:30.");
			Files.createDirectories(Path.of("target"));
			Files.write(Path.of("target/constancia-pago-demo.pdf"), archivo);
			ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0, 110), "png",
					Path.of("target/constancia-pago-demo.png").toFile());
		}
	}

	@Test
	void unPagoObservadoMuestraElEstadoYSuObservacion() throws Exception {
		byte[] archivo = ArchivoConstanciaPago.pdf(constancia("OBSERVADO", "La pasarela confirmó S/ 10.00 menos.",
				List.of(new CuotaConstancia("Matrícula", "Matrícula 2026", LocalDate.of(2026, 2, 28),
						new BigDecimal("420.00"), BigDecimal.ZERO))),
				GENERADO);
		try (PDDocument pdf = Loader.loadPDF(archivo)) {
			assertThat(new PDFTextStripper().getText(pdf)).contains("Observado",
					"Observación: La pasarela confirmó S/ 10.00 menos.");
		}
	}

	@Test
	void muchasCuotasContinuanEnOtraPaginaYElTotalVaAlFinal() throws Exception {
		List<CuotaConstancia> cuotas = IntStream.rangeClosed(1, 40)
				.mapToObj(n -> new CuotaConstancia("Cuota extraordinaria", "Cuota " + n, LocalDate.of(2026, 3, 1),
						new BigDecimal("10.00"), BigDecimal.ZERO))
				.toList();
		byte[] archivo = ArchivoConstanciaPago.pdf(constancia("CONFIRMADO", null, cuotas), GENERADO);
		try (PDDocument pdf = Loader.loadPDF(archivo)) {
			assertThat(pdf.getNumberOfPages()).isGreaterThan(1);
			PDFTextStripper lector = new PDFTextStripper();
			String todo = lector.getText(pdf);
			assertThat(todo).contains("Cuota 1", "Cuota 40", "Constancia de pago (continuación)");
			lector.setStartPage(1);
			lector.setEndPage(1);
			assertThat(lector.getText(pdf)).doesNotContain("Total pagado");
			lector.setStartPage(pdf.getNumberOfPages());
			lector.setEndPage(pdf.getNumberOfPages());
			assertThat(lector.getText(pdf)).contains("Total pagado");
		}
	}

	private static ConstanciaPago constancia(String estado, String observacion, List<CuotaConstancia> cuotas) {
		BigDecimal capital = cuotas.stream().map(CuotaConstancia::capital).reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal recargo = cuotas.stream().map(CuotaConstancia::recargo).reduce(BigDecimal.ZERO, BigDecimal::add);
		return new ConstanciaPago(UUID.randomUUID(), "C-2026-00008", PAGO, "TX848617", capital.add(recargo), capital,
				recargo, estado, observacion, "Sofía Paredes Rojas", "DNI 74125896", 2026, "4.° de secundaria", "A",
				cuotas);
	}
}
