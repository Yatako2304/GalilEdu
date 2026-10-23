import { ChevronDown, LogOut, PanelLeft } from 'lucide-react';
import * as React from 'react';
import { NavLink, useNavigate, useNavigation } from 'react-router';

import logo from '@/assets/logo.png';
import { Avatar } from '@/components/ui/avatar';
import { Drawer, DrawerContent, DrawerTitle } from '@/components/ui/drawer';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown';
import { Link } from '@/components/ui/link';
import { NAVIGATION, NavSection } from '@/config/navigation';
import { paths } from '@/config/paths';
import { ROLES, ROLE_CONFIG, getRoleHomeHref } from '@/config/roles';
import { useLogout, useUser } from '@/lib/auth';
import { useActiveRole } from '@/lib/authorization';
import { cn } from '@/utils/cn';

// ---------- Estado del sidebar (colapsado / drawer en móvil) ----------

const COLLAPSED_KEY = 'galiledu_sidebar_collapsed';

type SidebarContextValue = {
  collapsed: boolean;
  toggle: () => void;
};

const SidebarContext = React.createContext<SidebarContextValue>({
  collapsed: false,
  toggle: () => {},
});

const readCollapsed = () => {
  try {
    return window.localStorage.getItem(COLLAPSED_KEY) === 'true';
  } catch {
    return false;
  }
};

/** Botón del encabezado que colapsa el sidebar (desktop) o abre el drawer (móvil). */
export const SidebarToggle = () => {
  const { toggle } = React.useContext(SidebarContext);
  return (
    <button
      type="button"
      onClick={toggle}
      className="inline-flex size-8 items-center justify-center rounded-xl text-content-tertiary hover:bg-surface-tertiary"
    >
      <PanelLeft className="size-4" aria-hidden="true" />
      <span className="sr-only">Mostrar u ocultar menú</span>
    </button>
  );
};

// ---------- Barra de progreso en navegación ----------

const Progress = () => {
  const { state, location } = useNavigation();
  const [progress, setProgress] = React.useState(0);

  React.useEffect(() => {
    setProgress(0);
  }, [location?.pathname]);

  React.useEffect(() => {
    if (state === 'loading') {
      const timer = setInterval(() => {
        setProgress((oldProgress) => {
          if (oldProgress === 100) {
            clearInterval(timer);
            return 100;
          }
          const newProgress = oldProgress + 10;
          return newProgress > 100 ? 100 : newProgress;
        });
      }, 300);

      return () => {
        clearInterval(timer);
      };
    }
  }, [state]);

  if (state !== 'loading') {
    return null;
  }

  return (
    <div
      className="fixed left-0 top-0 z-50 h-1 bg-vibrant-blue-500 transition-all duration-200 ease-in-out"
      style={{ width: `${progress}%` }}
    ></div>
  );
};

// ---------- Piezas del sidebar (Figma > Componentes > Sidebar) ----------

