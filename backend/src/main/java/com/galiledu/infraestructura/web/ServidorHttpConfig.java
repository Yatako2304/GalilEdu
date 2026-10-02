package com.galiledu.infraestructura.web;

import org.apache.coyote.http11.Http11Nio2Protocol;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Protocolo HTTP compatible con el entorno local de ejecución. */
@Configuration
public class ServidorHttpConfig {
	@Bean
	WebServerFactoryCustomizer<TomcatServletWebServerFactory> protocoloHttp() {
		return servidor -> servidor.setProtocol(Http11Nio2Protocol.class.getName());
	}
}
