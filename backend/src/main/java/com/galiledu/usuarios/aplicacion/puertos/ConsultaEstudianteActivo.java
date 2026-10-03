package com.galiledu.usuarios.aplicacion.puertos;

import java.util.UUID;

/** Student identity offered by Users/Persons to other modules. */
public interface ConsultaEstudianteActivo {
	boolean existe(UUID estudianteId);
}
