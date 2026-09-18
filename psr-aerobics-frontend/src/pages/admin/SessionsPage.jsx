import { useState, useEffect } from "react";
import { Plus, X, Ban, Users, CheckCircle2, RefreshCw } from "lucide-react";
import { formatShortWeekdayDate as formatDate, formatTimeOfDay, toOffsetDateTime } from "../../utils/date";
import { useMockData } from "../../context/DataContext";
import { userService } from "../../services/userService";

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function SessionsPage() {
  const { sessions, createSession, cancelSession, reassignSessionTeacher, people, refreshSessions } = useMockData();
  const [teachersList, setTeachersList] = useState([]);
  const [formError, setFormError] = useState("");
  const [cancelError, setCancelError] = useState("");
  const [refreshing, setRefreshing] = useState(false);

  const [showAddModal, setShowAddModal] = useState(false);
  const [cancelTarget, setCancelTarget] = useState(null);
  const [creating, setCreating] = useState(false);

  const [date, setDate] = useState("");
  const [startTime, setStartTime] = useState("06:00");
  const [endTime, setEndTime] = useState("07:00");
  const [teacherUserId, setTeacherUserId] = useState("");

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

  // `sessions` viene de `DataContext`, que lo pide una sola vez al montar el provider — que
  // envuelve toda la app y no se desmonta al navegar (docs/correcciones-pendientes-18-09.md #7).
  // Sin este refetch propio, entrar a esta pantalla mucho después de haber iniciado sesión (una
  // sesión creada por el barrido del servidor entre tanto, o por otra persona) se veía con datos
  // viejos hasta recargar la página completa.
  useEffect(() => {
    refreshSessions();
  }, [refreshSessions]);

  async function handleRefresh() {
    setRefreshing(true);
    try {
      await refreshSessions();
    } finally {
      setRefreshing(false);
    }
  }

  async function handleReassignTeacher(sessionId, newTeacherUserId) {
    if (!newTeacherUserId) return;
    setReassignSaving(true);
    setReassignError("");
    try {
      await reassignSessionTeacher(sessionId, Number(newTeacherUserId));
      setReassigningId(null);
    } catch (err) {
      setReassignError(err.data?.message || err.message || "No se pudo asignar el profesor.");
    } finally {
      setReassignSaving(false);
    }
  }

  function teacherName(teacherUserId) {
    const teacher = teachersList.find((t) => t.userId === teacherUserId || t.id === teacherUserId);
    if (teacher) return teacher.fullName || `${teacher.firstName || ""} ${teacher.lastName || ""}`.trim();
    const fallbackPerson = people.find((p) => p.id === teacherUserId || p.userId === teacherUserId);
    return fallbackPerson ? `${fallbackPerson.firstName} ${fallbackPerson.lastName}` : "Sin asignar";
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
    } catch (err) {
      setFormError(err.data?.message || err.message || "No se pudo crear la sesión.");
    } finally {
      setCreating(false);
    }
  }

  // Sin filtro, esta pantalla mostraba TODAS las sesiones que devuelve el backend de una — y desde
  // que las sesiones se generan por adelantado (7 días, más las canceladas que se van
  // acumulando), eso deja de ser una lista manejable (problemas-finales, 13-sep: "no tiene ningún
  // tipo de filtro"). Front-only: ocultar canceladas por defecto, con un botón para volver a
  // verlas — no hace falta tocar el backend para esto.
  const [showCancelled, setShowCancelled] = useState(false);
  const visibleSessions = showCancelled ? sessions : sessions.filter((s) => !s.cancelled);
  const cancelledCount = sessions.filter((s) => s.cancelled).length;

  async function confirmCancel() {
    if (!cancelTarget) return;
    setCancelError("");
    try {
      await cancelSession(cancelTarget);
      setCancelTarget(null);
    } catch (err) {
      setCancelError(err.data?.message || err.message || "No se pudo cancelar la sesión.");
    }
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-4 md:px-6 py-4 font-body text-white">
      <div className="max-w-6xl mx-auto space-y-4">
        <div className="flex items-center justify-between gap-4 border-b border-neutral-800 pb-3 flex-wrap">
          <div>
            <h1 className="text-xl font-bold text-white tracking-tight">Sesiones de Clase</h1>
            <p className="text-neutral-400 text-xs mt-0.5">
              Instancias reales de clase. Creadas automáticamente por horario o de forma manual.
            </p>
          </div>
          <div className="flex items-center gap-2">
            {/* Botón manual además del refetch automático al montar — la sesión de un horario
                recién creado puede tardar hasta la siguiente pasada del servidor en aparecer
                (ver nota en Horarios), así que conviene poder pedirla sin recargar la página. */}
            <button
              onClick={handleRefresh}
              disabled={refreshing}
              title="Volver a pedir las sesiones al servidor"
              className="flex items-center gap-1.5 text-xs font-semibold text-neutral-400 hover:text-white px-3 py-1.5 rounded-xl border border-neutral-800 hover:border-neutral-700 transition-colors disabled:opacity-50"
            >
              <RefreshCw size={13} className={refreshing ? "animate-spin" : ""} />
              {refreshing ? "Actualizando…" : "Actualizar"}
            </button>
            {cancelledCount > 0 && (
              <button
                onClick={() => setShowCancelled((v) => !v)}
                className="text-xs font-semibold text-neutral-400 hover:text-white px-3 py-1.5 rounded-xl border border-neutral-800 hover:border-neutral-700 transition-colors"
              >
                {showCancelled ? "Ocultar canceladas" : `Mostrar canceladas (${cancelledCount})`}
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

        {visibleSessions.length === 0 ? (
          <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-8 text-center text-neutral-500 text-sm">
            {sessions.length === 0
              ? "No hay sesiones de clase registradas actualmente."
              : "No hay sesiones activas — todas las de este rango están canceladas."}
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
            {visibleSessions.map((session) => {
              const teacher = teacherName(session.teacherUserId);
              return (
                <div
                  key={session.id}
                  className={`bg-neutral-900 border rounded-2xl p-5 flex flex-col justify-between transition-colors ${
                    session.cancelled ? "border-red-500/30 opacity-75" : "border-neutral-800"
                  }`}
                >
                  <div>
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-xs font-semibold px-2.5 py-1 rounded-md bg-neutral-800 text-neutral-300">
                        {formatDate(session.date)}
                      </span>
                      {session.cancelled ? (
                        <span className="text-xs font-bold text-red-400 bg-red-500/10 border border-red-500/20 px-2 py-0.5 rounded-md">
                          Cancelada
                        </span>
                      ) : (
                        <span className="text-xs font-semibold text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-md">
                          Activa
                        </span>
                      )}
                    </div>

                    <h3 className="text-xl font-bold text-white mb-1">
                      {formatTimeOfDay(session.startTime)} - {formatTimeOfDay(session.endTime)}
                    </h3>

                    {reassigningId === session.id ? (
                      <div className="mb-4">
                        <select
                          autoFocus
                          defaultValue=""
                          disabled={reassignSaving}
                          onChange={(e) => handleReassignTeacher(session.id, e.target.value)}
                          className="w-full bg-neutral-800 border border-neutral-700 rounded-lg px-2 py-1.5 text-white text-xs outline-none focus:border-emerald-500"
                        >
                          <option value="" disabled>
                            {reassignSaving ? "Guardando…" : "Elegir profesor…"}
                          </option>
                          {teachersList.map((t) => (
                            <option key={t.userId || t.id} value={t.userId || t.id}>
                              {t.fullName || `${t.firstName} ${t.lastName}`}
                            </option>
                          ))}
                        </select>
                        <button
                          onClick={() => {
                            setReassigningId(null);
                            setReassignError("");
                          }}
                          disabled={reassignSaving}
                          className="text-neutral-500 hover:text-white text-[11px] mt-1"
                        >
                          Cancelar
                        </button>
                        {reassignError && <p className="text-rose-400 text-[11px] mt-1">{reassignError}</p>}
                      </div>
                    ) : (
                      <p className="text-sm text-neutral-400 mb-4">
                        Profesor(a): <span className="text-neutral-200">{teacher}</span>
                        {!session.cancelled && (
                          <button
                            onClick={() => {
                              setReassignError("");
                              setReassigningId(session.id);
                            }}
                            className="ml-2 text-emerald-400 hover:text-emerald-300 text-xs font-semibold underline underline-offset-2"
                          >
                            {session.teacherUserId ? "Cambiar" : "Asignar"}
                          </button>
                        )}
                      </p>
                    )}
                  </div>

                  <div className="pt-4 border-t border-neutral-800/80 flex items-center justify-between text-xs text-neutral-400">
                    <div className="flex gap-3">
                      <span className="flex items-center gap-1">
                        <Users size={14} className="text-neutral-500" /> {session.totalCheckins || 0} check-ins
                      </span>
                      <span className="flex items-center gap-1">
                        <CheckCircle2 size={14} className="text-emerald-400" /> {session.totalAttended || 0} asistieron
                      </span>
                    </div>

                    {!session.cancelled && (
                      <button
                        onClick={() => setCancelTarget(session.id)}
                        className="text-red-400 hover:text-red-300 font-semibold transition-colors"
                      >
                        Cancelar
                      </button>
                    )}
                  </div>
                </div>
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
              La sesión quedará marcada como cancelada y los miembros serán notificados.
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
