import { useState, useEffect } from "react";
import { ArrowLeft, Wifi, WifiOff } from "lucide-react";
import { formatHeaderDateLong as formatHeaderDate } from "../../utils/date";
import useClock from "../../hooks/useClock";
import { useNavigate } from "react-router-dom";
import { createQueueSocketClient } from "../../services/websocketService";
import { queueService } from "../../services/queueService";

export default function LobbyQueueDisplayPage({ peoplePerTurn = 2 }) {
  const now = useClock();
  const navigate = useNavigate();

  const [prepaidList, setPrepaidList] = useState([]);
  const [waitingList, setWaitingList] = useState([]);
  const [expiringIds, setExpiringIds] = useState([]);
  const [isConnected, setIsConnected] = useState(false);

  useEffect(() => {
    async function fetchWaiting() {
      try {
        const data = await queueService.getWaiting();
        setWaitingList(Array.isArray(data) ? data : data?.content || []);
      } catch (err) {
        console.error("Error al refrescar la fila de espera desde la API:", err);
      }
    }
    async function fetchPrepaid() {
      try {
        const data = await queueService.getPrepaid();
        setPrepaidList(Array.isArray(data) ? data : data?.content || []);
      } catch (err) {
        console.error("Error al refrescar la fila de prepagados desde la API:", err);
      }
    }

    // 1. Carga inicial vía REST de producción
    fetchWaiting();
    fetchPrepaid();

    // 2. Conexión WebSocket STOMP directa en producción — el mensaje que llega por estos topics
    // NO es la lista en sí, es solo la señal de que algo cambió (`QueueChangedEvent`:
    // `{sessionId, type}`), a propósito según el comentario del backend ("el cliente recibe la
    // señal y relee el endpoint REST"). Antes se trataba ese payload como si fuera la lista
    // (`Array.isArray(data) ? data : data?.content || []`, que siempre caía en `[]` porque nunca
    // es ninguna de las dos cosas), así que cada evento VACIABA las listas en vez de
    // actualizarlas — se corrige refetcheando por REST en cada señal, igual que
    // `DataContext.jsx` (errores-13sep-front.md #1).
    const client = createQueueSocketClient({
      onEsperaQueue: () => {
        setIsConnected(true);
        fetchWaiting();
      },
      onPrepaidQueue: () => {
        setIsConnected(true);
        fetchPrepaid();
      },
      onError: () => {
        setIsConnected(false);
      },
    });

    try {
      client.activate();
    } catch (e) {
      console.warn("WebSocket activate error:", e);
    }

    return () => {
      try {
        client.deactivate();
      } catch {
        // silent cleanup
      }
    };
  }, []);

  function handleHidePrepaid(checkinId) {
    setExpiringIds((ids) => [...ids, checkinId]);
    setTimeout(() => {
      setPrepaidList((prev) => prev.filter((p) => (p.checkinId || p.id) !== checkinId));
      setExpiringIds((ids) => ids.filter((id) => id !== checkinId));
    }, 400);
  }

  const hasPrepaid = prepaidList.length > 0;
  const sideSpan = hasPrepaid ? "lg:col-span-4" : "lg:col-span-6";

  const formattedTime = now.toLocaleTimeString("es-CO", {
    hour: "2-digit",
    minute: "2-digit",
    hour12: true,
  });

  const calledQueue = waitingList.slice(0, peoplePerTurn);
  const remainingQueue = waitingList.slice(peoplePerTurn);

  return (
    <div className="min-h-screen w-full bg-neutral-950 text-white flex flex-col p-4 md:p-6 font-body relative overflow-hidden">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-neutral-800 pb-3 mb-4">
        <div className="flex items-center gap-3 min-w-0">
          <button
            onClick={() => navigate("/lobby/panel")}
            className="shrink-0 flex items-center gap-1.5 text-neutral-400 hover:text-white text-xs bg-neutral-900 border border-neutral-800 px-3 py-1.5 rounded-xl transition-colors"
          >
            <ArrowLeft size={14} /> Salir a Panel
          </button>
          <div className="min-w-0">
            <h1 className="text-xl font-bold text-white tracking-tight">PSR • Aeróbicos — Fila Pública</h1>
            <p className="text-neutral-400 text-xs mt-0.5">
              Pioneros de la Salsa y Ritmos · Estado en tiempo real
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3 ml-auto">
          <span
            className={`flex items-center gap-1 text-[11px] font-bold px-3 py-1 rounded-full border ${isConnected
                ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-400"
                : "bg-amber-500/10 border-amber-500/30 text-amber-400"
              }`}
          >
            {isConnected ? <Wifi size={12} /> : <WifiOff size={12} />}
            {isConnected ? "WS En Vivo" : "Desconectado"}
          </span>
          <div className="text-right text-neutral-400 text-xs font-semibold">
            <div>{formatHeaderDate(now)}</div>
            <div className="text-emerald-400 font-bold font-mono text-sm">{formattedTime}</div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto w-full grid grid-cols-1 lg:grid-cols-12 gap-4 flex-1">
        {/* Sección 1: Pasando a Recepción (Llamados) */}
        <div className={`${sideSpan} bg-purple-950/20 border border-purple-500/40 rounded-3xl p-4 flex flex-col justify-between shadow-xl transition-all duration-300`}>
          <div>
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-lg font-bold text-purple-300">Pasar a Recepción</h2>
              <span className="text-xs font-bold bg-purple-500/20 text-purple-300 border border-purple-500/30 px-2 py-0.5 rounded-full">
                {calledQueue.length} llamado(s)
              </span>
            </div>
            <p className="text-purple-300/80 text-xs mb-3">
              Acércate al mostrador de recepción para ingresar.
            </p>

            {calledQueue.length === 0 ? (
              <div className="p-6 border border-dashed border-purple-500/20 rounded-2xl text-center text-purple-300/60 text-xs">
                Esperando llamado de turno...
              </div>
            ) : (
              <div className="space-y-2">
                {calledQueue.map((person, idx) => {
                  const fullName = `${person.firstName || ""} ${person.lastName || ""}`.trim() || person.name || "Alumno";
                  return (
                    <div
                      key={person.checkinId || person.id || idx}
                      className="bg-purple-600/30 border-2 border-purple-400 p-3.5 rounded-2xl flex items-center justify-between text-white shadow-lg shadow-purple-500/10"
                    >
                      <div className="flex items-center gap-3">
                        <span className="text-sm font-extrabold w-6 h-6 rounded-full bg-purple-400 text-neutral-950 flex items-center justify-center">
                          {idx + 1}
                        </span>
                        <span className="text-base font-bold">{fullName}</span>
                      </div>
                      <span className="text-[10px] font-bold uppercase tracking-wider bg-purple-500/30 px-2 py-0.5 rounded-full text-purple-200">
                        ¡Llamado!
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* Sección 2: Prepagados (Pasan Directo) */}
        {hasPrepaid && (
          <div className="lg:col-span-4 bg-emerald-950/20 border border-emerald-500/40 rounded-3xl p-4 flex flex-col justify-between shadow-xl transition-all duration-300">
            <div>
              <div className="flex items-center justify-between mb-2">
                <h2 className="text-lg font-bold text-emerald-300">Pasan Directo (Prepagados)</h2>
                <span className="text-xs font-bold bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 px-2 py-0.5 rounded-full">
                  {prepaidList.length} persona(s)
                </span>
              </div>
              <p className="text-emerald-300/80 text-xs mb-3">
                Sin pasar por recepción. Ingresan directo a la sala.
              </p>

              <div className="space-y-2">
                {prepaidList.map((person) => {
                  const checkinId = person.checkinId || person.id;
                  const fullName = `${person.firstName || ""} ${person.lastName || ""}`.trim() || person.name || "Alumno";
                  const isExpiring = expiringIds.includes(checkinId);
                  return (
                    <div
                      key={checkinId}
                      className={`bg-emerald-900/40 border border-emerald-500/50 p-3.5 rounded-2xl flex items-center justify-between text-emerald-200 transition-all duration-300 ${isExpiring ? "opacity-0 scale-95" : "opacity-100 scale-100"
                        }`}
                    >
                      <div className="flex items-center gap-2.5">
                        <div className="w-6 h-6 rounded-full bg-emerald-500 text-neutral-950 flex items-center justify-center font-bold text-xs shrink-0">
                          ✓
                        </div>
                        <span className="text-base font-bold text-white">{fullName}</span>
                      </div>
                      <button
                        onClick={() => handleHidePrepaid(checkinId)}
                        title="Ocultar de pantalla"
                        className="text-neutral-500 hover:text-emerald-300 text-xs px-2 py-0.5 rounded-md transition-colors"
                      >
                        Ocultar
                      </button>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {/* Sección 3: En Espera (Fila General) */}
        <div className={`${sideSpan} bg-neutral-900/60 border border-neutral-800 rounded-3xl p-4 flex flex-col justify-between shadow-xl transition-all duration-300`}>
          <div>
            <div className="flex items-center justify-between mb-2">
              <h2 className="text-lg font-bold text-white">En Espera (Fila General)</h2>
              <span className="text-xs font-semibold text-neutral-400">
                {remainingQueue.length} en espera
              </span>
            </div>
            <p className="text-neutral-400 text-xs mb-3">
              Permanece en sala de espera hasta escuchar tu llamado.
            </p>

            {remainingQueue.length === 0 ? (
              <div className="p-6 border border-dashed border-neutral-800 rounded-2xl text-center text-neutral-500 text-xs">
                No hay más personas en la fila de espera.
              </div>
            ) : (
              <div className="space-y-2">
                {remainingQueue.map((person, idx) => {
                  const fullName = `${person.firstName || ""} ${person.lastName || ""}`.trim() || person.name || "Alumno";
                  return (
                    <div
                      key={person.checkinId || person.id || idx}
                      className="bg-neutral-950/80 border border-neutral-800 p-3 rounded-2xl flex items-center justify-between text-neutral-300"
                    >
                      <div className="flex items-center gap-3">
                        <span className="text-xs font-bold text-neutral-500 w-5 text-center">
                          #{peoplePerTurn + idx + 1}
                        </span>
                        <span className="text-sm font-semibold text-white">{fullName}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
