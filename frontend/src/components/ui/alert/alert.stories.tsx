import { Meta, StoryObj } from '@storybook/react';

import { Button } from '../button';

import { Alert } from './alert';

const meta: Meta<typeof Alert> = {
  title: 'Components/Alert',
  component: Alert,
  args: {
    title: 'Todos los registros son válidos',
    children: '120 registros detectados · listos para crear',
  },
};

export default meta;
type Story = StoryObj<typeof Alert>;

export const Info: Story = { args: { type: 'info' } };
export const Success: Story = { args: { type: 'success' } };
export const Warning: Story = {
  args: { type: 'warning', title: 'No hay padre ni madre registrados' },
};
export const Error: Story = {
  args: { type: 'error', title: 'Se encontraron 3 registros con error' },
};
export const Default: Story = { args: { type: 'default' } };

export const ConAccion: Story = {
  args: {
    type: 'warning',
    title: 'Corrige los 3 registros pendientes',
    action: (
      <Button size="sm" variant="primary-light">
        Descargar detalle
      </Button>
    ),
  },
};

/** Toast: el mismo Alert con la X para cerrarlo. */
export const Toast: Story = {
  args: { type: 'success', title: 'Cambios guardados', onClose: () => {} },
};
