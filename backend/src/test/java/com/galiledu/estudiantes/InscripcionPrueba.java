package com.galiledu.estudiantes;

/** JSON de los pasos de la inscripción con todos los datos obligatorios, para armar solicitudes válidas. */
final class InscripcionPrueba {
	private InscripcionPrueba() {
	}

	/** Paso 1: "estudiante" de la solicitud. */
	static String estudiante(String documento, String nombres, String grado) {
		return """
				{"persona":{"tipoDocumento":"DNI","numeroDocumento":"%s","apellidoPaterno":"Paredes",
				  "apellidoMaterno":"Rojas","nombres":"%s","fechaNacimiento":"2015-05-10","sexo":"FEMENINO",
				  "nacionalidad":"Peruana","direccion":"Calle 123","distrito":"Magdalena del Mar"},
				 "paisNacimiento":"Perú","departamentoNacimiento":"Lima","provinciaNacimiento":"Lima",
				 "distritoNacimiento":"Magdalena del Mar","tieneInternet":true,"lenguaMaterna":"Castellano",
				 "tieneDiscapacidad":false,"colegioProcedencia":"IE San José","gestionProcedencia":"Pública",
				 "anioAcademico":2027,"gradoCodigo":"%s","formaIngreso":"Edad normativa"}"""
				.formatted(documento, nombres, grado);
	}

	/** Paso 2: "fichaMedica" de la solicitud (con alergia alimentaria). */
	static final String FICHA_MEDICA = """
			{"tipoSangre":"O+","pesoKg":22,"estaturaM":1.18,"tipoSeguro":"EsSalud",
			 "tieneAlergias":true,"alergiasAlimentarias":"Maní","fiebreMaxima":"39",
			 "haConvulsionado":false,"tieneEnfermedad":false,"tomaMedicamento":false}""";
}
