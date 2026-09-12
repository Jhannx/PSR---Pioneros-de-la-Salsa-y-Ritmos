/**
 * apiClient.js - Cliente HTTP base para la API REST de PSR Aeróbicos
 * Servidor base por defecto: http://localhost:8080
 */

const API_BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";

// Sesión persistente (docs/selector-rol-y-sesion.md §1, docs/notas-para-back.md §1) — apagada
// por defecto porque el backend todavía no emite refresh tokens. Con la bandera apagada, el
// comportamiento de acá abajo es idéntico al de siempre: cualquier 401 dispara
// "psr:unauthorized" y expulsa al login. El día que el backend publique
// POST /api/auth/refresh y POST /api/auth/logout, se enciende esta variable de entorno y no
// hace falta tocar código.
export const REFRESH_ENABLED = String(import.meta.env.VITE_AUTH_REFRESH_ENABLED).toLowerCase() === "true";

export const TOKEN_STORAGE_KEY = "psr_auth_token";
export const REFRESH_TOKEN_STORAGE_KEY = "psr_refresh_token";
// `expiresIn` (segundos) que manda el backend en /login y /refresh — NOTAS-PARA-FRONTEND.md §2
// lo agrega explícitamente "para renovar por reloj sin esperar un 401". Se guarda como
// timestamp absoluto (ms) para poder programar la renovación proactiva de abajo.
const EXPIRES_AT_STORAGE_KEY = "psr_token_expires_at";

export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
    // El backend manda { status, code, message } en varios errores (75001, 75002,
    // CUENTA_DESACTIVADA...). Se expone acá para que cada pantalla no tenga que destripar
    // `error.data?.code` a mano — ver docs/PLAN-IMPLEMENTACION.md §0.2 y §5.1.
    this.code = data?.code;
  }
}

// Una sola renovación en vuelo — si cinco peticiones fallan a la vez con 401 (ej. al recargar
// una pantalla con varios fetch en paralelo justo cuando el access token venció), no se
// disparan cinco POST /api/auth/refresh a la vez: el primero en llegar aquí sincrónicamente
// guarda su promesa en este módulo antes de que cualquier otro la revise, y todos esperan el
// mismo resultado. Sin esto, con rotación de refresh token activada del lado del backend, cinco
// renovaciones simultáneas se invalidarían entre sí. Ver docs/PLAN-IMPLEMENTACION.md Fase 4.
let refreshPromise = null;

async function doRefresh() {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);
  if (!refreshToken) {
    throw new ApiError("No hay sesión que renovar", 401, null);
  }
  // Pasa por `request()` como cualquier otra llamada (mismo parseo de error, mismo ApiError);
  // el guard `endpoint !== "/api/auth/refresh"` de más abajo evita que esta propia llamada
  // intente renovarse a sí misma si también responde 401.
  const data = await request("/api/auth/refresh", {
    method: "POST",
    body: JSON.stringify({ refreshToken }),
  });
  if (data?.token) localStorage.setItem(TOKEN_STORAGE_KEY, data.token);
  // Rotación desde el día uno del lado del backend (NOTAS-PARA-FRONTEND.md §2): cada /refresh
  // devuelve un refresh token nuevo e invalida el usado, así que siempre hay que guardar el que
  // llegue acá — no es opcional como en un esquema sin rotación.
  if (data?.refreshToken) localStorage.setItem(REFRESH_TOKEN_STORAGE_KEY, data.refreshToken);
  storeExpiry(data?.expiresIn);
  scheduleProactiveRefresh();
  return data;
}

