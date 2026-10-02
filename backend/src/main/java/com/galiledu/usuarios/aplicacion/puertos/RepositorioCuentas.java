package com.galiledu.usuarios.aplicacion.puertos;

import java.util.Optional;

import com.galiledu.usuarios.dominio.CuentaUsuario;

/** Contrato de persistencia para los casos de uso; JPA lo implementará cuando exista el esquema. */
public interface RepositorioCuentas {
	Optional<CuentaUsuario> buscarPorNombreUsuario(String nombreUsuario);

	void guardar(CuentaUsuario cuenta);
}
