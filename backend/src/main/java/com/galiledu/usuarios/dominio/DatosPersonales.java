package com.galiledu.usuarios.dominio;

import java.util.Objects;
import java.util.regex.Pattern;

/** Datos obligatorios y formatos definidos por RF-12. */
public record DatosPersonales(
	String nombres,
	String primerApellido,
	String segundoApellido,
	TipoDocumento tipoDocumento,
	String numeroDocumento,
	String correoElectronico,
	String telefono
) {
	private static final Pattern DNI = Pattern.compile("[0-9]{8}");
	private static final Pattern CARNET_EXTRANJERIA = Pattern.compile("[A-Za-z0-9]{1,12}");
	private static final Pattern CORREO = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
	private static final Pattern TELEFONO = Pattern.compile("[0-9]{9}");

	public DatosPersonales {
		nombres = textoObligatorio(nombres, "nombres");
		primerApellido = textoObligatorio(primerApellido, "primerApellido");
		segundoApellido = segundoApellido == null ? null : textoObligatorio(segundoApellido, "segundoApellido");
		tipoDocumento = Objects.requireNonNull(tipoDocumento, "tipoDocumento es obligatorio");
		numeroDocumento = textoObligatorio(numeroDocumento, "numeroDocumento");
		correoElectronico = textoObligatorio(correoElectronico, "correoElectronico");
		telefono = textoObligatorio(telefono, "telefono");

		if (nombres.codePoints().anyMatch(Character::isDigit)
			|| primerApellido.codePoints().anyMatch(Character::isDigit)
			|| (segundoApellido != null && segundoApellido.codePoints().anyMatch(Character::isDigit))) {
			throw new IllegalArgumentException("Los nombres y apellidos no pueden contener números");
		}
		Pattern formatoDocumento = tipoDocumento == TipoDocumento.DNI ? DNI : CARNET_EXTRANJERIA;
		if (!formatoDocumento.matcher(numeroDocumento).matches()) {
			throw new IllegalArgumentException("El número de documento no cumple el formato de " + tipoDocumento);
		}
		if (!CORREO.matcher(correoElectronico).matches()) {
			throw new IllegalArgumentException("El correo electrónico no tiene un formato válido");
		}
		if (!TELEFONO.matcher(telefono).matches()) {
			throw new IllegalArgumentException("El teléfono debe contener 9 dígitos");
		}
	}

	private static String textoObligatorio(String valor, String campo) {
		if (valor == null || valor.isBlank()) {
			throw new IllegalArgumentException(campo + " es obligatorio");
		}
		return valor.strip();
	}
}
