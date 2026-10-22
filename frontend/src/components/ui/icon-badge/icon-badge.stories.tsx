import { Meta, StoryObj } from '@storybook/react';
import { CircleCheck, Save, Trash2, Users } from 'lucide-react';

import { IconBadge } from './icon-badge';

const meta: Meta<typeof IconBadge> = {
  title: 'Components/IconBadge',
  component: IconBadge,
  args: { children: <Save aria-hidden="true" /> },
};

export default meta;
type Story = StoryObj<typeof IconBadge>;

export const Circulo: Story = {};

export const Cuadrado: Story = {
  args: { shape: 'square', children: <Users aria-hidden="true" /> },
};

export const Colores: Story = {
  render: () => (
    <div className="flex gap-3">
      <IconBadge color="accent">
        <Save />
      </IconBadge>
      <IconBadge color="success">
        <CircleCheck />
      </IconBadge>
      <IconBadge color="error">
        <Trash2 />
      </IconBadge>
      <IconBadge color="warning">
        <Save />
      </IconBadge>
      <IconBadge color="neutral">
        <Save />
      </IconBadge>
    </div>
  ),
};

export const Tamanos: Story = {
  render: () => (
    <div className="flex items-center gap-3">
      <IconBadge size="sm">1</IconBadge>
      <IconBadge size="md">
        <Save />
      </IconBadge>
      <IconBadge size="lg">
        <Save />
      </IconBadge>
      <IconBadge size="xl" color="success">
        <CircleCheck />
      </IconBadge>
    </div>
  ),
};
