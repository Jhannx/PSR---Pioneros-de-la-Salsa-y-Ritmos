import { useState } from "react";
import { TrendingUp, HandCoins } from "lucide-react";
import TabButton from "../../components/ui/TabButton";
import RevenueTab from "../../features/admin/components/RevenueTab";
import PendingChangeTab from "../../features/admin/components/PendingChangeTab";

export default function ReportsPage() {
  const [tab, setTab] = useState("recaudo"); // recaudo | vueltos

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-6 py-8">
      <div className="max-w-4xl mx-auto">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-white">Reportes</h1>
          <p className="text-neutral-500 text-sm mt-1">Recaudo por sesión y vueltos pendientes de entregar.</p>
        </div>

        <div className="flex bg-neutral-900 border border-neutral-800 rounded-xl p-1 mb-6 w-fit">
          <TabButton icon={<TrendingUp size={14} />} label="Recaudo" active={tab === "recaudo"} onClick={() => setTab("recaudo")} />
          <TabButton icon={<HandCoins size={14} />} label="Vueltos pendientes" active={tab === "vueltos"} onClick={() => setTab("vueltos")} />
        </div>

        {tab === "recaudo" && <RevenueTab />}
        {tab === "vueltos" && <PendingChangeTab />}
      </div>
    </div>
  );
}
