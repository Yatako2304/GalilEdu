import * as React from 'react';

import { cn } from '@/utils/cn';

import { IconBadge } from '../icon-badge';

export type StatCardProps = {
  icon: React.ReactNode;
  /** Valor principal (Figma: "1,248"). */
  value: React.ReactNode;
  label: React.ReactNode;
  /** Variación o dato extra a la derecha de la etiqueta (Figma: "+4.2%"). */
  trend?: React.ReactNode;
  trendTone?: 'success' | 'error' | 'neutral';
  className?: string;
};

const tonos = {
  success: 'text-content-success',
  error: 'text-content-error',
  neutral: 'text-content-quartiary',
};

// Figma > Componentes > Stat Card: tarjeta de indicador (KPI) para los inicios y resúmenes.
export const StatCard = ({
  icon,
  value,
  label,
  trend,
  trendTone = 'success',
  className,
}: StatCardProps) => (
  <div
    className={cn(
      'flex flex-col items-start gap-3 rounded-2xl border border-stroke-secondary bg-surface-primary p-5',
      className,
    )}
  >
    <IconBadge shape="square" aria-hidden="true">
      {icon}
    </IconBadge>
    <p className="text-[28px] font-bold leading-normal text-content-primary">
      {value}
    </p>
    <div className="flex w-full items-center gap-1.5">
      <p className="min-w-0 flex-1 text-[13px] text-content-secondary">
        {label}
      </p>
      {trend && (
        <p
          className={cn('shrink-0 text-body-4 font-semibold', tonos[trendTone])}
        >
          {trend}
        </p>
      )}
    </div>
  </div>
);
