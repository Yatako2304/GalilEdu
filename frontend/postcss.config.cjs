const path = require('path');

module.exports = {
  plugins: {
    // ruta explícita: funciona aunque Vite se ejecute desde otra carpeta
    tailwindcss: { config: path.join(__dirname, 'tailwind.config.cjs') },
    autoprefixer: {},
  },
};
