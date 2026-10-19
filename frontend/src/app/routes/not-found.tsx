import { Link } from '@/components/ui/link';
import { paths } from '@/config/paths';

const NotFoundRoute = () => {
  return (
    <div className="mt-52 flex flex-col items-center gap-2 text-center">
      <h1 className="text-h4 font-semibold text-content-primary">404</h1>
      <p className="text-body-3 text-content-quartiary">
        La página que buscas no existe.
      </p>
      <Link
        to={paths.home.getHref()}
        replace
        className="text-body-3 text-content-accent-secondary"
      >
        Ir al inicio
      </Link>
    </div>
  );
};

export default NotFoundRoute;
