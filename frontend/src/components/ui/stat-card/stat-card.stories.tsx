import { Meta, StoryObj } from '@storybook/react';
import { Users } from 'lucide-react';

import { StatCard } from './stat-card';

const meta: Meta<typeof StatCard> = {
  title: 'Components/StatCard',
  component: StatCard,
  args: {
    icon: <Users />,
    value: '1,248',
    label: 'Estudiantes matriculados',
    className: 'w-64',
  },
};

export default meta;
type Story = StoryObj<typeof StatCard>;

export const Default: Story = {};
export const ConVariacion: Story = { args: { trend: '+4.2%' } };
export const VariacionNegativa: Story = {
  args: { trend: '-1.3%', trendTone: 'error' },
};
