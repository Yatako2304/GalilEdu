import { Meta, StoryObj } from '@storybook/react';
import { Bookmark } from 'lucide-react';
import * as React from 'react';

import { SelectableTag } from './selectable-tag';
import { Status, Tag } from './tag';

const meta: Meta<typeof Tag> = {
  title: 'Components/Tag',
  component: Tag,
};

export default meta;
type Story = StoryObj<typeof Tag>;

export const Informativas: Story = {
  render: () => (
    <div className="flex flex-wrap items-center gap-2">
      <Tag color="primary">Primaria</Tag>
      <Tag color="info">Info</Tag>
      <Tag color="success">Pagado</Tag>
      <Tag color="warning">Por vencer</Tag>
      <Tag color="error">Vencido</Tag>
      <Tag color="neutral">Anulado</Tag>
      <Status active />
      <Status active={false} />
    </div>
  ),
};

const colores = ['primary', 'info', 'error', 'success', 'warning'] as const;

const DemoSeleccionables = () => {
  const [marcadas, setMarcadas] = React.useState<string[]>(['info']);
  const alternar = (c: string) =>
    setMarcadas(
      marcadas.includes(c) ? marcadas.filter((m) => m !== c) : [...marcadas, c],
    );
  return (
    <div className="flex flex-col gap-3">
      {(['square', 'circle'] as const).map((shape) => (
        <div key={shape} className="flex flex-wrap gap-2">
          {colores.map((c) => (
            <SelectableTag
              key={c}
              color={c}
              shape={shape}
              icon={<Bookmark />}
              selected={marcadas.includes(c)}
              onClick={() => alternar(c)}
            >
              Tag
            </SelectableTag>
          ))}
          <SelectableTag shape={shape} disabled>
            Tag
          </SelectableTag>
        </div>
      ))}
      <div className="flex gap-2">
        <SelectableTag size="md" shape="circle" onRemove={() => {}}>
          Matemática
        </SelectableTag>
        <SelectableTag size="sm" shape="circle" onRemove={() => {}}>
          Comunicación
        </SelectableTag>
      </div>
    </div>
  );
};

export const Seleccionables: Story = { render: () => <DemoSeleccionables /> };
