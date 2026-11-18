package com.galiledu.horarios.aplicacion;

import java.util.UUID;

/**
 * Cuántos alumnos tiene una sección (HU-48, aforo del espacio). Cuenta las matrículas que ocupan vacante: todas menos
 * las RETIRADO y ANULADA, igual que Configuración. Es de solo lectura. Hoy lo resuelve
 * {@code MatriculadosDeSeccionSql}.
 */
public interface MatriculadosDeSeccion {
	int de(UUID seccionId);
}
