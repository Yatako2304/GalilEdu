# Pruebas

Las pruebas corren en tu PC y en **GitHub Actions** ([`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)):

| Cuándo | ¿Corren? | ¿Bloquea si fallan? |
| --- | --- | --- |
| Push a **cualquier** rama | Sí | No: solo muestra ✅/❌ en GitHub para que te enteres temprano. |
| Pull request hacia `main` | Sí | **Sí:** no se puede integrar hasta que `Backend` y `Frontend` estén en verde. |
| `git commit` en tu PC | Solo lint, tipos y formato (hook); además regenera el [catálogo de pruebas](../pruebas.md) | Sí, si hay errores que no puede corregir solo. |

Así nadie rompe en `main` lo que otra pareja ya hizo.

## Qué hay

| Tipo | Dónde | Qué prueba | Comando |
| --- | --- | --- | --- |
| Backend · unitarias | `backend/src/test/java/**/*Tests.java` | Reglas de dominio sin BD | `.\mvnw.cmd test` |
| Backend · integración | `backend/src/test/java/**/*IT.java` | La API completa y las tablas de la BD contra PostgreSQL real | `.\mvnw.cmd verify` |
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

- Ponle **siempre** un `@DisplayName` en español, una frase que diga qué comprueba (`@DisplayName("Un estudiante activo no se puede inscribir de nuevo")`). Así el reporte de JUnit y el catálogo se leen solos. Va encima de `@Test`.
- `token("U20260001")` inicia sesión con un usuario de los datos de prueba.
- Cada prueba corre en una transacción que se **deshace** al final: no dejes datos ni dependas del orden.
- Prueba al menos: el caso feliz, cada validación de la HU y que otro rol reciba `403`.

### Pruebas de la base de datos

Las tablas de cada módulo tienen sus propias pruebas de integración, que le hablan a PostgreSQL con SQL a través de `JdbcClient` (`EsquemaConfiguracionIT`, `EsquemaMatriculaPagosIT`, `EsquemaAcademicoIT`, `IntegracionModulosIT`). Son de dos tipos:

- **Recorrido:** une tablas de varios módulos y comprueba que se llegue al dato correcto. Ejemplo: de la cuenta del apoderado a sus hijos, su sección y su deuda.
- **Rechazo:** usa un dato válido en todo, salvo en **una** regla, y exige que la BD lo rechace **por esa regla**, con `rechazaPor`:

```java
@Test
void soloUnAnioPuedeEstarActivo() {
	rechazaPor("ux_anio_escolar_un_activo", () -> jdbc.sql("""
			INSERT INTO configuracion.anio_escolar (anio, fecha_inicio, fecha_fin, regimen, estado)
			VALUES (2030, '2030-03-01', '2030-12-15', 'BIMESTRAL', 'ACTIVO')""").update());
}
```

El primer argumento es el nombre de la restricción en la migración. Si la BD rechaza el dato por **otro** motivo (por ejemplo, una columna obligatoria vacía), la prueba falla. Así no pasa por accidente.

Si agregas una migración con una regla nueva (`CHECK`, `UNIQUE`, FK), agrega su prueba de rechazo y, si hace falta, una contraprueba con un dato válido que sí se acepte.

## Frontend

- Lógica pura (esquemas zod, conversiones): pruébala directo, sin renderizar. Ver `features/estudiantes/components/inscripcion/__tests__/esquema.test.ts`.
- Componentes: `renderApp(<MiComponente />, { user: await createUser({ roles: [...] }) })` y simula el backend con `server.use(http.get(...))`. Ver `features/usuarios/components/__tests__/usuarios-lista.test.tsx`.
- Busca por texto o rol accesible (`getByRole('button', { name: /guardar/i })`), como lo haría un usuario.

## Catálogo de pruebas

[`docs/pruebas.md`](../pruebas.md) lista todas las pruebas por funcionalidad (para mostrarlas, por ejemplo al profesor). **No se edita a mano:** lo genera `node docs/generar-pruebas.mjs` y el hook de `git commit` lo regenera y lo agrega al commit. Para que tu funcionalidad se vea bien ahí:

- Backend: `@DisplayName` en español en cada prueba (ver arriba). Si falta, se usa el nombre del método con las tildes que pueda restaurar.
- Frontend: el texto de `test('…')` y `describe('…')` es lo que se muestra; escríbelo como frase. El título del archivo sale de su nombre; si no dice nada (por ejemplo `esquema.test.ts`), ponle en la primera línea `// @catalogo: Reglas de validación de la inscripción` y ese será el título.
- Un módulo o carpeta nueva se agrega a la lista `AREAS` de `docs/generar-pruebas.mjs`; si no, el script falla y te avisa qué archivo no tiene área.
- Si el commit se hizo con `--no-verify`, corre el script a mano antes de subir.

**Resultados:** el CI tiene un job `Resultados` (informativo, no bloquea) que arma `pruebas-resultados.md` con cada prueba marcada ✅ o ❌. Lo ves en el resumen de la ejecución en la pestaña Actions y lo descargas como artifact `pruebas-resultados`. En tu PC: corre las pruebas (Vitest con `--reporter=json --outputFile.json=test-results/vitest.json`) y luego `node docs/generar-pruebas.mjs --resultados`.

## Si el CI sale en rojo

1. En el PR, entra a **Details** del job que falló (`Backend` o `Frontend`).
2. Abre el paso en rojo y busca el primer `FAIL` / `ERROR`.
3. Corre ese mismo comando en tu PC, corrige y vuelve a subir.
