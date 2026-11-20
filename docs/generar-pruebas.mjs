// Genera docs/pruebas.md a partir de los tests del backend (Java) y del frontend (Vitest).
// Uso, desde la raíz del repo:  node docs/generar-pruebas.mjs
import { readdirSync, readFileSync, statSync, writeFileSync } from 'node:fs';
import { join, relative, sep } from 'node:path';

const RAIZ = join(import.meta.dirname, '..');
const BACK = join(RAIZ, 'backend', 'src', 'test', 'java', 'com', 'galiledu');
const FRONT = join(RAIZ, 'frontend', 'src');

// Funcionalidad que ve la gente: cada una agrupa módulos del backend y carpetas del frontend.
const AREAS = [
  { titulo: 'Estudiantes (inscripción, edición, estado)', back: ['estudiantes'], front: ['features/estudiantes'] },
  { titulo: 'Usuarios y acceso (login, Google, contraseñas, carga masiva)', back: ['usuarios'], front: ['features/usuarios', 'features/auth'] },
  { titulo: 'Matrícula', back: ['matricula'], front: ['features/matricula'] },
  { titulo: 'Pagos y descuentos', back: ['pagos'], front: ['features/pagos'] },
  { titulo: 'Horarios y reservas de ambientes', back: ['horarios'], front: ['features/horarios'] },
  { titulo: 'Configuración (año escolar, periodos, secciones, tarifario)', back: ['configuracion'], front: ['features/configuracion'] },
  { titulo: 'Académico (calificaciones, catálogo, ambientes)', back: ['academico'], front: ['features/calificaciones', 'features/catalogo-academico'] },
  { titulo: 'Base común (rutas, componentes, utilidades)', back: ['.'], front: ['app', 'components', 'config', 'hooks', 'lib', 'utils'] },
];

function archivos(dir, filtro) {
  const salida = [];
  for (const nombre of readdirSync(dir)) {
    if (nombre === 'node_modules') continue;
    const ruta = join(dir, nombre);
    if (statSync(ruta).isDirectory()) salida.push(...archivos(ruta, filtro));
    else if (filtro(nombre)) salida.push(ruta);
  }
  return salida.sort();
}

const rel = (ruta) => relative(RAIZ, ruta).split(sep).join('/');
const capitalizar = (t) => t.charAt(0).toUpperCase() + t.slice(1);
// Los nombres de los métodos Java no llevan tildes: se restauran las palabras más comunes.
const PALABRAS = {
  anio: 'año', ano: 'año', anios: 'años', matricula: 'matrícula', matriculas: 'matrículas', codigo: 'código',
  periodo: 'período', periodos: 'períodos', mas: 'más', tambien: 'también', despues: 'después', aun: 'aún',
  ultimo: 'último', ultima: 'última', unico: 'único', unica: 'única', numero: 'número', telefono: 'teléfono',
  dia: 'día', dias: 'días', pagina: 'página', valida: 'válida', valido: 'válido', invalida: 'inválida',
  invalido: 'inválido', maximo: 'máximo', minimo: 'mínimo', proximo: 'próximo', catalogo: 'catálogo',
  dialogo: 'diálogo', metodo: 'método', academico: 'académico', academica: 'académica', publico: 'público',
  logica: 'lógica', sofia: 'Sofía', maria: 'María', mayusculas: 'mayúsculas', minusculas: 'minúsculas',
  pestana: 'pestaña', esta: 'está', estan: 'están', exito: 'éxito', busqueda: 'búsqueda', calculo: 'cálculo',
  via: 'vía', pension: 'pensión', tenia: 'tenía', sera: 'será', habil: 'hábil', habiles: 'hábiles',
  contrasena: 'contraseña', contrasenas: 'contraseñas', ningun: 'ningún', algun: 'algún', sabado: 'sábado',
  miercoles: 'miércoles', credito: 'crédito', debito: 'débito', rapida: 'rápida', rapido: 'rápido',
  basico: 'básico', basica: 'básica', util: 'útil', politica: 'política', publica: 'pública',
  titulo: 'título', ademas: 'además', facil: 'fácil', area: 'área', areas: 'áreas', ingles: 'inglés',
  tecnico: 'técnico', tecnica: 'técnica', ambito: 'ámbito', condicion: 'condición', jerarquia: 'jerarquía',
  categoria: 'categoría', categorias: 'categorías', ordenes: 'órdenes', orden: 'orden', rango: 'rango',
};
const tildar = (palabra) => {
  if (PALABRAS[palabra]) return PALABRAS[palabra];
  return palabra.replace(/cion$/, 'ción').replace(/sion$/, 'sión');
};
const humanizarCamel = (t) =>
  capitalizar(
    t
      .replace(/([a-záéíóúñ0-9])([A-ZÁÉÍÓÚÑ])/g, '$1 $2')
      .replace(/([A-ZÁÉÍÓÚÑ])([A-ZÁÉÍÓÚÑ][a-záéíóúñ])/g, '$1 $2')
      .toLowerCase()
      .split(/\s+/)
      .map(tildar)
      .join(' ')
      .replace(/\b(sofía|maría)\b/g, (x) => capitalizar(x)),
  );
