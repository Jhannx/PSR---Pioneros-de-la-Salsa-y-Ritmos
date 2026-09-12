/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useCallback } from "react";
import { peopleService } from "../services/peopleService";
import { queueService } from "../services/queueService";
import { sessionsService } from "../services/sessionsService";
import { TOKEN_STORAGE_KEY } from "../services/apiClient";

const DataContext = createContext(null);

export function DataProvider({ children }) {
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
      const [peopleRes, queueRes, sessionsRes] = await Promise.allSettled([
        peopleService.getAll(),
        queueService.getAdminQueue(),
        sessionsService.getSessions(),
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
      const queueList = await queueService.getAdminQueue();
      if (Array.isArray(queueList)) {
        setQueue(queueList);
      }
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

  return (
    <DataContext.Provider
      value={{
        people,
        queue,
        sessions,
        loading,
        error,
        refreshData,
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
