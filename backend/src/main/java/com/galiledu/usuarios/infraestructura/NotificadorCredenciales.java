package com.galiledu.usuarios.infraestructura;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * RF-17: envía usuario y contraseña al correo de la cuenta nueva.
 * Si no hay SMTP configurado (MAIL_HOST vacío), las credenciales se escriben en el log del backend
 * para poder probar en local.
 */
@Component
public class NotificadorCredenciales {
	private static final Logger log = LoggerFactory.getLogger(NotificadorCredenciales.class);

	private final ObjectProvider<JavaMailSender> correo;
	private final String remitente;
	private final String servidor;

	public NotificadorCredenciales(ObjectProvider<JavaMailSender> correo,
			@Value("${galiledu.correo.remitente}") String remitente,
			@Value("${spring.mail.host:}") String servidor) {
		this.correo = correo;
		this.remitente = remitente;
		this.servidor = servidor;
	}

	public void enviar(String destinatario, String nombre, String usuario, String contrasena) {
		JavaMailSender emisor = correo.getIfAvailable();
		if (servidor.isBlank() || emisor == null) {
			log.warn("SMTP no configurado. Credenciales de {} <{}>: usuario={} contraseña={}",
					nombre, destinatario, usuario, contrasena);
			return;
		}
		SimpleMailMessage mensaje = new SimpleMailMessage();
		mensaje.setFrom(remitente);
		mensaje.setTo(destinatario);
		mensaje.setSubject("Tus credenciales de acceso a GalilEdu");
		mensaje.setText("""
				Hola %s:

				Se creó tu cuenta en GalilEdu.
				Usuario: %s
				Contraseña: %s

				Puedes cambiar tu contraseña cuando quieras desde tu perfil.
				""".formatted(nombre, usuario, contrasena));
		try {
			emisor.send(mensaje);
		} catch (RuntimeException e) {
			log.error("No se pudo enviar el correo de credenciales a {}", destinatario, e);
		}
	}
}
