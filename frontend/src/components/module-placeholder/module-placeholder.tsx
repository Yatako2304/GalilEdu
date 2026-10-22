import { Construction, ExternalLink } from 'lucide-react';

import { Card, CardContent } from '@/components/ui/card';
import { Tag } from '@/components/ui/tag';

const FIGMA_FILE =
  'https://www.figma.com/design/l8nXzicItzBnvPjFQP4G9O/GalilEdu';

type ModulePlaceholderProps = {
  /** Carpeta en src/features donde se implementará el módulo. */
  feature: string;
  /** Historias de usuario que cubre la pantalla. */
  historias?: string[];
  /** Nodo de Figma con el diseño (ej. "700:14588"). */
  figmaNodeId?: string;
};

// Pantalla temporal para rutas cuyo módulo aún no se implementa.
// Reemplázala por el componente real del feature cuando lo construyas.
// La carpeta, las HU y el enlace a Figma solo se ven en desarrollo: el usuario ve un aviso simple.
const enDesarrollo = import.meta.env.DEV;

export const ModulePlaceholder = ({
  feature,
  historias = [],
  figmaNodeId,
}: ModulePlaceholderProps) => (
  <Card>
    <CardContent className="flex flex-col items-center gap-4 py-16 text-center">
      <Construction
        className="size-10 text-content-placeholder"
        aria-hidden="true"
      />
      <div>
        <p className="text-label-1 font-semibold text-content-primary">
          Esta sección estará disponible pronto.
        </p>
        {enDesarrollo && (
          <p className="mt-1 text-body-3 text-content-quartiary">
            Se implementará en{' '}
            <code className="rounded bg-surface-tertiary px-1.5 py-0.5 text-body-4">
              src/features/{feature}
            </code>
          </p>
        )}
      </div>
      {enDesarrollo && historias.length > 0 && (
        <div className="flex flex-wrap justify-center gap-2">
          {historias.map((hu) => (
            <Tag key={hu} color="info">
              {hu}
            </Tag>
          ))}
        </div>
      )}
      {enDesarrollo && figmaNodeId && (
        <a
          href={`${FIGMA_FILE}?node-id=${figmaNodeId.replace(':', '-')}`}
          target="_blank"
          rel="noreferrer"
          className="inline-flex items-center gap-1 text-body-3 text-content-accent-secondary hover:underline"
        >
          Ver diseño en Figma
          <ExternalLink className="size-3.5" aria-hidden="true" />
        </a>
      )}
    </CardContent>
  </Card>
);
