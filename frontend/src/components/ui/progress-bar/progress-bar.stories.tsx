import { Meta, StoryObj } from '@storybook/react';

import { ProgressBar } from './progress-bar';

const meta: Meta<typeof ProgressBar> = {
  title: 'Components/ProgressBar',
  component: ProgressBar,
  args: { value: 74, label: 'Avance del año escolar', className: 'w-96' },
};

export default meta;
type Story = StoryObj<typeof ProgressBar>;

export const Default: Story = {};
export const SinEtiqueta: Story = { args: { showLabel: false } };
export const Vacia: Story = { args: { value: 0 } };
export const Completa: Story = { args: { value: 100 } };
