# Estructura de frontend — PSR Check-In

Los 14 mocks JSX ya diseñados, reorganizados en la estructura de
`Reglas_de_Desarrollo_CLAUDE.md`. Los datos siguen siendo falsos
(arrays hardcodeados, sin `fetch` real) — la migración a `services/`
contra los endpoints reales es trabajo aparte, intencionalmente no
incluido acá.

## Qué se hizo, exactamente

- Cada mock se movió a su carpeta en `pages/<contexto>/`, con el
  nombre que ya habíamos acordado.
- El `export default function` de cada uno se renombró para que
  coincida con el nombre del archivo.
- Lo que estaba literalmente duplicado entre archivos se sacó a un
  solo lugar: 2 componentes (`Field`, `TabButton`), 1 hook
  (`useClock`), y funciones de formato (moneda, fecha/hora, texto) a
  `utils/`.
- **Segunda pasada — páginas que definían sub-componentes o lógica de
  dominio propia adentro** (viola la regla del proyecto: "las páginas
  únicamente ensamblan componentes"). Se movieron a
  `features/<dominio>/components/` y `features/<dominio>/utils.js`:
  - `features/reception/`: `QueueRow`, `RefundModal`, `PersonRow`,
    `PersonFormModal`, `HardDeleteModal` + `utils.js` (`getBalance`,
    `matchesFilter`, `computeCounts`, `getUpcomingDays`, `DAILY_RATE`,
    generadores de datos mock).
  - `features/admin/`: `RevenueTab`, `PendingChangeTab`, `PricingTab`,
    `PaymentMethodsTab`, `QueueConfigTab` + `utils.js` (`TEACHERS`,
    `teacherName`, `timeLabel`).
  - `features/lobby/`: `utils.js` (`minutesAgo`, `turnMessage`).
  - Las páginas quedaron solo ensamblando: importan y renderizan, sin
    definir nada nuevo adentro.
- Lo que **no** era idéntico entre archivos (ej. `inputClass` tiene 3
  variantes reales distintas) se dejó local en cada página/componente
  — fusionarlo habría cambiado el look de alguno sin que lo pidieras.

## Verificación

No se dio por bueno nada a ojo. Se corrió, en este orden:
1. Cada `.jsx`/`.js` transpilado con Babel (32 archivos, 0 errores).
2. Cada import relativo (`../../utils/...`, `../../features/...`, etc.)
   verificado programáticamente contra el sistema de archivos — 0 rutas
   rotas.
3. Cada import con nombre (`{ algo }`) verificado contra los `export`
   reales del archivo destino — 0 desajustes.


## Qué NO incluye, a propósito

- **`App.jsx` / `main.jsx` / routing** — cablear rutas, guards por rol
  y navegación es construir la aplicación, no reorganizar los mocks.
- **`services/`, `types/`, `layouts/`, `features/*`** están vacías
  (con `.gitkeep`) — son la carpeta correcta esperándote, no el
  contenido.
- **`LoginPage`, `RequestPasswordResetPage`, `ResetPasswordPage`** no
  existen — nunca se diseñó un mock para esas 3, así que no hay nada
  que mover.

## Mapeo original → destino

| Mock original | Destino |
|---|---|
| `vista_registro_movil.jsx` | `pages/public/SelfRegistrationPage.jsx` |
| `vista_checkin_kiosko.jsx` | `pages/lobby/LobbyCheckinPage.jsx` |
| `vista_registro_kiosko.jsx` | `pages/lobby/LobbyRegistrationPage.jsx` |
| `vista_cola_sala_espera.jsx` | `pages/lobby/LobbyQueueDisplayPage.jsx` |
| `vista_asistencia_movil.jsx` | `pages/member/MemberHomePage.jsx` |
| `vista_contacto_emergencia_movil.jsx` | `pages/member/MemberEmergencyContactPage.jsx` |
| `admin_checkin_desktop.jsx` | `pages/reception/ReceptionCheckinPage.jsx` |
| `admin_people_registry.jsx` | `pages/reception/PeopleListPage.jsx` |
| `admin_schedule.jsx` | `pages/admin/SchedulePage.jsx` |
| `admin_sessions.jsx` | `pages/admin/SessionsPage.jsx` |
| `admin_settings.jsx` | `pages/admin/SettingsPage.jsx` |
| `admin_reports.jsx` | `pages/admin/ReportsPage.jsx` |
| `admin_audit.jsx` | `pages/admin/AuditPage.jsx` |
| `vista_exploracion_contacto_emergencia.jsx` | `design-tools/EmergencyContactExploration.jsx` (fuera de `src/` — no es una ruta real de la app, sigue siendo tu herramienta de decisión) |

Todos los `.jsx` se validaron con Babel antes de entregarse.
