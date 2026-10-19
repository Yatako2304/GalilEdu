import { type ClassValue, clsx } from 'clsx';
import { extendTailwindMerge } from 'tailwind-merge';

// tailwind-merge no conoce los tamaños de letra del design system (text-body-3, text-h4…)
// y los confundía con colores: al combinar "text-white text-body-3" borraba text-white.
// Aquí se registran como tamaños de fuente (deben coincidir con fontSize en tailwind.config.cjs).
const twMerge = extendTailwindMerge({
  extend: {
    classGroups: {
      'font-size': [
        {
          text: [
            'title-1',
            'title-2',
            'title-3',
            'h1',
            'h2',
            'h3',
            'h4',
            'h5',
            'h6',
            'label-1',
            'label-2',
            'label-3',
            'body-1',
            'body-2',
            'body-3',
            'body-4',
            'caption-1',
            'caption-2',
          ],
        },
      ],
    },
  },
});

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
