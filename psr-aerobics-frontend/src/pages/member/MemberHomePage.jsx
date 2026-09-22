import { useCallback, useEffect, useState } from "react";
import { CheckCircle2, UserCheck, Cake } from "lucide-react";
import { getInitialsFromFullName } from "../../utils/text";
import { formatCOP } from "../../utils/currency";
import { formatBirthDate } from "../../utils/date";
import { useAuth } from "../../context/AuthContext";
import { peopleService } from "../../services/peopleService";
import { paymentsService } from "../../services/paymentsService";
import { queueService } from "../../services/queueService";

export default function MemberHomePage() {
  const { user } = useAuth();
  const [profile, setProfile] = useState(null);
  const [error, setError] = useState("");
  const [checkingIn, setCheckingIn] = useState(false);
  // Mientras se consulta la llegada de hoy no se muestra el botón: verlo un instante y que
  // desaparezca (o peor, pulsarlo) sería el mismo defecto que se está corrigiendo.
  const [checkinLoading, setCheckinLoading] = useState(true);
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

  // El perfil trae las sesiones pagadas por adelantado (`availableCreditDays`). Se relee también al
  // marcar la llegada: si la sesión estaba cubierta por crédito el servidor ya confirmó la asistencia
  // y descontó una, y el contador tiene que bajar sin recargar la página.
  const personId = user?.personId;
  const loadProfile = useCallback(() => {
    if (!personId) return Promise.resolve();
    return peopleService.getProfile(personId)
      .then(setProfile)
      .catch((err) => setError(err.data?.message || err.message || "No fue posible cargar tu perfil."));
  }, [personId]);

  useEffect(() => {
    Promise.resolve().then(loadProfile);
  }, [loadProfile]);

  useEffect(() => {
    paymentsService.getPricing()
      .then((data) => setPricing(data?.amount ?? null))
      .catch((err) => console.warn("No se pudo cargar la tarifa vigente:", err));
  }, []);

  const name = profile?.fullName || user?.fullName || "Usuario";
  const initials = getInitialsFromFullName(name);

  // Antes esta franja mostraba "asistencias de la semana" con un check falso cada día
  // (`isPaid` fijo en `false`) — no eran datos reales de asistencia, contradecía lo que la
  // persona sabía de sí misma (errores-12sep-front.md #5). `vista_perfil_persona` ya calcula
  // `dias_credito_disponible` (columna) → `PersonProfileViewResponse.availableCreditDays` (record,
  // psr-aerobics-backend) — confirmado leyendo el backend real, GET /api/personas/{id}/perfil ya
  // lo expone con ese nombre, no hace falta pedir nada nuevo.
  const creditDays = profile?.availableCreditDays ?? null;

  // La llegada de hoy sale del servidor (`GET /api/checkin/propio`), no de la cola de personal ni
  // de memoria: antes solo se recordaba en el estado de la pantalla, así que un F5 la borraba y
  // volvía el botón "Marcar mi llegada" (docs/correciones 20-09.md). `myCheckin` es null si hoy no
  // marcó llegada (204).
  //
  // Una llegada de hoy cuya sesión ya TERMINÓ o se CANCELÓ (`sessionStatus` del servidor, sin
  // comparar horas acá) sigue siendo "la llegada de hoy": antes se ignoraba y volvía el botón
  // "Marcar mi llegada" como si no hubiera marcado nada — justo el síntoma reportado (docs/correciones
  // 20-09.md, 2ª ronda: "vuelve el marcar mi asistencia hoy"). Ahora se muestra que ya marcó, y solo
  // se ofrece marcar OTRA aparte, por si hay una clase más tarde.
  const pastCheckin = myCheckin && ["ENDED", "CANCELLED"].includes(myCheckin.sessionStatus) ? myCheckin : null;
  const checkin = myCheckin && !pastCheckin ? myCheckin : null;
  // Dos cosas distintas, que antes se mezclaban en una sola: tener la LLEGADA registrada
  // (`isCheckedInToday`) y que recepción ya haya CONFIRMADO la asistencia (`isAttendedToday`).
  const isCheckedInToday = Boolean(checkin);
  const isAttendedToday = Boolean(checkin?.attended);
  // El estado de pago de hoy lo dice el servidor (`paymentStatus` en la respuesta del check-in propio,
  // NOTAS-PARA-FRONTEND.md Bloque 5, punto 6), sin deducirlo de los días de crédito:
  //   PAGADA_POR_ADELANTADO → "tu sesión ya está paga, puedes pasar directo"
  //   PAGADA                → asistencia confirmada y sin deuda
  //   PENDIENTE             → tiene que pagar `amountDue` en recepción
  const paymentStatus = isCheckedInToday ? (checkin.paymentStatus ?? null) : null;
  const canPassThrough = paymentStatus === "PAGADA_POR_ADELANTADO" || paymentStatus === "PAGADA";
  const amountDueToday = Number(checkin?.amountDue) > 0 ? Number(checkin.amountDue) : 0;
  const isPaidToday = paymentStatus ? paymentStatus !== "PENDIENTE" : Number(checkin?.amountCharged || 0) > 0;

  const [checkinError, setCheckinError] = useState("");

  const loadOwnCheckin = useCallback(async () => {
    setCheckinError("");
    try {
      const result = await queueService.getOwnCheckin();
      setMyCheckin(result);
    } catch (err) {
      // 403 = una cuenta sin rol de miembro que igual abrió esta pantalla: no hay nada que mostrar.
      // Cualquier otro fallo SE MUESTRA: antes se tragaba en silencio y, al fallar la consulta, la
      // pantalla se veía como "no has marcado" aunque sí lo hubiera hecho — indistinguible del defecto
      // que se intentaba arreglar.
      if (err.status !== 403) {
        console.warn("No se pudo consultar la llegada de hoy:", err);
        setCheckinError(err.data?.message || err.message || "No se pudo consultar tu llegada de hoy.");
      }
    } finally {
      setCheckinLoading(false);
    }
  }, []);

  useEffect(() => {
    Promise.resolve().then(loadOwnCheckin);
  }, [loadOwnCheckin]);

  async function handleMarkAttendance() {
    if (!user?.personId || checkingIn || checkin) return;
    setCheckingIn(true);
    setError("");
    try {
      // Repetible: 201 si se registró ahora, 200 si ya la tenía (misma respuesta, no una segunda).
      const result = await queueService.checkinPropio();
      setMyCheckin(result);
      await loadProfile();
    } catch (err) {
      // Fuera de horario el servidor responde con un mensaje listo para mostrar tal cual
      // ("No hay ninguna clase programada en este momento").
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

          {/* Sesiones pagadas por adelantado — reemplaza la franja semanal de asistencias falsas. Son
              SESIONES, no días: si un día hay dos clases, cada una descuenta una. */}
          {creditDays !== null && (
            <div className="mt-6">
              <p className="text-neutral-500 text-xs font-semibold uppercase tracking-wide mb-3 text-center">
                Sesiones pagadas por adelantado
              </p>
              <div className="flex items-center justify-center gap-2 bg-neutral-950 border border-neutral-800 rounded-2xl p-4">
                <span className="text-3xl font-bold text-emerald-400">{creditDays}</span>
                <span className="text-neutral-500 text-sm">
                  {creditDays === 1 ? "sesión disponible" : "sesiones disponibles"}
                </span>
              </div>
            </div>
          )}

          {/* Attendance Action */}
          <div className="mt-6 pt-4 border-t border-neutral-800">
            {checkinLoading ? (
              <div className="h-[52px] rounded-2xl bg-neutral-800/50 animate-pulse" aria-label="Consultando tu llegada de hoy" />
            ) : isCheckedInToday ? (
              <div className="bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 p-4 rounded-2xl text-center flex flex-col items-center justify-center gap-1.5 font-semibold text-sm">
                <span className="flex items-center gap-2">
                  <UserCheck size={18} />
                  {isAttendedToday ? "¡Tu asistencia de hoy está confirmada!" : "¡Tu llegada de hoy está registrada!"}
                </span>
                {/* Con la sesión ya paga (por adelantado o cobrada) solo se dice que puede pasar directo:
                    el turno de la fila no le sirve y confunde. */}
                {canPassThrough ? (
                  <span className="text-emerald-200 text-xs font-semibold mt-1">
                    Tu sesión de hoy ya está paga — puedes pasar directo a la clase.
                  </span>
                ) : (
                  <>
                    {!isAttendedToday && checkin?.queuePosition != null && (
                      <span className="text-emerald-300/80 text-xs font-normal">
                        Tu turno en la fila de hoy: #{checkin.queuePosition}
                      </span>
                    )}
                    {amountDueToday > 0 && (
                      <span className="text-amber-300 text-xs font-semibold mt-1">
                        Te falta pagar {formatCOP(amountDueToday)} en recepción.
                      </span>
                    )}
                  </>
                )}
              </div>
            ) : pastCheckin ? (
              // Ya marcó hoy, pero esa sesión ya no cuenta (terminó o se canceló): se le dice que ya
              // marcó y solo se ofrece marcar OTRA, aparte — no vuelve el botón como si nada.
              <div className="bg-neutral-800/60 border border-neutral-700 p-4 rounded-2xl text-center text-sm">
                <p className="text-neutral-200 font-semibold flex items-center justify-center gap-2">
                  <UserCheck size={18} className="text-emerald-400" />
                  Ya marcaste tu llegada hoy
                </p>
                <p className="text-neutral-500 text-xs mt-1">
                  {pastCheckin.sessionStatus === "CANCELLED"
                    ? "Esa sesión se canceló."
                    : "Esa sesión ya terminó."}
                </p>
                <button
                  onClick={handleMarkAttendance}
                  disabled={checkingIn}
                  className="mt-3 text-xs font-semibold text-emerald-400 hover:text-emerald-300 underline underline-offset-2 disabled:opacity-50"
                >
                  {checkingIn ? "Registrando…" : "Marcar llegada a otra sesión de hoy"}
                </button>
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

            {/* La consulta de la llegada de hoy falló: se dice, en vez de dejar el botón como si no
                hubiera marcado nada. */}
            {checkinError && !checkinLoading && (
              <div className="mt-3 p-3 rounded-xl bg-amber-500/10 border border-amber-500/30 text-amber-300 text-xs">
                <p>
                  No pudimos consultar si ya marcaste tu llegada hoy ({checkinError}). Si ya la marcaste, es posible
                  que no se vea aquí.
                </p>
                <button
                  onClick={loadOwnCheckin}
                  className="mt-1 font-semibold text-amber-200 hover:text-white underline underline-offset-2"
                >
                  Reintentar
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
