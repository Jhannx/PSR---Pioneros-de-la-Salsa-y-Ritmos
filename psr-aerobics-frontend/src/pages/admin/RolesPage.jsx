import { useCallback, useEffect, useState } from "react";
import { Search, ShieldCheck, Power } from "lucide-react";
import { peopleService } from "../../services/peopleService";
import { userService } from "../../services/userService";

const ROLE_OPTIONS = [
  { key: "ADMIN", label: "Administrador" },
  { key: "RECEPTIONIST", label: "Recepción" },
  { key: "TEACHER", label: "Profesor" },
  { key: "MEMBER", label: "Miembro" },
  { key: "PSR_TERMINAL", label: "Terminal PSR" },
];

export default function RolesPage() {
  const [people, setPeople] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Detalle de cuenta por persona: { [userId]: { roles: string[], active: boolean, saving: boolean, error: string } }
  const [accounts, setAccounts] = useState({});

  const loadPeople = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const buscar = searchQuery.trim().length >= 2 ? searchQuery.trim() : undefined;
      const params = { page, size: 20 };
      if (buscar) params.buscar = buscar;

      // Antes: una consulta por persona (`getUserRoles` + `getById`, hasta 40 peticiones por
      // página de 20) — esa era la causa real de la lentitud reportada, no algo que un índice
      // fuera a arreglar (NOTAS-PARA-FRONTEND.md ronda 5, §0.1). Ahora `GET /api/usuarios` ya
      // trae `roles` y `active` para toda la página en una sola consulta; se piden en paralelo
      // con las personas y se cruzan por `userId` (= `UserResponse.id`), sin pedir nada por fila.
      const usersParams = buscar ? { buscar, size: 200 } : { size: 200 };
      const [peopleRes, usersRes] = await Promise.allSettled([
        peopleService.getAll(params),
        userService.getAll(usersParams),
      ]);

      if (peopleRes.status !== "fulfilled") throw peopleRes.reason;
      const response = peopleRes.value;
      const content = response?.content || (Array.isArray(response) ? response : []);
      setPeople(content);
      setPageInfo({ totalPages: response?.totalPages || 0, totalElements: response?.totalElements || 0 });

      if (usersRes.status === "fulfilled") {
        const usersContent = usersRes.value?.content || (Array.isArray(usersRes.value) ? usersRes.value : []);
        const byId = new Map(usersContent.map((u) => [u.id, u]));
        setAccounts((prev) => {
          const next = { ...prev };
          content.forEach((p) => {
            if (!p.userId) return;
            const u = byId.get(p.userId);
            if (!u) return; // no vino en esta tanda de 200 — se deja el estado previo si había alguno
            next[p.userId] = {
              roles: Array.isArray(u.roles) ? u.roles : [],
              active: u.active ?? true,
              saving: false,
              error: "",
            };
          });
          return next;
        });
      }
    } catch (err) {
      setPeople([]);
      setError(err.data?.message || err.message || "No fue posible consultar las personas.");
    } finally {
      setLoading(false);
    }
  }, [page, searchQuery]);

  useEffect(() => {
    const timer = setTimeout(loadPeople, searchQuery.trim().length === 1 ? 0 : 300);
    return () => clearTimeout(timer);
  }, [loadPeople, searchQuery]);

  function handleSearchChange(value) {
    setPage(0);
    setSearchQuery(value);
  }

  function setAccountState(userId, updates) {
    setAccounts((prev) => ({ ...prev, [userId]: { ...prev[userId], ...updates } }));
  }

  async function toggleRole(userId, roleKey) {
    const account = accounts[userId];
    if (!account || account.saving) return;
    const hasRole = account.roles.includes(roleKey);
    if (hasRole && account.roles.length === 1) return;

    setAccountState(userId, { saving: true, error: "" });
    try {
      if (hasRole) {
        await userService.removeRole(userId, roleKey);
        setAccountState(userId, { roles: account.roles.filter((r) => r !== roleKey), saving: false });
      } else {
        await userService.addRole(userId, roleKey);
        setAccountState(userId, { roles: [...account.roles, roleKey], saving: false });
      }
    } catch (err) {
      setAccountState(userId, {
        saving: false,
        error: err.data?.message || err.message || "No se pudo actualizar el rol.",
      });
    }
  }

  async function toggleAccountActive(userId) {
    const account = accounts[userId];
    if (!account || account.saving) return;
    setAccountState(userId, { saving: true, error: "" });
    try {
      if (account.active) {
        await userService.deactivateUser(userId);
      } else {
        await userService.activateUser(userId);
      }
      setAccountState(userId, { active: !account.active, saving: false });
    } catch (err) {
      setAccountState(userId, {
        saving: false,
        error: err.data?.message || err.message || "No se pudo actualizar el acceso de la cuenta.",
      });
    }
  }

  return (
    <div className="min-h-screen bg-neutral-950 text-white px-4 md:px-6 py-4 font-body">
      <div className="max-w-6xl mx-auto space-y-3">
        {/* Encabezado compacto */}
        <div className="border-b border-neutral-800 pb-3">
          <div className="flex items-center gap-2">
            <ShieldCheck className="text-neutral-400" size={18} />
            <h1 className="text-xl font-bold text-white tracking-tight">Roles y Accesos</h1>
          </div>
          <p className="text-neutral-400 text-xs mt-0.5">
            Asigna o retira roles del sistema y activa o desactiva el acceso de una persona. Exclusivo de
            Administrador.
          </p>
        </div>

        {/* Buscador */}
        <div className="relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-neutral-500" size={18} />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => handleSearchChange(e.target.value)}
            placeholder="Buscar por nombre, apellido o número de documento..."
            className="w-full bg-neutral-900 border border-neutral-800 rounded-2xl pl-11 pr-4 py-3 text-sm text-white placeholder-neutral-500 outline-none focus:border-emerald-500 transition-colors"
          />
        </div>

        {/* Lista */}
        <div className="bg-neutral-900/80 border border-neutral-800 rounded-3xl overflow-hidden shadow-xl">
          {error ? (
            <div className="p-8 text-center text-rose-300 text-sm">{error}</div>
          ) : loading ? (
            <div className="p-8 text-center text-neutral-500 text-sm">Consultando personas…</div>
          ) : people.length === 0 ? (
            <div className="p-8 text-center text-neutral-500 text-sm">
              No se encontraron personas con el filtro ingresado.
            </div>
          ) : (
            <div className="divide-y divide-neutral-800/60">
              {people.map((person) => {
                const fullName = `${person.firstName} ${person.lastName}`.trim();
                const account = person.userId ? accounts[person.userId] : null;
                const roles = account?.roles || [];
                const isInactive = account ? account.active === false : false;

                return (
                  <div
                    key={person.id}
                    className={`flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 ${
                      isInactive ? "opacity-60" : ""
                    }`}
                  >
                    <div className="min-w-0 sm:w-56 shrink-0">
                      <p className="text-white font-bold text-sm truncate" title={fullName}>
                        {fullName}
                      </p>
                      <p className="text-neutral-500 text-xs font-mono mt-0.5">
                        {person.documentType || "CC"} {person.documentNumber}
                      </p>
                    </div>

                    {!person.userId ? (
                      <div className="flex-1 text-neutral-500 text-xs italic">
                        Esta persona no tiene una cuenta de acceso todavía.
                      </div>
                    ) : (
                      <>
                        <div className="flex-1 flex flex-wrap gap-2">
                          {ROLE_OPTIONS.map((role) => {
                            const active = roles.includes(role.key);
                            return (
                              <button
                                key={role.key}
                                type="button"
                                onClick={() => toggleRole(person.userId, role.key)}
                                disabled={isInactive || account?.saving}
                                className={`px-3 py-1.5 rounded-full text-xs font-semibold border transition-colors disabled:opacity-40 disabled:cursor-not-allowed ${
                                  active
                                    ? "bg-emerald-500 text-neutral-950 border-emerald-500"
                                    : "bg-neutral-950 text-neutral-400 border-neutral-800 hover:border-neutral-700"
                                }`}
                              >
                                {role.label}
                              </button>
                            );
                          })}
                        </div>

                        <button
                          onClick={() => toggleAccountActive(person.userId)}
                          disabled={account?.saving}
                          className={`shrink-0 flex items-center gap-1.5 text-xs font-bold px-3 py-1.5 rounded-full transition-colors disabled:opacity-40 ${
                            isInactive
                              ? "bg-emerald-500/15 text-emerald-400 hover:bg-emerald-500/25"
                              : "bg-neutral-800 text-neutral-400 hover:text-amber-400"
                          }`}
                        >
                          <Power size={12} />
                          {isInactive ? "Activar acceso" : "Bloquear acceso"}
                        </button>
                      </>
                    )}

                    {account?.error && (
                      <p className="text-rose-400 text-xs w-full sm:w-auto">{account.error}</p>
                    )}
                  </div>
                );
              })}
            </div>
          )}
        </div>

        {pageInfo.totalPages > 1 && (
          <div className="flex justify-center gap-3 text-sm">
            <button
              disabled={page === 0}
              onClick={() => setPage((current) => current - 1)}
              className="px-3 py-2 rounded-lg bg-neutral-800 disabled:opacity-40"
            >
              Anterior
            </button>
            <span className="py-2 text-neutral-400">
              Página {page + 1} de {pageInfo.totalPages}
            </span>
            <button
              disabled={page + 1 >= pageInfo.totalPages}
              onClick={() => setPage((current) => current + 1)}
              className="px-3 py-2 rounded-lg bg-neutral-800 disabled:opacity-40"
            >
              Siguiente
            </button>
          </div>
        )}

        <p className="text-neutral-600 text-[11px]">
          Toda persona con acceso al sistema debe conservar al menos un rol asignado.
        </p>
      </div>
    </div>
  );
}