const humanizarArchivo = (nombre) =>
  humanizarCamel(
    nombre
      .replace(/\.(test\.tsx?|java)$/, '')
      .replace(/(IT|Tests|Prueba)$/, '')
      .replace(/-/g, ' ')
      .replace(/\b(\w)/g, (x) => x.toUpperCase()),
  );

// ---- Backend ----
function leerJava(ruta) {
  const texto = readFileSync(ruta, 'utf8');
  const lineas = texto.split('\n');
  const tests = [];
  for (let i = 0; i < lineas.length; i++) {
    if (!/@(Test|ParameterizedTest)\b/.test(lineas[i])) continue;
    // Prefiere el @DisplayName (vive junto a la prueba); si falta, humaniza el nombre del método.
    let nombre = null;
    for (let j = Math.max(0, i - 3); j < Math.min(i + 6, lineas.length); j++) {
      const d = lineas[j].match(/@DisplayName\("(.*)"\)/);
      if (d) nombre = d[1];
      const m = lineas[j].match(/void\s+(\w+)\s*\(/);
      if (m) {
        tests.push({ nombre: nombre ?? humanizarCamel(m[1]), metodo: m[1] });
        break;
      }
    }
  }
  return tests;
}

// ---- Frontend ----
function leerVitest(ruta) {
  const texto = readFileSync(ruta, 'utf8');
  const items = [];
  const re = /^\s*(describe|it|test)(?:\.each\([^)]*\))?\(\s*(['"`])((?:\\.|(?!\2).)*)\2/gm;
  let m;
  while ((m = re.exec(texto))) items.push({ tipo: m[1], texto: m[3].replace(/\\(['"`])/g, '$1') });
  return items;
}

const areas = AREAS.map((a) => ({ ...a, back_: [], front_: [] }));

function areaDeBack(ruta) {
  const partes = relative(BACK, ruta).split(sep);
  const modulo = partes.length > 1 ? partes[0] : '.';
  return areas.find((a) => a.back.includes(modulo));
}
function areaDeFront(ruta) {
  const r = relative(FRONT, ruta).split(sep).join('/');
  return areas.find((a) => a.front.some((p) => r === p || r.startsWith(p + '/')));
}

for (const ruta of archivos(BACK, (n) => /(IT|Tests)\.java$/.test(n))) {
  const tests = leerJava(ruta);
  if (!tests.length) continue;
  const tipo = /Tests\.java$/.test(ruta) ? 'Unitaria' : 'Integración';
  areaDeBack(ruta).back_.push({ ruta, titulo: humanizarArchivo(ruta.split(sep).pop()), tipo, tests });
}
for (const ruta of archivos(FRONT, (n) => /\.test\.tsx?$/.test(n))) {
  const items = leerVitest(ruta);
  const total = items.filter((i) => i.tipo !== 'describe').length;
  if (!total) continue;
  const area = areaDeFront(ruta);
  if (!area) throw new Error(`Sin área para ${rel(ruta)}`);
  // Título propio: una línea `// @catalogo: Mi título` al inicio del archivo de prueba.
  const etiqueta = readFileSync(ruta, 'utf8').slice(0, 2000).match(/^\/\/\s*@catalogo:\s*(.+)$/m);
  const titulo = etiqueta ? etiqueta[1].trim() : humanizarArchivo(ruta.split(sep).pop());
  area.front_.push({ ruta, titulo, items, total, propio: Boolean(etiqueta) });
}

// Si dos archivos de un mismo apartado quedan con el mismo título, se distinguen con su carpeta.
for (const area of areas) {
  for (const lista of [area.back_, area.front_]) {
    const repetidos = new Set(lista.map((f) => f.titulo).filter((t, i, todos) => todos.indexOf(t) !== i));
    for (const f of lista) {
      if (!repetidos.has(f.titulo)) continue;
      const partes = relative(RAIZ, f.ruta).split(sep).filter((x) => x !== '__tests__');
      f.titulo += ` (${humanizarCamel(partes[partes.length - 2].replace(/-/g, ' ')).toLowerCase()})`;
    }
  }
}

// ---- Resultados (solo con --resultados) ----
// Uso: node docs/generar-pruebas.mjs --resultados [--front=ruta/al/vitest.json]
// Lee lo que dejaron las pruebas (backend: target/surefire-reports y failsafe-reports en XML; frontend: el JSON de
// `npx vitest run --reporter=json --outputFile.json=<ruta>`) y marca cada prueba con ✅, ❌ u ⏭️.
const CON_RESULTADOS = process.argv.includes('--resultados');
const RUTA_FRONT =
  process.argv.find((a) => a.startsWith('--front='))?.slice(8) ?? join(RAIZ, 'frontend', 'test-results', 'vitest.json');
const PEOR = { ok: 0, skip: 1, fail: 2 };
const peor = (a, b) => (a === undefined || PEOR[b] > PEOR[a] ? b : a);
const resBack = new Map(); // "clase.metodo" -> estado
const resFront = new Map(); // ruta absoluta -> [{ titulo, estado }]
let fechaResultados = null;

if (CON_RESULTADOS) {
  const fechas = [];
  for (const carpeta of ['surefire-reports', 'failsafe-reports']) {
    const dir = join(RAIZ, 'backend', 'target', carpeta);
    let nombres = [];
    try {
      nombres = readdirSync(dir).filter((n) => /^TEST-.*\.xml$/.test(n));
    } catch {
      continue;
    }
    for (const n of nombres) {
      const ruta = join(dir, n);
      fechas.push(statSync(ruta).mtimeMs);
      for (const trozo of readFileSync(ruta, 'utf8').split('<testcase ').slice(1)) {
        const cabecera = trozo.slice(0, trozo.indexOf('>'));
        const cuerpo = trozo.slice(trozo.indexOf('>'), trozo.indexOf('</testcase>') === -1 ? 0 : trozo.indexOf('</testcase>'));
        const nombre = cabecera.match(/name="([^"]*)"/)?.[1];
        const clase = cabecera.match(/classname="([^"]*)"/)?.[1];
        if (!nombre || !clase) continue;
        const metodo = nombre.replace(/\(.*$/, '');
        const estado = /<(failure|error)\b/.test(cuerpo) ? 'fail' : /<skipped\b/.test(cuerpo) ? 'skip' : 'ok';
        const clave = `${clase}.${metodo}`;
        resBack.set(clave, peor(resBack.get(clave), estado));
      }
    }
  }
  let json;
  try {
    json = JSON.parse(readFileSync(RUTA_FRONT, 'utf8'));
    fechas.push(statSync(RUTA_FRONT).mtimeMs);
  } catch {
    throw new Error(`No encuentro los resultados del frontend en ${RUTA_FRONT}. Corre vitest con --reporter=json.`);
  }
  for (const archivo of json.testResults) {
    resFront.set(
      archivo.name.split(sep).join('/').toLowerCase(),
      archivo.assertionResults.map((a) => ({
        titulo: a.title,
        estado: a.status === 'passed' ? 'ok' : a.status === 'failed' ? 'fail' : 'skip',
      })),
    );
  }
  fechaResultados = new Date(Math.max(...fechas));
}

const MARCA = { ok: '✅', fail: '❌', skip: '⏭️' };
function estadoBack(f, t) {
  const clase = relative(BACK, f.ruta).replace(/\.java$/, '').split(sep).join('.');
  return resBack.get(`com.galiledu.${clase}.${t.metodo}`) ?? 'skip';
}
function estadoFront(f, item) {
  const pruebas = resFront.get(f.ruta.split(sep).join('/').toLowerCase()) ?? [];
  const exactas = pruebas.filter((p) => p.titulo === item.texto);
  // test.each / it.each: el título del archivo trae %s, %i…; en el resultado ya vienen con los valores.
  const regex = /%[sidojfp#%]/.test(item.texto)
    ? new RegExp('^' + item.texto.split(/%[sidojfp#%]/).map((x) => x.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')).join('.*') + '$')
    : null;
  const candidatas = exactas.length ? exactas : regex ? pruebas.filter((p) => regex.test(p.titulo)) : [];
  return candidatas.reduce((acc, p) => peor(acc, p.estado), undefined) ?? 'skip';
}
const conteo = { back: { ok: 0, fail: 0, skip: 0 }, front: { ok: 0, fail: 0, skip: 0 } };
if (CON_RESULTADOS) {
  for (const a of areas) {
    for (const f of a.back_)
      for (const t of f.tests) conteo.back[(t.estado = estadoBack(f, t))]++;
    for (const f of a.front_)
      for (const i of f.items) if (i.tipo !== 'describe') conteo.front[(i.estado = estadoFront(f, i))]++;
  }
}
const marca = (x) => (CON_RESULTADOS ? `${MARCA[x.estado]} ` : '');
const resumenEstado = (c) =>
  `${c.ok} pasan` + (c.fail ? `, **${c.fail} fallan**` : '') + (c.skip ? `, ${c.skip} omitidas` : '');

// ---- Markdown ----
const contar = (a) => ({
  back: a.back_.reduce((s, f) => s + f.tests.length, 0),
  front: a.front_.reduce((s, f) => s + f.total, 0),
});
const ancla = (t) =>
  t.toLowerCase().replace(/[()]/g, '').replace(/[^a-z0-9áéíóúñ ]+/g, '').trim().replace(/ /g, '-');

const out = [];
out.push(CON_RESULTADOS ? '# Resultados de las pruebas de GalilEdu' : '# Pruebas automáticas de GalilEdu');
out.push('');
if (CON_RESULTADOS) {
  const fecha = fechaResultados.toLocaleString('es-PE', { dateStyle: 'long', timeStyle: 'short' });
  out.push(
    '> Generado con `node docs/generar-pruebas.mjs --resultados` a partir de la última ejecución de las pruebas. ' +
      'No se edita a mano.',
  );
  out.push('');
  out.push(`**Ejecución:** ${fecha}`);
  out.push('');
  out.push(`- Backend: ${resumenEstado(conteo.back)}.`);
  out.push(`- Frontend: ${resumenEstado(conteo.front)}.`);
  out.push('');
  out.push('Leyenda: ✅ pasa · ❌ falla · ⏭️ omitida o sin resultado.');
  out.push('');
} else {
  out.push(
    '> Archivo generado con `node docs/generar-pruebas.mjs`. No se edita a mano: se vuelve a generar cuando cambien los tests. ' +
      'Es la lista de pruebas que existen, no el resultado de ejecutarlas: para ver cuáles pasan, corre las pruebas y luego `node docs/generar-pruebas.mjs --resultados` (genera `docs/pruebas-resultados.md`, que no se sube al repo).',
  );
  out.push('');
}
out.push(
  'Cada funcionalidad tiene pruebas en el **backend** (Java: de integración contra PostgreSQL real y unitarias de reglas de negocio) y en el **frontend** (Vitest + Testing Library, con la API simulada). Cada línea de abajo es una prueba que se ejecuta en cada validación.',
);
out.push('');
out.push('## Resumen');
out.push('');
out.push('| Funcionalidad | Backend | Frontend | Total |');
out.push('|---|---:|---:|---:|');
let tb = 0;
let tf = 0;
for (const a of areas) {
  const c = contar(a);
  tb += c.back;
  tf += c.front;
  out.push(`| [${a.titulo}](#${ancla(a.titulo)}) | ${c.back} | ${c.front} | ${c.back + c.front} |`);
}
out.push(`| **Total** | **${tb}** | **${tf}** | **${tb + tf}** |`);
out.push('');
out.push('Cómo correrlas:');
out.push('');
out.push('```bash');
out.push('# backend (necesita Docker para PostgreSQL)');
out.push('cd backend && ./mvnw verify');
out.push('# frontend');
out.push('cd frontend && npm test');
out.push('```');
out.push('');

for (const a of areas) {
  const c = contar(a);
  out.push(`## ${a.titulo}`);
  out.push('');
  out.push(`${c.back} pruebas de backend y ${c.front} de frontend.`);
  out.push('');
  if (a.back_.length) {
    out.push('### Backend');
    out.push('');
    for (const f of a.back_) {
      out.push(`#### ${f.titulo} · ${f.tipo.toLowerCase()} (${f.tests.length})`);
      out.push('');
      out.push(`\`${rel(f.ruta)}\``);
      out.push('');
      for (const t of f.tests) out.push(`- ${marca(t)}${t.nombre}`);
      out.push('');
    }
  }
  if (a.front_.length) {
    out.push('### Frontend');
    out.push('');
    for (const f of a.front_) {
      out.push(`#### ${f.titulo} (${f.total})`);
      out.push('');
      out.push(`\`${rel(f.ruta)}\``);
      out.push('');
      for (const i of f.items) {
        if (i.tipo === 'describe') out.push(`**${i.texto}**`, '');
        else out.push(`- ${marca(i)}${i.texto}`);
      }
      out.push('');
    }
  }
}

const archivoSalida = CON_RESULTADOS ? 'pruebas-resultados.md' : 'pruebas.md';
writeFileSync(join(RAIZ, 'docs', archivoSalida), out.join('\n').replace(/\n{3,}/g, '\n\n'), 'utf8');
console.log(`docs/${archivoSalida}: ${tb} backend, ${tf} frontend`);
if (CON_RESULTADOS) console.log(`Backend: ${resumenEstado(conteo.back)}. Frontend: ${resumenEstado(conteo.front)}.`);
