package com.galiledu.configuracion.dominio;

/** Cómo se divide el año escolar (HU-74): 4 bimestres o 3 trimestres. */
public enum Regimen {
	BIMESTRAL(4, "Bimestre"), TRIMESTRAL(3, "Trimestre");

	private static final String[] ROMANOS = {"I", "II", "III", "IV"};

	private final int cantidadPeriodos;
	private final String unidad;

	Regimen(int cantidadPeriodos, String unidad) {
		this.cantidadPeriodos = cantidadPeriodos;
		this.unidad = unidad;
	}

	public int cantidadPeriodos() {
		return cantidadPeriodos;
	}

	/** "I Bimestre", "II Trimestre"… Es el nombre que usan los datos de prueba y Figma. */
	public String nombrePeriodo(int numero) {
		return ROMANOS[numero - 1] + " " + unidad;
	}
}
