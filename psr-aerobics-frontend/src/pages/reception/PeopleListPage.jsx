import { useState } from "react";
import { UserPlus, Search, QrCode } from "lucide-react";
import { daysAgo, randomPhone, randomDocumento } from "../../features/reception/utils";
import PersonRow from "../../features/reception/components/PersonRow";
import PersonFormModal from "../../features/reception/components/PersonFormModal";
import HardDeleteModal from "../../features/reception/components/HardDeleteModal";

// Initial mock data — sin aprobación: todo el mundo que aparece aquí ya está activo,
// sin importar si se registró por QR (autoregistro) o lo agregó el admin (manual).
//
// Nota: tipo de sangre y teléfono de emergencia se quitaron — esos datos
// ahora viven en un paso/pantalla aparte, pendiente de decidir el formato
// final. documento es nuevo: toda persona registrada crea una cuenta con
// contraseña por defecto = documento (RF-01), así que ahora es obligatorio.
const initialPeople = [
  { id: 1, firstName: "Laura", lastName: "Gómez Restrepo", documento: "1001234567", phone: "3001234567", source: "manual", registeredAt: daysAgo(40) },
  { id: 2, firstName: "Camilo", lastName: "Pérez Londoño", documento: "1012345678", phone: "3012345678", source: "qr", registeredAt: daysAgo(25) },
  { id: 3, firstName: "Valentina", lastName: "Suárez Duque", documento: "1023456789", phone: "3023456789", source: "manual", registeredAt: daysAgo(12) },
  { id: 4, firstName: "Santiago", lastName: "Herrera Cano", documento: "1034567890", phone: "3034567890", source: "qr", registeredAt: daysAgo(5) },
  { id: 5, firstName: "Mariana", lastName: "Duque Salazar", documento: "1056789012", phone: "3056789012", source: "qr", registeredAt: daysAgo(1) },
];

// Pool usado solo para simular autoregistros entrando por QR — caen directo a la
// lista, sin ningún paso intermedio.
const REQUEST_POOL = [
  { firstName: "Isabella", lastName: "Correa Muñoz" },
  { firstName: "Tomás", lastName: "Vargas Peña" },
  { firstName: "Manuela", lastName: "Zapata Ríos" },
  { firstName: "Sebastián", lastName: "Londoño Cárdenas" },
];


export default function PeopleListPage() {
  const [people, setPeople] = useState(initialPeople);
  const [nextId, setNextId] = useState(initialPeople.length + 1);
  const [requestPoolIndex, setRequestPoolIndex] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");

  const [formModal, setFormModal] = useState(null); // null | { mode: 'add' } | { mode: 'edit', person }
  const [confirmDeactivateId, setConfirmDeactivateId] = useState(null);
  const [hardDeleteTarget, setHardDeleteTarget] = useState(null); // null | person

  const visiblePeople = people
    .filter((p) => {
      const q = searchQuery.trim().toLowerCase();
      if (!q) return true;
      return `${p.firstName} ${p.lastName} ${p.documento}`.toLowerCase().includes(q);
    })
    .sort((a, b) => a.lastName.localeCompare(b.lastName));

  function savePerson(data) {
    if (formModal?.mode === "edit") {
      setPeople((prev) => prev.map((p) => (p.id === formModal.person.id ? { ...p, ...data } : p)));
    } else {
      setPeople((prev) => [
        ...prev,
        { id: nextId, ...data, source: "manual", registeredAt: new Date().toISOString() },
      ]);
      setNextId((n) => n + 1);
    }
    setFormModal(null);
  }

  // Desactivar = soft-delete reversible ("dejó la academia"). Esto es lo
  // que antes se llamaba simplemente "Eliminar" — se renombra para no
  // confundirlo con la eliminación definitiva de abajo.
  function confirmDeactivateYes() {
    if (!confirmDeactivateId) return;
    setPeople((prev) => prev.filter((p) => p.id !== confirmDeactivateId));
    setConfirmDeactivateId(null);
  }

  // Eliminación definitiva (RF-08) — irreversible. El nombre, documento y
  // teléfono se van de verdad; el historial de asistencias/pagos se
  // conserva, ya sin ningún dato que identifique a la persona.
  function confirmHardDelete() {
    if (!hardDeleteTarget) return;
    setPeople((prev) => prev.filter((p) => p.id !== hardDeleteTarget.id));
    setHardDeleteTarget(null);
  }

  function simulateQrSelfRegistration() {
    const template = REQUEST_POOL[requestPoolIndex % REQUEST_POOL.length];
    setRequestPoolIndex((i) => i + 1);
    setPeople((prev) => [
      ...prev,
      {
        id: nextId,
        firstName: template.firstName,
        lastName: template.lastName,
        documento: randomDocumento(),
        phone: randomPhone(),
        source: "qr",
        registeredAt: new Date().toISOString(),
      },
    ]);
    setNextId((n) => n + 1);
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-6 py-8">
      <div className="max-w-4xl mx-auto">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 mb-6 flex-wrap">
          <div>
            <h1 className="text-2xl font-bold text-white">Personas registradas</h1>
            <p className="text-neutral-500 text-sm mt-1">{people.length} en total</p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={simulateQrSelfRegistration}
              className="flex items-center gap-2 border border-neutral-700 text-neutral-300 hover:bg-neutral-800 font-semibold text-sm px-4 py-2.5 rounded-xl transition-colors"
            >
              <QrCode size={16} />
              Simular autoregistro QR
            </button>
            <button
              onClick={() => setFormModal({ mode: "add" })}
              className="flex items-center gap-2 bg-white text-neutral-950 hover:bg-neutral-200 font-semibold text-sm px-4 py-2.5 rounded-xl transition-colors"
            >
              <UserPlus size={16} />
              Registrar manual
            </button>
          </div>
        </div>

        {/* Search */}
        <div className="relative mb-4">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-600" />
          <input
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Buscar por nombre o documento..."
            className="w-full bg-neutral-900 border border-neutral-800 rounded-xl pl-9 pr-3 py-2.5 text-white text-sm placeholder-neutral-600 outline-none focus:border-emerald-500"
          />
        </div>

        {/* List */}
        <div className="flex flex-col gap-2">
          {visiblePeople.map((person) => (
            <PersonRow
              key={person.id}
              person={person}
              isConfirmingDeactivate={confirmDeactivateId === person.id}
              onEdit={() => setFormModal({ mode: "edit", person })}
              onDeactivateClick={() => setConfirmDeactivateId(person.id)}
              onConfirmDeactivateYes={confirmDeactivateYes}
              onConfirmDeactivateCancel={() => setConfirmDeactivateId(null)}
              onHardDeleteClick={() => setHardDeleteTarget(person)}
            />
          ))}
          {visiblePeople.length === 0 && (
            <div className="text-center py-10 text-neutral-600 text-sm">No hay personas registradas todavía.</div>
          )}
        </div>
      </div>

      {formModal && (
        <PersonFormModal
          key={formModal.mode === "edit" ? formModal.person.id : "add"}
          title={formModal.mode === "edit" ? "Editar persona" : "Registrar manual"}
          initialData={formModal.mode === "edit" ? formModal.person : null}
          onSave={savePerson}
          onClose={() => setFormModal(null)}
        />
      )}

      {hardDeleteTarget && (
        <HardDeleteModal
          person={hardDeleteTarget}
          onConfirm={confirmHardDelete}
          onClose={() => setHardDeleteTarget(null)}
        />
      )}
    </div>
  );
}
