import { Meta, StoryObj } from '@storybook/react';

import { ScheduleBlock } from './schedule-block';

const meta: Meta<typeof ScheduleBlock> = {
  title: 'Components/ScheduleBlock',
  component: ScheduleBlock,
  decorators: [
    (Story) => (
      <div className="w-[184px]">
        <Story />
      </div>
    ),
  ],
};

export default meta;
type Story = StoryObj<typeof ScheduleBlock>;

export const Asignado: Story = {
  args: {
    type: 'assigned',
    courses: [
      { name: 'Ciencia y tecnología', teacher: 'Prof. Lucía Ayala Prado' },
    ],
    room: 'A-401',
  },
};

export const Subgrupos: Story = {
  args: {
    type: 'assigned',
    courses: [
      { name: 'Arte y Cultura', teacher: 'Prof. Lucía Ayala Prado' },
      { name: 'Música', teacher: 'Prof. Enrique Moquillaza' },
    ],
    room: 'E-401 · Z-908',
  },
};

export const VistaProfesor: Story = {
  args: {
    type: 'assigned',
    courses: [{ name: 'Matemáticas', teacher: 'Prof. Lucía Ayala Prado' }],
    room: 'E-401',
    section: '3A',
  },
};

export const Recreo: Story = { args: { type: 'break' } };
export const SinAsignar: Story = {
  args: { type: 'empty', onAssign: () => {} },
};
export const HoraLibre: Story = { args: { type: 'empty' } };

export const AsignadoEditable: Story = {
  args: {
    type: 'assigned',
    courses: [{ name: 'Matemáticas', teacher: 'Prof. Pedro García Ramos' }],
    room: 'A-502',
    onSelect: () => {},
  },
};

export const ConCruce: Story = {
  args: {
    type: 'assigned',
    courses: [{ name: 'Matemáticas', teacher: 'Prof. Pedro García Ramos' }],
    room: 'A-502',
    conflict: 'El docente ya dicta Álgebra en 5.° B a esa hora.',
  },
};
