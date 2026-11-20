package com.galiledu.usuarios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

/** HU-4 y RF-17: los correos del código de recuperación y de credenciales (HTML con logo y versión de texto). */
class NotificadorCredencialesTests {
	private final JavaMailSender emisor = mock(JavaMailSender.class);

	@SuppressWarnings("unchecked")
	private NotificadorCredenciales notificador(String servidor) {
		ObjectProvider<JavaMailSender> proveedor = mock(ObjectProvider.class);
		when(proveedor.getIfAvailable()).thenReturn(emisor);
		when(emisor.createMimeMessage()).thenAnswer(i -> new MimeMessage((Session) null));
		return new NotificadorCredenciales(proveedor, "GalilEdu <galiledu.notificaciones@gmail.com>", servidor,
				"https://galiledu.example/");
	}

	@Test
	void laPlantillaLlevaElCodigoElNombreEscapadoYNingunMarcadorSinReemplazar() {
		String html = NotificadorCredenciales.htmlCodigo("<b>Ana</b>", "482913", 10);

		assertThat(html).contains("482913", "10 minutos", "&lt;b&gt;Ana&lt;/b&gt;", "cid:logo-galiledu")
				.doesNotContain("{{", "<b>Ana</b>");
	}

	@Test
	void enviaUnCorreoHtmlConElLogoIncrustadoYLaVersionDeTexto() throws Exception {
		assertThat(notificador("smtp.gmail.com").enviarCodigoRecuperacion("selene@correo.com", "Selene", "482913", 10))
				.isTrue();

		ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
		verify(emisor).send(enviado.capture());
		MimeMessage mensaje = enviado.getValue();
		assertThat(mensaje.getSubject()).isEqualTo("Código para restablecer tu contraseña de GalilEdu");
		assertThat(mensaje.getAllRecipients()[0].toString()).isEqualTo("selene@correo.com");

		ByteArrayOutputStream crudo = new ByteArrayOutputStream();
		mensaje.writeTo(crudo);
		String contenido = crudo.toString(StandardCharsets.UTF_8);
		assertThat(contenido).contains("text/html", "text/plain", "Content-ID: <logo-galiledu>", "image/png");
	}

	@Test
	void sinSmtpNoEnviaNadaYElCodigoVaAlLog() {
		assertThat(notificador("").enviarCodigoRecuperacion("selene@correo.com", "Selene", "482913", 10)).isTrue();
		verify(emisor, never()).send(any(MimeMessage.class));
	}

	@Test
	void siElServidorRechazaElCorreoAvisaConFalse() {
		NotificadorCredenciales conSmtp = notificador("smtp.gmail.com");
		doThrow(new MailSendException("rechazado")).when(emisor).send(any(MimeMessage.class));

		assertThat(conSmtp.enviarCodigoRecuperacion("selene@correo.com", "Selene", "482913", 10)).isFalse();
	}

	@Test
	void laPlantillaDeCredencialesLlevaUsuarioContrasenaYEnlaceSinMarcadores() {
		String html = NotificadorCredenciales.htmlCredenciales("<b>Ana</b>", "U20260010", "Xy7&kq2#",
				"https://x/auth/login");

		assertThat(html).contains("U20260010", "Xy7&amp;kq2#", "&lt;b&gt;Ana&lt;/b&gt;", "cid:logo-galiledu",
				"href=\"https://x/auth/login\"", "Cambiar contraseña").doesNotContain("{{", "<b>Ana</b>");
	}

	@Test
	void enviaLasCredencialesEnUnCorreoHtmlConElLogoYElEnlaceAlLogin() throws Exception {
		notificador("smtp.gmail.com").enviar("ana@correo.com", "Ana", "U20260010", "Xy7kq2#a");

		ArgumentCaptor<MimeMessage> enviado = ArgumentCaptor.forClass(MimeMessage.class);
		verify(emisor).send(enviado.capture());
		MimeMessage mensaje = enviado.getValue();
		assertThat(mensaje.getSubject()).isEqualTo("Tus credenciales de acceso a GalilEdu");
		mensaje.saveChanges();
		ByteArrayOutputStream crudo = new ByteArrayOutputStream();
		mensaje.writeTo(crudo);
		assertThat(crudo.toString(StandardCharsets.UTF_8)).contains("text/html", "text/plain",
				"Content-ID: <logo-galiledu>");
		// El enlace del botón: la dirección del frontend sin la barra final, más /auth/login.
		assertThat(textos(mensaje)).contains("href=\"https://galiledu.example/auth/login\"");
	}

	/** El texto de todas las partes del correo, ya decodificado. */
	private static String textos(jakarta.mail.Part parte) throws Exception {
		Object contenido = parte.getContent();
		if (contenido instanceof String texto) {
			return texto;
		}
		if (contenido instanceof jakarta.mail.Multipart multiparte) {
			StringBuilder todo = new StringBuilder();
			for (int i = 0; i < multiparte.getCount(); i++) {
				todo.append(textos(multiparte.getBodyPart(i)));
			}
			return todo.toString();
		}
		return "";
	}

	@Test
	void sinSmtpLasCredencialesSoloVanAlLog() {
		notificador("").enviar("ana@correo.com", "Ana", "U20260010", "Xy7kq2#a");
		verify(emisor, never()).send(any(MimeMessage.class));
	}
}
