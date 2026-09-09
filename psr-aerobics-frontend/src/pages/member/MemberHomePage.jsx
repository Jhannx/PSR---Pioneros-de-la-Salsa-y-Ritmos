import { useEffect, useState } from "react";
import { CheckCircle2, UserCheck } from "lucide-react";
import { getInitialsFromFullName } from "../../utils/text";
import { useAuth } from "../../context/AuthContext";
import { useMockData } from "../../context/DataContext";
import { peopleService } from "../../services/peopleService";

const WEEK_DAYS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];

export default function MemberHomePage() {
  const { user } = useAuth();
  const { queue, addToQueue } = useMockData();
  const [markedLocally, setMarkedLocally] = useState(false);
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const [checkingIn, setCheckingIn] = useState(false);

  useEffect(() => {
    if (!user?.personId) return;
    peopleService.getProfile(user.personId)
      .then(setProfile)
      .catch((err) => setError(err.data?.message || err.message || "No fue posible cargar tu perfil."));
  }, [user?.personId]);

  const name = profile?.fullName || user?.fullName || "Usuario";
  const initials = getInitialsFromFullName(name);

  // Check if member is in today's reception queue
  const memberQueueItem = queue.find((p) => p.personId === user?.personId);
  const isPaidToday = memberQueueItem ? Number(memberQueueItem.amountApplied || memberQueueItem.amountCharged || 0) > 0 : false;
  const isAttendedToday = memberQueueItem ? memberQueueItem.attended : markedLocally;

  async function handleMarkAttendance() {
    if (!user?.personId || checkingIn) return;
    setCheckingIn(true);
    setError("");
    if (!memberQueueItem) {
      try {
        await addToQueue({ personId: user.personId, checkinMethod: "DOCUMENT" });
      } catch (err) {
        setError(err.data?.message || err.message || "No fue posible registrar la llegada.");
        setCheckingIn(false);
        return;
      }
    }
    setMarkedLocally(true);
    setCheckingIn(false);
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-start justify-center px-4 py-10 font-body">
      <div className="w-full max-w-md">
        {error && <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm">{error}</div>}
        {/* Header */}
        <div className="text-center mb-6">
          <p className="text-neutral-500 text-xs font-semibold tracking-wide uppercase">
            PSR Aeróbicos · Panel de Miembro
          </p>
          <p className="text-neutral-300 text-sm mt-1">Sesión Activa de Hoy</p>
        </div>

        {/* Profile Card */}
        <div className="bg-neutral-900/80 rounded-3xl p-6 border border-neutral-800 shadow-xl">
          <div className="flex flex-col items-center text-center">
            <div className="w-20 h-20 rounded-full bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mb-4 text-emerald-400 font-bold text-2xl">
              {initials}
            </div>
            <h1 className="text-2xl font-bold font-display text-white leading-tight">
              {name}
            </h1>
            <p className="text-xs text-neutral-400 mt-1 font-mono">
              Doc: {profile?.documentType || ""} {profile?.documentNumber || user?.documentNumber || "—"}
            </p>

            {isPaidToday ? (
              <div className="mt-4 inline-flex items-center gap-1.5 bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 px-3.5 py-1.5 rounded-full text-xs font-semibold">
                <CheckCircle2 size={16} />
                Pago al día para la sesión de hoy
              </div>
            ) : (
              <div className="mt-4 inline-flex items-center gap-1.5 bg-amber-500/15 text-amber-400 border border-amber-500/30 px-3.5 py-1.5 rounded-full text-xs font-semibold">
                Pago de tarifa ($4.000) por registrar en recepción
              </div>
            )}
          </div>

          {/* Week Strip */}
          <div className="mt-6">
            <p className="text-neutral-500 text-xs font-semibold uppercase tracking-wide mb-3 text-center">
              Tus asistencias esta semana
            </p>
            <div className="flex justify-between bg-neutral-950 border border-neutral-800 rounded-2xl p-3">
              {WEEK_DAYS.map((day) => {
                const isPaid = false;
                return (
                  <div key={day} className="flex flex-col items-center gap-1.5">
                    <span className="text-[10px] text-neutral-500 font-medium">{day}</span>
                    <div
                      className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all ${
                        isPaid
                          ? "bg-emerald-500 text-neutral-950 shadow-md shadow-emerald-500/20"
                          : "bg-neutral-900 border border-neutral-800 text-neutral-600"
                      }`}
                    >
                      {isPaid ? "✓" : ""}
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* Attendance Action */}
          <div className="mt-6 pt-4 border-t border-neutral-800">
            {isAttendedToday ? (
              <div className="bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 p-4 rounded-2xl text-center flex items-center justify-center gap-2 font-semibold text-sm">
                <UserCheck size={18} />
                ¡Tu asistencia de hoy está registrada!
              </div>
            ) : (
              <button
                onClick={handleMarkAttendance}
                disabled={checkingIn}
                className="w-full bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold text-base py-3.5 rounded-2xl transition-all shadow-lg shadow-emerald-500/20 flex items-center justify-center gap-2"
              >
                <UserCheck size={18} />
                {checkingIn ? "Registrando…" : "Marcar mi llegada hoy"}
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
