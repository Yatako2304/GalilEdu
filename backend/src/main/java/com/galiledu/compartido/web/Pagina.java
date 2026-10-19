package com.galiledu.compartido.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Respuesta paginada estándar: { data: [...], meta: { page, size, total, totalPages } }. page empieza en 1. */
public record Pagina<T>(List<T> data, Meta meta) {
	public record Meta(int page, int size, long total, int totalPages) {
	}

	public static <E, T> Pagina<T> de(Page<E> pagina, Function<E, T> mapeo) {
		return new Pagina<>(
				pagina.getContent().stream().map(mapeo).toList(),
				new Meta(pagina.getNumber() + 1, pagina.getSize(), pagina.getTotalElements(),
						Math.max(1, pagina.getTotalPages())));
	}
}
