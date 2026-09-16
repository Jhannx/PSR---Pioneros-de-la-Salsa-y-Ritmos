/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useCallback } from "react";
import { peopleService } from "../services/peopleService";
import { queueService } from "../services/queueService";
import { sessionsService } from "../services/sessionsService";
import { TOKEN_STORAGE_KEY } from "../services/apiClient";
import { createQueueSocketClient } from "../services/websocketService";
import { useAuth } from "./AuthContext";
import { toLocalDateString } from "../utils/date";

const DataContext = createContext(null);

export function DataProvider({ children }) {
  const { isAuthenticated } = useAuth();
  const [people, setPeople] = useState([]);
  const [queue, setQueue] = useState([]);
  const [sessions, setSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  // Carga inicial de datos solo si existe un token de autenticación
  const refreshData = useCallback(async () => {
    const token = localStorage.getItem(TOKEN_STORAGE_KEY);
    if (!token) {
      setLoading(false);
      setPeople([]);
      setQueue([]);
      setSessions([]);
      return;
    }

    setLoading(true);
    setError(null);
    try {
      // Rango real (7 días atrás, 7 adelante) en vez de pedir sin filtro — sin esto, la sesión de
      // HOY podía caer en una página que nunca se pedía (RESPUESTA-NOTAS-FINAL.md #2: sin
      // parámetros es un histórico completo ordenado por fecha descendente, paginado de 20 en 20).
      const today = new Date();
      const desde = new Date(today);
      desde.setDate(desde.getDate() - 7);
      const hasta = new Date(today);
      hasta.setDate(hasta.getDate() + 7);

      const [peopleRes, queueRes, sessionsRes] = await Promise.allSettled([
        peopleService.getAll(),
        queueService.getAdminQueue(),
        sessionsService.getSessions(toLocalDateString(desde), toLocalDateString(hasta)),
      ]);

      if (peopleRes.status === "fulfilled") {
        const pData = peopleRes.value;
        setPeople(Array.isArray(pData) ? pData : pData?.content || []);
      }

      if (queueRes.status === "fulfilled") {
        const qData = queueRes.value;
        setQueue(Array.isArray(qData) ? qData : qData?.content || []);
      }

      if (sessionsRes.status === "fulfilled") {
        const sData = sessionsRes.value;
        setSessions(Array.isArray(sData) ? sData : sData?.content || []);
      }
    } catch (err) {
      console.error("Error al consultar datos del backend:", err);
      setError(err.message || "Error al conectar con la API de producción");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    Promise.resolve().then(refreshData);
  }, [refreshData]);

  const refreshQueue = useCallback(async () => {
    try {
      const qData = await queueService.getAdminQueue();
      setQueue(Array.isArray(qData) ? qData : qData?.content || []);
    } catch (err) {
      console.warn("No se pudo refrescar la cola de admin:", err);
    }
  }, []);

  // Un check-in propio (QR/self-checkin desde MemberHomePage) o de terminal quedaba invisible en
  // `reception/checkin`: `queue` solo se pedía una vez al montar y después de la propia acción
  // `addToQueue` de esta pantalla — nunca se enteraba de check-ins hechos desde otro lado
  // (errores-13sep-front.md #1). El backend ya difunde exactamente esto por WebSocket en
  // `/topic/cola/admin` (confirmado en `QueueEventBroadcaster.java` del backend); el mismo canal
  // que ya usa `LobbyQueueDisplayPage.jsx` para `onEsperaQueue`/`onPrepaidQueue`, solo faltaba
  // suscribirse aquí también. Depende de `isAuthenticated` (no de `[]`) porque `DataProvider`
  // envuelve toda la app y sigue montado entre login/logout — sin esto, alguien que entra después
  // de que la app cargó se quedaría sin conexión en vivo hasta recargar la página.
  //
  // OJO: el mensaje que llega por este topic NO es la cola — es solo la señal de que algo cambió
  // (`QueueChangedEvent`: `{sessionId, type}`), a propósito, según el propio comentario del backend
  // ("el cliente recibe la señal y relee el endpoint REST que ya tiene permitido"). La primera
  // versión de este arreglo trataba ese `{sessionId, type}` como si fuera la lista completa
  // (`Array.isArray(data) ? data : data?.content || []` → siempre caía en `[]` porque nunca es un
  // array ni trae `.content`), lo que en la práctica VACIABA la cola en cada evento en vez de
  // actualizarla. Correcto es: la señal solo dispara un refetch real por REST.
  useEffect(() => {
    if (!isAuthenticated) return;

    const client = createQueueSocketClient({
      onAdminQueue: () => {
        refreshQueue();
      },
      onError: (err) => {
        console.warn("WebSocket de cola (admin) con error:", err);
      },
    });

    try {
      client.activate();
    } catch (err) {
      console.warn("No se pudo activar el WebSocket de cola:", err);
    }

    return () => {
      try {
        client.deactivate();
      } catch {
        // Ignorado: si ya estaba desconectado no hay nada que limpiar.
      }
    };
  }, [isAuthenticated, refreshQueue]);

  // Operaciones de personas
  async function addPerson(personData) {
    try {
      const created = await peopleService.create({
        documentType: personData.documentType || "CC",
        documentNumber: personData.documentNumber,
        firstName: personData.firstName,
        lastName: personData.lastName,
        phone: personData.phone,
        email: personData.email || undefined,
      });
      setPeople((prev) => [created, ...prev]);
      return created;
    } catch (err) {
      console.error("Error al crear persona en backend:", err);
      throw err;
    }
  }

  async function updatePerson(id, personData) {
    try {
      const updated = await peopleService.update(id, personData);
      setPeople((prev) => prev.map((p) => (p.id === id ? { ...p, ...updated } : p)));
      return updated;
    } catch (err) {
      console.error("Error al actualizar persona en backend:", err);
      throw err;
    }
  }

  async function deactivatePerson(id) {
    try {
      await peopleService.deactivate(id);
      setPeople((prev) => prev.map((p) => (p.id === id ? { ...p, active: false } : p)));
    } catch (err) {
      console.error("Error al desactivar persona en backend:", err);
      throw err;
    }
  }

  async function reactivatePerson(id) {
    try {
      await peopleService.reactivate(id);
      setPeople((prev) => prev.map((p) => (p.id === id ? { ...p, active: true } : p)));
    } catch (err) {
      console.error("Error al reactivar persona en backend:", err);
      throw err;
    }
  }

  async function deletePerson(id) {
    try {
      await peopleService.remove(id);
      setPeople((prev) => prev.filter((p) => p.id !== id));
    } catch (err) {
      console.error("Error al eliminar persona en backend:", err);
      throw err;
    }
  }

  // Operaciones de cola de recepción
  async function addToQueue({ personId }) {
    try {
      const response = await queueService.checkin(personId);
      // El propio WebSocket (arriba) ya dispara `refreshQueue()` cuando el backend confirma el
      // check-in — este refetch queda igual por si la conexión WS no llegó a tiempo o falló.
      await refreshQueue();
      return response;
    } catch (err) {
      console.error("Error al ingresar a la cola:", err);
      throw err;
    }
  }

  function updateQueueItem(checkinId, updates) {
    setQueue((prev) => prev.map((item) => (item.checkinId === checkinId ? { ...item, ...updates } : item)));
  }

  function removeFromQueue(checkinId) {
    setQueue((prev) => prev.filter((item) => item.checkinId !== checkinId));
  }

  // Operaciones de sesiones
  async function createSession(sessionPayload) {
    try {
      const created = await sessionsService.createSession(sessionPayload);
      setSessions((prev) => [created, ...prev]);
      return created;
    } catch (err) {
      console.error("Error al crear sesión de clase:", err);
      throw err;
    }
  }

  async function cancelSession(id) {
    try {
      const updated = await sessionsService.cancelSession(id);
      setSessions((prev) => prev.map((s) => (s.id === id ? { ...s, ...(updated || { cancelled: true }) } : s)));
      return updated;
    } catch (err) {
      console.error("Error al cancelar sesión:", err);
      throw err;
    }
  }

  // Ahora relevante de verdad: las sesiones nacen sin profesor por defecto — un proceso las genera
  // por adelantado desde los horarios activos, y el horario no guarda quién dicta
  // (NOTAS-PARA-FRONTEND.md ronda 5, §0.6). `reassignTeacher` ya existía en el service pero nadie
  // la llamaba desde ninguna pantalla.
  async function reassignSessionTeacher(id, teacherUserId) {
    try {
      const updated = await sessionsService.reassignTeacher(id, teacherUserId);
      setSessions((prev) => prev.map((s) => (s.id === id ? { ...s, ...updated } : s)));
      return updated;
    } catch (err) {
      console.error("Error al asignar profesor a la sesión:", err);
      throw err;
    }
  }

  return (
    <DataContext.Provider
      value={{
        people,
        queue,
        sessions,
        loading,
        error,
        refreshData,
        refreshQueue,
        addPerson,
        updatePerson,
        deactivatePerson,
        reactivatePerson,
        deletePerson,
        addToQueue,
        updateQueueItem,
        removeFromQueue,
        createSession,
        cancelSession,
        reassignSessionTeacher,
      }}
    >
      {children}
    </DataContext.Provider>
  );
}

export function useMockData() {
  const context = useContext(DataContext);
  if (!context) {
    throw new Error("useMockData debe ser usado dentro de un DataProvider");
  }
  return context;
}
