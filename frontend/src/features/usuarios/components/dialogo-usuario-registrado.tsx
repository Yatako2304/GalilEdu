import { CircleCheck, Contact, Mail, MailCheck, UserPlus } from 'lucide-react';

import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogTitle,
} from '@/components/ui/dialog';
import { Status } from '@/components/ui/tag';
import { UsuarioCreado } from '@/types/api';

// Figma > Usuarios > modal "Usuario registrado" (después de HU-6).
export const DialogoUsuarioRegistrado = ({
  usuario,
  onCerrar,
}: {
  usuario: UsuarioCreado | null;
  onCerrar: () => void;
}) => (
  <Dialog open={!!usuario} onOpenChange={(open) => !open && onCerrar()}>
    <DialogContent className="max-w-[800px] gap-0 rounded-[12px] border-stroke-tertiary bg-white p-0">
      {usuario && (
        <>
          <div className="flex flex-col items-center gap-3 px-8 pt-10 text-center">
            <span className="inline-flex size-16 items-center justify-center rounded-full bg-surface-success-light text-content-success">
              <UserPlus className="size-7" aria-hidden="true" />
            </span>
            <DialogTitle className="text-h5 font-semibold text-content-primary">
              Usuario registrado
            </DialogTitle>
            <DialogDescription className="text-body-3 text-content-secondary">
              La cuenta se creó correctamente y ya puede ingresar a GalilEdu.
            </DialogDescription>
          </div>
          <div className="flex flex-col gap-4 px-10 py-6">
            <dl className="divide-y divide-stroke-tertiary rounded-[12px] border border-stroke-tertiary bg-surface-secondary px-5">
              <div className="flex items-center justify-between py-3">
                <dt className="flex items-center gap-2 text-body-3 text-content-secondary">
                  <Contact className="size-4" aria-hidden="true" /> Usuario
                </dt>
                <dd className="text-body-3 font-semibold text-content-primary">
                  {usuario.username}
                </dd>
              </div>
              <div className="flex items-center justify-between py-3">
                <dt className="flex items-center gap-2 text-body-3 text-content-secondary">
                  <Mail className="size-4" aria-hidden="true" /> Correo
                </dt>
                <dd className="text-body-3 font-semibold text-content-primary">
                  {usuario.correo}
                </dd>
              </div>
              <div className="flex items-center justify-between py-3">
                <dt className="flex items-center gap-2 text-body-3 text-content-secondary">
                  <CircleCheck className="size-4" aria-hidden="true" /> Estado
                </dt>
                <dd>
                  <Status active={usuario.estado === 'ACTIVO'} />
                </dd>
              </div>
            </dl>
            <div className="flex gap-3 rounded-[12px] bg-surface-accent-light px-4 py-3">
              <MailCheck
                className="mt-0.5 size-4 shrink-0 text-content-accent-primary"
                aria-hidden="true"
              />
              <div>
                <p className="text-label-2 font-medium text-content-primary">
                  Credenciales enviadas al correo
                </p>
                <p className="text-body-4 text-content-tertiary">
                  El usuario recibirá su contraseña temporal y podrá cambiarla
                  cuando lo desee desde su perfil.
                </p>
              </div>
            </div>
          </div>
          <div className="border-t border-stroke-tertiary px-8 py-5">
            <Button className="w-full" onClick={onCerrar}>
              Continuar
            </Button>
          </div>
        </>
      )}
    </DialogContent>
  </Dialog>
);
