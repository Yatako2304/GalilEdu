import { Meta, StoryObj } from '@storybook/react';
import { Accessibility, Plus } from 'lucide-react';

import { Accordion, AccordionFolder } from './accordion';

const meta: Meta<typeof Accordion> = {
  title: 'Components/Accordion',
  component: Accordion,
  args: {
    title: 'Ciencia y tecnología',
    children: <p className="text-body-3">Contenido de la sección.</p>,
    className: 'max-w-md',
  },
};

export default meta;
type Story = StoryObj<typeof Accordion>;

export const Cerrado: Story = {};
export const Abierto: Story = { args: { defaultOpen: true } };
export const ConIcono: Story = { args: { icon: <Accessibility /> } };
export const Deshabilitado: Story = { args: { disabled: true } };

export const Carpeta: Story = {
  render: () => (
    <div className="flex max-w-md flex-col gap-3">
      <AccordionFolder title="Unidad 1" subtitle="18 archivos">
        <p className="text-body-3">Archivos de la unidad.</p>
      </AccordionFolder>
      <AccordionFolder
        title="Unidad 2"
        subtitle="4 archivos"
        withIcon
        defaultOpen
        action={
          <button
            type="button"
            aria-label="Agregar archivo"
            className="rounded-full p-1.5"
          >
            <Plus className="size-6" />
          </button>
        }
      >
        <p className="text-body-3">Archivos de la unidad.</p>
      </AccordionFolder>
    </div>
  ),
};
