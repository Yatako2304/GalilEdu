import { Meta, StoryObj } from '@storybook/react';
import { Plus } from 'lucide-react';

import { Button } from './button';

const meta: Meta<typeof Button> = {
  component: Button,
};

export default meta;
type Story = StoryObj<typeof Button>;

export const Primary: Story = {
  args: { children: 'Guardar', variant: 'primary' },
};

export const Secondary: Story = {
  args: { children: 'Iniciar sesión', variant: 'secondary' },
};

export const PrimaryLight: Story = {
  args: { children: 'Cancelar', variant: 'primary-light' },
};

export const Tertiary: Story = {
  args: { children: 'Ver detalle', variant: 'tertiary' },
};

export const Error: Story = {
  args: { children: 'Eliminar', variant: 'error' },
};

export const ErrorLight: Story = {
  args: { children: 'Desactivar', variant: 'error-light' },
};

export const WithIcon: Story = {
  args: {
    children: 'Nuevo usuario',
    icon: <Plus className="size-4" />,
  },
};

export const Disabled: Story = {
  args: { children: 'Iniciar sesión', disabled: true },
};
