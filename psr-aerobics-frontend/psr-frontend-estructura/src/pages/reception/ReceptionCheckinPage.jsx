import { useState } from "react";
import { Plus, QrCode, CalendarDays, X } from "lucide-react";
import { formatCOP } from "../../utils/currency";
import { formatHeaderDateShort as formatHeaderDate } from "../../utils/date";
import { DAILY_RATE, matchesFilter, computeCounts, getUpcomingDays } from "../../features/reception/utils";
import QueueRow from "../../features/reception/components/QueueRow";
import RefundModal from "../../features/reception/components/RefundModal";

// Initial mock queue, in scan order — this order IS the line.
//
// Nota de rediseño: "atendido" (confirmar asistencia) y "amount" (dinero
// registrado) son ahora completamente independientes — antes un solo
// "status: paid" mezclaba las dos cosas. Por eso "amount" ya NO empieza
// en DAILY_RATE por defecto: empieza en 0, porque nadie ha pagado nada
// todavía hasta que el admin lo registre. Que alguien deba $4.000 desde
// que llega es la realidad honesta, no un error.
const initialQueue = [
  { id: 1, name: "Juan Martínez", source: "qr", atendido: false, amount: 0, note: null },
  { id: 2, name: "María López", source: "qr", atendido: true, amount: 4000, note: null },
  { id: 3, name: "Carlos Ruiz", source: "qr", atendido: true, amount: 2000, note: null },
  { id: 4, name: "Ana Torres", source: "qr", atendido: true, amount: 50000, note: null },
  { id: 5, name: "Sofía Ramírez", source: "qr", atendido: false, amount: 4000, note: "Pagó 4 días por adelantado" },
  { id: 6, name: "Pedro Gómez", source: "manual", atendido: false, amount: 0, note: null },
];

// Pool of names used only to simulate incoming QR scans during a live demo
const SCAN_NAME_POOL = [
  "Luisa Fernanda Rojas",
  "Camilo Restrepo",
  "Diana Marcela Ortiz",
  "Andrés Felipe Cano",
  "Paola Jiménez",
  "Jorge Iván Castaño",
];

const FILTERS = [
  { key: "all", label: "Todos" },
  { key: "pending", label: "Sin atender" },
  { key: "balance", label: "Con saldo" },
  { key: "settled", label: "Al día" },
];

