import { useState, useEffect, useCallback, useMemo } from "react";
import { Plus, X, Ban, RefreshCw, CalendarClock } from "lucide-react";
import { toOffsetDateTime, toLocalDateString } from "../../utils/date";
import { useAuth } from "../../context/AuthContext";
import { useMockData } from "../../context/DataContext";
import { userService } from "../../services/userService";
import { sessionsService } from "../../services/sessionsService";
import { getWeekRange, groupSessionsByDate, formatDayHeading } from "../../features/sessions/weekUtils";
import SessionDayBand from "../../features/sessions/SessionDayBand";
import SessionCard from "../../features/sessions/SessionCard";
import WeekNavigator from "../../features/sessions/WeekNavigator";

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

const ESTADOS = [
  { value: "activas", label: "Activas" },
  { value: "canceladas", label: "Canceladas" },
  { value: "todas", label: "Todas" },
];

// El backend genera la programación con UNA SEMANA de anticipación (hoy + 6 días), por decisión de
// producto (NOTAS-PARA-FRONTEND.md Bloque 5, punto 4): más allá de eso no hay clases creadas todavía, y
// no es un fallo. Cuánto de "la semana que viene" existe depende del día en que se mire (un lunes no se
// ve nada del lunes siguiente). Se avisa en pantalla para que un día vacío del futuro no se lea como
// "no se generó". Es un número del servidor copiado acá (PROVISIONAL, mejoras-v2.md punto 10, E3):
// debería llegar como dato (`horizonDays`).
const SCHEDULE_HORIZON_DAYS = 6;

