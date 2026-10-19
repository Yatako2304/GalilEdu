// Qué se revisa en el commit (lo llama el hook frontend/.husky/pre-commit).
// Solo los archivos del frontend que vas a commitear; el CI vuelve a revisar todo.
module.exports = {
  // ESLint (incluye Prettier y las reglas de arquitectura) corrige lo que pueda en los archivos staged.
  // La revisión de tipos es de todo el proyecto, por eso se ejecuta una sola vez (función sin argumentos).
  '*.{ts,tsx}': ['eslint --fix', () => 'tsc --noEmit -p tsconfig.json'],
  '*.{json,css,cjs,mjs,js}': ['prettier --write'],
};
