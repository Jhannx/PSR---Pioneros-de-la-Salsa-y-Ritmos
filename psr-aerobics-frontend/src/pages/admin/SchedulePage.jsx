import { useEffect, useState } from "react";
import { Plus, Pencil, X, CalendarOff, Trash2 } from "lucide-react";
import { timeLabel } from "../../features/admin/utils";
import { useAuth } from "../../context/AuthContext";
import { sessionsService } from "../../services/sessionsService";

const DAY_LABELS = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"];

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

// A partir de F2 el backend rechaza con 400 una excepción cuya fecha no caiga en el mismo día
// de la semana que declara el horario (NOTAS-PARA-FRONTEND.md §4). El 400 es la red de
// seguridad, no la experiencia — este date-picker no debería dejar llegar a él en uso normal.

// "YYYY-MM-DD" → día ISO (1 = lunes … 7 = domingo). Se parsea como UTC a propósito: construir
// un Date a partir de un string "YYYY-MM-DD" con el constructor normal lo interpreta en la
// zona horaria local, y cerca de medianoche eso puede correr la fecha un día — parsear como UTC
// y comparar en UTC evita ese desfase.
function isoDayOfWeek(dateStr) {
  const [y, m, d] = dateStr.split("-").map(Number);
  const utcDay = new Date(Date.UTC(y, m - 1, d)).getUTCDay(); // 0 = domingo … 6 = sábado
  return utcDay === 0 ? 7 : utcDay;
}

// Próxima fecha (hoy incluido) cuyo día ISO coincide con `dayOfWeek` — se usa para preseleccionar
// el campo con un valor ya válido y para sugerir una alternativa cuando la persona elige uno que
// no coincide.
function nextDateForDayOfWeek(dayOfWeek) {
  const today = new Date();
  const from = new Date(Date.UTC(today.getFullYear(), today.getMonth(), today.getDate()));
  const fromIso = from.getUTCDay() === 0 ? 7 : from.getUTCDay();
  const diff = (dayOfWeek - fromIso + 7) % 7;
  from.setUTCDate(from.getUTCDate() + diff);
  return from.toISOString().slice(0, 10);
}

