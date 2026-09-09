import { useState, useEffect } from "react";
import { Plus } from "lucide-react";
import { formatCOP } from "../../utils/currency";
import { formatHeaderDateShort as formatHeaderDate } from "../../utils/date";
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
  const { queue, updateQueueItem, refreshData } = useMockData();
  const [activeFilter, setActiveFilter] = useState("all");
  const [dailyRate, setDailyRate] = useState(FALLBACK_RATE);
  const [paymentMethods, setPaymentMethods] = useState([]);

  const [showAddModal, setShowAddModal] = useState(false);
  const [newDocumento, setNewDocumento] = useState("");
  const [addingManual, setAddingManual] = useState(false);
  const [addManualError, setAddManualError] = useState("");

  const [advanceModalId, setAdvanceModalId] = useState(null);
  const [selectedDays, setSelectedDays] = useState([0]);
  const [refundTarget, setRefundTarget] = useState(null);
  const [actionError, setActionError] = useState("");

  useEffect(() => {
    async function loadData() {
      try {
        const [pricingRes, methodsRes] = await Promise.allSettled([
          paymentsService.getPricing(),
          paymentsService.getPaymentMethods(),
        ]);

        if (pricingRes.status === "fulfilled" && pricingRes.value?.monto) {
          setDailyRate(Number(pricingRes.value.monto));
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

  async function handleAmountChange(checkinId, newAmount) {
    const current = queue.find((p) => p.checkinId === checkinId);
    if (!current) return;
    const newBalance = newAmount - dailyRate;
    updateQueueItem(checkinId, {
      amountCharged: newAmount,
      balance: newBalance,
    });
  }

  async function handleSettle(checkinId) {
    const item = queue.find((p) => p.checkinId === checkinId);
    if (!item || !item.personId) return;
    const owed = Math.abs(Number(item.balance || 0));
    if (owed <= 0) return;
    setActionError("");
    try {
      const defaultMethodId = paymentMethods[0]?.id || 1;
      await paymentsService.createMovement({
        personId: item.personId,
        checkinId,
        paymentMethodId: defaultMethodId,
        amountReceived: owed,
        amountApplied: owed,
        note: "Cobro de entrada en recepción",
      });
      updateQueueItem(checkinId, {
        amountCharged: dailyRate,
        balance: 0.0,
      });
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo registrar el cobro.");
    }
  }

  async function handleDeliverChange(checkinId) {
    setActionError("");
    try {
      const movements = await paymentsService.getMovementsByCheckin(checkinId);
      const paymentMovement = (Array.isArray(movements) ? movements : []).find(
        (m) => m.movementType === "PAYMENT"
      );
      if (!paymentMovement) {
        setActionError("No se encontró el movimiento original de pago para entregar el vuelto.");
        return;
      }
      await paymentsService.deliverChange(paymentMovement.id);
      updateQueueItem(checkinId, { amountCharged: dailyRate, balance: 0.0 });
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo entregar el vuelto.");
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
      updateQueueItem(advanceModalId, {
        amountCharged: totalAdvance,
        balance: 0.0,
        note: `Pagó ${days} día${days > 1 ? "s" : ""} por adelantado`,
      });
      setAdvanceModalId(null);
      setSelectedDays([0]);
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo registrar el pago por adelantado.");
    }
  }

  async function handleConfirmRefund(amount, selectedMethodId) {
    if (!refundTarget) return;
    const current = queue.find((p) => p.checkinId === refundTarget.checkinId);
    if (!current) {
      setRefundTarget(null);
      return;
    }

    setActionError("");
    try {
      const movements = await paymentsService.getMovementsByCheckin(refundTarget.checkinId);
      const paymentMovement = (Array.isArray(movements) ? movements : []).find(
        (m) => m.movementType === "PAYMENT"
      );
      if (!paymentMovement) {
        setActionError("No se encontró el movimiento original de pago para reembolsar.");
        return;
      }
      await paymentsService.processRefund(paymentMovement.id, {
        paymentMethodId: selectedMethodId,
        amount,
        note: "Reembolso solicitado en recepción",
      });
      const newAmount = Math.max(0, Number(current.amountCharged || 0) - amount);
      updateQueueItem(refundTarget.checkinId, {
        amountCharged: newAmount,
        balance: newAmount - dailyRate,
      });
      setRefundTarget(null);
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo procesar el reembolso.");
    }
  }

  const advancePerson = queue.find((p) => p.checkinId === advanceModalId);
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

        {actionError && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm rounded-xl">
            {actionError}
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
                onAmountChange={(val) => handleAmountChange(item.checkinId, val)}
                onSettle={() => handleSettle(item.checkinId)}
                onDeliverChange={() => handleDeliverChange(item.checkinId)}
                onConfirmAttendance={() => handleConfirmAttendance(item.checkinId)}
                onUndoAttendance={() => handleUndoAttendance(item.checkinId)}
                onOpenAdvance={() => {
                  setAdvanceModalId(item.checkinId);
                  setSelectedDays([0]);
                }}
                onOpenRefund={() => setRefundTarget(item)}
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
            amount: refundTarget.amountCharged,
          }}
          onConfirm={handleConfirmRefund}
          onClose={() => {
            setRefundTarget(null);
            setActionError("");
          }}
          error={actionError}
        />
      )}
    </div>
  );
}
