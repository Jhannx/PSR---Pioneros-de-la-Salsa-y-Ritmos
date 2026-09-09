import { useState } from "react";
import { Plus, Pencil, X, CalendarOff, Power } from "lucide-react";
import { timeLabel } from "../../features/admin/utils";

const DAY_LABELS = ["Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"];

// dia_semana: ISO, 1 = lunes ... 7 = domingo — mismo criterio que la BD.
const initialSchedules = [
  { id: 1, dayOfWeek: 1, startTime: "06:00", endTime: "07:00", active: true, exceptions: [] },
  { id: 2, dayOfWeek: 1, startTime: "18:00", endTime: "19:00", active: true, exceptions: [{ id: 1, date: "2026-09-16", reason: "Festivo" }] },
  { id: 3, dayOfWeek: 3, startTime: "06:00", endTime: "07:00", active: true, exceptions: [] },
  { id: 4, dayOfWeek: 5, startTime: "18:00", endTime: "19:00", active: false, exceptions: [] },
  { id: 5, dayOfWeek: 6, startTime: "09:00", endTime: "10:00", active: true, exceptions: [] },
];

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function SchedulePage() {
  const [schedules, setSchedules] = useState(initialSchedules);
  const [formModal, setFormModal] = useState(null); // null | { mode: 'add' } | { mode: 'edit', schedule }
  const [exceptionModal, setExceptionModal] = useState(null); // null | scheduleId

  const [dayOfWeek, setDayOfWeek] = useState(1);
  const [startTime, setStartTime] = useState("06:00");
  const [endTime, setEndTime] = useState("07:00");

  const [exceptionDate, setExceptionDate] = useState("");
  const [exceptionReason, setExceptionReason] = useState("");

  function openAdd() {
    setDayOfWeek(1);
    setStartTime("06:00");
    setEndTime("07:00");
    setFormModal({ mode: "add" });
  }

  function openEdit(schedule) {
    setDayOfWeek(schedule.dayOfWeek);
    setStartTime(schedule.startTime);
    setEndTime(schedule.endTime);
    setFormModal({ mode: "edit", schedule });
  }

  function saveSchedule() {
    if (formModal.mode === "add") {
      setSchedules((prev) => [
        ...prev,
        { id: Math.max(0, ...prev.map((s) => s.id)) + 1, dayOfWeek, startTime, endTime, active: true, exceptions: [] },
      ]);
    } else {
      setSchedules((prev) =>
        prev.map((s) => (s.id === formModal.schedule.id ? { ...s, dayOfWeek, startTime, endTime } : s))
      );
    }
    setFormModal(null);
  }

  function toggleActive(id) {
    setSchedules((prev) => prev.map((s) => (s.id === id ? { ...s, active: !s.active } : s)));
  }

  function addException() {
    if (!exceptionDate.trim()) return;
    setSchedules((prev) =>
      prev.map((s) =>
        s.id === exceptionModal
          ? { ...s, exceptions: [...s.exceptions, { id: Date.now(), date: exceptionDate, reason: exceptionReason.trim() || null }] }
          : s
      )
    );
    setExceptionDate("");
    setExceptionReason("");
    setExceptionModal(null);
  }

  const activeSchedule = schedules.find((s) => s.id === exceptionModal);

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-6 py-8">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-start justify-between gap-4 mb-6 flex-wrap">
          <div>
            <h1 className="text-2xl font-bold text-white">Horarios</h1>
            <p className="text-neutral-500 text-sm mt-1">
              Programación recurrente de clases. Cada horario genera una sesión automática cuando llega su turno.
            </p>
          </div>
          <button
            onClick={openAdd}
            className="flex items-center gap-2 bg-white text-neutral-950 hover:bg-neutral-200 font-semibold text-sm px-4 py-2.5 rounded-xl transition-colors"
          >
            <Plus size={16} />
            Nuevo horario
          </button>
        </div>

        <div className="flex items-center gap-4 px-4 pb-2 text-xs font-semibold text-neutral-600 uppercase tracking-wide">
          <span className="w-28">Día</span>
          <span className="flex-1">Horario</span>
          <span className="w-40">Excepciones</span>
          <span className="w-20 text-right">Estado</span>
          <span className="w-20"></span>
        </div>

        <div className="flex flex-col gap-2">
          {schedules.map((schedule) => (
            <div
              key={schedule.id}
              className={`flex items-center gap-4 px-4 py-3.5 rounded-2xl border transition-colors ${
                schedule.active ? "bg-neutral-900 border-neutral-800" : "bg-neutral-900/40 border-neutral-800/60"
              }`}
            >
              <span className={`w-28 font-semibold ${schedule.active ? "text-white" : "text-neutral-600"}`}>
                {DAY_LABELS[schedule.dayOfWeek - 1]}
              </span>
              <span className={`flex-1 ${schedule.active ? "text-neutral-300" : "text-neutral-600"}`}>
                {timeLabel(schedule.startTime, schedule.endTime)}
              </span>
              <button
                onClick={() => setExceptionModal(schedule.id)}
                className="w-40 flex items-center gap-1.5 text-neutral-500 hover:text-white text-sm transition-colors"
              >
                <CalendarOff size={14} />
                {schedule.exceptions.length > 0 ? `${schedule.exceptions.length} declarada(s)` : "Sin excepciones"}
              </button>
              <div className="w-20 flex justify-end">
                <button
                  onClick={() => toggleActive(schedule.id)}
                  className={`text-xs font-semibold px-2.5 py-1 rounded-full transition-colors ${
                    schedule.active
                      ? "bg-emerald-500/15 text-emerald-400"
                      : "bg-neutral-800 text-neutral-500"
                  }`}
                >
                  {schedule.active ? "Activo" : "Inactivo"}
                </button>
              </div>
              <div className="w-20 flex justify-end gap-1">
                <button
                  onClick={() => openEdit(schedule)}
                  className="p-2 text-neutral-500 hover:text-white hover:bg-neutral-800 rounded-lg transition-colors"
                >
                  <Pencil size={15} />
                </button>
              </div>
            </div>
          ))}
        </div>
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

            <button
              onClick={saveSchedule}
              className="w-full bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
            >
              Guardar
            </button>
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

            {activeSchedule.exceptions.length > 0 && (
              <div className="flex flex-col gap-2 mb-4">
                {activeSchedule.exceptions.map((exception) => (
                  <div key={exception.id} className="bg-neutral-800/60 rounded-xl px-3 py-2.5">
                    <p className="text-white text-sm font-semibold">{exception.date}</p>
                    {exception.reason && <p className="text-neutral-500 text-xs mt-0.5">{exception.reason}</p>}
                  </div>
                ))}
              </div>
            )}

            <label className="text-neutral-400 text-sm font-medium">Fecha en que no aplica</label>
            <input
              type="date"
              value={exceptionDate}
              onChange={(e) => setExceptionDate(e.target.value)}
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
          </div>
        </div>
      )}
    </div>
  );
}