// Sesiones de clase, por SEMANA (lunes a domingo) y agrupadas por día — no un histórico corrido
// (docs/correciones 20-09.md). Lo que se ve lo decide el servidor: el rango (`desde`/`hasta`), el
// filtro de estado (`estado`: activas por defecto, canceladas o todas) y el estado de cada sesión
// (`status`), que también dice si se puede finalizar. El histórico se consulta con un rango de
// fechas, como en Reportes, no con un botón de "mostrar canceladas".
export default function SessionsPage() {
  // `sessions` del contexto ya no se muestra acá (la lista sale de una consulta propia por semana o
  // rango); se usa solo como señal de que algo cambió y hay que releer (ver el efecto de `load`).
  const {
    sessions: contextSessions,
    createSession,
    cancelSession,
    closeSession,
    reassignSessionTeacher,
  } = useMockData();

  const { user } = useAuth();
  const isAdmin = Boolean(user?.roles?.includes("ADMIN"));

  const [teachersList, setTeachersList] = useState([]);
  const [formError, setFormError] = useState("");
  const [cancelError, setCancelError] = useState("");
  const [closeError, setCloseError] = useState("");

  const [showAddModal, setShowAddModal] = useState(false);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [creating, setCreating] = useState(false);
  const [closingId, setClosingId] = useState(null);

  const [date, setDate] = useState("");
  const [startTime, setStartTime] = useState("06:00");
  const [endTime, setEndTime] = useState("07:00");
  const [teacherUserId, setTeacherUserId] = useState("");

  // Qué se está mirando: una semana (con navegación) o un rango de fechas para el histórico.
  const [mode, setMode] = useState("semana"); // semana | rango
  const [weekOffset, setWeekOffset] = useState(0);
  const [estado, setEstado] = useState("activas");
  const [rangeFrom, setRangeFrom] = useState("");
  const [rangeTo, setRangeTo] = useState("");
  const [appliedRange, setAppliedRange] = useState(null);
  const [rangeError, setRangeError] = useState("");

  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const week = useMemo(() => getWeekRange(weekOffset), [weekOffset]);
  const range = useMemo(
    () => (mode === "semana" ? { desde: week.desde, hasta: week.hasta } : appliedRange),
    [mode, week, appliedRange]
  );

  // Ahora los profesores asignables incluyen ADMIN, no solo PROFESOR
  // (NOTAS-PARA-FRONTEND.md ronda 5, §0.2) — sin cambios de nuestro lado, el endpoint ya devuelve
  // la lista correcta. Se carga al montar, no solo al abrir el modal de crear: la mayoría de las
  // sesiones ahora nacen sin profesor (generadas por adelantado desde los horarios, §0.6) y cada
  // tarjeta necesita poder asignar uno sin pasar por "Crear Sesión Manual".
  const [reassigningId, setReassigningId] = useState(null);
  const [reassignError, setReassignError] = useState("");
  const [reassignSaving, setReassignSaving] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function loadTeachers() {
      try {
        const result = await userService.getTeachers();
        if (!cancelled && Array.isArray(result)) {
          setTeachersList(result);
        }
      } catch (err) {
        console.warn("No se cargó lista de profesores desde la API:", err);
      }
    }
    loadTeachers();
    return () => {
      cancelled = true;
    };
  }, []);

  // Pide al servidor lo que se está mirando.
  const load = useCallback(async () => {
    if (!range) {
      setList([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    setLoadError("");
    try {
      // Cada sesión ya trae `teacherName`, `totalCheckins`, `totalAttended`, `canClose` y `canCancel`
      // (NOTAS-PARA-FRONTEND.md Bloque 5, punto 6): una sola respuesta, sin cruzar nada acá.
      const raw = await sessionsService.getSessions(range.desde, range.hasta, 200, estado);
      setList(Array.isArray(raw) ? raw : raw?.content || []);
    } catch (err) {
      setLoadError(err.data?.message || err.message || "No se pudieron cargar las sesiones.");
      setList([]);
    } finally {
      setLoading(false);
    }
  }, [range, estado]);

  // Se relee al cambiar de semana/rango/estado, y también cuando `DataContext` se entera de un
  // cambio de sesiones (el WebSocket publica `SESSION_CHANGED` al crear, cerrar o cancelar una — de
  // otra pestaña o de otra persona): así esta pantalla no se queda con datos viejos.
  useEffect(() => {
    Promise.resolve().then(load);
  }, [load, contextSessions]);

  // El nombre del profesor viene resuelto en la sesión (`teacherName`); ya no se cruza con la lista.
  function teacherLabelOf(session) {
    if (session.teacherUserId === null || session.teacherUserId === undefined) return "Sin asignar";
    return session.teacherName || "Profesor asignado";
  }

  // Regenerar la programación (`POST /api/horarios/generar-sesiones`, ADMIN): vuelve a crear las
  // sesiones que falten de los horarios recurrentes. Idempotente. Está también en Horarios; acá
  // porque es donde se nota que falta una sesión (docs/correciones 20-09.md, 2ª ronda).
  const [regenerating, setRegenerating] = useState(false);
  const [regenerateMessage, setRegenerateMessage] = useState("");
  async function handleRegenerate() {
    setRegenerating(true);
    setRegenerateMessage("");
    setCloseError("");
    try {
      const result = await sessionsService.generateSessions();
      setRegenerateMessage(result?.message || "Programación regenerada.");
      await load();
      setTimeout(() => setRegenerateMessage(""), 6000);
    } catch (err) {
      setCloseError(err.data?.message || err.message || "No se pudo regenerar la programación.");
    } finally {
      setRegenerating(false);
    }
  }

  async function handleReassignTeacher(sessionId, newTeacherUserId) {
    if (!newTeacherUserId) return;
    setReassignSaving(true);
    setReassignError("");
    try {
      await reassignSessionTeacher(sessionId, Number(newTeacherUserId));
      setReassigningId(null);
      await load();
    } catch (err) {
      setReassignError(err.data?.message || err.message || "No se pudo asignar el profesor.");
    } finally {
      setReassignSaving(false);
    }
  }

  async function handleCreateSession() {
    if (!date.trim()) return;
    setFormError("");
    // El backend rechaza esto con un 500 crudo (violación del constraint `chk_sesion_horas`,
    // ver errores-nuevo-compartido.md #1) en vez de un mensaje claro — se valida acá antes de
    // llegar a ese camino.
    if (endTime <= startTime) {
      setFormError("La hora de fin debe ser posterior a la hora de inicio.");
      return;
    }
    setCreating(true);
    try {
      await createSession({
        date,
        // `startTime`/`endTime` son OffsetDateTime en el backend, no LocalDateTime — el error
        // "cuerpo de la solicitud JSON inválido" (errores-frontend.md #4) era justamente que
        // faltaba el offset de zona horaria (NOTAS-PARA-FRONTEND.md §1); sin él, Jackson ni
        // siquiera llega a deserializar el resto del cuerpo.
        startTime: toOffsetDateTime(date, startTime),
        endTime: toOffsetDateTime(date, endTime),
        // Se omite la clave por completo si no hay profesor: es opcional en el contrato,
        // y mandar `null` explícito rompe la deserialización si el campo del lado del
        // backend está tipado como primitivo (int) en vez de Integer.
        ...(teacherUserId ? { teacherUserId: Number(teacherUserId) } : {}),
      });
      setShowAddModal(false);
      setDate("");
      setTeacherUserId("");
      await load();
    } catch (err) {
      setFormError(err.data?.message || err.message || "No se pudo crear la sesión.");
    } finally {
      setCreating(false);
    }
  }

  async function confirmCancel() {
    if (!cancelTarget) return;
    setCancelError("");
    try {
      await cancelSession(cancelTarget);
      setCancelTarget(null);
      await load();
    } catch (err) {
      setCancelError(err.data?.message || err.message || "No se pudo cancelar la sesión.");
    }
  }

  // Finalizar = cerrarla ahora, sin borrarla (antes la única salida era cancelarla). El botón solo se
  // ofrece si el servidor dice que la sesión está en curso; si igual respondiera 403 (un profesor
  // sobre una sesión ajena) se muestra su mensaje tal cual.
  async function handleClose(sessionId) {
    setClosingId(sessionId);
    setCloseError("");
    try {
      await closeSession(sessionId);
      await load();
    } catch (err) {
      setCloseError(err.data?.message || err.message || "No se pudo finalizar la sesión.");
    } finally {
      setClosingId(null);
    }
  }

  function applyRange() {
    if (!rangeFrom || !rangeTo) {
      setRangeError("Elige las dos fechas.");
      return;
    }
    if (rangeTo < rangeFrom) {
      setRangeError("La fecha final no puede ser anterior a la inicial.");
      return;
    }
    setRangeError("");
    setAppliedRange({ desde: rangeFrom, hasta: rangeTo });
  }

  const grouped = useMemo(() => groupSessionsByDate(list), [list]);
  // En semana se muestran siempre los siete días (aunque no tengan sesiones); en un rango, solo los
  // días que sí tienen.
  const dayKeys = mode === "semana" ? week.days : [...grouped.keys()].sort();
  const todayIso = toLocalDateString(new Date());
  const horizonDate = new Date();
  horizonDate.setDate(horizonDate.getDate() + SCHEDULE_HORIZON_DAYS);
  const horizonIso = toLocalDateString(horizonDate);
  // Hay días fuera del horizonte en lo que se está viendo (semana o rango).
  const beyondHorizon = mode === "semana" ? week.hasta > horizonIso : Boolean(range && range.hasta > horizonIso);

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-4 md:px-6 py-4 font-body text-white">
      <div className="max-w-6xl mx-auto space-y-4">
        <div className="flex items-center justify-between gap-4 border-b border-neutral-800 pb-3 flex-wrap">
          <div>
            <h1 className="text-xl font-bold text-white tracking-tight">Sesiones de Clase</h1>
            <p className="text-neutral-400 text-xs mt-0.5">
              Instancias reales de clase, por semana. Creadas automáticamente por horario o de forma manual.
            </p>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={load}
              disabled={loading}
              title="Volver a pedir las sesiones al servidor"
              className="flex items-center gap-1.5 text-xs font-semibold text-neutral-400 hover:text-white px-3 py-1.5 rounded-xl border border-neutral-800 hover:border-neutral-700 transition-colors disabled:opacity-50"
            >
              <RefreshCw size={13} className={loading ? "animate-spin" : ""} />
              {loading ? "Actualizando…" : "Actualizar"}
            </button>
            {isAdmin && (
              <button
                onClick={handleRegenerate}
                disabled={regenerating}
                title="Crea las sesiones que falten de los horarios recurrentes"
                className="flex items-center gap-1.5 text-xs font-semibold text-neutral-400 hover:text-white px-3 py-1.5 rounded-xl border border-neutral-800 hover:border-neutral-700 transition-colors disabled:opacity-50"
              >
                <CalendarClock size={13} className={regenerating ? "animate-pulse" : ""} />
                {regenerating ? "Regenerando…" : "Regenerar programación"}
              </button>
            )}
            <button
              onClick={() => setShowAddModal(true)}
              className="flex items-center gap-1.5 bg-emerald-500 text-neutral-950 hover:bg-emerald-400 font-bold text-xs px-3.5 py-1.5 rounded-xl transition-colors shadow-md shadow-emerald-500/20"
            >
              <Plus size={14} />
              Crear Sesión Manual
            </button>
          </div>
        </div>

        {/* Filtros: semana o rango de fechas, y estado (activas por defecto). */}
        <div className="flex items-end justify-between gap-3 flex-wrap">
          <div className="flex items-center gap-3 flex-wrap">
            <div className="flex bg-neutral-900 border border-neutral-800 rounded-xl p-1 text-xs font-semibold">
              <button
                type="button"
                onClick={() => setMode("semana")}
                className={`px-3 py-1.5 rounded-lg transition-colors ${
                  mode === "semana" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400 hover:text-white"
                }`}
              >
                Semana
              </button>
              <button
                type="button"
                onClick={() => setMode("rango")}
                className={`px-3 py-1.5 rounded-lg transition-colors ${
                  mode === "rango" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400 hover:text-white"
                }`}
              >
                Rango de fechas
              </button>
            </div>

            {mode === "semana" ? (
              <WeekNavigator offset={weekOffset} onChange={setWeekOffset} desde={week.desde} hasta={week.hasta} />
            ) : (
              <div className="flex items-end gap-2 flex-wrap">
                <div>
                  <label className="text-neutral-400 text-xs font-medium block mb-1">Desde</label>
                  <input
                    type="date"
                    value={rangeFrom}
                    onChange={(e) => setRangeFrom(e.target.value)}
                    className="bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-1.5 text-white text-sm outline-none focus:border-emerald-500"
                  />
                </div>
                <div>
                  <label className="text-neutral-400 text-xs font-medium block mb-1">Hasta</label>
                  <input
                    type="date"
                    value={rangeTo}
                    onChange={(e) => setRangeTo(e.target.value)}
                    className="bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-1.5 text-white text-sm outline-none focus:border-emerald-500"
                  />
                </div>
                <button
                  type="button"
                  onClick={applyRange}
                  className="bg-white text-neutral-950 hover:bg-neutral-200 font-bold text-xs px-3.5 py-2 rounded-xl transition-colors"
                >
                  Aplicar
                </button>
              </div>
            )}
          </div>

          <div>
            <label className="text-neutral-400 text-xs font-medium block mb-1">Estado</label>
            <select
              value={estado}
              onChange={(e) => setEstado(e.target.value)}
              className="bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-1.5 text-white text-sm outline-none focus:border-emerald-500"
            >
              {ESTADOS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        </div>

        {rangeError && <p className="text-amber-400 text-xs">{rangeError}</p>}
        {loadError && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm rounded-xl">{loadError}</div>
        )}
        {closeError && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm rounded-xl">{closeError}</div>
        )}
        {regenerateMessage && (
          <div className="p-3 bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm rounded-xl">
            {regenerateMessage}
          </div>
        )}

        {beyondHorizon && (
          <div className="p-3 bg-sky-500/10 border border-sky-500/20 text-sky-300 text-xs rounded-xl">
            La programación se genera con <strong>una semana de anticipación</strong>. Los días posteriores al{" "}
            {formatDayHeading(horizonIso)} todavía no tienen sesiones programadas: es lo esperado, no un fallo.
            Se pueden crear a mano con "Crear Sesión Manual".
          </div>
        )}

        {mode === "rango" && !range ? (
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-8 text-center text-neutral-500 text-sm">
            Elige un rango de fechas para consultar sesiones pasadas.
          </div>
        ) : loading && list.length === 0 ? (
          <div className="p-8 text-center text-neutral-500 text-sm">Cargando sesiones…</div>
        ) : mode === "rango" && list.length === 0 ? (
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-8 text-center text-neutral-500 text-sm">
            No hay sesiones en ese rango con el estado elegido.
          </div>
        ) : (
          <div className="space-y-3">
            {dayKeys.map((dayKey) => {
              const daySessions = grouped.get(dayKey) || [];
              return (
                <SessionDayBand
                  key={dayKey}
                  dayKey={dayKey}
                  isToday={dayKey === todayIso}
                  isPast={dayKey < todayIso}
                  emptyText={dayKey > horizonIso ? "Aún sin programar" : "Sin sesiones"}
                >
                  {daySessions.length > 0 && (
                    <div className="grid grid-cols-1 lg:grid-cols-2 2xl:grid-cols-3 gap-3">
                      {daySessions.map((session) => (
                        <SessionCard
                          key={session.id}
                          session={session}
                          teacherLabel={teacherLabelOf(session)}
                          teachersList={teachersList}
                          isReassigning={reassigningId === session.id}
                          reassignSaving={reassignSaving}
                          reassignError={reassignError}
                          closing={closingId === session.id}
                          onStartReassign={() => {
                            setReassignError("");
                            setReassigningId(session.id);
                          }}
                          onCancelReassign={() => {
                            setReassigningId(null);
                            setReassignError("");
                          }}
                          onReassign={(value) => handleReassignTeacher(session.id, value)}
                          onClose={() => handleClose(session.id)}
                          onCancel={() => setCancelTarget(session.id)}
                        />
                      ))}
                    </div>
                  )}
                </SessionDayBand>
              );
            })}
          </div>
        )}
      </div>

      {/* Add Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 w-full max-w-md">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-bold text-white">Crear Sesión de Clase</h3>
              <button onClick={() => setShowAddModal(false)} className="text-neutral-500 hover:text-white">
                <X size={18} />
              </button>
            </div>

            <div className="space-y-4 mb-6">
              <div>
                <label className="text-xs text-neutral-400 font-semibold mb-1 block">Fecha</label>
                <input
                  type="date"
                  value={date}
                  onChange={(e) => setDate(e.target.value)}
                  className={inputClass}
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="text-xs text-neutral-400 font-semibold mb-1 block">Hora Inicio</label>
                  <input
                    type="time"
                    value={startTime}
                    onChange={(e) => setStartTime(e.target.value)}
                    className={inputClass}
                  />
                </div>
                <div>
                  <label className="text-xs text-neutral-400 font-semibold mb-1 block">Hora Fin</label>
                  <input
                    type="time"
                    value={endTime}
                    onChange={(e) => setEndTime(e.target.value)}
                    className={inputClass}
                  />
                </div>
              </div>

              <div>
                <label className="text-xs text-neutral-400 font-semibold mb-1 block">Profesor asignado</label>
                <select
                  value={teacherUserId}
                  onChange={(e) => setTeacherUserId(e.target.value)}
                  className={inputClass}
                >
                  <option value="">Sin profesor</option>
                  {teachersList.map((t) => (
                    <option key={t.userId || t.id} value={t.userId || t.id}>
                      {t.fullName || `${t.firstName} ${t.lastName}`}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {formError && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl text-center">
                {formError}
              </div>
            )}

            <div className="flex justify-end gap-2">
              <button
                onClick={() => {
                  setShowAddModal(false);
                  setFormError("");
                }}
                className="px-4 py-2 text-neutral-400 text-sm"
              >
                Cancelar
              </button>
              <button
                onClick={handleCreateSession}
                disabled={!date.trim() || creating}
                className="bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold text-sm px-4 py-2 rounded-xl"
              >
                {creating ? "Creando..." : "Crear Sesión"}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Cancel Confirmation Modal */}
      {cancelTarget && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center p-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 w-full max-w-sm text-center">
            <div className="w-12 h-12 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto mb-3 text-red-400">
              <Ban size={24} />
            </div>
            <h3 className="text-lg font-bold text-white mb-2">¿Cancelar esta sesión?</h3>
            <p className="text-neutral-400 text-xs mb-6">
              La sesión quedará marcada como cancelada y los miembros serán notificados. Para darla por
              terminada sin cancelarla, usa "Finalizar".
            </p>
            {cancelError && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl text-center">
                {cancelError}
              </div>
            )}
            <div className="flex justify-center gap-3">
              <button
                onClick={() => {
                  setCancelTarget(null);
                  setCancelError("");
                }}
                className="px-4 py-2 bg-neutral-800 text-neutral-300 hover:bg-neutral-700 text-sm font-semibold rounded-xl"
              >
                Volver
              </button>
              <button
                onClick={confirmCancel}
                className="px-4 py-2 bg-red-500 hover:bg-red-400 text-white text-sm font-bold rounded-xl"
              >
                Sí, Cancelar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
