import { useState, useEffect } from "react";
import { Search, Calendar as CalendarIcon, Wallet, CreditCard, BarChart2 } from "lucide-react";
import { formatCOP } from "../../utils/currency";
import { reportsService } from "../../services/reportsService";
import RevenueTab from "../../features/admin/components/RevenueTab";
import PendingChangeTab from "../../features/admin/components/PendingChangeTab";

const PERIODS = [
  { key: "day", label: "Día" },
  { key: "week", label: "Semana" },
  { key: "month", label: "Mes" },
  { key: "year", label: "Año" },
];

const DAY_LABELS = ["Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"];
const EMPTY_WEEK = DAY_LABELS.map((dayLabel) => ({ dayLabel, averageCount: 0 }));

export default function ReportsPage() {
  const [activeTab, setActiveTab] = useState("resumen"); // resumen | recaudo | cuentas | historial
  const [searchQuery, setSearchQuery] = useState("");
  const [accountFilter, setAccountFilter] = useState("all");
  const [period, setPeriod] = useState("day");
  const [dashboardError, setDashboardError] = useState("");

  const [accounts, setAccounts] = useState([]);
  const [accountsPage, setAccountsPage] = useState(0);
  const [accountsPageInfo, setAccountsPageInfo] = useState({ totalPages: 0, totalElements: 0 });
  const [accountsLoading, setAccountsLoading] = useState(true);
  const [accountsError, setAccountsError] = useState("");

  const [dashboardData, setDashboardData] = useState({
    totals: { day: { attendees: 0, revenue: 0 }, week: { attendees: 0, revenue: 0 }, month: { attendees: 0, revenue: 0 }, year: { attendees: 0, revenue: 0 } },
    averageAttendees: 0,
    averageRevenue: 0,
    mainPaymentMethod: "—",
    weeklyAttendanceAverage: EMPTY_WEEK,
  });

  useEffect(() => {
    async function loadAccounts() {
      setAccountsLoading(true);
      setAccountsError("");
      try {
        const response = await reportsService.getPersonBalances({ page: accountsPage, size: 20 });
        setAccounts(response?.content || []);
        setAccountsPageInfo({
          totalPages: response?.totalPages || 0,
          totalElements: response?.totalElements || 0,
        });
      } catch (err) {
        setAccounts([]);
        setAccountsError(err.data?.message || err.message || "No se pudo cargar el estado de cuenta.");
      } finally {
        setAccountsLoading(false);
      }
    }
    if (activeTab === "cuentas") loadAccounts();
  }, [activeTab, accountsPage]);

  const filteredAccounts = accounts.filter((a) => {
    const q = searchQuery.trim().toLowerCase();
    if (q && !`${a.firstName} ${a.lastName}`.toLowerCase().includes(q)) return false;
    if (accountFilter === "debe") return a.balance < 0;
    if (accountFilter === "al_dia") return a.balance === 0;
    if (accountFilter === "a_favor") return a.balance > 0;
    return true;
  });

  useEffect(() => {
    async function loadDashboard() {
      setDashboardError("");
      try {
        const data = await reportsService.getDashboard();
        if (!data) return;

        const weeklyAttendanceAverage = Array.isArray(data.weeklyAttendanceAverage)
          ? data.weeklyAttendanceAverage.map((item) => ({
              dayLabel: DAY_LABELS[(item.dayOfWeek || 1) - 1] || "Día",
              averageCount: item.averageCount || 0,
            }))
          : EMPTY_WEEK;

        setDashboardData({
          totals: data.totals || {},
          averageAttendees: data.averageAttendees || 0,
          averageRevenue: data.averageRevenue || 0,
          mainPaymentMethod: data.mainPaymentMethod || "—",
          weeklyAttendanceAverage,
        });
      } catch (err) {
        setDashboardError(err.data?.message || err.message || "No se pudo cargar el dashboard de reportes.");
      }
    }

    loadDashboard();
  }, []);

  const currentTotals = dashboardData.totals?.[period] || { attendees: 0, revenue: 0 };
  const maxWeeklyCount = Math.max(...dashboardData.weeklyAttendanceAverage.map((d) => d.averageCount), 1);

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-4 md:px-6 py-4 font-body text-white">
      <div className="max-w-6xl mx-auto space-y-3">
        {/* Encabezado compacto */}
        <div className="flex items-center justify-between border-b border-neutral-800 pb-3">
          <h1 className="text-xl font-bold text-white tracking-tight">Reportes</h1>
        </div>

        {/* Pestañas principales */}
        <div className="flex border-b border-neutral-800 text-sm font-bold">
          {[
            { key: "resumen", label: "Resumen", icon: BarChart2 },
            { key: "recaudo", label: "Recaudo", icon: Wallet },
            { key: "cuentas", label: "Cuentas", icon: CreditCard },
            { key: "historial", label: "Historial", icon: CalendarIcon },
          ].map((tab) => {
            const active = activeTab === tab.key;
            return (
              <button
                key={tab.key}
                onClick={() => setActiveTab(tab.key)}
                className={`px-6 py-3 border-b-2 transition-colors flex items-center gap-2 ${
                  active
                    ? "border-emerald-500 text-emerald-400 font-extrabold"
                    : "border-transparent text-neutral-400 hover:text-neutral-200"
                }`}
              >
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {/* Contenido Pestaña Resumen */}
        {activeTab === "resumen" && (
          <div className="space-y-6">
            {dashboardError && (
              <div className="p-4 bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm rounded-2xl">
                {dashboardError}
              </div>
            )}

            {/* Selector de periodo — misma respuesta, sin llamadas nuevas */}
            <div className="flex flex-wrap gap-2">
              {PERIODS.map((p) => (
                <button
                  key={p.key}
                  onClick={() => setPeriod(p.key)}
                  className={`px-4 py-2 rounded-full text-xs font-bold transition-colors border ${
                    period === p.key
                      ? "bg-white text-neutral-950 border-white"
                      : "bg-neutral-900 text-neutral-400 border-neutral-800 hover:border-neutral-700"
                  }`}
                >
                  {p.label}
                </button>
              ))}
            </div>

            {/* Tarjeta 1: Asistentes */}
            <div className="bg-neutral-900/80 border border-neutral-800 rounded-3xl p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <span className="text-xs font-extrabold text-neutral-400 uppercase tracking-wider block mb-1">
                  ASISTENTES · {PERIODS.find((p) => p.key === period)?.label.toUpperCase()}
                </span>
                <span className="text-3xl font-extrabold font-display text-white">
                  {currentTotals.attendees ?? 0}
                </span>
              </div>
              <div className="text-left sm:text-right">
                <span className="text-xs font-extrabold text-neutral-400 uppercase tracking-wider block mb-1">
                  PROMEDIO DIARIO DE ASISTENTES
                </span>
                <span className="text-2xl font-bold text-neutral-200">
                  {dashboardData.averageAttendees}
                </span>
              </div>
              <div className="text-neutral-500 shrink-0">
                <CalendarIcon size={24} />
              </div>
            </div>

            {/* Tarjeta 2: Recaudo */}
            <div className="bg-neutral-900/80 border border-neutral-800 rounded-3xl p-6 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <span className="text-xs font-extrabold text-neutral-400 uppercase tracking-wider block mb-1">
                  RECAUDO · {PERIODS.find((p) => p.key === period)?.label.toUpperCase()}
                </span>
                <span className="text-3xl font-extrabold font-display text-white">
                  {formatCOP(currentTotals.revenue ?? 0)}
                </span>
              </div>
              <div className="text-left sm:text-right">
                <span className="text-xs font-extrabold text-neutral-400 uppercase tracking-wider block mb-1">
                  PROMEDIO DIARIO DE RECAUDO
                </span>
                <span className="text-2xl font-bold text-neutral-200">
                  {formatCOP(dashboardData.averageRevenue).replace(" COP", "")}
                </span>
              </div>
              <div className="text-neutral-500 shrink-0">
                <CalendarIcon size={24} />
              </div>
            </div>

            {/* Tarjeta 3: Método de Pago Principal */}
            <div className="bg-neutral-900/80 border border-neutral-800 rounded-3xl p-6">
              <span className="text-xs font-extrabold text-neutral-400 uppercase tracking-wider block mb-1">
                MÉTODO DE PAGO PRINCIPAL
              </span>
              <span className="text-xl font-bold text-white">{dashboardData.mainPaymentMethod}</span>
            </div>

            {/* Tarjeta 4: Gráfico de Barras Promedio de Asistentes por Día */}
            <div className="bg-neutral-900/80 border border-neutral-800 rounded-3xl p-6">
              <span className="text-xs font-extrabold text-neutral-400 uppercase tracking-wider block mb-6">
                PROMEDIO DE ASISTENTES POR DÍA
              </span>

              <div className="h-48 flex items-end justify-between gap-3 pt-6 border-b border-neutral-800 pb-2">
                {dashboardData.weeklyAttendanceAverage.map((item) => {
                  const heightPercent = maxWeeklyCount > 0 ? (item.averageCount / maxWeeklyCount) * 100 : 0;
                  return (
                    <div key={item.dayLabel} className="flex-1 flex flex-col items-center h-full justify-end group">
                      <span className="text-xs text-emerald-400 font-bold mb-1 opacity-0 group-hover:opacity-100 transition-opacity">
                        {item.averageCount}
                      </span>
                      <div
                        style={{ height: `${Math.max(heightPercent, 4)}%` }}
                        className="w-full max-w-[48px] bg-emerald-500 rounded-t-xl transition-all group-hover:bg-emerald-400"
                      />
                      <span className="text-xs font-semibold text-neutral-400 mt-3">{item.dayLabel}</span>
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        )}

        {/* Contenido Pestaña Cuentas */}
        {activeTab === "cuentas" && (
          <div className="space-y-6">
            <div>
              <h2 className="text-lg font-bold text-white mb-1">Estado de cuenta de los participantes</h2>
              <p className="text-neutral-500 text-xs">
                {accountsPageInfo.totalElements} personas con movimientos registrados
              </p>
            </div>

            {accountsError && (
              <div className="p-4 bg-rose-500/10 border border-rose-500/20 text-rose-300 text-sm rounded-2xl">
                {accountsError}
              </div>
            )}

            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 text-neutral-500" size={18} />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Filtrar por nombre en esta página..."
                className="w-full bg-neutral-900 border border-neutral-800 rounded-2xl pl-11 pr-4 py-3 text-sm text-white placeholder-neutral-500 outline-none focus:border-emerald-500 transition-colors"
              />
            </div>

            <div className="flex flex-wrap gap-2">
              {[
                { key: "all", label: "TODOS" },
                { key: "debe", label: "DEBEN" },
                { key: "al_dia", label: "AL DÍA" },
                { key: "a_favor", label: "A FAVOR" },
              ].map((f) => {
                const active = accountFilter === f.key;
                return (
                  <button
                    key={f.key}
                    onClick={() => setAccountFilter(f.key)}
                    className={`px-4 py-2 rounded-full text-xs font-bold transition-colors border ${
                      active
                        ? "bg-white text-neutral-950 border-white"
                        : "bg-neutral-900 text-neutral-400 border-neutral-800 hover:border-neutral-700"
                    }`}
                  >
                    {f.label}
                  </button>
                );
              })}
            </div>

            <div className="bg-neutral-900/80 border border-neutral-800 rounded-3xl overflow-hidden shadow-xl">
              <div className="grid grid-cols-12 px-6 py-3.5 border-b border-neutral-800 text-xs font-extrabold text-neutral-400 uppercase tracking-wider">
                <div className="col-span-4">PERSONA</div>
                <div className="col-span-3 text-center">TOTAL PAGADO</div>
                <div className="col-span-2 text-center">TOTAL CARGADO</div>
                <div className="col-span-3 text-right">ESTADO</div>
              </div>

              <div className="divide-y divide-neutral-800/60">
                {accountsLoading ? (
                  <div className="p-8 text-center text-neutral-500 text-sm">Consultando saldos…</div>
                ) : filteredAccounts.length === 0 ? (
                  <div className="p-8 text-center text-neutral-500 text-sm">
                    No hay personas que coincidan con el filtro.
                  </div>
                ) : (
                  filteredAccounts.map((account) => {
                    let statusLabel = "Al día";
                    let statusStyle = "text-emerald-400 font-bold";
                    if (account.balance < 0) {
                      statusLabel = `Debe ${formatCOP(Math.abs(account.balance))}`;
                      statusStyle = "text-red-400 font-bold";
                    } else if (account.balance > 0) {
                      statusLabel = `A favor ${formatCOP(account.balance)}`;
                      statusStyle = "text-sky-400 font-bold";
                    }

                    return (
                      <div
                        key={account.personId}
                        className="grid grid-cols-12 px-6 py-4 items-center text-sm hover:bg-neutral-800/40 transition-colors"
                      >
                        <div className="col-span-4 font-bold text-white">
                          {account.firstName} {account.lastName}
                        </div>
                        <div className="col-span-3 text-center text-neutral-400 font-medium">
                          {formatCOP(account.totalPaid || 0)}
                        </div>
                        <div className="col-span-2 text-center text-neutral-400 font-medium">
                          {formatCOP(account.totalCharged || 0)}
                        </div>
                        <div className={`col-span-3 text-right ${statusStyle}`}>{statusLabel}</div>
                      </div>
                    );
                  })
                )}
              </div>
            </div>

            {accountsPageInfo.totalPages > 1 && (
              <div className="flex justify-center gap-3 text-sm">
                <button
                  disabled={accountsPage === 0}
                  onClick={() => setAccountsPage((p) => p - 1)}
                  className="px-3 py-2 rounded-lg bg-neutral-800 disabled:opacity-40"
                >
                  Anterior
                </button>
                <span className="py-2 text-neutral-400">
                  Página {accountsPage + 1} de {accountsPageInfo.totalPages}
                </span>
                <button
                  disabled={accountsPage + 1 >= accountsPageInfo.totalPages}
                  onClick={() => setAccountsPage((p) => p + 1)}
                  className="px-3 py-2 rounded-lg bg-neutral-800 disabled:opacity-40"
                >
                  Siguiente
                </button>
              </div>
            )}
          </div>
        )}

        {/* Contenido Pestaña Recaudo */}
        {activeTab === "recaudo" && (
          <div className="space-y-8">
            <div>
              <h2 className="text-lg font-bold text-white mb-3">Recaudo por sesión</h2>
              <RevenueTab mode="sesion" />
            </div>
            <div className="border-t border-neutral-800 pt-6">
              <h2 className="text-lg font-bold text-white mb-1">Vueltos pendientes por entregar</h2>
              <p className="text-neutral-500 text-xs mb-3">
                Dinero que quedó a favor de la persona en un pago y todavía no se le ha devuelto en mano.
              </p>
              <PendingChangeTab />
            </div>
          </div>
        )}

        {/* Contenido Pestaña Historial (recaudo diario / caja) */}
        {activeTab === "historial" && (
          <div>
            <h2 className="text-lg font-bold text-white mb-1">Recaudo diario (caja)</h2>
            <p className="text-neutral-500 text-xs mb-3">
              Dinero que entró ese día, sin importar a qué fecha de clase se aplicó — úsalo para cuadrar
              caja, no para comparar contra "Recaudo por sesión" (esa es la cifra devengada).
            </p>
            <RevenueTab mode="diario" />
          </div>
        )}
      </div>
    </div>
  );
}
