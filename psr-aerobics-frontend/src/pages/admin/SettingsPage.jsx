import { useState } from "react";
import { DollarSign, CreditCard, Users2 } from "lucide-react";
import TabButton from "../../components/ui/TabButton";
import PricingTab from "../../features/admin/components/PricingTab";
import PaymentMethodsTab from "../../features/admin/components/PaymentMethodsTab";
import QueueConfigTab from "../../features/admin/components/QueueConfigTab";
import { useAuth } from "../../context/AuthContext";

export default function SettingsPage() {
  const { user } = useAuth();
  const isAdmin = user.roles.includes("ADMIN");
  // Matriz de permisos (PENDIENTES.md): tarifa y config de cola son "ver" para
  // RECEPCIÓN/PROFESOR; métodos de pago solo trae "ver" para RECEPCIÓN, no PROFESOR.
  const canViewPaymentMethods = isAdmin || user.roles.includes("RECEPTIONIST");
  const readOnly = !isAdmin;

  const [tab, setTab] = useState("tarifa"); // tarifa | metodos | cola

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-4 md:px-6 py-4 text-white font-body">
      <div className="max-w-6xl mx-auto space-y-3">
        <div className="border-b border-neutral-800 pb-3">
          <h1 className="text-xl font-bold text-white tracking-tight">Configuración General</h1>
          <p className="text-neutral-400 text-xs mt-0.5">
            Tarifa vigente, métodos de pago aceptados y parámetros de la cola.
            {readOnly && " Solo lectura."}
          </p>
        </div>

        <div className="flex bg-neutral-900 border border-neutral-800 rounded-xl p-1 mb-6 w-fit">
          <TabButton icon={<DollarSign size={14} />} label="Tarifa" active={tab === "tarifa"} onClick={() => setTab("tarifa")} />
          {canViewPaymentMethods && (
            <TabButton icon={<CreditCard size={14} />} label="Métodos de pago" active={tab === "metodos"} onClick={() => setTab("metodos")} />
          )}
          <TabButton icon={<Users2 size={14} />} label="Cola" active={tab === "cola"} onClick={() => setTab("cola")} />
        </div>

        {tab === "tarifa" && <PricingTab readOnly={readOnly} />}
        {tab === "metodos" && canViewPaymentMethods && <PaymentMethodsTab readOnly={readOnly} />}
        {tab === "cola" && <QueueConfigTab readOnly={readOnly} />}
      </div>
    </div>
  );
}