function refreshAccessToken() {
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

export function storeExpiry(expiresInSeconds) {
  if (!expiresInSeconds) return;
  localStorage.setItem(EXPIRES_AT_STORAGE_KEY, String(Date.now() + expiresInSeconds * 1000));
}

let proactiveRefreshTimer = null;

// Renovación por reloj, sin esperar a que una petición real dé 401 (lo que pide
// NOTAS-PARA-FRONTEND.md §2 al agregar `expiresIn`). Se programa un solo timer, 30s antes de
// que venza el access token; si ya venció o está por vencer ya mismo, renueva de inmediato.
// Llamado desde AuthContext al iniciar la app (si ya había sesión guardada) y después de cada
// login/refresh exitoso — nunca desde una pantalla.
export function scheduleProactiveRefresh() {
  if (!REFRESH_ENABLED) return;
  if (proactiveRefreshTimer) {
    clearTimeout(proactiveRefreshTimer);
    proactiveRefreshTimer = null;
  }
  const expiresAt = Number(localStorage.getItem(EXPIRES_AT_STORAGE_KEY));
  if (!expiresAt || !localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY)) return;

  const BUFFER_MS = 30_000;
  const delay = Math.max(0, expiresAt - Date.now() - BUFFER_MS);
  proactiveRefreshTimer = setTimeout(() => {
    refreshAccessToken().catch((err) => {
      // No es un error de pantalla — si falla, la próxima petición real dispara el flujo
      // reactivo de 401 de todos modos (o expulsa al login si el refresh token ya venció).
      console.warn("Renovación proactiva de sesión falló:", err);
    });
  }, delay);
}

export function cancelProactiveRefresh() {
  if (proactiveRefreshTimer) {
    clearTimeout(proactiveRefreshTimer);
    proactiveRefreshTimer = null;
  }
  localStorage.removeItem(EXPIRES_AT_STORAGE_KEY);
}

// `options.signal` (un AbortSignal) se propaga tal cual a `fetch` a través de `config` — no
// necesita tratamiento especial acá. Cualquier llamada puede cancelarse pasando
// `apiClient.get(endpoint, { signal })`; el buscador de personas lo usa para descartar
// respuestas atrasadas (ver docs/PLAN-IMPLEMENTACION.md §1.3). Si el fetch se aborta, la
// excepción (`AbortError`) se lanza antes de llegar a la lógica de abajo, así que nunca dispara
// `psr:unauthorized` ni se envuelve en ApiError — quien cancela debe descartarla por su cuenta.
//
// `isRetry` es un parámetro interno — nunca lo pasa una pantalla, solo la recursión de renovar
// y reintentar de acá abajo. Evita reintentar en bucle si la petición ya renovada vuelve a dar
// 401 (ej. el refresh token también venció mientras tanto).
export async function request(endpoint, options = {}, isRetry = false) {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);

  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const config = {
    ...options,
    headers,
  };

  const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

  if (response.status === 204) {
    return null;
  }

  let data;
  const contentType = response.headers.get("content-type");
  if (contentType && contentType.includes("application/json")) {
    data = await response.json();
  } else {
    const text = await response.text();
    data = text ? { message: text } : null;
  }

  if (!response.ok) {
    const canRenew =
      response.status === 401 &&
      REFRESH_ENABLED &&
      endpoint !== "/api/auth/refresh" &&
      !isRetry &&
      !!localStorage.getItem(REFRESH_TOKEN_STORAGE_KEY);

    if (canRenew) {
      try {
        await refreshAccessToken();
        return request(endpoint, options, true);
      } catch (refreshErr) {
        // El propio /refresh respondió 401 (o algo peor) — ahí sí es final: el refresh token
        // venció, no existe o fue revocado. Se limpia todo y se expulsa al login.
        localStorage.removeItem(TOKEN_STORAGE_KEY);
        localStorage.removeItem(REFRESH_TOKEN_STORAGE_KEY);
        window.dispatchEvent(new Event("psr:unauthorized"));
        throw refreshErr;
      }
    }

    const message = data?.message || data?.error || `Error HTTP ${response.status}`;
    if (response.status === 401) {
      window.dispatchEvent(new Event("psr:unauthorized"));
    }
    throw new ApiError(message, response.status, data);
  }

  return data;
}

export const apiClient = {
  get: (endpoint, options) => request(endpoint, { ...options, method: "GET" }),
  post: (endpoint, body, options) =>
    request(endpoint, { ...options, method: "POST", body: JSON.stringify(body) }),
  put: (endpoint, body, options) =>
    request(endpoint, { ...options, method: "PUT", body: JSON.stringify(body) }),
  patch: (endpoint, body, options) =>
    request(endpoint, { ...options, method: "PATCH", body: body ? JSON.stringify(body) : undefined }),
  delete: (endpoint, options) => request(endpoint, { ...options, method: "DELETE" }),
};
