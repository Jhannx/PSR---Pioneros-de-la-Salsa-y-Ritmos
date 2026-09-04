import { useState, useEffect } from "react";
import { Users, Sparkles, CheckCircle2 } from "lucide-react";
import { formatTimeShort as formatTime } from "../../utils/date";
import { formatHeaderDateLong as formatHeaderDate } from "../../utils/date";
import { formatHeaderTime24h as formatHeaderTime } from "../../utils/date";
import useClock from "../../hooks/useClock";
import { minutesAgo, turnMessage } from "../../features/lobby/utils";

// Nombre fijo — constante de marca, no un dato de tabla (ver nota en el
// chat: mientras el sistema sea de un solo negocio, esto no necesita
// backend). Si algún día hay multi-negocio, esto sí pasaría a venir de
// una configuración por negocio.
const ACADEMY_NAME = "PSR Aeróbicos";

const initialQueue = [
  { id: 1, firstName: "Juan", lastName: "Martínez", acceptedAt: minutesAgo(9) },
  { id: 2, firstName: "María", lastName: "López", acceptedAt: minutesAgo(7) },
  { id: 3, firstName: "Carlos", lastName: "Ruiz", acceptedAt: minutesAgo(5) },
  { id: 4, firstName: "Ana", lastName: "Torres", acceptedAt: minutesAgo(3) },
  { id: 5, firstName: "Sofía", lastName: "Ramírez", acceptedAt: minutesAgo(1) },
];

// Ya tenían saldo cubierto ANTES de llegar (vista_fila_prepagada) — ver
// Notas_Frontend_Pendientes.md punto 5 para el dinamismo real.
const initialPrepaid = [{ id: 201, firstName: "Diana", lastName: "Ortiz" }];

const DEMO_NAME_POOL = [
  { firstName: "Pedro", lastName: "Gómez" },
  { firstName: "Luisa", lastName: "Rojas" },
  { firstName: "Andrés", lastName: "Cano" },
];
const DEMO_PREPAID_POOL = [
  { firstName: "Camilo", lastName: "Restrepo" },
  { firstName: "Paola", lastName: "Jiménez" },
];