export default function SchedulePage() {
  const { user } = useAuth();
  const isAdmin = user.roles.includes("ADMIN");

  const [schedules, setSchedules] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [formModal, setFormModal] = useState(null); // null | { mode: 'add' } | { mode: 'edit', schedule }
  const [formError, setFormError] = useState("");
  const [saving, setSaving] = useState(false);

  const [deleteTarget, setDeleteTarget] = useState(null);
  const [deleteError, setDeleteError] = useState("");

  const [exceptionModal, setExceptionModal] = useState(null); // null | scheduleId
  const [exceptions, setExceptions] = useState([]);
  const [exceptionsLoading, setExceptionsLoading] = useState(false);
  const [exceptionError, setExceptionError] = useState("");

  const [dayOfWeek, setDayOfWeek] = useState(1);
  const [startTime, setStartTime] = useState("06:00");
  const [endTime, setEndTime] = useState("07:00");

  const [exceptionDate, setExceptionDate] = useState("");
  const [exceptionReason, setExceptionReason] = useState("");

  async function loadSchedules() {
    setLoading(true);
    setError("");
    try {
      const data = await sessionsService.getSchedules();
      setSchedules(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.data?.message || err.message || "No se pudo cargar los horarios.");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    Promise.resolve().then(loadSchedules);
  }, []);

  function openAdd() {
    setDayOfWeek(1);
    setStartTime("06:00");
    setEndTime("07:00");
    setFormError("");
    setFormModal({ mode: "add" });
  }

  function openEdit(schedule) {
    setDayOfWeek(schedule.dayOfWeek);
    setStartTime(schedule.startTime);
    setEndTime(schedule.endTime);
    setFormError("");
    setFormModal({ mode: "edit", schedule });
  }

  async function saveSchedule() {
    setSaving(true);
    setFormError("");
    try {
      if (formModal.mode === "add") {
        await sessionsService.createSchedule({ dayOfWeek, startTime, endTime });
      } else {
        await sessionsService.updateSchedule(formModal.schedule.id, { dayOfWeek, startTime, endTime });
      }
      setFormModal(null);
      await loadSchedules();
    } catch (err) {
      setFormError(err.data?.message || err.message || "No se pudo guardar el horario.");
    } finally {
      setSaving(false);
    }
  }

  async function confirmDelete() {
    if (!deleteTarget) return;
    setDeleteError("");
    try {
      await sessionsService.deleteSchedule(deleteTarget);
      setDeleteTarget(null);
      await loadSchedules();
    } catch (err) {
      setDeleteError(err.data?.message || err.message || "No se pudo eliminar el horario.");
    }
  }

  async function openExceptions(scheduleId) {
    const schedule = schedules.find((s) => s.id === scheduleId);
    setExceptionModal(scheduleId);
    setExceptionError("");
    // Precargado con una fecha ya válida (el próximo día de ese horario) en vez de vacío — así
    // el caso normal es simplemente confirmar, no adivinar qué fecha sirve.
    setExceptionDate(schedule ? nextDateForDayOfWeek(schedule.dayOfWeek) : "");
    setExceptionReason("");
    setExceptionsLoading(true);
    try {
      const data = await sessionsService.getScheduleExceptions(scheduleId);
      setExceptions(Array.isArray(data) ? data : []);
    } catch (err) {
      setExceptionError(err.data?.message || err.message || "No se pudo cargar las excepciones.");
    } finally {
      setExceptionsLoading(false);
    }
  }

  async function addException() {
    if (!exceptionDate.trim() || !exceptionModal) return;
    setExceptionError("");
    try {
      const created = await sessionsService.createScheduleException(exceptionModal, {
        date: exceptionDate,
        reason: exceptionReason.trim() || undefined,
      });
      setExceptions((prev) => [...prev, created]);
      setExceptionDate("");
      setExceptionReason("");
    } catch (err) {
      setExceptionError(err.data?.message || err.message || "No se pudo declarar la excepción.");
    }
  }

  // Rechaza en el cliente cualquier fecha que no caiga en el día declarado del horario, en vez
  // de dejar que se intente guardar y falle con el 400 del backend. Al ser un <input> controlado,
  // no llamar a setExceptionDate acá hace que el valor "rebote" de vuelta al último válido.
  function handleExceptionDateChange(value) {
    if (!value || !activeSchedule || isoDayOfWeek(value) === activeSchedule.dayOfWeek) {
      setExceptionError("");
      setExceptionDate(value);
      return;
    }
    setExceptionError(
      `Esa fecha no es ${DAY_LABELS[activeSchedule.dayOfWeek - 1].toLowerCase()}, que es el día de este horario. ` +
        `Por ejemplo, el ${nextDateForDayOfWeek(activeSchedule.dayOfWeek)} sí lo es.`
    );
  }

  async function removeException(exceptionId) {
    setExceptionError("");
    try {
      await sessionsService.deleteScheduleException(exceptionId);
      setExceptions((prev) => prev.filter((e) => e.id !== exceptionId));
    } catch (err) {
      setExceptionError(err.data?.message || err.message || "No se pudo eliminar la excepción.");
    }
  }

  const activeSchedule = schedules.find((s) => s.id === exceptionModal);

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-4 md:px-6 py-4 text-white font-body">
      <div className="max-w-6xl mx-auto space-y-4">
        <div className="flex items-center justify-between gap-4 border-b border-neutral-800 pb-3 flex-wrap">
          <div>
            <h1 className="text-xl font-bold text-white tracking-tight">Horarios Recurrentes</h1>
            <p className="text-neutral-400 text-xs mt-0.5">
              Programación semanal de clases. Genera las sesiones automáticas.
            </p>
          </div>
          {isAdmin && (
            <button
              onClick={openAdd}
              className="flex items-center gap-1.5 bg-white text-neutral-950 hover:bg-neutral-200 font-bold text-xs px-3.5 py-1.5 rounded-xl transition-colors shadow-md"
            >
              <Plus size={14} />
              Nuevo horario
            </button>
          )}
        </div>

        {error && (
          <div className="p-4 bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm rounded-2xl">
            {error}
          </div>
        )}

        {!error && (
          <>
            <div className="flex items-center gap-4 px-4 pb-2 text-xs font-semibold text-neutral-600 uppercase tracking-wide">
              <span className="w-28">Día</span>
              <span className="flex-1">Horario</span>
              <span className="w-40">Excepciones</span>
              <span className="w-20 text-right">Estado</span>
              <span className="w-20"></span>
            </div>

            <div className="flex flex-col gap-2">
              {loading ? (
                <div className="p-8 text-center text-neutral-500 text-sm">Cargando horarios…</div>
              ) : schedules.length === 0 ? (
                <div className="p-8 text-center text-neutral-500 text-sm">No hay horarios registrados.</div>
              ) : (
                schedules.map((schedule) => (
                  <div
                    key={schedule.id}
                    className={`flex items-center gap-4 px-4 py-3.5 rounded-2xl border transition-colors ${
                      schedule.active !== false ? "bg-neutral-900 border-neutral-800" : "bg-neutral-900/40 border-neutral-800/60"
                    }`}
                  >
                    <span className={`w-28 font-semibold ${schedule.active !== false ? "text-white" : "text-neutral-600"}`}>
                      {DAY_LABELS[schedule.dayOfWeek - 1]}
                    </span>
                    <span className={`flex-1 ${schedule.active !== false ? "text-neutral-300" : "text-neutral-600"}`}>
                      {timeLabel(schedule.startTime, schedule.endTime)}
                    </span>
                    <button
                      onClick={() => openExceptions(schedule.id)}
                      className="w-40 flex items-center gap-1.5 text-neutral-500 hover:text-white text-sm transition-colors"
                    >
                      <CalendarOff size={14} />
                      Ver excepciones
                    </button>
                    <div className="w-20 flex justify-end">
                      <span
                        title="El backend todavía no expone cómo activar/desactivar un horario sin borrarlo"
                        className={`text-xs font-semibold px-2.5 py-1 rounded-full ${
                          schedule.active !== false
                            ? "bg-emerald-500/15 text-emerald-400"
                            : "bg-neutral-800 text-neutral-500"
                        }`}
                      >
                        {schedule.active !== false ? "Activo" : "Inactivo"}
                      </span>
                    </div>
                    <div className="w-20 flex justify-end gap-1">
                      {isAdmin && (
                        <>
                          <button
                            onClick={() => openEdit(schedule)}
                            className="p-2 text-neutral-500 hover:text-white hover:bg-neutral-800 rounded-lg transition-colors"
                          >
                            <Pencil size={15} />
                          </button>
                          <button
                            onClick={() => {
                              setDeleteTarget(schedule.id);
                              setDeleteError("");
                            }}
                            className="p-2 text-neutral-500 hover:text-red-400 hover:bg-neutral-800 rounded-lg transition-colors"
                          >
                            <Trash2 size={15} />
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                ))
              )}
            </div>
          </>
        )}
      </div>

      {/* Modal: crear/editar horario */}
      {formModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 w-full max-w-sm">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-white font-bold text-lg">
                {formModal.mode === "add" ? "Nuevo horario" : "Editar horario"}
              </h2>
              <button onClick={() => setFormModal(null)} className="text-neutral-500 hover:text-white">
                <X size={20} />
              </button>
            </div>

            <label className="text-neutral-400 text-sm font-medium">Día de la semana</label>
            <select
              value={dayOfWeek}
              onChange={(e) => setDayOfWeek(Number(e.target.value))}
              className={`${inputClass} mt-1.5 mb-4`}
            >
              {DAY_LABELS.map((label, index) => (
                <option key={label} value={index + 1}>
                  {label}
                </option>
              ))}
            </select>

            <div className="grid grid-cols-2 gap-3 mb-4">
              <div>
                <label className="text-neutral-400 text-sm font-medium">Hora inicio</label>
                <input
                  type="time"
                  value={startTime}
                  onChange={(e) => setStartTime(e.target.value)}
                  className={`${inputClass} mt-1.5`}
                />
              </div>
              <div>
                <label className="text-neutral-400 text-sm font-medium">Hora fin</label>
                <input
                  type="time"
                  value={endTime}
                  onChange={(e) => setEndTime(e.target.value)}
                  className={`${inputClass} mt-1.5`}
                />
              </div>
            </div>

            {formError && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl">
                {formError}
              </div>
            )}

            <button
              onClick={saveSchedule}
              disabled={saving}
              className="w-full bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
            >
              {saving ? "Guardando..." : "Guardar"}
            </button>
          </div>
        </div>
      )}

      {/* Modal: confirmar eliminación */}
      {deleteTarget && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 w-full max-w-sm text-center">
            <h2 className="text-white font-bold text-lg mb-2">¿Eliminar este horario?</h2>
            <p className="text-neutral-400 text-xs mb-4">
              Ya no se generarán nuevas sesiones automáticas a partir de él.
            </p>
            {deleteError && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl">
                {deleteError}
              </div>
            )}
            <div className="flex justify-center gap-3">
              <button
                onClick={() => setDeleteTarget(null)}
                className="px-4 py-2 bg-neutral-800 text-neutral-300 hover:bg-neutral-700 text-sm font-semibold rounded-xl"
              >
                Volver
              </button>
              <button
                onClick={confirmDelete}
                className="px-4 py-2 bg-red-500 hover:bg-red-400 text-white text-sm font-bold rounded-xl"
              >
                Sí, eliminar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: excepciones de un horario */}
      {exceptionModal && activeSchedule && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 w-full max-w-sm">
            <div className="flex items-center justify-between mb-1">
              <h2 className="text-white font-bold text-lg">Excepciones</h2>
              <button onClick={() => setExceptionModal(null)} className="text-neutral-500 hover:text-white">
                <X size={20} />
              </button>
            </div>
            <p className="text-neutral-500 text-sm mb-4">
              {DAY_LABELS[activeSchedule.dayOfWeek - 1]} · {timeLabel(activeSchedule.startTime, activeSchedule.endTime)}
            </p>

            {exceptionError && (
              <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl">
                {exceptionError}
              </div>
            )}

            {exceptionsLoading ? (
              <p className="text-neutral-500 text-sm mb-4">Cargando excepciones…</p>
            ) : exceptions.length > 0 ? (
              <div className="flex flex-col gap-2 mb-4">
                {exceptions.map((exception) => (
                  <div
                    key={exception.id}
                    className="bg-neutral-800/60 rounded-xl px-3 py-2.5 flex items-center justify-between gap-2"
                  >
                    <div>
                      <p className="text-white text-sm font-semibold">{exception.date}</p>
                      {exception.reason && <p className="text-neutral-500 text-xs mt-0.5">{exception.reason}</p>}
                    </div>
                    {isAdmin && (
                      <button
                        onClick={() => removeException(exception.id)}
                        className="p-1.5 text-neutral-500 hover:text-red-400 rounded-lg transition-colors shrink-0"
                      >
                        <Trash2 size={14} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-neutral-500 text-sm mb-4">Sin excepciones declaradas.</p>
            )}

            {isAdmin && (
              <>
                <label className="text-neutral-400 text-sm font-medium">
                  Fecha en que no aplica{" "}
                  <span className="text-neutral-600 font-normal">
                    (debe ser {DAY_LABELS[activeSchedule.dayOfWeek - 1].toLowerCase()})
                  </span>
                </label>
                <input
                  type="date"
                  value={exceptionDate}
                  onChange={(e) => handleExceptionDateChange(e.target.value)}
                  className={`${inputClass} mt-1.5 mb-3`}
                />
                <label className="text-neutral-400 text-sm font-medium">Motivo (opcional)</label>
                <input
                  value={exceptionReason}
                  onChange={(e) => setExceptionReason(e.target.value)}
                  placeholder="Ej. Festivo, mantenimiento"
                  className={`${inputClass} mt-1.5 mb-4`}
                />

                <button
                  onClick={addException}
                  disabled={!exceptionDate.trim()}
                  className="w-full bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
                >
                  Declarar excepción
                </button>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
