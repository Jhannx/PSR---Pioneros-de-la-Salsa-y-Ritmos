import { useState, useEffect } from "react";
import { Plus, X } from "lucide-react";
import { formatCOP } from "../../utils/currency";
import { formatHeaderDateShort as formatHeaderDate, formatTimeOfDay } from "../../utils/date";
import { DAILY_RATE as FALLBACK_RATE, getUpcomingDays } from "../../features/reception/utils";
import QueueRow from "../../features/reception/components/QueueRow";
import RefundModal from "../../features/reception/components/RefundModal";
import { useMockData } from "../../context/DataContext";
import { paymentsService } from "../../services/paymentsService";
import { attendanceService } from "../../services/attendanceService";
import { queueService } from "../../services/queueService";

const FILTERS = [
  { key: "all", label: "TODOS" },
  { key: "pending", label: "SIN ATENDER" },
  { key: "balance", label: "SALDO PENDIENTE" },
  { key: "settled", label: "AL DÍA" },
];

export default function ReceptionCheckinPage() {
  const { queue, sessions, updateQueueItem, refreshData, refreshQueue } = useMockData();
  const [activeFilter, setActiveFilter] = useState("all");
  const [dailyRate, setDailyRate] = useState(FALLBACK_RATE);
  const [paymentMethods, setPaymentMethods] = useState([]);

  const [showAddModal, setShowAddModal] = useState(false);
  const [newDocumento, setNewDocumento] = useState("");
  const [addingManual, setAddingManual] = useState(false);
  const [addManualError, setAddManualError] = useState("");

  const [advanceModalId, setAdvanceModalId] = useState(null);
  const [selectedDays, setSelectedDays] = useState([0]);
  // Se guarda el `checkinId`, no el objeto de la fila (docs/correcciones-pendientes-18-09.md #6):
  // `queue` se reemplaza entero por un array nuevo en cada `refreshQueue()`, así que un objeto
  // capturado en el momento del clic queda congelado con el saldo de ESE instante. Guardando solo
  // el id, `refundTarget` (abajo) se recalcula contra la cola actual en cada render — el modal
  // siempre muestra el saldo real, incluso si cambió mientras estaba abierto.
  const [refundCheckinId, setRefundCheckinId] = useState(null);
  const [actionError, setActionError] = useState("");
  const [actionSuccess, setActionSuccess] = useState("");

  // Autodescarte de los avisos de la barra de acciones — antes el de error se quedaba pegado para
  // siempre y no tenía ni una "X" para cerrarlo (mismo punto del reporte).
  useEffect(() => {
    if (!actionError) return;
    const timer = setTimeout(() => setActionError(""), 8000);
    return () => clearTimeout(timer);
  }, [actionError]);

  useEffect(() => {
    if (!actionSuccess) return;
    const timer = setTimeout(() => setActionSuccess(""), 4000);
    return () => clearTimeout(timer);
  }, [actionSuccess]);

  useEffect(() => {
    async function loadData() {
      try {
        const [pricingRes, methodsRes] = await Promise.allSettled([
          paymentsService.getPricing(),
          paymentsService.getPaymentMethods(),
        ]);

        // `PricingResponse.amount` (openapi.md) — leía `.monto`, un campo que el backend nunca
        // manda, así que esta condición era siempre falsa y la pantalla se quedaba callada con
        // `FALLBACK_RATE` para siempre, aunque el fetch sí se hacía (errores-12sep-front.md #7).
        if (pricingRes.status === "fulfilled" && pricingRes.value?.amount) {
          setDailyRate(Number(pricingRes.value.amount));
        }

        if (methodsRes.status === "fulfilled" && Array.isArray(methodsRes.value)) {
          setPaymentMethods(methodsRes.value);
        }
      } catch (err) {
        console.warn("No se cargaron datos de tarifa/métodos desde la API:", err);
      }
    }
    loadData();
  }, []);

  const totalCollected = queue.reduce((sum, p) => sum + Number(p.amountCharged || 0), 0);

  // "No se que estoy cobrando, ni a que sesión le estoy asignando el pago" (problemas-finales,
  // 13-sep) — `TodayQueueViewResponse.sessionId` ya viaja en cada fila, solo faltaba mostrarlo.
  // `sessions` puede no tener cargada la del checkin todavía (recién creada por adelantado, por
  // ejemplo) — en ese caso se muestra el id crudo en vez de romper.
  const sessionsById = new Map(sessions.map((s) => [s.id, s]));
  function sessionLabel(sessionId) {
    const session = sessionsById.get(sessionId);
    if (!session) return sessionId ? `Sesión #${sessionId}` : "Sin sesión";
    return `${formatTimeOfDay(session.startTime)} - ${formatTimeOfDay(session.endTime)}`;
  }

  const filteredQueue = queue.filter((item) => {
    const balance = Number(item.balance || 0);
    if (activeFilter === "pending") return !item.attended;
    if (activeFilter === "balance") return balance !== 0;
    if (activeFilter === "settled") return balance === 0;
    return true;
  });

  const counts = {
    all: queue.length,
    pending: queue.filter((p) => !p.attended).length,
    balance: queue.filter((p) => Number(p.balance || 0) !== 0).length,
    settled: queue.filter((p) => Number(p.balance || 0) === 0).length,
  };

  // Reemplaza `handleAmountChange` + `handleSettle` + `handleDeliverChange` de antes — esos tres
  // manipulaban `item.balance`/`amountCharged` a mano, pisando el saldo REAL de la cuenta que
  // manda el backend (`vista_saldo_persona`, un crédito acumulado de toda la vida, no algo "de
  // hoy"). Ahora una sola acción registra el cobro real (`POST /api/movimientos`, que el backend ya
  // soporta con `monto_recibido > monto_abonado` sin cambios de su lado — confirmado en
  // `sp_registrar_movimiento`), y el saldo mostrado se refresca solo vía WebSocket/REST, nunca se
  // calcula en el front.
  //
  // `mode`:
  //  - "change": aplica como máximo la tarifa del día; cualquier excedente se entrega como vuelto
  //    de inmediato (nunca queda pendiente).
  //  - "advance": aplica tantos días completos como el monto alcance a cubrir (tarifa de hoy +
  //    días por adelantado); lo que sobra sin llegar a cubrir un día más también se entrega como
  //    vuelto de inmediato — decisión del 13-sep: "dejar sesiones pagas es pagarlas por completo".
  async function handleCharge(checkinId, amount, mode = "change") {
    const item = queue.find((p) => p.checkinId === checkinId);
    if (!item || !item.personId || !(amount > 0)) return;
    setActionError("");
    try {
      const defaultMethodId = paymentMethods[0]?.id || 1;
      const daysCovered = mode === "advance" ? Math.max(1, Math.floor(amount / dailyRate)) : 1;
      const amountApplied = Math.min(amount, daysCovered * dailyRate);
      const remainder = amount - amountApplied;
      const note =
        daysCovered > 1
          ? `Cobro de entrada + ${daysCovered - 1} día(s) por adelantado`
          : "Cobro de entrada en recepción";

      const movement = await paymentsService.createMovement({
        personId: item.personId,
        checkinId,
        paymentMethodId: defaultMethodId,
        amountReceived: amount,
        amountApplied,
        note,
      });

      if (remainder > 0 && movement?.id) {
        await paymentsService.deliverChange(movement.id);
      }

      await refreshQueue();
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo registrar el cobro.");
    }
  }

  async function handleConfirmAttendance(checkinId) {
    setActionError("");
    try {
      const attendance = await attendanceService.confirm(checkinId);
      updateQueueItem(checkinId, {
        attended: true,
        attendanceId: attendance?.attendanceId ?? null,
        markedAt: attendance?.markedAt ?? new Date().toISOString(),
      });
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo confirmar la asistencia.");
    }
  }

  async function handleUndoAttendance(checkinId) {
    setActionError("");
    try {
      await attendanceService.cancel(checkinId);
      updateQueueItem(checkinId, {
        attended: false,
        attendanceId: null,
        markedAt: null,
      });
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo anular la asistencia.");
    }
  }

  async function handleAddManualEntry() {
    const documento = newDocumento.trim();
    if (!documento) return;
    setAddingManual(true);
    setAddManualError("");
    try {
      await queueService.checkinByDocument(documento);
      await refreshData();
      setNewDocumento("");
      setShowAddModal(false);
    } catch (err) {
      setAddManualError(err.data?.message || err.message || "No se pudo registrar el check-in.");
    } finally {
      setAddingManual(false);
    }
  }

  async function handleConfirmAdvancePayment() {
    if (!advanceModalId) return;
    const days = selectedDays.length || 1;
    const totalAdvance = dailyRate * days;
    const item = queue.find((p) => p.checkinId === advanceModalId);
    if (!item || !item.personId) return;

    setActionError("");
    try {
      const defaultMethodId = paymentMethods[0]?.id || 1;
      await paymentsService.createMovement({
        personId: item.personId,
        checkinId: advanceModalId,
        paymentMethodId: defaultMethodId,
        amountReceived: totalAdvance,
        amountApplied: totalAdvance,
        note: `Pago por adelantado (${days} días)`,
      });
      // Antes esto también adivinaba `amountCharged`/`balance` en local — mismo defecto que
      // `handleCharge`/`handleConfirmRefund` ya corrigen: el saldo real llega solo por
      // WebSocket/REST, nunca se recalcula a mano (era parte de por qué el saldo de $40.500 del
      // reporte de hoy no cuadraba con lo que el reembolso veía).
      await refreshQueue();
      setAdvanceModalId(null);
      setSelectedDays([0]);
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo registrar el pago por adelantado.");
    }
  }

  // `POST /api/personas/{id}/reembolso` (RESPUESTA-NOTAS-FINAL.md #1) reemplaza todo el parche
  // anterior: antes había que pedir los movimientos del checkin, calcular a mano cuánto quedaba
  // pendiente en cada uno, y encadenar una llamada por abono — lógica de negocio que vivía en el
  // front sin necesidad. Ahora el backend recibe un monto total y decide él mismo el reparto
  // (FIFO, todo o nada, en una sola transacción); si el monto pedido no alcanza, ni siquiera
  // aplica nada y responde 400 con lo disponible en el mensaje. El tope que se muestra en el modal
  // es directamente el saldo real de la cuenta (`item.balance`, cuando es positivo = "a favor") —
  // es la misma bolsa de dinero que el backend usa para decidir "cuánto le queda pendiente por
  // devolver".
  function handleOpenRefund(item) {
    setActionError("");
    setActionSuccess("");
    setRefundCheckinId(item.checkinId);
  }

  async function handleConfirmRefund(amount, selectedMethodId) {
    if (!refundTarget) return;
    setActionError("");
    try {
      await paymentsService.refundPerson(refundTarget.personId, {
        amount,
        paymentMethodId: selectedMethodId,
        note: "Reembolso solicitado en recepción",
      });
      // El saldo real llega solo, vía WebSocket/REST — nunca se recalcula a mano en el front.
      await refreshQueue();
      setActionSuccess(`Reembolso de ${formatCOP(amount)} registrado correctamente.`);
      setRefundCheckinId(null);
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo procesar el reembolso.");
    }
  }

  const advancePerson = queue.find((p) => p.checkinId === advanceModalId);
  // Derivado de `queue` en cada render, no un objeto capturado al abrir el modal — ver el
  // comentario de `refundCheckinId` más arriba.
  const refundTarget = refundCheckinId ? queue.find((p) => p.checkinId === refundCheckinId) : null;
  const upcomingDays = getUpcomingDays(10);

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-4 md:px-6 py-4 font-body">
      <div className="max-w-6xl mx-auto space-y-3">
        {/* Encabezado compacto */}
        <div className="flex items-center justify-between gap-4 border-b border-neutral-800 pb-3 flex-wrap">
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold text-white tracking-tight">Aeróbicos</h1>
              <span className="text-xs font-semibold text-neutral-500">
                · {formatHeaderDate(new Date())}
              </span>
            </div>
            <p className="text-neutral-400 text-xs mt-0.5">
              Tarifa día: <span className="text-white font-bold">{formatCOP(dailyRate)}</span>
              <span className="mx-2 text-neutral-700">|</span>
              Recaudado hoy: <span className="text-emerald-400 font-bold">{formatCOP(totalCollected)}</span>
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowAddModal(true)}
              className="flex items-center gap-1.5 bg-white hover:bg-neutral-200 text-neutral-950 text-xs font-bold px-3.5 py-1.5 rounded-full transition-colors shadow-md"
            >
              <Plus size={14} />
              + Agregar Manual
            </button>
          </div>
        </div>

        {/* Con "X" para cerrar y descarte automático a los 8s — antes se quedaba pegado en
            pantalla sin forma de quitarlo (docs/correcciones-pendientes-18-09.md #6). */}
        {actionError && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm rounded-xl flex items-center justify-between gap-3">
            <span>{actionError}</span>
            <button
              onClick={() => setActionError("")}
              className="text-rose-400/70 hover:text-rose-300 shrink-0"
              aria-label="Cerrar aviso"
            >
              <X size={16} />
            </button>
          </div>
        )}

        {actionSuccess && (
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm rounded-xl flex items-center justify-between gap-3">
            <span>{actionSuccess}</span>
            <button
              onClick={() => setActionSuccess("")}
              className="text-emerald-400/70 hover:text-emerald-300 shrink-0"
              aria-label="Cerrar aviso"
            >
              <X size={16} />
            </button>
          </div>
        )}

        {/* Pastillas de filtro */}
        <div className="flex flex-wrap gap-1.5">
          {FILTERS.map((f) => {
            const active = activeFilter === f.key;
            return (
              <button
                key={f.key}
                onClick={() => setActiveFilter(f.key)}
                className={`flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold transition-colors border ${
                  active
                    ? "bg-white text-neutral-950 border-white"
                    : "bg-neutral-900 text-neutral-400 border-neutral-800 hover:border-neutral-700"
                }`}
              >
                <span>{f.label}</span>
                <span
                  className={`text-[10px] px-1.5 py-0.2 rounded-full ${
                    active ? "bg-neutral-950/10 text-neutral-950 font-extrabold" : "bg-neutral-800 text-neutral-400"
                  }`}
                >
                  ({counts[f.key]})
                </span>
              </button>
            );
          })}
        </div>

        {/* Lista de Fila de Recepción */}
        <div className="flex flex-col gap-2.5">
          {/* Encabezado de columnas — antes la lista no tenía ninguno y no se distinguía qué era
              cada campo sin pasar el mouse (errores-13sep-front.md #4). Oculto por debajo de `lg`
              porque QueueRow ya repite cada etiqueta ahí (`block lg:hidden`) en su tarjeta
              apilada. El quiebre es `lg` (1024px) y no `sm` (640px): las cinco columnas de
              QueueRow en fila necesitan ~738px mínimo, y entre 640-1024px el sidebar (240px desde
              `md`) deja menos que eso — la fila se salía de la pantalla sin poder desplazarse
              (`overflow-x-hidden` en AppLayout), la columna de Asistencia quedaba inalcanzable
              (docs/correcciones-pendientes-18-09.md #5). */}
          {filteredQueue.length > 0 && (
            <div className="hidden lg:flex items-center gap-3 px-4 text-[10px] font-semibold uppercase tracking-wider text-neutral-500">
              <span className="w-6 text-center shrink-0">#</span>
              <span className="flex-1 min-w-0">Nombre / Fuente</span>
              <span className="w-40 shrink-0">Monto a Cobrar</span>
              <span className="w-40 shrink-0">Saldo</span>
              <span className="w-36 shrink-0 text-right">Asistencia</span>
            </div>
          )}
          {filteredQueue.length === 0 ? (
            <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-8 text-center text-neutral-500 text-sm">
              No hay personas registradas en la fila de recepción con los filtros seleccionados.
            </div>
          ) : (
            filteredQueue.map((item, index) => (
              <QueueRow
                key={item.checkinId || index}
                item={item}
                position={index + 1}
                dailyRate={dailyRate}
                sessionLabel={sessionLabel(item.sessionId)}
                onCharge={(amount, mode) => handleCharge(item.checkinId, amount, mode)}
                onConfirmAttendance={() => handleConfirmAttendance(item.checkinId)}
                onUndoAttendance={() => handleUndoAttendance(item.checkinId)}
                onOpenAdvance={() => {
                  setAdvanceModalId(item.checkinId);
                  setSelectedDays([0]);
                }}
                onOpenRefund={() => handleOpenRefund(item)}
              />
            ))
          )}
        </div>
      </div>

      {/* Modal Agregar Manualmente */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 w-full max-w-sm">
            <h3 className="text-lg font-bold text-white mb-1">Agregar a la fila</h3>
            <p className="text-neutral-500 text-xs mb-4">
              La persona debe estar registrada previamente. Se busca por su número de documento.
            </p>
            <div className="space-y-3 mb-4">
              <input
                type="text"
                inputMode="numeric"
                autoFocus
                value={newDocumento}
                onChange={(e) => setNewDocumento(e.target.value.replace(/\D/g, ""))}
                placeholder="Número de documento"
                className="w-full bg-neutral-950 border border-neutral-800 rounded-xl px-4 py-3 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors text-sm"
              />
            </div>
            {addManualError && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl">
                {addManualError}
              </div>
            )}
            <div className="flex justify-end gap-2">
              <button
                onClick={() => {
                  setShowAddModal(false);
                  setAddManualError("");
                }}
                className="px-4 py-2 text-neutral-400 hover:text-white text-sm"
              >
                Cancelar
              </button>
              <button
                onClick={handleAddManualEntry}
                disabled={!newDocumento.trim() || addingManual}
                className="bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold text-sm px-4 py-2 rounded-xl transition-colors"
              >
                {addingManual ? "Agregando..." : "Agregar"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal Pago por Adelantado */}
      {advanceModalId && advancePerson && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 w-full max-w-md">
            <h3 className="text-lg font-bold text-white mb-1 font-display">Pago por Adelantado</h3>
            <p className="text-neutral-400 text-sm mb-4">
              Registrar sesiones futuras para{" "}
              <span className="text-white font-bold">{`${advancePerson.firstName || ""} ${advancePerson.lastName || ""}`}</span>
            </p>

            <div className="grid grid-cols-2 gap-2 mb-6 max-h-48 overflow-y-auto">
              {upcomingDays.map((day, idx) => {
                const selected = selectedDays.includes(idx);
                return (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      setSelectedDays((prev) =>
                        prev.includes(idx) ? prev.filter((d) => d !== idx) : [...prev, idx].sort((a, b) => a - b)
                      );
                    }}
                    className={`p-2.5 rounded-xl border text-left text-xs transition-colors ${
                      selected
                        ? "bg-emerald-500/10 border-emerald-500/40 text-emerald-400 font-semibold"
                        : "bg-neutral-950 border-neutral-800 text-neutral-400 hover:border-neutral-700"
                    }`}
                  >
                    {day.label}
                  </button>
                );
              })}
            </div>

            <div className="flex items-center justify-between border-t border-neutral-800 pt-4">
              <div>
                <span className="text-neutral-500 text-xs block">Total a registrar:</span>
                <span className="text-emerald-400 font-bold text-lg">
                  {formatCOP(dailyRate * (selectedDays.length || 1))}
                </span>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => setAdvanceModalId(null)}
                  className="px-4 py-2 text-neutral-400 hover:text-white text-sm"
                >
                  Cancelar
                </button>
                <button
                  onClick={handleConfirmAdvancePayment}
                  className="bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold text-sm px-4 py-2 rounded-xl transition-colors"
                >
                  Confirmar Pago
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Modal Reembolso */}
      {refundTarget && (
        <RefundModal
          person={{
            name: `${refundTarget.firstName || ""} ${refundTarget.lastName || ""}`,
            // `refundable`, no `balance` (NOTAS-PARA-FRONTEND.md Bloque 2, A5) — es el mismo
            // número que usa la validación de `POST /api/personas/{id}/reembolso` del lado del
            // servidor. `balance` descuenta lo ya consumido en asistencias, `refundable` no; usar
            // `balance` acá era justo la causa del 400 "la persona solo tiene X pendiente de
            // devolver" con un tope que parecía correcto pero no lo era.
            amount: Math.max(0, Number(refundTarget.refundable ?? refundTarget.balance ?? 0)),
          }}
          onConfirm={handleConfirmRefund}
          onClose={() => {
            setRefundCheckinId(null);
            setActionError("");
          }}
          error={actionError}
        />
      )}
    </div>
  );
}