export default function LobbyQueueDisplayPage({ turnRule = { count: 2 } }) {
  const now = useClock();
  const [queue, setQueue] = useState(initialQueue);
  const [nextId, setNextId] = useState(initialQueue.length + 1);
  const [poolIndex, setPoolIndex] = useState(0);

  const [prepaid, setPrepaid] = useState(initialPrepaid);
  const [nextPrepaidId, setNextPrepaidId] = useState(initialPrepaid.length + 201);
  const [prepaidPoolIndex, setPrepaidPoolIndex] = useState(0);

  const [calledGroupIds, setCalledGroupIds] = useState([]);

  useEffect(() => {
    const stillWaiting = calledGroupIds.filter((id) => queue.some((p) => p.id === id));
    if (stillWaiting.length === 0) {
      const ordered = [...queue].sort((a, b) => a.acceptedAt - b.acceptedAt);
      setCalledGroupIds(ordered.slice(0, turnRule.count).map((p) => p.id));
    } else if (stillWaiting.length !== calledGroupIds.length) {
      setCalledGroupIds(stillWaiting);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [queue, turnRule.count]);

  const ordered = [...queue].sort((a, b) => a.acceptedAt - b.acceptedAt);
  const hasPrepaid = prepaid.length > 0;

  function demoAddPerson() {
    const template = DEMO_NAME_POOL[poolIndex % DEMO_NAME_POOL.length];
    setPoolIndex((i) => i + 1);
    setQueue((prev) => [...prev, { id: nextId, ...template, acceptedAt: new Date() }]);
    setNextId((n) => n + 1);
  }

  function demoMarkPaid(id) {
    setQueue((prev) => prev.filter((p) => p.id !== id));
  }

  function demoAddPrepaid() {
    const template = DEMO_PREPAID_POOL[prepaidPoolIndex % DEMO_PREPAID_POOL.length];
    setPrepaidPoolIndex((i) => i + 1);
    setPrepaid((prev) => [...prev, { id: nextPrepaidId, ...template }]);
    setNextPrepaidId((n) => n + 1);
  }

  function demoRemovePrepaid(id) {
    setPrepaid((prev) => prev.filter((p) => p.id !== id));
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex flex-col">
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,500;9..144,600;9..144,700&family=Manrope:wght@500;600;700;800&display=swap');
        .font-display { font-family: 'Fraunces', serif; font-optical-sizing: auto; }
        .font-body { font-family: 'Manrope', sans-serif; }
        .tabular { font-variant-numeric: tabular-nums; }
      `}</style>

      {/* Cabecera — identidad de la academia + reloj en vivo */}
      <header className="font-body flex items-center justify-between px-10 py-6 border-b border-neutral-800/80">
        <h1 className="font-display text-3xl text-white tracking-tight">{ACADEMY_NAME}</h1>
        <div className="text-right">
          <p className="text-neutral-500 text-sm capitalize">{formatHeaderDate(now)}</p>
          <p className="tabular text-white text-2xl font-bold tracking-wide">{formatHeaderTime(now)}</p>
        </div>
      </header>

      {/* Cuerpo — A|B lado a lado. B solo ocupa espacio si tiene gente;
          si no, A se expande solo (sin dejar un hueco vacío al lado). */}
      <div className="font-body flex-1 flex overflow-hidden">
        {/* A — fila de ingreso, pendientes de confirmación */}
        <main className={`flex flex-col px-10 py-8 overflow-y-auto ${hasPrepaid ? "flex-[0.68]" : "flex-1"}`}>
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-3">
              <Users className="text-neutral-500" size={30} />
              <h2 className="text-2xl font-bold text-white">Fila de ingreso</h2>
            </div>
            <span className="text-neutral-500 text-lg">{ordered.length} en espera</span>
          </div>

          <div className="bg-violet-500/10 border-2 border-violet-500/40 rounded-3xl px-7 py-4 mb-6 flex items-center gap-3">
            <Sparkles className="text-violet-400 shrink-0" size={22} />
            <p className="text-violet-300 font-bold text-lg">{turnMessage(turnRule.count)}</p>
          </div>

          <div className="flex flex-col gap-3">
            {ordered.map((person, index) => {
              const isYourTurn = calledGroupIds.includes(person.id);
              return (
                <div
                  key={person.id}
                  className={`flex items-center gap-5 px-6 py-4 rounded-2xl border-2 transition-colors ${
                    isYourTurn ? "bg-violet-500/15 border-violet-500" : "bg-neutral-900 border-neutral-800"
                  }`}
                >
                  <span
                    className={`w-9 h-9 rounded-full flex items-center justify-center text-base font-bold shrink-0 ${
                      isYourTurn ? "bg-violet-500 text-neutral-950" : "bg-neutral-800 text-neutral-400"
                    }`}
                  >
                    {index + 1}
                  </span>
                  <p className={`flex-1 text-xl font-bold ${isYourTurn ? "text-white" : "text-neutral-200"}`}>
                    {person.firstName} {person.lastName}
                  </p>
                  <span className="tabular text-neutral-600 text-sm shrink-0">{formatTime(person.acceptedAt)}</span>
                </div>
              );
            })}
            {ordered.length === 0 && (
              <div className="text-center py-16 text-neutral-600 text-lg">No hay nadie en fila en este momento.</div>
            )}
          </div>
        </main>

        {/* B — ya pagaron con anticipación. Solo existe en el DOM cuando
            hay al menos una persona; su desaparición es lo que hace que
            A vuelva a ocupar el 100% del ancho. */}
        {hasPrepaid && (
          <aside className="flex-[0.32] border-l border-neutral-800/80 bg-neutral-900/40 px-8 py-8 overflow-y-auto">
            <div className="flex items-center gap-3 mb-6">
              <CheckCircle2 className="text-emerald-400" size={24} />
              <h2 className="text-xl font-bold text-emerald-400">Ya pagaron</h2>
            </div>
            <p className="text-emerald-300/70 text-sm mb-6 -mt-3">Pasan directo, sin esperar turno.</p>
            <div className="flex flex-col gap-3">
              {prepaid.map((person) => (
                <div
                  key={person.id}
                  className="flex items-center gap-4 px-5 py-4 rounded-2xl border-2 bg-emerald-500/10 border-emerald-500/40"
                >
                  <CheckCircle2 className="text-emerald-400 shrink-0" size={20} />
                  <p className="text-lg font-bold text-white leading-tight">
                    {person.firstName}
                    <br />
                    {person.lastName}
                  </p>
                </div>
              ))}
            </div>
          </aside>
        )}
      </div>

      {/* Controles de demo — no forman parte de la pantalla real de TV. */}
      <div className="fixed bottom-4 right-4 flex flex-col items-end gap-2 opacity-40 hover:opacity-100 transition-opacity">
        {calledGroupIds.length > 0 && (
          <div className="flex gap-2">
            {calledGroupIds.map((id) => {
              const person = queue.find((p) => p.id === id);
              if (!person) return null;
              return (
                <button
                  key={id}
                  onClick={() => demoMarkPaid(id)}
                  className="text-xs bg-violet-500/20 text-violet-300 px-3 py-2 rounded-lg border border-violet-500/40 font-body"
                >
                  Demo: confirmar a {person.firstName}
                </button>
              );
            })}
          </div>
        )}
        {prepaid.length > 0 && (
          <div className="flex gap-2">
            {prepaid.map((person) => (
              <button
                key={person.id}
                onClick={() => demoRemovePrepaid(person.id)}
                className="text-xs bg-emerald-500/20 text-emerald-300 px-3 py-2 rounded-lg border border-emerald-500/40 font-body"
              >
                Demo: entrar {person.firstName}
              </button>
            ))}
          </div>
        )}
        <div className="flex gap-2">
          <button
            onClick={demoAddPerson}
            className="text-xs bg-neutral-800 text-neutral-400 px-3 py-2 rounded-lg border border-neutral-700 font-body"
          >
            + Demo: nueva persona
          </button>
          <button
            onClick={demoAddPrepaid}
            className="text-xs bg-neutral-800 text-neutral-400 px-3 py-2 rounded-lg border border-neutral-700 font-body"
          >
            + Demo: prepagado
          </button>
        </div>
      </div>
    </div>
  );
}
