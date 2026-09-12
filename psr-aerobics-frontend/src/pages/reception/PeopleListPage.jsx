import { useCallback, useEffect, useRef, useState } from "react";
import { UserPlus, Search } from "lucide-react";
import PersonRow from "../../features/reception/components/PersonRow";
import PersonFormModal from "../../features/reception/components/PersonFormModal";
import HardDeleteModal from "../../features/reception/components/HardDeleteModal";
import { useAuth } from "../../context/AuthContext";
import { peopleService } from "../../services/peopleService";

// Un solo debounce para todo — ver el porqué del arreglo completo junto a loadPeople más abajo
// (errores-frontend.md #3).
const SEARCH_DEBOUNCE_MS = 350;

export default function PeopleListPage() {
  const { user } = useAuth();
  const isAdmin = user.roles.includes("ADMIN");
  const isReadOnly = user.roles.includes("TEACHER") && !isAdmin && !user.roles.includes("RECEPTIONIST");
  const [people, setPeople] = useState([]);
  const [searchQuery, setSearchQuery] = useState("");
  const [page, setPage] = useState(0);
  const [pageInfo, setPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  // Conteo real, independiente del filtro de búsqueda (errores-nuevo-front.md #2): `pageInfo`
  // sale de la misma respuesta paginada que la búsqueda, así que al filtrar refleja los
  // resultados filtrados, no el total. Este se pide aparte, sin `buscar`, y se refresca solo
  // tras altas/bajas — no en cada tecleo del buscador.
  const [activeTotal, setActiveTotal] = useState(null);

  const [formModal, setFormModal] = useState(null); // null | { mode: 'add' } | { mode: 'edit', person }
  const [confirmDeactivateId, setConfirmDeactivateId] = useState(null);
  const [hardDeleteTarget, setHardDeleteTarget] = useState(null);

  // Antes de este arreglo había una carrera entre dos peticiones: con el primer carácter
  // escrito, `searchQuery.trim().length === 1` disparaba el fetch con retardo 0 pero
  // `length >= 2` seguía siendo falso, así que salía una consulta SIN filtro (la lista
  // completa). Con el segundo carácter salía la consulta filtrada, a 300ms. La consulta sin
  // filtro es la más lenta (trae todo) y solía responder después, pisando el resultado
  // filtrado ya en pantalla — eso era el "se recarga solo y vuelve a mostrar todas las
  // personas" (errores-frontend.md #3). El arreglo tiene dos mitades: un solo debounce para
  // todos los casos (abajo) y, acá, un `requestId` que descarta cualquier respuesta que no sea
  // la de la petición más reciente en curso, sin importar el orden en que lleguen.
  const searchAbortRef = useRef(null);
  const requestIdRef = useRef(0);

  const loadPeople = useCallback(async () => {
    searchAbortRef.current?.abort();
    const controller = new AbortController();
    searchAbortRef.current = controller;
    const requestId = ++requestIdRef.current;

    setLoading(true);
    setError("");
    try {
      const params = { page, size: 20 };
      if (searchQuery.trim().length >= 2) params.buscar = searchQuery.trim();
      const response = await peopleService.getAll(params, { signal: controller.signal });
      if (requestId !== requestIdRef.current) return; // ya hay una petición más nueva en curso
      setPeople(response?.content || (Array.isArray(response) ? response : []));
      setPageInfo({ totalPages: response?.totalPages || 0, totalElements: response?.totalElements || 0 });
    } catch (err) {
      if (err.name === "AbortError") return; // cancelada a propósito, no es un error real
      if (requestId !== requestIdRef.current) return;
      setPeople([]);
      setError(err.data?.message || err.message || "No fue posible consultar las personas.");
    } finally {
      if (requestId === requestIdRef.current) setLoading(false);
    }
  }, [page, searchQuery]);

  useEffect(() => {
    const timer = setTimeout(loadPeople, SEARCH_DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [loadPeople]);

  const loadActiveTotal = useCallback(async () => {
    try {
      const response = await peopleService.getAll({ page: 0, size: 1 });
      setActiveTotal(response?.totalElements ?? 0);
    } catch {
      // No es crítico: si falla, el encabezado simplemente no muestra el conteo.
    }
  }, []);

  useEffect(() => {
    Promise.resolve().then(loadActiveTotal);
  }, [loadActiveTotal]);

  function handleSearchChange(value) {
    setPage(0);
    setSearchQuery(value);
  }

  async function handleSavePerson(data) {
    try {
      if (formModal?.mode === "edit") {
        await peopleService.update(formModal.person.id, data);
      } else {
        await peopleService.create(data);
      }
      setFormModal(null);
      await loadPeople();
      await loadActiveTotal();
    } catch (err) {
      setError(err.data?.message || err.message || "No fue posible guardar la persona.");
    }
  }

  // Actualiza solo la fila afectada en el estado local en vez de volver a pedir la página
  // completa: esa reconsulta era lo que hacía "sentir" lenta la acción (errores-frontend.md #1),
  // además de disparar de nuevo la consulta N+1 que el backend ya tiene identificada como lenta.
  async function handleConfirmDeactivate() {
    if (!confirmDeactivateId) return;
    try {
      await peopleService.deactivate(confirmDeactivateId);
      setPeople((prev) => prev.map((p) => (p.id === confirmDeactivateId ? { ...p, active: false } : p)));
      await loadActiveTotal();
    } catch (err) {
      setError(err.data?.message || err.message || "No fue posible desactivar la persona.");
    }
    setConfirmDeactivateId(null);
  }

  async function handleReactivate(personId) {
    try {
      await peopleService.reactivate(personId);
      setPeople((prev) => prev.map((p) => (p.id === personId ? { ...p, active: true } : p)));
      await loadActiveTotal();
    } catch (err) {
      setError(err.data?.message || err.message || "No fue posible reactivar la persona.");
    }
  }

  async function handleConfirmHardDelete() {
    if (!hardDeleteTarget) return;
    try {
      await peopleService.remove(hardDeleteTarget.id);
      setHardDeleteTarget(null);
      await loadPeople();
      await loadActiveTotal();
    } catch (err) {
      setError(err.data?.message || err.message || "No fue posible eliminar la persona.");
    }
  }

  return (
    <div className="min-h-screen bg-neutral-950 text-white px-4 md:px-6 py-4 font-body">
      <div className="max-w-6xl mx-auto space-y-3">
        {/* Encabezado compacto */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-neutral-800 pb-3">
          <div>
            <h1 className="text-xl font-bold text-white tracking-tight">Personas Registradas</h1>
            <p className="text-neutral-400 text-xs mt-0.5">
              Directorio general de alumnos y miembros
              {activeTotal !== null && ` (${activeTotal} usuarios activos)`}
              {isReadOnly && " · Solo lectura"}
            </p>
          </div>
          {!isReadOnly && (
            <div className="flex items-center gap-2">
              <button
                onClick={() => setFormModal({ mode: "add" })}
                className="flex items-center gap-1.5 bg-emerald-500 hover:bg-emerald-400 text-neutral-950 px-3.5 py-1.5 rounded-full text-xs font-extrabold transition-colors shadow-md shadow-emerald-500/20"
              >
                <UserPlus size={14} /> Agregar Persona
              </button>
            </div>
          )}
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
              No se encontraron personas registradas con el filtro ingresado.
            </div>
          ) : (
            <div className="divide-y divide-neutral-800/60">
              {people.map((person) => (
                <PersonRow
                  key={person.id}
                  person={person}
                  readOnly={isReadOnly}
                  canHardDelete={isAdmin}
                  isDeactivating={confirmDeactivateId === person.id}
                  onEdit={() => setFormModal({ mode: "edit", person })}
                  onDeactivateRequest={() => setConfirmDeactivateId(person.id)}
                  onDeactivateConfirm={handleConfirmDeactivate}
                  onDeactivateCancel={() => setConfirmDeactivateId(null)}
                  onReactivate={() => handleReactivate(person.id)}
                  onHardDeleteRequest={() => setHardDeleteTarget(person)}
                />
              ))}
            </div>
          )}
        </div>
        {pageInfo.totalPages > 1 && (
          <div className="flex justify-center gap-3 text-sm">
            <button disabled={page === 0} onClick={() => setPage((current) => current - 1)} className="px-3 py-2 rounded-lg bg-neutral-800 disabled:opacity-40">Anterior</button>
            <span className="py-2 text-neutral-400">Página {page + 1} de {pageInfo.totalPages}</span>
            <button disabled={page + 1 >= pageInfo.totalPages} onClick={() => setPage((current) => current + 1)} className="px-3 py-2 rounded-lg bg-neutral-800 disabled:opacity-40">Siguiente</button>
          </div>
        )}
      </div>

      {/* Modales */}
      {formModal && (
        <PersonFormModal
          mode={formModal.mode}
          person={formModal.person}
          onSave={handleSavePerson}
          onClose={() => setFormModal(null)}
        />
      )}

      {hardDeleteTarget && (
        <HardDeleteModal
          person={hardDeleteTarget}
          onConfirm={handleConfirmHardDelete}
          onClose={() => setHardDeleteTarget(null)}
        />
      )}
    </div>
  );
}