export default function ReceptionCheckinPage() {
  const [queue, setQueue] = useState(initialQueue);
  const [nextId, setNextId] = useState(initialQueue.length + 1);
  const [scanPoolIndex, setScanPoolIndex] = useState(0);
  const [activeFilter, setActiveFilter] = useState("all");

  const [showAddModal, setShowAddModal] = useState(false);
  const [newName, setNewName] = useState("");

  const [advanceModalId, setAdvanceModalId] = useState(null);
  const [selectedDays, setSelectedDays] = useState([0]);

  const [refundTarget, setRefundTarget] = useState(null); // null | person

  const totalCollected = queue.reduce((sum, p) => sum + Number(p.amount || 0), 0);

  const counts = computeCounts(queue);
  const filteredQueue = queue.filter((p) => matchesFilter(p, activeFilter));

  function updateAmount(id, value) {
    setQueue((prev) => prev.map((p) => (p.id === id ? { ...p, amount: value } : p)));
  }

  function settleBalance(id) {
    // "Completar pago" / "Entregar vuelto" — ajusta el monto registrado
    // para que el saldo quede en $0. No toca "atendido" para nada.
    setQueue((prev) => prev.map((p) => (p.id === id ? { ...p, amount: DAILY_RATE } : p)));
  }

  // Confirmar asistencia — acción propia, sin relación con el pago.
  // Corresponde a sp_registrar_asistencia: crea la confirmación, cobra
  // la tarifa vigente. No exige que "amount" ya esté en DAILY_RATE.
  function confirmarAsistencia(id) {
    setQueue((prev) => prev.map((p) => (p.id === id ? { ...p, atendido: true } : p)));
  }

  // Anular — corresponde a sp_anular_asistencia: borra la confirmación
  // por completo. El monto pagado NO se toca, sigue como estaba.
  function anularAsistencia(id) {
    setQueue((prev) => prev.map((p) => (p.id === id ? { ...p, atendido: false } : p)));
  }

  function addManualEntry() {
    const trimmed = newName.trim();
    if (!trimmed) return;
    setQueue((prev) => [
      ...prev,
      { id: nextId, name: trimmed, source: "manual", atendido: false, amount: 0, note: null },
    ]);
    setNextId((n) => n + 1);
    setNewName("");
    setShowAddModal(false);
  }

  function simulateScan() {
    const name = SCAN_NAME_POOL[scanPoolIndex % SCAN_NAME_POOL.length];
    setScanPoolIndex((i) => i + 1);
    setQueue((prev) => [
      ...prev,
      { id: nextId, name, source: "qr", atendido: false, amount: 0, note: null },
    ]);
    setNextId((n) => n + 1);
  }

  function openAdvanceModal(id) {
    setAdvanceModalId(id);
    setSelectedDays([0]);
  }

  function toggleDay(index) {
    setSelectedDays((prev) =>
      prev.includes(index) ? prev.filter((d) => d !== index) : [...prev, index].sort((a, b) => a - b)
    );
  }

  // Pago por adelantado — SOLO registra el pago (sp_registrar_movimiento).
  // A propósito no toca "atendido": pagar por adelantado no es lo mismo
  // que estar aquí hoy. La persona puede estar pagando días futuros sin
  // siquiera haber llegado todavía.
  function confirmAdvancePayment() {
    const days = selectedDays.length || 1;
    setQueue((prev) =>
      prev.map((p) =>
        p.id === advanceModalId
          ? { ...p, amount: DAILY_RATE, note: `Pagó ${days} día${days > 1 ? "s" : ""} por adelantado` }
          : p
      )
    );
    setAdvanceModalId(null);
    setSelectedDays([0]);
  }

  // Reembolso — corresponde a sp_registrar_reembolso: dinero que entró y
  // sale de verdad, distinto de corregir un monto mal digitado. Reduce
  // lo abonado, sin borrar el pago original (aquí simplificado a solo
  // bajar "amount", ya que el mock no modela movimientos por separado).
  function confirmRefund(amount) {
    if (!refundTarget) return;
    setQueue((prev) =>
      prev.map((p) =>
        p.id === refundTarget.id ? { ...p, amount: Math.max(0, Number(p.amount || 0) - amount) } : p
      )
    );
    setRefundTarget(null);
  }

  const advancePerson = queue.find((p) => p.id === advanceModalId);
  const upcomingDays = getUpcomingDays(10);

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-6 py-8">
      <div className="max-w-5xl mx-auto">
        {/* Header */}
        <div className="flex items-start justify-between gap-4 mb-6 flex-wrap">
          <div>
            <h1 className="text-2xl font-bold text-white">Aeróbicos · {formatHeaderDate(new Date())}</h1>
            <p className="text-neutral-500 text-sm mt-1">
              Tarifa del día: <span className="text-neutral-300 font-semibold">{formatCOP(DAILY_RATE)}</span>
              <span className="mx-2 text-neutral-700">·</span>
              Recaudado hoy: <span className="text-emerald-400 font-semibold">{formatCOP(totalCollected)}</span>
            </p>
          </div>
          <div className="flex gap-2">
            <button
              onClick={simulateScan}
              className="flex items-center gap-2 border border-neutral-700 text-neutral-300 hover:bg-neutral-800 font-semibold text-sm px-4 py-2.5 rounded-xl transition-colors"
            >
              <QrCode size={16} />
              Simular escaneo
            </button>
            <button
              onClick={() => setShowAddModal(true)}
              className="flex items-center gap-2 bg-white text-neutral-950 hover:bg-neutral-200 font-semibold text-sm px-4 py-2.5 rounded-xl transition-colors"
            >
              <Plus size={16} />
              Agregar sin QR
            </button>
          </div>
        </div>

        {/* Filters: change what's visible, never the underlying scan order */}
        <div className="flex flex-wrap gap-2 mb-4">
          {FILTERS.map((f) => {
            const active = activeFilter === f.key;
            return (
              <button
                key={f.key}
                onClick={() => setActiveFilter(f.key)}
                className={`flex items-center gap-2 px-3.5 py-2 rounded-full text-sm font-semibold border transition-colors ${
                  active
                    ? "bg-white text-neutral-950 border-white"
                    : "bg-neutral-900 text-neutral-400 border-neutral-800 hover:border-neutral-700"
                }`}
              >
                {f.label}
                <span
                  className={`text-xs px-1.5 py-0.5 rounded-full ${
                    active ? "bg-neutral-950/10 text-neutral-950" : "bg-neutral-800 text-neutral-500"
                  }`}
                >
                  {counts[f.key]}
                </span>
              </button>
            );
          })}
        </div>

        {/* Column labels */}
        <div className="flex items-center gap-4 px-4 pb-2 text-xs font-semibold text-neutral-600 uppercase tracking-wide">
          <span className="w-6 text-center">#</span>
          <span className="flex-1">Persona</span>
          <span className="w-32">Monto pagado</span>
          <span className="w-36 text-right">Saldo</span>
          <span className="w-36 text-right">Asistencia</span>
          <span className="w-9"></span>
        </div>

        {/* Queue — position (#) always reflects the true scan order,
            even when a filter is hiding other rows */}
        <div className="flex flex-col gap-2">
          {filteredQueue.map((person) => {
            const position = queue.findIndex((p) => p.id === person.id) + 1;
            return (
              <QueueRow
                key={person.id}
                person={person}
                position={position}
                onAmountChange={(value) => updateAmount(person.id, value)}
                onSettle={() => settleBalance(person.id)}
                onConfirmar={() => confirmarAsistencia(person.id)}
                onAnular={() => anularAsistencia(person.id)}
                onOpenAdvance={() => openAdvanceModal(person.id)}
                onOpenRefund={() => setRefundTarget(person)}
              />
            );
          })}
          {filteredQueue.length === 0 && (
            <div className="text-center py-10 text-neutral-600 text-sm">
              No hay nadie en esta categoría por ahora.
            </div>
          )}
        </div>
      </div>

      {/* Modal: add someone without a phone/QR */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 w-full max-w-sm">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-white font-bold text-lg">Agregar sin QR</h2>
              <button onClick={() => setShowAddModal(false)} className="text-neutral-500 hover:text-white">
                <X size={20} />
              </button>
            </div>
            <label className="text-neutral-400 text-sm font-medium">Nombre completo</label>
            <input
              autoFocus
              value={newName}
              onChange={(e) => setNewName(e.target.value)}
              onKeyDown={(e) => e.key === "Enter" && addManualEntry()}
              placeholder="Ej. Rosa Martínez"
              className="w-full mt-1.5 mb-4 bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500"
            />
            <button
              onClick={addManualEntry}
              className="w-full bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
            >
              Agregar a la fila
            </button>
          </div>
        </div>
      )}

      {/* Modal: pay several days at once — SOLO pago, no toca asistencia */}
      {advanceModalId && advancePerson && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 w-full max-w-sm">
            <div className="flex items-center justify-between mb-1">
              <h2 className="text-white font-bold text-lg">Pago por varios días</h2>
              <button onClick={() => setAdvanceModalId(null)} className="text-neutral-500 hover:text-white">
                <X size={20} />
              </button>
            </div>
            <p className="text-neutral-500 text-sm mb-4">{advancePerson.name}</p>

            <div className="grid grid-cols-5 gap-2 mb-4">
              {upcomingDays.map((day, index) => {
                const selected = selectedDays.includes(index);
                return (
                  <button
                    key={index}
                    onClick={() => toggleDay(index)}
                    className={`flex flex-col items-center py-2 rounded-lg border text-xs font-semibold transition-colors ${
                      selected
                        ? "bg-emerald-500 border-emerald-500 text-neutral-950"
                        : "bg-neutral-800 border-neutral-700 text-neutral-400"
                    }`}
                  >
                    <span>{day.weekday}</span>
                    <span className="text-xs opacity-80">{day.date}</span>
                  </button>
                );
              })}
            </div>

            <div className="flex items-center justify-between text-sm mb-4">
              <span className="text-neutral-500">
                {selectedDays.length} día{selectedDays.length !== 1 ? "s" : ""} seleccionado{selectedDays.length !== 1 ? "s" : ""}
              </span>
              <span className="text-white font-bold">{formatCOP(DAILY_RATE * (selectedDays.length || 1))}</span>
            </div>

            <button
              onClick={confirmAdvancePayment}
              className="w-full bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
            >
              Confirmar pago
            </button>
          </div>
        </div>
      )}

      {/* Modal: reembolso real — dinero que sale, distinto de corregir un monto */}
      {refundTarget && (
        <RefundModal person={refundTarget} onConfirm={confirmRefund} onClose={() => setRefundTarget(null)} />
      )}
    </div>
  );
}
