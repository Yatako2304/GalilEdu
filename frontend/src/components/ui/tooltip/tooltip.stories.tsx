import { Meta, StoryObj } from '@storybook/react';

import { Button } from '../button';

import { Tooltip } from './tooltip';

const meta: Meta<typeof Tooltip> = {
  title: 'Components/Tooltip',
  component: Tooltip,
  args: {
    content: 'Más información',
    children: <Button variant="primary-light">Pasa el mouse</Button>,
  },
  decorators: [
    (Story) => (
      <div className="p-24">
        <Story />
      </div>
    ),
  ],
};

export default meta;
type Story = StoryObj<typeof Tooltip>;

export const Pequeno: Story = {};
export const Grande: Story = {
  args: {
    content: 'Pensión vencida',
    description: 'La cuota venció el 31/03. Se cobrará mora desde mañana.',
  },
};
export const Abajo: Story = { args: { side: 'bottom', align: 'start' } };
export const Derecha: Story = { args: { side: 'right' } };
