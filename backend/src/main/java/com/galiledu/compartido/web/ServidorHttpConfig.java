package com.galiledu.compartido.web;

import org.apache.coyote.http11.Http11Nio2Protocol;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Usa NIO2 en Tomcat: en algunos Windows el conector NIO por defecto falla al arrancar con
 * "Unable to establish loopback connection".
 */
@Configuration
public class ServidorHttpConfig {
	@Bean
	WebServerFactoryCustomizer<TomcatServletWebServerFactory> protocoloHttp() {
		return servidor -> servidor.setProtocol(Http11Nio2Protocol.class.getName());
	}
}
