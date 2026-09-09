import { useState } from "react";
import { DollarSign, CreditCard, Users2 } from "lucide-react";
import TabButton from "../../components/ui/TabButton";
import PricingTab from "../../features/admin/components/PricingTab";
import PaymentMethodsTab from "../../features/admin/components/PaymentMethodsTab";
import QueueConfigTab from "../../features/admin/components/QueueConfigTab";

export default function SettingsPage() {
  const [tab, setTab] = useState("tarifa"); // tarifa | metodos | cola

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-6 py-8">
      <div className="max-w-3xl mx-auto">
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-white">Configuración</h1>
          <p className="text-neutral-500 text-sm mt-1">Tarifa vigente, métodos de pago y parámetros de la cola.</p>
        </div>

        <div className="flex bg-neutral-900 border border-neutral-800 rounded-xl p-1 mb-6 w-fit">
          <TabButton icon={<DollarSign size={14} />} label="Tarifa" active={tab === "tarifa"} onClick={() => setTab("tarifa")} />
          <TabButton icon={<CreditCard size={14} />} label="Métodos de pago" active={tab === "metodos"} onClick={() => setTab("metodos")} />
          <TabButton icon={<Users2 size={14} />} label="Cola" active={tab === "cola"} onClick={() => setTab("cola")} />
        </div>

        {tab === "tarifa" && <PricingTab />}
        {tab === "metodos" && <PaymentMethodsTab />}
        {tab === "cola" && <QueueConfigTab />}
      </div>
    </div>
  );
}
