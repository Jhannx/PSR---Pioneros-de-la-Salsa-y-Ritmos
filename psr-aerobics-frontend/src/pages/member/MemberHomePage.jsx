import { useEffect, useState } from "react";
import { CheckCircle2, UserCheck, Cake } from "lucide-react";
import { getInitialsFromFullName } from "../../utils/text";
import { formatCOP } from "../../utils/currency";
import { formatBirthDate } from "../../utils/date";
import { useAuth } from "../../context/AuthContext";
import { useMockData } from "../../context/DataContext";
import { peopleService } from "../../services/peopleService";
import { paymentsService } from "../../services/paymentsService";
import { queueService } from "../../services/queueService";

const WEEK_DAYS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];

export default function MemberHomePage() {
  const { user } = useAuth();
  const { queue } = useMockData();
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const [checkingIn, setCheckingIn] = useState(false);
  // Resultado de POST /api/checkin/propio — turno real que devuelve el backend. Antes el botón
  // llamaba a POST /api/cola con { personId }, que para un MIEMBRO da 403 a propósito: ese
  // endpoint recibe el personId en el cuerpo, así que abrirlo dejaría que cualquier miembro
  // marcara la llegada de otra persona (NOTAS-PARA-FRONTEND.md §1). El endpoint correcto no
  // recibe cuerpo — la persona sale del token.
  const [myCheckin, setMyCheckin] = useState(null);
  // Antes esta pantalla mostraba "$4.000" escrito a mano en el JSX — el valor por defecto del
  // mock original, nunca conectado a la tarifa real (errores-frontend.md #5). `null` mientras
  // carga: mostrar una cifra vieja un instante es peor que no mostrar ninguna.
  const [pricing, setPricing] = useState(null);

  useEffect(() => {
    if (!user?.personId) return;
    peopleService.getProfile(user.personId)
      .then(setProfile)
      .catch((err) => setError(err.data?.message || err.message || "No fue posible cargar tu perfil."));
  }, [user?.personId]);

  useEffect(() => {
    paymentsService.getPricing()
      .then((data) => setPricing(data?.amount ?? null))
      .catch((err) => console.warn("No se pudo cargar la tarifa vigente:", err));
  }, []);

  const name = profile?.fullName || user?.fullName || "Usuario";
  const initials = getInitialsFromFullName(name);

  // `queue` (GET /api/cola/admin) solo responde para roles de personal — para un MIEMBRO puro
  // llega vacía en silencio (DataContext.refreshData descarta el 403 con Promise.allSettled).
  // Sigue siendo útil para una cuenta que además tiene rol de personal (ej. ADMIN viendo su
  // propio panel de miembro), así que se conserva como fuente de verdad cuando está disponible.
  const memberQueueItem = queue.find((p) => p.personId === user?.personId);
  const isPaidToday = memberQueueItem ? Number(memberQueueItem.amountApplied || memberQueueItem.amountCharged || 0) > 0 : false;
  const isAttendedToday = memberQueueItem ? memberQueueItem.attended : !!myCheckin;

  async function handleMarkAttendance() {
    if (!user?.personId || checkingIn || memberQueueItem || myCheckin) return;
    setCheckingIn(true);
    setError("");
    try {
      const result = await queueService.checkinPropio();
      setMyCheckin(result);
    } catch (err) {
      setError(err.data?.message || err.message || "No fue posible registrar la llegada.");
    } finally {
      setCheckingIn(false);
    }
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
            {/* `birthDate` puede llegar null — personas registradas antes de que el campo
                existiera no lo tienen (NOTAS-PARA-FRONTEND.md §5). Solo se muestra si hay dato. */}
            {formatBirthDate(profile?.birthDate) && (
              <p className="text-xs text-neutral-500 mt-1 flex items-center gap-1">
                <Cake size={12} />
                {formatBirthDate(profile.birthDate)}
              </p>
            )}

            {isPaidToday ? (
              <div className="mt-4 inline-flex items-center gap-1.5 bg-emerald-500/15 text-emerald-400 border border-emerald-500/30 px-3.5 py-1.5 rounded-full text-xs font-semibold">
                <CheckCircle2 size={16} />
                Pago al día para la sesión de hoy
              </div>
            ) : (
              <div className="mt-4 inline-flex items-center gap-1.5 bg-amber-500/15 text-amber-400 border border-amber-500/30 px-3.5 py-1.5 rounded-full text-xs font-semibold">
                {pricing != null ? `Pago de tarifa (${formatCOP(pricing)}) por registrar en recepción` : "Pago de tarifa por registrar en recepción"}
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
              <div className="bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 p-4 rounded-2xl text-center flex flex-col items-center justify-center gap-1.5 font-semibold text-sm">
                <span className="flex items-center gap-2">
                  <UserCheck size={18} />
                  ¡Tu asistencia de hoy está registrada!
                </span>
                {myCheckin?.queuePosition != null && (
                  <span className="text-emerald-300/80 text-xs font-normal">
                    Tu turno en la fila de hoy: #{myCheckin.queuePosition}
                  </span>
                )}
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
