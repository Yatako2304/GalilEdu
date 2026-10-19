# Pruebas

Las pruebas corren en tu PC y en **GitHub Actions** ([`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)):

| Cuándo | ¿Corren? | ¿Bloquea si fallan? |
| --- | --- | --- |
| Push a **cualquier** rama | Sí | No: solo muestra ✅/❌ en GitHub para que te enteres temprano. |
| Pull request hacia `main` | Sí | **Sí:** no se puede integrar hasta que `Backend` y `Frontend` estén en verde. |
| `git commit` en tu PC | Solo lint, tipos y formato (hook) | Sí, si hay errores que no puede corregir solo. |

Así nadie rompe en `main` lo que otra pareja ya hizo.

## Qué hay

| Tipo | Dónde | Qué prueba | Comando |
| --- | --- | --- | --- |
| Backend · unitarias | `backend/src/test/java/**/*Tests.java` | Reglas de dominio sin BD | `.\mvnw.cmd test` |
| Backend · integración | `backend/src/test/java/**/*IT.java` | La API completa contra PostgreSQL real | `.\mvnw.cmd verify` |
| Frontend | `frontend/src/**/__tests__/*.test.ts(x)` | Esquemas, componentes y pantallas (Vitest + Testing Library + MSW) | `npm test` |
| Frontend · calidad | todo `frontend/src` | Lint (incluye reglas de arquitectura), tipos y build | `npm run lint`, `npm run check-types`, `npm run build` |

## Backend

**Primera vez:** crea una base vacía `galiledu_test` en tu PostgreSQL (distinta de `galiledu`). Si no usas `localhost:5433`, define `TEST_DB_URL`.

Una prueba de integración nueva:

```java
class MatriculaIT extends PruebaIntegracion {
	@Test
	void registraPeriodo() throws Exception {
		mvc.perform(conToken(post("/matriculas/periodos"), token(PERSONAL_ADMINISTRATIVO), """
				{"anioEscolar":2027,"inicio":"2027-01-10","fin":"2027-02-28"}"""))
			.andExpect(status().isCreated());
	}
}
```

- `token("U20260001")` inicia sesión con un usuario de los datos de prueba.
- Cada prueba corre en una transacción que se **deshace** al final: no dejes datos ni dependas del orden.
- Prueba al menos: el caso feliz, cada validación de la HU y que otro rol reciba `403`.

## Frontend

- Lógica pura (esquemas zod, conversiones): pruébala directo, sin renderizar. Ver `features/estudiantes/components/inscripcion/__tests__/esquema.test.ts`.
- Componentes: `renderApp(<MiComponente />, { user: await createUser({ roles: [...] }) })` y simula el backend con `server.use(http.get(...))`. Ver `features/usuarios/components/__tests__/usuarios-lista.test.tsx`.
- Busca por texto o rol accesible (`getByRole('button', { name: /guardar/i })`), como lo haría un usuario.

## Si el CI sale en rojo

1. En el PR, entra a **Details** del job que falló (`Backend` o `Frontend`).
2. Abre el paso en rojo y busca el primer `FAIL` / `ERROR`.
3. Corre ese mismo comando en tu PC, corrige y vuelve a subir.
