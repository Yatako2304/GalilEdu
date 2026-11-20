import { Meta, StoryObj } from '@storybook/react';

import { BarraDistribucion, LeyendaNiveles } from './barra-distribucion';

const meta: Meta<typeof BarraDistribucion> = {
  title: 'Components/BarraDistribucion',
  component: BarraDistribucion,
  args: { conteo: { ad: 1, a: 11, b: 5, c: 5 }, className: 'w-64' },
};

export default meta;
type Story = StoryObj<typeof BarraDistribucion>;

export const Default: Story = {};
export const Mediana: Story = { args: { size: 'md' } };
export const SoloUnNivel: Story = {
  args: { conteo: { ad: 0, a: 4, b: 0, c: 0 } },
};
export const SinCalificaciones: Story = {
  args: { conteo: { ad: 0, a: 0, b: 0, c: 0 } },
};
export const ConLeyenda: Story = {
  render: (args) => (
    <div className="flex w-96 flex-col gap-2">
      <BarraDistribucion {...args} size="md" />
      <LeyendaNiveles />
    </div>
  ),
};
