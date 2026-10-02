package com.galiledu.usuarios.infraestructura.demo;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.galiledu.usuarios.aplicacion.puertos.RepositorioAccesos;
import com.galiledu.usuarios.aplicacion.puertos.RepositorioCuentas;
import com.galiledu.usuarios.dominio.CuentaUsuario;
import com.galiledu.usuarios.dominio.EstadoAcceso;
import com.galiledu.usuarios.dominio.EstadoUsuario;

/** Adaptador temporal solo para el perfil demo; no guarda datos entre reinicios. */
final class RepositorioUsuariosDemo implements RepositorioCuentas, RepositorioAccesos {
	private final Map<String, Registro> registros = new HashMap<>();

	RepositorioUsuariosDemo(CuentaUsuario inicial) {
		guardar(inicial);
	}

	synchronized void reiniciar(CuentaUsuario inicial) {
		registros.clear();
		guardar(inicial);
	}

	@Override
	public synchronized Optional<CuentaUsuario> buscarPorNombreUsuario(String nombreUsuario) {
		Registro registro = registros.get(nombreUsuario);
		if (registro == null) {
			return Optional.empty();
		}
		CuentaUsuario cuenta = registro.cuenta;
		return Optional.of(new CuentaUsuario(cuenta.persona(), cuenta.nombreUsuario(),
			cuenta.hashContrasena(), cuenta.roles(),
			registro.acceso.estado() == EstadoUsuario.ACTIVO,
			cuenta.cambioContrasenaObligatorio()));
	}

	@Override
	public synchronized void guardar(CuentaUsuario cuenta) {
		Registro anterior = registros.get(cuenta.nombreUsuario());
		EstadoUsuario estado = !cuenta.activa() ? EstadoUsuario.INACTIVO
			: anterior == null ? EstadoUsuario.ACTIVO : anterior.acceso.estado();
		int fallos = anterior == null ? 0 : anterior.acceso.intentosFallidos();
		var bloqueo = anterior == null ? null : anterior.acceso.bloqueadoHasta();
		EstadoAcceso acceso = new EstadoAcceso(cuenta.nombreUsuario(), cuenta.hashContrasena(),
			estado, fallos, bloqueo, cuenta.cambioContrasenaObligatorio(), cuenta.roles());
		registros.put(cuenta.nombreUsuario(), new Registro(cuenta, acceso));
	}

	@Override
	public synchronized Optional<EstadoAcceso> buscarParaAutenticacion(String nombreUsuario) {
		Registro registro = registros.get(nombreUsuario);
		return Optional.ofNullable(registro == null ? null : registro.acceso);
	}

	@Override
	public synchronized void guardarEstado(EstadoAcceso acceso) {
		Registro anterior = registros.get(acceso.nombreUsuario());
		if (anterior == null) {
			throw new IllegalArgumentException("La cuenta no existe");
		}
		registros.put(acceso.nombreUsuario(), new Registro(anterior.cuenta, acceso));
	}

	private record Registro(CuentaUsuario cuenta, EstadoAcceso acceso) {}
}
