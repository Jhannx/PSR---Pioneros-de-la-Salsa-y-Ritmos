import { useState } from "react";
import { Plus, X, Ban, Users, CheckCircle2 } from "lucide-react";
import { formatShortWeekdayDate as formatDate } from "../../utils/date";
import { TEACHERS, teacherName } from "../../features/admin/utils";

const initialSessions = [
  { id: 41, date: "2026-08-27", startTime: "06:00", endTime: "07:00", teacherUserId: 12, cancelled: false, totalCheckins: 18, totalAttended: 15 },
  { id: 40, date: "2026-08-26", startTime: "18:00", endTime: "19:00", teacherUserId: 18, cancelled: false, totalCheckins: 11, totalAttended: 11 },
  { id: 39, date: "2026-08-25", startTime: "06:00", endTime: "07:00", teacherUserId: null, cancelled: true, totalCheckins: 3, totalAttended: 0 },
  { id: 38, date: "2026-08-24", startTime: "09:00", endTime: "10:00", teacherUserId: 12, cancelled: false, totalCheckins: 22, totalAttended: 20 },
];

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-3 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function SessionsPage() {
  const [sessions, setSessions] = useState(initialSessions);
  const [showAddModal, setShowAddModal] = useState(false);
  const [cancelTarget, setCancelTarget] = useState(null);

  const [date, setDate] = useState("");
  const [startTime, setStartTime] = useState("06:00");
  const [endTime, setEndTime] = useState("07:00");
  const [teacherUserId, setTeacherUserId] = useState("");

  function createSession() {
    if (!date.trim()) return;
    setSessions((prev) => [
      {
        id: Math.max(0, ...prev.map((s) => s.id)) + 1,
        date,
        startTime,
        endTime,
        teacherUserId: teacherUserId ? Number(teacherUserId) : null,
        cancelled: false,
        totalCheckins: 0,
        totalAttended: 0,
      },
      ...prev,
    ]);
    setShowAddModal(false);
    setDate("");
    setTeacherUserId("");
  }

  function confirmCancel() {
    setSessions((prev) => prev.map((s) => (s.id === cancelTarget ? { ...s, cancelled: true } : s)));
    setCancelTarget(null);
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-6 py-8">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-start justify-between gap-4 mb-6 flex-wrap">
          <div>
            <h1 className="text-2xl font-bold text-white">Sesiones</h1>
            <p className="text-neutral-500 text-sm mt-1">
              Instancias reales de una clase. Se crean solas desde los horarios, o manualmente para talleres puntuales.
            </p>
          </div>
          <button
            onClick={() => setShowAddModal(true)}
            className="flex items-center gap-2 bg-white text-neutral-950 hover:bg-neutral-200 font-semibold text-sm px-4 py-2.5 rounded-xl transition-colors"
          >
            <Plus size={16} />
            Crear sesión manual
          </button>
        </div>

        <div className="flex items-center gap-4 px-4 pb-2 text-xs font-semibold text-neutral-600 uppercase tracking-wide">
          <span className="w-32">Fecha</span>
          <span className="w-28">Horario</span>
          <span className="flex-1">Profesor</span>
          <span className="w-28 text-right">Llegadas</span>
          <span className="w-28 text-right">Atendidos</span>
          <span className="w-24"></span>
        </div>

        <div className="flex flex-col gap-2">
          {sessions.map((session) => (
            <div
              key={session.id}
              className={`flex items-center gap-4 px-4 py-3.5 rounded-2xl border ${
                session.cancelled ? "bg-neutral-900/40 border-neutral-800/60" : "bg-neutral-900 border-neutral-800"
              }`}
            >
              <span className={`w-32 font-semibold capitalize ${session.cancelled ? "text-neutral-600 line-through" : "text-white"}`}>
                {formatDate(session.date)}
              </span>
              <span className={`w-28 ${session.cancelled ? "text-neutral-600" : "text-neutral-300"}`}>
                {session.startTime} – {session.endTime}
              </span>
              <span className={`flex-1 ${session.cancelled ? "text-neutral-600" : "text-neutral-300"}`}>
                {teacherName(session.teacherUserId)}
              </span>
              <span className="w-28 flex items-center justify-end gap-1.5 text-neutral-400">
                <Users size={13} />
                {session.totalCheckins}
              </span>
              <span className="w-28 flex items-center justify-end gap-1.5 text-emerald-400">
                <CheckCircle2 size={13} />
                {session.totalAttended}
              </span>
              <div className="w-24 flex justify-end">
                {session.cancelled ? (
                  <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-red-500/15 text-red-400">
                    Cancelada
                  </span>
                ) : (
                  <button
                    onClick={() => setCancelTarget(session.id)}
                    className="flex items-center gap-1.5 text-neutral-500 hover:text-red-400 text-sm transition-colors"
                  >
                    <Ban size={14} />
                    Cancelar
                  </button>
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Modal: crear sesión manual */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 w-full max-w-sm">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-white font-bold text-lg">Crear sesión manual</h2>
              <button onClick={() => setShowAddModal(false)} className="text-neutral-500 hover:text-white">
                <X size={20} />
              </button>
            </div>

            <label className="text-neutral-400 text-sm font-medium">Fecha</label>
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className={`${inputClass} mt-1.5 mb-3`}
            />

            <div className="grid grid-cols-2 gap-3 mb-3">
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

            <label className="text-neutral-400 text-sm font-medium">Profesor (opcional)</label>
            <select
              value={teacherUserId}
              onChange={(e) => setTeacherUserId(e.target.value)}
              className={`${inputClass} mt-1.5 mb-4`}
            >
              <option value="">Sin asignar</option>
              {TEACHERS.map((teacher) => (
                <option key={teacher.userId} value={teacher.userId}>
                  {teacher.fullName}
                </option>
              ))}
            </select>

            <button
              onClick={createSession}
              disabled={!date.trim()}
              className="w-full bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
            >
              Crear sesión
            </button>
          </div>
        </div>
      )}

      {/* Modal: confirmar cancelación */}
      {cancelTarget && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center px-4 z-50">
          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 w-full max-w-sm text-center">
            <div className="w-14 h-14 rounded-2xl bg-red-500/15 border-2 border-red-500 flex items-center justify-center mx-auto mb-4">
              <Ban className="text-red-400" size={24} />
            </div>
            <h2 className="text-white font-bold text-lg mb-1.5">¿Cancelar esta sesión?</h2>
            <p className="text-neutral-500 text-sm mb-6">
              Las llegadas y asistencias ya registradas no se borran, solo queda marcada como cancelada.
            </p>
            <div className="flex gap-2">
              <button
                onClick={() => setCancelTarget(null)}
                className="flex-1 border border-neutral-700 text-neutral-300 hover:bg-neutral-800 font-semibold py-2.5 rounded-xl transition-colors"
              >
                Volver
              </button>
              <button
                onClick={confirmCancel}
                className="flex-1 bg-red-500 hover:bg-red-400 text-neutral-950 font-bold py-2.5 rounded-xl transition-colors"
              >
                Cancelar sesión
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