const RoleSwitcher = ({
  activeRole,
  roles,
  collapsed,
}: {
  activeRole: ROLES;
  roles: ROLES[];
  collapsed: boolean;
}) => {
  const navigate = useNavigate();
  const label = ROLE_CONFIG[activeRole].label;

  if (collapsed) return null;

  if (roles.length < 2) {
    return (
      <div className="px-3 py-2 text-body-4 text-content-tertiary">{label}</div>
    );
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger className="flex w-full items-center justify-between rounded-xl px-3 py-2 text-body-4 text-content-tertiary hover:bg-surface-tertiary focus:outline-none focus-visible:ring-2 focus-visible:ring-stroke-focus">
        {label}
        <ChevronDown className="size-4" aria-hidden="true" />
        <span className="sr-only">Cambiar de rol</span>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="start" className="w-52">
        {roles.map((role) => (
          <DropdownMenuItem
            key={role}
            onClick={() => navigate(getRoleHomeHref(role))}
            className={cn(role === activeRole && 'font-semibold')}
          >
            {ROLE_CONFIG[role].label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
};

const NavSections = ({
  sections,
  collapsed,
  onNavigate,
}: {
  sections: NavSection[];
  collapsed: boolean;
  onNavigate?: () => void;
}) => (
  <nav className="flex flex-col gap-4">
    {sections.map((section) => (
      <div key={section.title} className="flex flex-col gap-1">
        {!collapsed && (
          <p className="px-3 pb-1 text-caption-1 font-semibold uppercase tracking-wide text-content-quartiary">
            {section.title}
          </p>
        )}
        {section.items.map((item) => (
          <NavLink
            key={item.href}
            to={item.href}
            end={item.exact}
            onClick={onNavigate}
            title={collapsed ? item.label : undefined}
            className={({ isActive }) =>
              cn(
                'flex h-10 items-center gap-3 rounded-xl px-3 text-label-2 font-medium text-content-secondary transition-colors hover:bg-surface-tertiary',
                isActive &&
                  'bg-surface-accent-light font-semibold text-content-accent-primary hover:bg-deep-blue-100',
                collapsed && 'justify-center px-0',
              )
            }
          >
            <item.icon className="size-4 shrink-0" aria-hidden="true" />
            {!collapsed && <span className="truncate">{item.label}</span>}
          </NavLink>
        ))}
      </div>
    ))}
  </nav>
);

const SidebarBody = ({
  collapsed,
  onNavigate,
}: {
  collapsed: boolean;
  onNavigate?: () => void;
}) => {
  const user = useUser();
  const activeRole = useActiveRole();
  // Al salir se recarga la página en el login (sin "volver a"): así se descarta todo lo que quedó en
  // memoria (caché de consultas, notificaciones) y quien entre después no ve datos del usuario anterior.
  const logout = useLogout({
    onSuccess: () => window.location.replace(paths.auth.login.getHref()),
  });

  if (!user.data || !activeRole) return null;

  const fullName = `${user.data.firstName} ${user.data.lastName}`;

  return (
    <div className="flex h-full flex-col">
      <div className="flex flex-col gap-2 border-b border-stroke-tertiary px-3 pb-3 pt-5">
        <Link
          to={getRoleHomeHref(activeRole)}
          className="flex items-center px-3"
          onClick={onNavigate}
        >
          <img
            src={logo}
            alt="Galiledu"
            className={cn(
              'h-7 w-auto object-contain object-left',
              collapsed && 'h-6 w-6 object-cover',
            )}
          />
        </Link>
        <RoleSwitcher
          activeRole={activeRole}
          roles={user.data.roles}
          collapsed={collapsed}
        />
      </div>

      <div className="flex-1 overflow-y-auto px-3 py-4">
        <NavSections
          sections={NAVIGATION[activeRole]}
          collapsed={collapsed}
          onNavigate={onNavigate}
        />
      </div>

      <div
        className={cn(
          'flex items-center gap-3 border-t border-stroke-tertiary p-3',
          collapsed && 'flex-col',
        )}
      >
        <Avatar name={fullName} />
        {!collapsed && (
          <div className="min-w-0 flex-1">
            <p className="truncate text-label-2 font-medium text-content-primary">
              {fullName}
            </p>
            <p className="truncate text-body-4 text-content-quartiary">
              {ROLE_CONFIG[activeRole].label}
            </p>
          </div>
        )}
        <button
          type="button"
          onClick={() => logout.mutate({})}
          className="inline-flex size-8 items-center justify-center rounded-xl text-content-tertiary hover:bg-surface-tertiary"
        >
          <LogOut className="size-4" aria-hidden="true" />
          <span className="sr-only">Cerrar sesión</span>
        </button>
      </div>
    </div>
  );
};

// ---------- Layout ----------

export function DashboardLayout({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = React.useState(readCollapsed);
  const [mobileOpen, setMobileOpen] = React.useState(false);

  const toggle = React.useCallback(() => {
    if (window.matchMedia('(max-width: 767px)').matches) {
      setMobileOpen((open) => !open);
      return;
    }
    setCollapsed((value) => {
      try {
        window.localStorage.setItem(COLLAPSED_KEY, String(!value));
      } catch {
        // sin almacenamiento: solo se pierde la preferencia
      }
      return !value;
    });
  }, []);

  return (
    <SidebarContext.Provider value={{ collapsed, toggle }}>
      <div className="flex min-h-screen w-full bg-surface-secondary">
        <Progress />
        <aside
          className={cn(
            'fixed inset-y-0 left-0 z-20 hidden border-r border-stroke-tertiary bg-white transition-[width] md:block',
            collapsed ? 'w-[68px]' : 'w-sidebar',
          )}
        >
          <SidebarBody collapsed={collapsed} />
        </aside>

        <Drawer open={mobileOpen} onOpenChange={setMobileOpen}>
          <DrawerContent side="left" className="w-sidebar p-0 sm:max-w-sidebar">
            <DrawerTitle className="sr-only">Menú</DrawerTitle>
            <SidebarBody
              collapsed={false}
              onNavigate={() => setMobileOpen(false)}
            />
          </DrawerContent>
        </Drawer>

        <div
          className={cn(
            'flex min-w-0 flex-1 flex-col transition-[padding]',
            collapsed ? 'md:pl-[68px]' : 'md:pl-sidebar',
          )}
        >
          <main className="flex-1">{children}</main>
        </div>
      </div>
    </SidebarContext.Provider>
  );
}
