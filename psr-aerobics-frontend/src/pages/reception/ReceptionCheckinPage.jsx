import { useState, useEffect, useCallback } from "react";
import { Plus, X, Flag } from "lucide-react";
import { formatCOP } from "../../utils/currency";
import { formatHeaderDateShort as formatHeaderDate, formatTimeOfDay } from "../../utils/date";
import {
  DAILY_RATE as FALLBACK_RATE,
  getUpcomingDays,
  getRefundCap,
  getAccountStatus,
  getAmountDue,
  pickDefaultPaymentMethodId,
} from "../../features/reception/utils";
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

// "11:40 a. m. – 11:50 a. m.", o "desde las 11:40 a. m." si la sesión no tiene hora de fin.
function formatSessionRange(session) {
  const start = formatTimeOfDay(session?.startTime);
  const end = formatTimeOfDay(session?.endTime);
  return end ? `${start} – ${end}` : `desde las ${start}`;
}

export default function ReceptionCheckinPage() {
  const { queue, sessions, updateQueueItem, refreshQueue, closeSession } = useMockData();
  // Por defecto "Sin atender" (docs/correciones 20-09.md): la lista de trabajo de quien recibe es
  // atender a quienes van llegando — cada persona atendida sale de esta vista sola, y "Todos", "Al
  // día" y "Saldo pendiente" siguen a un clic para revisar el resto.
  const [activeFilter, setActiveFilter] = useState("pending");
  const [dailyRate, setDailyRate] = useState(FALLBACK_RATE);
  // Solo métodos ACTIVOS (ver `paymentsService.getActivePaymentMethods`).
  const [paymentMethods, setPaymentMethods] = useState([]);
  // Efectivo si está activo, si no el primero — lo que queda preseleccionado en cada cobro. Cada
  // fila (y el modal de adelanto) puede cambiarlo para esa transacción puntual.
  const defaultMethodId = pickDefaultPaymentMethodId(paymentMethods);
  // Método elegido en el modal de "pago por adelantado"; `null` = el de por defecto.
  const [advanceMethodId, setAdvanceMethodId] = useState(null);

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
          paymentsService.getActivePaymentMethods(),
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

  // Resumen de la cola (`GET /api/cola/resumen`, NOTAS-PARA-FRONTEND.md Bloque 6): la sesión VIGENTE
  // completa (en curso; si no, la próxima de hoy; si no, la última de hoy) y las dos cifras de recaudo,
  // ya calculadas por el servidor. Es lo que alimenta el encabezado (hora de fin de la clase y las dos
  // cifras) y el botón "Finalizar sesión": la sesión sale de aquí, no de elegirla en una lista.
  //   - `collectedThisSession`: lo cobrado en las asistencias confirmadas de ESA sesión (devengado).
  //   - `collectedToday`: todo lo abonado hoy, adelantos incluidos y menos reembolsos (caja).
  // Ambas vienen en `null` para un PROFESOR (no tiene reportes de ingresos): en ese caso no se muestra
  // ninguna cifra en vez de un cero falso.
  const [summary, setSummary] = useState(null);
  const loadSummary = useCallback(async () => {
    try {
      setSummary(await queueService.getSummary());
    } catch {
      setSummary(null);
    }
  }, []);

  // Se relee cuando la cola cambia (un cobro, un reembolso, cualquier señal del WebSocket: la cola se
  // relee con todas). Con una pausa corta: la cola puede cambiar varias veces seguidas.
  useEffect(() => {
    const timer = setTimeout(loadSummary, 300);
    return () => clearTimeout(timer);
  }, [queue, loadSummary]);

  // Red de seguridad, PROVISIONAL (mejoras-v2.md punto 10, F1): las señales del WebSocket ya llegan
  // (la llegada propia, y el inicio y fin de una sesión por el reloj, con hasta ~1 minuto de retraso),
  // pero si el WebSocket no conecta — por ejemplo un backend en `http` detrás de una página `https` —
  // la pantalla quedaría con la foto del momento en que se abrió. Al volver a la pestaña y cada 30 s
  // se relee la cola (lo que a su vez relee el resumen). Una actualización de menos de ~5 s de una
  // llegada prueba que la señal funciona; si tarda hasta 30 s, es este relleno el que actualiza.
  useEffect(() => {
    const tick = () => {
      if (!document.hidden) refreshQueue();
    };
    const timer = setInterval(tick, 30000);
    document.addEventListener("visibilitychange", tick);
    return () => {
      clearInterval(timer);
      document.removeEventListener("visibilitychange", tick);
    };
  }, [refreshQueue]);

  // Qué sesión se puede finalizar: la vigente que dice el servidor, solo si está en curso.
  const currentSession = summary?.session ?? null;
  const sessionInProgress = currentSession?.status === "IN_PROGRESS" ? currentSession : null;
  const [showCloseModal, setShowCloseModal] = useState(false);
  const [closingSession, setClosingSession] = useState(false);
  // La cola ya solo trae las personas de la sesión vigente (la sesión en curso o la próxima, y `[]` si
  // no hay ninguna viva): las de una sesión terminada no vuelven a aparecer, así que "Todos", "Sin
  // atender", "Saldo pendiente" y "Al día" se reinician solos al terminar la clase.
  const viewRows = queue;

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

  // "Saldo pendiente" y "Al día" salen del `accountStatus` que calcula el servidor, no de comparar
  // `balance` con 0: alguien que pagó 10k, abonó 2 días y consumió 1 tiene un día a favor — no se
  // le debe nada — y aparecía como "saldo pendiente" (docs/correciones 20-09.md). Pendiente = debe
  // dinero (DEBE) o se le debe vuelto (VUELTO_PENDIENTE); todo lo demás está al día.
  const hasPendingBalance = (item) => {
    const status = getAccountStatus(item);
    return status === "DEBE" || status === "VUELTO_PENDIENTE";
  };

  const filteredQueue = viewRows.filter((item) => {
    if (activeFilter === "pending") return !item.attended;
    if (activeFilter === "balance") return hasPendingBalance(item);
    if (activeFilter === "settled") return !hasPendingBalance(item);
    return true;
  });

  const counts = {
    all: viewRows.length,
    pending: viewRows.filter((p) => !p.attended).length,
    balance: viewRows.filter(hasPendingBalance).length,
    settled: viewRows.filter((p) => !hasPendingBalance(p)).length,
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
  //  - "pending" (20-sep): aplica la tarifa del día y NO entrega el vuelto — el excedente queda
  //    como vuelto pendiente ("se le debe"), para cuando no hay efectivo para dar el cambio. Registrar
  //    un movimiento con `amountReceived > amountApplied` no entrega nada por sí solo: si no se llama
  //    a "entregar vuelto", la diferencia queda pendiente (NOTAS-PARA-FRONTEND.md Bloque 4, 3.1).
  // Todo registro de dinero lleva su método de pago (cobro, adelanto, reembolso). Antes el cobro
  // nunca lo preguntaba y caía en `paymentMethods[0]?.id || 1` — el id 1 aunque estuviera
  // desactivado o ni existiera. Ahora, si no hay ninguno disponible, se avisa en vez de inventar uno.
  function requireMethodId(methodId) {
    if (!methodId) {
      setActionError(
        "No hay un método de pago activo disponible. Pide a un administrador que active al menos uno, o recarga la página."
      );
      return null;
    }
    return methodId;
  }

  async function handleCharge(checkinId, amount, mode = "change", chosenMethodId = defaultMethodId) {
    const item = queue.find((p) => p.checkinId === checkinId);
    if (!item || !item.personId || !(amount > 0)) return;
    setActionError("");
    const paymentMethodId = requireMethodId(chosenMethodId);
    if (!paymentMethodId) return;
    try {
      // Lo recibido se aplica PRIMERO a lo que la persona debe pagar ahora (lo que ya debía + la
      // tarifa de hoy − lo que tenga a favor, ver `getAmountDue`); solo lo que sobra de ahí es
      // excedente. Antes se aplicaba `min(monto, tarifa)` y todo lo demás era vuelto, así que quien
      // debía dinero nunca podía saldar la deuda (docs/correciones 20-09.md, 2ª ronda).
      //   - "change" / "pending": se aplica hasta lo que debe; el resto se entrega o queda pendiente.
      //   - "advance": además de lo que debe, se abonan por adelantado tantos días completos como
      //     alcance el excedente; lo que no llegue a un día se entrega.
      const due = getAmountDue(item);
      const extraDays = mode === "advance" && dailyRate > 0 ? Math.floor(Math.max(0, amount - due) / dailyRate) : 0;
      const amountApplied = Math.min(amount, due + extraDays * dailyRate);
      const remainder = amount - amountApplied;
      const debt = Number(item.balance || 0) < 0 ? -Number(item.balance) : 0;
      const note =
        extraDays > 0
          ? `Cobro de entrada + ${extraDays} sesión(es) por adelantado`
          : amountApplied < due
            ? "Pago parcial en recepción"
            : debt > 0
              ? "Cobro de entrada + pago de deuda"
              : mode === "pending" && remainder > 0
                ? "Cobro de entrada en recepción (vuelto pendiente)"
                : "Cobro de entrada en recepción";

      const movement = await paymentsService.createMovement({
        personId: item.personId,
        checkinId,
        paymentMethodId,
        amountReceived: amount,
        amountApplied,
        note,
      });

      if (mode !== "pending" && remainder > 0 && movement?.id) {
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
      // Confirmar consume la tarifa de la cuenta: el estado de la cuenta (`accountStatus`) y el
      // saldo cambian, y los calcula el servidor — se relee en vez de dejar el valor de antes.
      await refreshQueue();
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
      await refreshQueue();
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo anular la asistencia.");
    }
  }

  // Vuelto pendiente ("se le debe"): entregarlo, o convertirlo en días abonados con el mismo dinero.
  // Las dos acciones del backend son POR MOVIMIENTO (`{id}` es el del cobro, no el de la persona),
  // pero la fila solo trae el total pendiente de la persona (`pendingChange`) — así que se buscan
  // sus movimientos con vuelto sin entregar (`GET /api/movimientos/pendientes/vueltos`) y se actúa
  // sobre ellos, del más antiguo al más nuevo. PROVISIONAL: es orquestación que debería vivir en el
  // servidor (una acción por persona); pedido en pendientes-back-20-09-seguimiento.md. Mientras
  // tanto, cuando la persona tiene un solo cobro con vuelto (el caso normal) es una sola llamada.
  const [pendingBusyId, setPendingBusyId] = useState(null);

  async function findPendingMovements(item) {
    const data = await paymentsService.getPendingChanges();
    const list = Array.isArray(data) ? data : data?.content || [];
    const norm = (value) => String(value || "").trim().toLowerCase();
    const fullName = norm(`${item.firstName} ${item.lastName}`);
    return list
      .filter((m) => (m.personId != null ? m.personId === item.personId : norm(m.fullName) === fullName))
      .map((m) => ({
        id: m.movementId ?? m.id,
        pending: Number(m.change ?? Number(m.amountReceived || 0) - Number(m.amountApplied || 0)),
      }))
      .filter((m) => m.id != null && m.pending > 0)
      .sort((a, b) => a.id - b.id);
  }

  async function handleDeliverPending(item) {
    setActionError("");
    setActionSuccess("");
    setPendingBusyId(item.checkinId);
    try {
      const movements = await findPendingMovements(item);
      if (movements.length === 0) {
        setActionError("No se encontró el vuelto pendiente de esta persona. Recarga la lista e intenta de nuevo.");
        return;
      }
      for (const movement of movements) {
        await paymentsService.deliverChange(movement.id);
      }
      await refreshQueue();
      const total = movements.reduce((sum, movement) => sum + movement.pending, 0);
      setActionSuccess(`Vuelto entregado: ${formatCOP(total)}.`);
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo entregar el vuelto.");
    } finally {
      setPendingBusyId(null);
    }
  }

  async function handleApplyPending(item, days) {
    if (!(days >= 1)) return;
    setActionError("");
    setActionSuccess("");
    setPendingBusyId(item.checkinId);
    try {
      const amount = days * dailyRate;
      const movements = await findPendingMovements(item);
      const available = movements.reduce((sum, movement) => sum + movement.pending, 0);
      if (movements.length === 0 || amount > available) {
        setActionError("El vuelto pendiente no alcanza para ese abono. Recarga la lista e intenta de nuevo.");
        return;
      }
      let remaining = amount;
      for (const movement of movements) {
        if (remaining <= 0) break;
        const take = Math.min(remaining, movement.pending);
        await paymentsService.payChangeAsCredit(movement.id, {
          amount: take,
          note: `Abona ${days} sesión(es) con el vuelto`,
        });
        remaining -= take;
      }
      await refreshQueue();
      setActionSuccess(`Se abonaron ${days} sesión(es) (${formatCOP(amount)}) con el vuelto pendiente.`);
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo abonar el vuelto.");
    } finally {
      setPendingBusyId(null);
    }
  }

  async function handleCloseSession() {
    if (!sessionInProgress) return;
    setClosingSession(true);
    setActionError("");
    setActionSuccess("");
    try {
      await closeSession(sessionInProgress.id);
      await loadSummary();
      setActionSuccess("Sesión finalizada.");
    } catch (err) {
      setActionError(err.data?.message || err.message || "No se pudo finalizar la sesión.");
    } finally {
      setClosingSession(false);
      setShowCloseModal(false);
    }
  }

  async function handleAddManualEntry() {
    const documento = newDocumento.trim();
    if (!documento) return;
    setAddingManual(true);
    setAddManualError("");
    try {
      // `POST /api/checkin/manual/documento` (no el de la terminal): registra con método MANUAL — antes
      // se registraba como DOCUMENT y salía "Terminal PSR" — y es el único canal que puede registrar
      // fuera de horario: entra a la última sesión del día en vez de rechazar (NOTAS-PARA-FRONTEND.md
      // Bloque 4, puntos 2 y 5.1). Quien atiende tiene delante a la persona y ve el caso.
      await queueService.checkinManualByDocument(documento);
      await refreshQueue();
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
    const paymentMethodId = requireMethodId(advanceMethodId ?? defaultMethodId);
    if (!paymentMethodId) return;
    try {
      await paymentsService.createMovement({
        personId: item.personId,
        checkinId: advanceModalId,
        paymentMethodId,
        amountReceived: totalAdvance,
        amountApplied: totalAdvance,
        note: `Pago por adelantado (${days} sesión(es))`,
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
            </p>
            {/* La clase vigente y a qué hora termina — para tener presente cuánto le queda. Sale del
                resumen del servidor (`session`): en curso, la próxima de hoy o la última de hoy. */}
            {currentSession && (
              <p className="text-xs mt-0.5 text-neutral-400">
                {currentSession.status === "IN_PROGRESS" && (
                  <>
                    <span className="text-emerald-400 font-bold">Clase en curso</span>{" "}
                    {formatSessionRange(currentSession)}
                    {currentSession.endTime && (
                      <span className="text-white font-semibold"> · termina {formatTimeOfDay(currentSession.endTime)}</span>
                    )}
                  </>
                )}
                {currentSession.status === "UPCOMING" && (
                  <>
                    <span className="text-sky-400 font-bold">Próxima clase</span> {formatSessionRange(currentSession)}
                  </>
                )}
                {currentSession.status === "ENDED" && (
                  <>
                    <span className="text-neutral-300 font-bold">Última clase de hoy</span>{" "}
                    {formatSessionRange(currentSession)}
                  </>
                )}
              </p>
            )}
            {/* Dos cifras distintas, con su nombre — antes una sola ("Recaudo de hoy") confundía lo de
                la clase con lo del día. Las dos vienen ya calculadas del servidor y en `null` para un
                profesor (sin permiso de reportes): entonces no se muestran. */}
            {summary && (summary.collectedThisSession != null || summary.collectedToday != null) && (
              <p className="text-neutral-400 text-xs mt-0.5">
                {summary.collectedThisSession != null && (
                  <span title="Lo cobrado en las asistencias confirmadas de esta sesión (se atribuye a la fecha de la clase).">
                    Esta sesión:{" "}
                    <span className="text-emerald-400 font-bold">{formatCOP(summary.collectedThisSession)}</span>
                  </span>
                )}
                {summary.collectedThisSession != null && summary.collectedToday != null && (
                  <span className="mx-2 text-neutral-700">|</span>
                )}
                {summary.collectedToday != null && (
                  <span title="Todo lo que ingresó hoy en todas las sesiones, incluidos los pagos por adelantado, menos los reembolsos (se atribuye a la fecha del pago).">
                    Ingresó hoy: <span className="text-emerald-400 font-bold">{formatCOP(summary.collectedToday)}</span>
                  </span>
                )}
              </p>
            )}
          </div>

          <div className="flex items-center gap-2 flex-wrap">
            {/* Botón grande y visible para cerrar la clase en curso — antes la única salida era
                cancelar la sesión, que equivale a borrarla (docs/correciones 20-09.md). Solo
                aparece si el servidor dice que hay una sesión en curso (`status`). */}
            {sessionInProgress && (
              <button
                onClick={() => setShowCloseModal(true)}
                className="flex items-center gap-2 bg-amber-500 hover:bg-amber-400 text-neutral-950 text-sm font-bold px-5 py-2.5 rounded-full transition-colors shadow-md shadow-amber-500/20"
              >
                <Flag size={16} />
                Finalizar sesión
              </button>
            )}
            <button
              onClick={() => setShowAddModal(true)}
              className="flex items-center gap-1.5 bg-white hover:bg-neutral-200 text-neutral-950 text-xs font-bold px-3.5 py-1.5 rounded-full transition-colors shadow-md"
            >
              <Plus size={14} />
              Agregar Manual
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
              <span className="w-40 shrink-0">Monto recibido</span>
              {/* "Saldo" a la derecha, igual que los valores de la fila (que van alineados a la
                  derecha): con el encabezado a la izquierda no quedaba sobre el dato. "Estado"
                  reemplaza a "Asistencia", que no decía nada (docs/correciones 20-09.md). */}
              <span className="w-40 shrink-0 text-right">Saldo</span>
              <span className="w-36 shrink-0 text-right">Estado</span>
            </div>
          )}
          {filteredQueue.length === 0 ? (
            <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-8 text-center">
              {queue.length === 0 ? (
                // Sin filas: el estado normal entre clases, no una alerta. La cola solo trae la sesión
                // vigente, así que se reinicia sola al terminar la clase y se llena con la próxima.
                <>
                  <p className="text-white font-bold text-base">
                    {sessionInProgress ? "Todavía no hay nadie en la fila de esta sesión" : "No hay clase en curso"}
                  </p>
                  <p className="text-neutral-500 text-sm mt-1">
                    {sessionInProgress
                      ? "Las personas aparecen aquí a medida que van llegando."
                      : currentSession?.status === "UPCOMING"
                        ? `La próxima clase empieza a las ${formatTimeOfDay(currentSession.startTime)}.`
                        : currentSession?.status === "ENDED"
                          ? "Hoy no quedan más clases."
                          : "Hoy no hay clases programadas."}
                  </p>
                </>
              ) : (
                <p className="text-neutral-500 text-sm">
                  No hay personas en la fila con los filtros seleccionados.
                </p>
              )}
            </div>
          ) : (
            filteredQueue.map((item, index) => (
              <QueueRow
                key={item.checkinId || index}
                item={item}
                position={index + 1}
                dailyRate={dailyRate}
                sessionLabel={sessionLabel(item.sessionId)}
                paymentMethods={paymentMethods}
                defaultMethodId={defaultMethodId}
                pendingBusy={pendingBusyId === item.checkinId}
                onDeliverPending={() => handleDeliverPending(item)}
                onApplyPending={(days) => handleApplyPending(item, days)}
                onCharge={(amount, mode, methodId) => handleCharge(item.checkinId, amount, mode, methodId)}
                onConfirmAttendance={() => handleConfirmAttendance(item.checkinId)}
                onUndoAttendance={() => handleUndoAttendance(item.checkinId)}
                onOpenAdvance={() => {
                  setAdvanceModalId(item.checkinId);
                  setSelectedDays([0]);
                  setAdvanceMethodId(null);
                }}
                onOpenRefund={() => handleOpenRefund(item)}
              />
            ))
          )}
        </div>
      </div>

      {/* Modal Finalizar sesión */}
      {showCloseModal && sessionInProgress && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 w-full max-w-sm text-center">
            <div className="w-12 h-12 rounded-full bg-amber-500/10 border border-amber-500/30 flex items-center justify-center mx-auto mb-3 text-amber-400">
              <Flag size={22} />
            </div>
            <h3 className="text-lg font-bold text-white mb-2">¿Finalizar esta sesión?</h3>
            <p className="text-neutral-300 text-sm mb-1">
              {formatSessionRange(sessionInProgress)}
            </p>
            <p className="text-neutral-500 text-xs mb-5">
              Se cierra ahora, no se borra: queda registrada con lo que pasó. Las personas que lleguen después
              ya no entran a ella.
            </p>
            <div className="flex justify-center gap-3">
              <button
                onClick={() => setShowCloseModal(false)}
                disabled={closingSession}
                className="px-4 py-2 bg-neutral-800 text-neutral-300 hover:bg-neutral-700 text-sm font-semibold rounded-xl disabled:opacity-40"
              >
                Volver
              </button>
              <button
                onClick={handleCloseSession}
                disabled={closingSession}
                className="px-4 py-2 bg-amber-500 hover:bg-amber-400 text-neutral-950 text-sm font-bold rounded-xl disabled:opacity-40"
              >
                {closingSession ? "Finalizando…" : "Sí, finalizar"}
              </button>
            </div>
          </div>
        </div>
      )}

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
                    {/* `getUpcomingDays` devuelve { weekday, date }, no `label` — antes se pintaba
                        `day.label` (siempre undefined) y los botones de día salían en blanco. */}
                    {day.weekday} {day.date}
                  </button>
                );
              })}
            </div>

            <div className="mb-5">
              <label className="text-neutral-400 text-xs font-medium">Método de pago</label>
              <select
                value={(advanceMethodId ?? defaultMethodId) ?? ""}
                onChange={(e) => setAdvanceMethodId(Number(e.target.value))}
                disabled={paymentMethods.length === 0}
                className="w-full mt-1 bg-neutral-950 border border-neutral-800 rounded-xl px-3 py-2.5 text-white text-sm outline-none focus:border-emerald-500 disabled:opacity-50"
              >
                {paymentMethods.length === 0 && <option value="">Sin método activo</option>}
                {paymentMethods.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.name}
                  </option>
                ))}
              </select>
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
            // `refundable` del backend, ya acotado al saldo a favor — ver `getRefundCap`
            // (features/reception/utils.js). El servidor valida contra ese mismo número.
            amount: getRefundCap(refundTarget),
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
