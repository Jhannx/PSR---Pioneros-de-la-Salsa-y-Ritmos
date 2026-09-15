import { Navigate, useLocation, Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { ROLE_LABELS } from "../../context/mockDataConstants";
import { ShieldAlert, ArrowLeft } from "lucide-react";

export default function ProtectedRoute({ children, allowedRoles }) {
  const { user, isAuthenticated } = useAuth();
  const location = useLocation();

  if (!isAuthenticated) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // Verificar si alguno de los roles del usuario coincide con los roles permitidos
  const hasAccess =
    !allowedRoles || (user.roles && user.roles.some((r) => allowedRoles.includes(r)));

  if (!hasAccess) {
    const userRoleLabels = user.roles.map((r) => ROLE_LABELS[r] || r).join(", ");

    return (
      <div className="min-h-screen bg-neutral-950 flex flex-col items-center justify-center p-6 text-center font-body">
        <div className="bg-red-500/10 border border-red-500/20 p-4 rounded-full mb-4 text-red-400">
          <ShieldAlert size={48} />
        </div>
        <h1 className="text-2xl font-bold text-white mb-2 font-display">Acceso Restringido</h1>
        <p className="text-neutral-400 max-w-md mb-6 text-sm">
          Tu rol actual (
          <span className="font-semibold text-emerald-400">{userRoleLabels}</span>
          ) no posee autorización para ingresar a la ruta{" "}
          <code className="text-neutral-300 font-mono bg-neutral-900 px-2 py-1 rounded">
            {location.pathname}
          </code>
          .
        </p>
        <Link
          to="/"
          className="flex items-center gap-2 bg-neutral-800 hover:bg-neutral-700 text-white px-5 py-2.5 rounded-xl transition-colors font-medium text-sm"
        >
          <ArrowLeft size={16} /> Volver al Inicio
        </Link>
      </div>
    );
  }

  return children;
}
