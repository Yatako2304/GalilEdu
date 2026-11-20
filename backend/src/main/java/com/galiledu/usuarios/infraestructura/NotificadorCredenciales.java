package com.galiledu.usuarios.infraestructura;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Year;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * RF-17: envía usuario y contraseña al correo de la cuenta nueva. HU-4: envía el código para restablecer la
 * contraseña. Los dos son correos HTML con el mismo diseño (logo, franja de colores y el dato grande y centrado) y una
 * versión de solo texto. Si no hay SMTP configurado (MAIL_HOST vacío), las credenciales y el código se escriben en el
 * log del backend para poder probar en local.
 */
@Component
public class NotificadorCredenciales {
	private static final Logger log = LoggerFactory.getLogger(NotificadorCredenciales.class);

	private final ObjectProvider<JavaMailSender> correo;
	private final String remitente;
	private final String servidor;
	/** Dirección del frontend para el botón «Ingresar a GalilEdu» (la misma a la que vuelve el pago en línea). */
	private final String urlFrontend;

	public NotificadorCredenciales(ObjectProvider<JavaMailSender> correo,
			@Value("${galiledu.correo.remitente}") String remitente,
			@Value("${spring.mail.host:}") String servidor,
			@Value("${galiledu.pagos.url-frontend:http://localhost:3000}") String urlFrontend) {
		this.correo = correo;
		this.remitente = remitente;
		this.servidor = servidor;
		this.urlFrontend = urlFrontend;
	}

	public void enviar(String destinatario, String nombre, String usuario, String contrasena) {
		JavaMailSender emisor = correo.getIfAvailable();
		if (servidor.isBlank() || emisor == null) {
			log.warn("SMTP no configurado. Credenciales de {} <{}>: usuario={} contraseña={}",
					nombre, destinatario, usuario, contrasena);
			return;
		}
		try {
			MimeMessage mensaje = emisor.createMimeMessage();
			MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
			ayudante.setFrom(remitente);
			ayudante.setTo(destinatario);
			ayudante.setSubject("Tus credenciales de acceso a GalilEdu");
			ayudante.setText(textoCredenciales(nombre, usuario, contrasena, urlLogin()),
					htmlCredenciales(nombre, usuario, contrasena, urlLogin()));
			ayudante.addInline(CID_LOGO, LOGO, "image/png");
			emisor.send(mensaje);
		} catch (MessagingException | RuntimeException e) {
			log.error("No se pudo enviar el correo de credenciales a {}", destinatario, e);
		}
	}

	private String urlLogin() {
		return urlFrontend.replaceAll("/+$", "") + "/auth/login";
	}

	static String textoCredenciales(String nombre, String usuario, String contrasena, String url) {
		return """
				Hola %s:

				Se creó tu cuenta en GalilEdu.
				Usuario: %s
				Contraseña: %s

				Ingresa en %s
				Por seguridad, cambia esta contraseña al ingresar: pulsa tu nombre en el menú lateral y elige «Cambiar contraseña».
				"""
				.formatted(nombre, usuario, contrasena, url);
	}

	/** La plantilla con los datos; todo se escapa porque el nombre lo escribió una persona. */
	static String htmlCredenciales(String nombre, String usuario, String contrasena, String url) {
		return plantilla(PLANTILLA_CREDENCIALES)
				.replace("{{nombre}}", HtmlUtils.htmlEscape(nombre == null ? "" : nombre))
				.replace("{{usuario}}", HtmlUtils.htmlEscape(usuario))
				.replace("{{contrasena}}", HtmlUtils.htmlEscape(contrasena))
				.replace("{{url}}", HtmlUtils.htmlEscape(url))
				.replace("{{anio}}", Year.now().toString());
	}

	/** Diseño del correo del código (HU-4) y el logo que va incrustado en él (Content-ID {@value #CID_LOGO}). */
	private static final String PLANTILLA_CODIGO = "correo/codigo-recuperacion.html";
	/** Diseño del correo de credenciales (RF-17), con el mismo estilo. */
	private static final String PLANTILLA_CREDENCIALES = "correo/credenciales.html";
	private static final ClassPathResource LOGO = new ClassPathResource("correo/logo-galiledu.png");
	static final String CID_LOGO = "logo-galiledu";

	/**
	 * HU-4: código de 6 dígitos para restablecer la contraseña, en un correo HTML con el logo y el código grande
	 * (con una versión de solo texto para los lectores de correo que no muestran HTML). Devuelve false si el servidor
	 * de correo lo rechazó (así la pantalla puede avisar en vez de esperar un correo que no llegará).
	 */
	public boolean enviarCodigoRecuperacion(String destinatario, String nombre, String codigo, long minutos) {
		JavaMailSender emisor = correo.getIfAvailable();
		if (servidor.isBlank() || emisor == null) {
			log.warn("SMTP no configurado. Código de recuperación de {} <{}>: {}", nombre, destinatario, codigo);
			return true;
		}
		try {
			MimeMessage mensaje = emisor.createMimeMessage();
			MimeMessageHelper ayudante = new MimeMessageHelper(mensaje, true, StandardCharsets.UTF_8.name());
			ayudante.setFrom(remitente);
			ayudante.setTo(destinatario);
			ayudante.setSubject("Código para restablecer tu contraseña de GalilEdu");
			ayudante.setText(textoCodigo(nombre, codigo, minutos), htmlCodigo(nombre, codigo, minutos));
			ayudante.addInline(CID_LOGO, LOGO, "image/png");
			emisor.send(mensaje);
			return true;
		} catch (MessagingException | RuntimeException e) {
			log.error("No se pudo enviar el código de recuperación a {}", destinatario, e);
			return false;
		}
	}

	static String textoCodigo(String nombre, String codigo, long minutos) {
		return """
				Hola %s:

				Tu código de verificación es: %s

				Vence en %d minutos. Escríbelo en la pantalla «Código de verificación» para crear una nueva contraseña.

				Si no pediste restablecer tu contraseña, ignora este correo: tu contraseña actual sigue igual.
				""".formatted(nombre, codigo, minutos);
	}

	/** La plantilla con los datos; el nombre se escapa porque lo escribió una persona. */
	static String htmlCodigo(String nombre, String codigo, long minutos) {
		return plantilla(PLANTILLA_CODIGO)
				.replace("{{nombre}}", HtmlUtils.htmlEscape(nombre == null ? "" : nombre))
				.replace("{{codigo}}", codigo)
				.replace("{{minutos}}", Long.toString(minutos))
				.replace("{{anio}}", Year.now().toString());
	}

	private static String plantilla(String ruta) {
		try (var entrada = new ClassPathResource(ruta).getInputStream()) {
			return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("No se encontró la plantilla de correo " + ruta, e);
		}
	}
}
