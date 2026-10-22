import { Meta, StoryObj } from '@storybook/react';

import { Stepper } from './stepper';

const meta: Meta<typeof Stepper> = {
  title: 'Components/Stepper',
  component: Stepper,
  args: {
    steps: [
      { label: 'Estudiante', description: 'Datos e identificación' },
      { label: 'Ficha médica', description: 'Salud del estudiante' },
      { label: 'Padres y apoderado', description: 'Personas vinculadas' },
      { label: 'Contactos y salida', description: 'Emergencia y recojo' },
      { label: 'Responsable', description: 'Revisión y registro' },
    ],
    current: 3,
  },
};

export default meta;
type Story = StoryObj<typeof Stepper>;

export const Default: Story = {};

export const ConError: Story = {
  args: {
    steps: [
      { label: 'Subir archivo' },
      { label: 'Validar datos', state: 'error' },
      { label: 'Resumen' },
    ],
    current: 2,
  },
};
