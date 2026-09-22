import { useState, useEffect, useCallback, useMemo } from "react";
import { RefreshCw, Users, CheckCircle2, Flag, ChevronDown, ChevronUp, Clock } from "lucide-react";
import { formatTimeOfDay } from "../../utils/date";
import { useMockData } from "../../context/DataContext";
import { sessionsService } from "../../services/sessionsService";
import { getWeekRange, formatDayHeading, groupSessionsByDate } from "../../features/sessions/weekUtils";
import SessionStatusBadge from "../../features/sessions/SessionStatusBadge";
import WeekNavigator from "../../features/sessions/WeekNavigator";

// "Mis sesiones" del profesor (docs/correciones 20-09.md: "hoy el rol profesor es inútil, ya que
// como profesor no puedo ver mis sesiones de clases"). Muestra SOLO las sesiones asignadas a quien
// está logueado — el servidor saca al profesor del token, esta pantalla no manda ningún id — de la
// semana elegida (lunes a domingo), agrupadas por día. Desde acá el profesor puede finalizar las
// suyas y ver quién asistió a cada una, incluidos los que llegaron y no fueron confirmados.
// Qué sesiones son suyas, si se puede finalizar (403 si la sesión no es suya) y su estado los decide
// el servidor.
export default function TeacherSessionsPage() {
  const { sessions: contextSessions, closeSession } = useMockData();

  const [weekOffset, setWeekOffset] = useState(0);
  const [list, setList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");

  const [closingId, setClosingId] = useState(null);
  const [closeError, setCloseError] = useState("");

  // Asistentes por sesión, pedidos solo al abrir la tarjeta: { [sessionId]: { loading, error, list } }.
  const [openId, setOpenId] = useState(null);
  const [attendees, setAttendees] = useState({});

  const week = useMemo(() => getWeekRange(weekOffset), [weekOffset]);

  const load = useCallback(async () => {
    setLoading(true);
    setLoadError("");
    try {
      // `/mias` ya trae los totales (`totalCheckins`, `totalAttended`) y las banderas `canClose` /
      // `canCancel` (NOTAS-PARA-FRONTEND.md Bloque 5, punto 6): una sola respuesta, sin cruzar nada.
      const raw = await sessionsService.getMySessions(week.desde, week.hasta);
      setList(Array.isArray(raw) ? raw : raw?.content || []);
    } catch (err) {
      setLoadError(err.data?.message || err.message || "No se pudieron cargar tus sesiones.");
      setList([]);
    } finally {
      setLoading(false);
    }
  }, [week]);

  // Se relee al cambiar de semana y cuando `DataContext` se entera de un cambio de sesiones
  // (`SESSION_CHANGED` por WebSocket: crear, cerrar o cancelar una).
  useEffect(() => {
    Promise.resolve().then(load);
  }, [load, contextSessions]);

  const grouped = useMemo(() => groupSessionsByDate(list), [list]);

  async function toggleAttendees(sessionId) {
    if (openId === sessionId) {
      setOpenId(null);
      return;
    }
    setOpenId(sessionId);
    setAttendees((prev) => ({ ...prev, [sessionId]: { loading: true, error: "", list: [] } }));
    try {
      const data = await sessionsService.getSessionAttendees(sessionId);
      setAttendees((prev) => ({
        ...prev,
        [sessionId]: { loading: false, error: "", list: Array.isArray(data) ? data : data?.content || [] },
      }));
    } catch (err) {
      setAttendees((prev) => ({
        ...prev,
        [sessionId]: {
          loading: false,
          error: err.data?.message || err.message || "No se pudo cargar la lista de asistentes.",
          list: [],
        },
      }));
    }
  }

  async function handleClose(sessionId) {
    setClosingId(sessionId);
    setCloseError("");
    try {
      await closeSession(sessionId);
      await load();
    } catch (err) {
      // Un 403 llega con su mensaje (la sesión no es suya) — se muestra tal cual.
      setCloseError(err.data?.message || err.message || "No se pudo finalizar la sesión.");
    } finally {
      setClosingId(null);
    }
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-4 md:px-6 py-4 font-body text-white">
      <div className="max-w-5xl mx-auto space-y-4">
        <div className="flex items-center justify-between gap-4 border-b border-neutral-800 pb-3 flex-wrap">
          <div>
            <h1 className="text-xl font-bold text-white tracking-tight">Mis Sesiones</h1>
            <p className="text-neutral-400 text-xs mt-0.5">
              Las sesiones de clase que tienes asignadas, por semana.
            </p>
          </div>
          <button
            onClick={load}
            disabled={loading}
            title="Volver a pedir tus sesiones al servidor"
            className="flex items-center gap-1.5 text-xs font-semibold text-neutral-400 hover:text-white px-3 py-1.5 rounded-xl border border-neutral-800 hover:border-neutral-700 transition-colors disabled:opacity-50"
          >
            <RefreshCw size={13} className={loading ? "animate-spin" : ""} />
            {loading ? "Actualizando…" : "Actualizar"}
          </button>
        </div>

        <WeekNavigator offset={weekOffset} onChange={setWeekOffset} desde={week.desde} hasta={week.hasta} />

        {loadError && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm rounded-xl">{loadError}</div>
        )}
        {closeError && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm rounded-xl">{closeError}</div>
        )}

        {loading && list.length === 0 ? (
          <div className="p-8 text-center text-neutral-500 text-sm">Cargando tus sesiones…</div>
        ) : (
          <div className="space-y-5">
            {list.length === 0 && !loadError && (
              <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 text-center text-neutral-500 text-sm">
                No tienes sesiones asignadas esta semana.
              </div>
            )}

            {week.days.map((dayKey) => {
              const daySessions = grouped.get(dayKey) || [];
              return (
                <section key={dayKey}>
                  <h2 className="text-sm font-bold text-neutral-300 mb-2 flex items-center gap-2">
                    {formatDayHeading(dayKey)}
                    <span className="text-xs font-normal text-neutral-600">
                      {daySessions.length === 0 ? "Sin sesiones" : ""}
                    </span>
                  </h2>

                  <div className="space-y-3">
                    {daySessions.map((session) => {
                      const isCancelled = session.status === "CANCELLED" || session.cancelled;
                      const inProgress = session.status === "IN_PROGRESS";
                      const detail = attendees[session.id];
                      const open = openId === session.id;
                      return (
                        <div
                          key={session.id}
                          className={`bg-neutral-900 border rounded-2xl p-5 ${
                            isCancelled
                              ? "border-red-500/30 opacity-75"
                              : inProgress
                                ? "border-emerald-500/40"
                                : "border-neutral-800"
                          }`}
                        >
                          <div className="flex items-center justify-between gap-3 flex-wrap">
                            <div className="flex items-center gap-3">
                              <Clock size={18} className="text-neutral-500" />
                              <h3 className="text-xl font-bold text-white">
                                {formatTimeOfDay(session.startTime)} - {formatTimeOfDay(session.endTime)}
                              </h3>
                              <SessionStatusBadge status={isCancelled ? "CANCELLED" : session.status} />
                            </div>

                            {session.canClose === true && (
                              <button
                                onClick={() => handleClose(session.id)}
                                disabled={closingId === session.id}
                                className="flex items-center gap-1.5 bg-amber-500 hover:bg-amber-400 text-neutral-950 font-bold text-sm px-4 py-2 rounded-xl transition-colors disabled:opacity-50"
                              >
                                <Flag size={15} />
                                {closingId === session.id ? "Finalizando…" : "Finalizar sesión"}
                              </button>
                            )}
                          </div>

                          <div className="flex items-center justify-between gap-3 mt-4 pt-4 border-t border-neutral-800/80 flex-wrap">
                            <div className="flex gap-4 text-sm text-neutral-400">
                              <span className="flex items-center gap-1.5">
                                <Users size={15} className="text-neutral-500" /> {session.totalCheckins || 0} check-ins
                              </span>
                              <span className="flex items-center gap-1.5">
                                <CheckCircle2 size={15} className="text-emerald-400" /> {session.totalAttended || 0}{" "}
                                asistieron
                              </span>
                            </div>
                            {!isCancelled && (
                              <button
                                onClick={() => toggleAttendees(session.id)}
                                className="flex items-center gap-1 text-xs font-semibold text-emerald-400 hover:text-emerald-300 underline underline-offset-2"
                              >
                                {open ? "Ocultar asistentes" : "Ver quién asistió"}
                                {open ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
                              </button>
                            )}
                          </div>

                          {open && (
                            <div className="mt-3">
                              {detail?.loading && <p className="text-neutral-500 text-sm">Cargando asistentes…</p>}
                              {detail?.error && <p className="text-rose-400 text-sm">{detail.error}</p>}
                              {detail && !detail.loading && !detail.error && detail.list.length === 0 && (
                                <p className="text-neutral-500 text-sm">Nadie ha llegado a esta sesión.</p>
                              )}
                              {detail && detail.list.length > 0 && (
                                <ul className="divide-y divide-neutral-800/80 bg-neutral-950/50 border border-neutral-800 rounded-xl">
                                  {detail.list.map((person) => (
                                    <li
                                      key={person.personId}
                                      className="flex items-center justify-between gap-3 px-4 py-2.5 text-sm"
                                    >
                                      <span className="text-white font-medium truncate">{person.fullName}</span>
                                      <span className="flex items-center gap-3 shrink-0">
                                        {person.arrivalTime && (
                                          <span className="text-neutral-500 text-xs">
                                            Llegó {formatTimeOfDay(person.arrivalTime)}
                                          </span>
                                        )}
                                        {person.attended ? (
                                          <span className="text-emerald-400 text-xs font-semibold">Asistió</span>
                                        ) : (
                                          <span className="text-amber-400 text-xs font-semibold">Sin confirmar</span>
                                        )}
                                      </span>
                                    </li>
                                  ))}
                                </ul>
                              )}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </section>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
