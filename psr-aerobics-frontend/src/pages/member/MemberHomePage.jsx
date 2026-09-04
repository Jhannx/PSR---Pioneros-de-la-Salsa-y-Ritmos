import { useState } from "react";
import { CheckCircle2 } from "lucide-react";
import { getInitialsFromFullName } from "../../utils/text";

// Day-of-week labels for the "paid days" strip (Mon-Sun)
const WEEK_DAYS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];

export default function MemberHomePage({
  name = "María López",
  activity = "Aeróbicos",
  todayLabel = "Miércoles 5 de agosto",
  todayIndex = 2, // 0 = Mon ... 6 = Sun
  paidToday = true,
  tariff = 4000,
  paidDays = [0, 1, 2, 3], // week-day indexes already paid in advance
  queuePosition = 5,
}) {
  const [marked, setMarked] = useState(false);

  const initials = getInitialsFromFullName(name);

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-start justify-center px-4 py-10">
      <div className="w-full max-w-sm">
        {/* Header */}
        <div className="text-center mb-6">
          <p className="text-neutral-500 text-sm font-semibold tracking-wide uppercase">
            {activity}
          </p>
          <p className="text-neutral-300 text-sm mt-1">{todayLabel}</p>
        </div>

        {/* Profile card */}
        <div className="bg-neutral-900 rounded-3xl p-6 border border-neutral-800">
          <div className="flex flex-col items-center text-center">
            <div className="w-20 h-20 rounded-full bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mb-4">
              <span className="text-2xl font-bold text-emerald-400">
                {initials}
              </span>
            </div>
            <h1 className="text-2xl font-bold text-white leading-tight">
              {name}
            </h1>

            {paidToday ? (
              <div className="mt-3 inline-flex items-center gap-1.5 bg-emerald-500/15 text-emerald-400 px-3 py-1.5 rounded-full text-sm font-semibold">
                <CheckCircle2 size={16} />
                Ya pagaste hoy
              </div>
            ) : (
              <div className="mt-3 inline-flex items-center gap-1.5 bg-amber-500/15 text-amber-400 px-3 py-1.5 rounded-full text-sm font-semibold">
                Pago pendiente · ${tariff.toLocaleString("es-CO")}
              </div>
            )}
          </div>

          {/* Week strip showing which days are already paid */}
          <div className="mt-6">
            <p className="text-neutral-500 text-xs font-semibold uppercase tracking-wide mb-2 text-center">
              Tus días pagos esta semana
            </p>
            <div className="flex justify-between">
              {WEEK_DAYS.map((day, index) => {
                const isPaid = paidDays.includes(index);
                const isToday = index === todayIndex;
                return (
                  <div key={day} className="flex flex-col items-center gap-1">
                    <div
                      className={`w-9 h-9 rounded-full flex items-center justify-center text-xs font-bold ${
                        isPaid
                          ? "bg-emerald-500 text-neutral-950"
                          : "bg-neutral-800 text-neutral-500"
                      } ${
                        isToday
                          ? "ring-2 ring-offset-2 ring-offset-neutral-900 ring-white"
                          : ""
                      }`}
                    >
                      {isPaid ? <CheckCircle2 size={16} /> : day[0]}
                    </div>
                    <span className="text-[10px] text-neutral-500">{day}</span>
                  </div>
                );
              })}
            </div>
          </div>
        </div>

        {/* Main action: mark attendance (same button for everyone, paid or not) */}
        <div className="mt-6">
          {!marked ? (
            <button
              onClick={() => setMarked(true)}
              className="w-full bg-emerald-500 hover:bg-emerald-400 active:scale-[0.98] transition-all text-neutral-950 font-bold text-lg py-5 rounded-2xl shadow-lg shadow-emerald-500/20 flex items-center justify-center gap-2"
            >
              <CheckCircle2 size={22} />
              Marcar asistencia
            </button>
          ) : (
            <div className="bg-emerald-500/10 border-2 border-emerald-500 rounded-2xl p-6 text-center">
              <CheckCircle2 className="mx-auto text-emerald-400 mb-2" size={40} />
              <p className="text-emerald-400 font-bold text-lg">
                ¡Asistencia registrada!
              </p>
              <p className="text-neutral-300 text-sm mt-1">
                Eres el turno número{" "}
                <span className="font-bold text-white">{queuePosition}</span>
              </p>
              <p className="text-neutral-500 text-xs mt-3">
                Acércate con el encargado cuando te llame
              </p>
            </div>
          )}

          {marked && (
            <p className="text-center text-neutral-600 text-xs mt-3">
              Ya registraste tu asistencia para hoy
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
