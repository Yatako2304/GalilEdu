package com.galiledu.pagos.infraestructura.persistencia;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.galiledu.pagos.aplicacion.puertos.RepositorioTarifarios;
import com.galiledu.pagos.dominio.ConfiguracionTarifario;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.stereotype.Repository;

@Repository
public class RepositorioTarifariosPostgres implements RepositorioTarifarios {
	private final JdbcOperations jdbc;

	public RepositorioTarifariosPostgres(JdbcOperations jdbc) {
		this.jdbc = jdbc;
	}

	@Override
	public UUID crear(ConfiguracionTarifario datos) {
		UUID tarifarioId = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO pagos.tarifario_escolar (id, anio_escolar_id, estado, cantidad_cuotas)
			VALUES (?, ?, 'VIGENTE'::pagos.estado_tarifario, ?)
			""", tarifarioId, datos.anioEscolarId(), datos.cantidadCuotas());
		jdbc.update("""
			INSERT INTO pagos.configuracion_mora
			(id, tarifario_escolar_id, dias_gracia, tasa_mensual, tope_porcentaje)
			VALUES (?, ?, ?, ?, ?)
			""", UUID.randomUUID(), tarifarioId, datos.diasGracia(), datos.tasaMensual(), datos.topePorcentaje());

		UUID matriculaCategoria = categoria("MATRICULA", "Matrícula");
		UUID pensionCategoria = categoria("PENSION", "Pensión");
		UUID matriculaDetalleId = registrarDetalle(tarifarioId, matriculaCategoria, "Matrícula",
			datos.montoMatricula(), false);
		UUID pensionDetalleId = registrarDetalle(tarifarioId, pensionCategoria, "Pensión mensual",
			datos.montoPension(), true);

		UUID calendarioId = UUID.randomUUID();
		jdbc.update("INSERT INTO pagos.calendario_pagos (id, anio_escolar_id) VALUES (?, ?)",
			calendarioId, datos.anioEscolarId());
		List<LocalDate> fechas = datos.vencimientosPensiones().stream().sorted().toList();
		for (int i = 0; i < fechas.size(); i++) {
			jdbc.update("""
				INSERT INTO pagos.detalle_calendario
				(id, calendario_pagos_id, detalle_tarifa_id, numero, fecha_vencimiento)
				VALUES (?, ?, ?, ?, ?)
				""", UUID.randomUUID(), calendarioId, pensionDetalleId, i + 1, fechas.get(i));
		}
		// The schema requires a unique number across concepts. Pension installments remain 1..N.
		jdbc.update("""
			INSERT INTO pagos.detalle_calendario
			(id, calendario_pagos_id, detalle_tarifa_id, numero, fecha_vencimiento)
			VALUES (?, ?, ?, ?, ?)
			""", UUID.randomUUID(), calendarioId, matriculaDetalleId,
			datos.cantidadCuotas() + 1, datos.vencimientoMatricula());
		return tarifarioId;
	}

	@Override
	public Optional<Tarifario> buscarPorAnio(UUID anioEscolarId) {
		List<Cabecera> cabeceras = jdbc.query("""
			SELECT t.id, t.anio_escolar_id, t.estado::text AS estado, t.cantidad_cuotas,
			       m.dias_gracia, m.tasa_mensual, m.tope_porcentaje
			FROM pagos.tarifario_escolar t
			JOIN pagos.configuracion_mora m ON m.tarifario_escolar_id = t.id
			WHERE t.anio_escolar_id = ?
			""", (rs, row) -> new Cabecera(rs.getObject("id", UUID.class),
			rs.getObject("anio_escolar_id", UUID.class), rs.getString("estado"),
			rs.getInt("cantidad_cuotas"), rs.getInt("dias_gracia"),
			rs.getBigDecimal("tasa_mensual"), rs.getBigDecimal("tope_porcentaje")), anioEscolarId);
		if (cabeceras.isEmpty()) return Optional.empty();
		Cabecera cabecera = cabeceras.getFirst();
		BigDecimal matricula = monto(cabecera.id(), "MATRICULA");
		BigDecimal pension = monto(cabecera.id(), "PENSION");
		LocalDate vencimientoMatricula = jdbc.queryForObject("""
			SELECT d.fecha_vencimiento FROM pagos.detalle_calendario d
			JOIN pagos.calendario_pagos c ON c.id = d.calendario_pagos_id
			JOIN pagos.detalle_tarifa t ON t.id = d.detalle_tarifa_id
			JOIN pagos.categoria_tarifa ct ON ct.id = t.categoria_tarifa_id
			WHERE c.anio_escolar_id = ? AND t.tarifario_escolar_id = ? AND ct.codigo = 'MATRICULA'
			""", (rs, row) -> rs.getDate("fecha_vencimiento").toLocalDate(),
			anioEscolarId, cabecera.id());
		List<LocalDate> vencimientos = jdbc.query("""
			SELECT d.fecha_vencimiento FROM pagos.detalle_calendario d
			JOIN pagos.calendario_pagos c ON c.id = d.calendario_pagos_id
			JOIN pagos.detalle_tarifa t ON t.id = d.detalle_tarifa_id
			JOIN pagos.categoria_tarifa ct ON ct.id = t.categoria_tarifa_id
			WHERE c.anio_escolar_id = ? AND ct.codigo = 'PENSION'
			ORDER BY d.numero
			""", (rs, row) -> rs.getDate("fecha_vencimiento").toLocalDate(), anioEscolarId);
		return Optional.of(new Tarifario(cabecera.id(), cabecera.anioEscolarId(), cabecera.estado(),
			matricula, pension, cabecera.cantidadCuotas(), vencimientoMatricula, vencimientos,
			cabecera.diasGracia(), cabecera.tasaMensual(), cabecera.topePorcentaje()));
	}

	private UUID categoria(String codigo, String nombre) {
		jdbc.update("""
			INSERT INTO pagos.categoria_tarifa (id, codigo, nombre)
			VALUES (?, ?, ?) ON CONFLICT (codigo) DO NOTHING
			""", UUID.randomUUID(), codigo, nombre);
		List<Categoria> categorias = jdbc.query("""
			SELECT id, activo FROM pagos.categoria_tarifa WHERE codigo = ?
			""", (rs, row) -> new Categoria(rs.getObject("id", UUID.class), rs.getBoolean("activo")), codigo);
		if (categorias.size() != 1 || !categorias.getFirst().activo()) {
			throw new IllegalStateException("El concepto de pago requerido no está activo");
		}
		return categorias.getFirst().id();
	}

	private UUID registrarDetalle(UUID tarifarioId, UUID categoriaId, String nombre,
		BigDecimal monto, boolean aplicaMora) {
		UUID id = UUID.randomUUID();
		jdbc.update("""
			INSERT INTO pagos.detalle_tarifa
			(id, tarifario_escolar_id, categoria_tarifa_id, nombre, monto, aplica_mora)
			VALUES (?, ?, ?, ?, ?, ?)
			""", id, tarifarioId, categoriaId, nombre, monto, aplicaMora);
		return id;
	}

	private BigDecimal monto(UUID tarifarioId, String codigo) {
		return jdbc.queryForObject("""
			SELECT d.monto FROM pagos.detalle_tarifa d
			JOIN pagos.categoria_tarifa c ON c.id = d.categoria_tarifa_id
			WHERE d.tarifario_escolar_id = ? AND c.codigo = ?
			""", BigDecimal.class, tarifarioId, codigo);
	}

	private record Cabecera(UUID id, UUID anioEscolarId, String estado, int cantidadCuotas,
		int diasGracia, BigDecimal tasaMensual, BigDecimal topePorcentaje) {}
	private record Categoria(UUID id, boolean activo) {}
}
