/** @type {import('tailwindcss').Config} */

const path = require('path');

const defaultTheme = require('tailwindcss/defaultTheme');

// Tokens tomados del Design System de Figma (GalilEdu > 🗂️ DESIGN SYSTEM > Foundations).
// - Primitives: paletas propias del proyecto (slate, red y amber coinciden con Tailwind).
// - Semantics: se exponen como clases `bg-*`, `text-*`, `border-*` vía variables CSS (src/index.css).

module.exports = {
  content: [
    path.join(__dirname, 'index.html').replace(/\\/g, '/'),
    path.join(__dirname, 'src/**/*.{js,ts,jsx,tsx}').replace(/\\/g, '/'),
  ],
  theme: {
    container: {
      center: true,
      padding: '2rem',
      screens: {
        '2xl': '1400px',
      },
    },
    extend: {
      fontFamily: {
        sans: ['Inter', ...defaultTheme.fontFamily.sans],
      },
      fontSize: {
        // Typography (Figma): [fontSize, lineHeight]
        'title-1': ['72px', { lineHeight: '88px', letterSpacing: '-0.8px' }],
        'title-2': ['64px', { lineHeight: '76px', letterSpacing: '-0.8px' }],
        'title-3': ['56px', { lineHeight: '68px', letterSpacing: '-0.6px' }],
        h1: ['56px', { lineHeight: '68px', letterSpacing: '-0.5px' }],
        h2: ['48px', { lineHeight: '58px', letterSpacing: '-0.4px' }],
        h3: ['40px', { lineHeight: '48px', letterSpacing: '-0.3px' }],
        h4: ['32px', { lineHeight: '38px', letterSpacing: '-0.2px' }],
        h5: ['24px', { lineHeight: '30px', letterSpacing: '-0.15px' }],
        h6: ['20px', { lineHeight: '24px' }],
        'label-1': ['16px', { lineHeight: '22px', letterSpacing: '-0.18px' }],
        'label-2': ['14px', { lineHeight: '20px', letterSpacing: '-0.16px' }],
        'label-3': ['12px', { lineHeight: '16px', letterSpacing: '-0.12px' }],
        'body-1': ['18px', { lineHeight: '28px' }],
        'body-2': ['16px', { lineHeight: '24px' }],
        'body-3': ['14px', { lineHeight: '20px' }],
        'body-4': ['12px', { lineHeight: '16px' }],
        'caption-1': ['10px', { lineHeight: '12px' }],
        'caption-2': ['9px', { lineHeight: '10px' }],
      },
      colors: {
        // ---- Primitives (Figma) ----
        'deep-blue': {
          50: '#ecf1fd',
          100: '#d0dcfa',
          200: '#a1bdf6',
          300: '#72a3f3',
          400: '#3d8ae9',
          500: '#3173c4',
          600: '#275da1',
          700: '#1d4980',
          800: '#143661',
          900: '#0b2545',
          950: '#031124',
        },
        'vibrant-blue': {
          50: '#edf0fe',
          100: '#d3dbfd',
          200: '#aabcfc',
          300: '#81a0fb',
          400: '#5386f9',
          500: '#1f6feb',
          600: '#195fcb',
          700: '#124aa1',
          800: '#093373',
          900: '#031c45',
          950: '#01102d',
        },
        'growth-green': {
          50: '#cefcd2',
          100: '#92f4a0',
          200: '#57d969',
          300: '#4abd5a',
          400: '#3fa34d',
          500: '#379043',
          600: '#2c7736',
          700: '#205c28',
          800: '#143f1a',
          900: '#08230b',
          950: '#031505',
        },
        'warm-yellow': {
          50: '#fdeede',
          100: '#fcd9ad',
          200: '#f5b841',
          300: '#e1a93b',
          400: '#c69433',
          500: '#a87d29',
          600: '#886520',
          700: '#684c16',
          800: '#47330c',
          900: '#281b04',
          950: '#180f02',
        },
        turquoise: {
          50: '#defdff',
          100: '#79eef1',
          200: '#32d9de',
          300: '#2bbfc4',
          400: '#26acb1',
          500: '#1f9497',
          600: '#17787b',
          700: '#0f5b5e',
          800: '#073e40',
          900: '#022223',
          950: '#011415',
        },

        // ---- Semantics (Figma > Semantics) ----
        surface: {
          primary: 'var(--bg-primary)',
          secondary: 'var(--bg-secondary)',
          tertiary: 'var(--bg-tertiary)',
          quartiary: 'var(--bg-quartiary)',
          'accent-light': 'var(--bg-accent-light)',
          'accent-primary': 'var(--bg-accent-primary-solid)',
          'accent-secondary': 'var(--bg-accent-secondary-solid)',
          'error-light': 'var(--bg-error-light)',
          error: 'var(--bg-error-solid)',
          'success-light': 'var(--bg-success-light)',
          success: 'var(--bg-success-solid)',
          'warning-light': 'var(--bg-warning-light)',
          warning: 'var(--bg-warning-solid)',
          'info-light': 'var(--bg-info-primary-light)',
          info: 'var(--bg-info-primary-solid)',
          disabled: 'var(--bg-disabled)',
        },
        content: {
          primary: 'var(--text-primary)',
          secondary: 'var(--text-secondary)',
          tertiary: 'var(--text-tertiary)',
          quartiary: 'var(--text-quartiary)',
          placeholder: 'var(--text-placeholder)',
          disabled: 'var(--text-disabled)',
          'accent-primary': 'var(--text-accent-primary)',
          'accent-secondary': 'var(--text-accent-secondary)',
          error: 'var(--text-error-primary)',
          success: 'var(--text-success-primary)',
          warning: 'var(--text-warning-primary)',
          info: 'var(--text-info-primary)',
        },
        stroke: {
          primary: 'var(--stroke-primary)',
          secondary: 'var(--stroke-secondary)',
          tertiary: 'var(--stroke-tertiary)',
          'accent-primary': 'var(--stroke-accent-primary)',
          focus: 'var(--stroke-accent-focus-alt)',
          error: 'var(--stroke-error-primary)',
          'error-secondary': 'var(--stroke-error-secondary)',
          success: 'var(--stroke-success-primary)',
          warning: 'var(--stroke-warning-primary)',
        },

        // ---- Alias usados por los componentes base de bulletproof-react (shadcn) ----
        border: 'hsl(var(--border))',
        input: 'hsl(var(--input))',
        ring: 'hsl(var(--ring))',
        background: 'hsl(var(--background))',
        foreground: 'hsl(var(--foreground))',
        primary: {
          DEFAULT: 'hsl(var(--primary))',
          foreground: 'hsl(var(--primary-foreground))',
        },
        secondary: {
          DEFAULT: 'hsl(var(--secondary))',
          foreground: 'hsl(var(--secondary-foreground))',
        },
        destructive: {
          DEFAULT: 'hsl(var(--destructive))',
          foreground: 'hsl(var(--destructive-foreground))',
        },
        muted: {
          DEFAULT: 'hsl(var(--muted))',
          foreground: 'hsl(var(--muted-foreground))',
        },
        accent: {
          DEFAULT: 'hsl(var(--accent))',
          foreground: 'hsl(var(--accent-foreground))',
        },
        popover: {
          DEFAULT: 'hsl(var(--popover))',
          foreground: 'hsl(var(--popover-foreground))',
        },
        card: {
          DEFAULT: 'hsl(var(--card))',
          foreground: 'hsl(var(--card-foreground))',
        },
      },
      spacing: {
        sidebar: '240px',
      },
      maxWidth: {
        content: '1200px',
      },
      borderRadius: {
        lg: 'var(--radius)',
        md: 'calc(var(--radius) - 4px)',
        sm: 'calc(var(--radius) - 8px)',
      },
      keyframes: {
        'accordion-down': {
          from: { height: '0' },
          to: { height: 'var(--radix-accordion-content-height)' },
        },
        'accordion-up': {
          from: { height: 'var(--radix-accordion-content-height)' },
          to: { height: '0' },
        },
      },
      animation: {
        'accordion-down': 'accordion-down 0.2s ease-out',
        'accordion-up': 'accordion-up 0.2s ease-out',
      },
    },
  },
  plugins: [require('tailwindcss-animate'), require('@tailwindcss/typography')],
};
