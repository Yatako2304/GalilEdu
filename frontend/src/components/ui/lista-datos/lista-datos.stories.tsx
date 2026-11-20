import { Meta, StoryObj } from '@storybook/react';

import { ListaDatos } from './lista-datos';

const meta: Meta<typeof ListaDatos> = {
  title: 'Components/ListaDatos',
  component: ListaDatos,
  args: {
    titulo: 'Contacto',
    datos: [
      { etiqueta: 'Celular', valor: '987000003' },
      { etiqueta: 'Correo electrónico', valor: 'luis.paredes@galiledu.edu.pe' },
      { etiqueta: 'Teléfono de casa' },
      { etiqueta: 'Dirección', valor: 'Calle 123', ancho: true },
    ],
  },
};

export default meta;
type Story = StoryObj<typeof ListaDatos>;

export const TresColumnas: Story = {};

export const DosColumnas: Story = { args: { columnas: 2 } };
