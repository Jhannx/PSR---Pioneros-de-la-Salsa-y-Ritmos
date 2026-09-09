// Repetido idéntico en admin_settings.jsx y admin_reports.jsx.
export default function TabButton({ icon, label, active, onClick }) {
  return (
    <button
      onClick={onClick}
      className={`flex items-center gap-1.5 px-4 py-2 rounded-lg text-sm font-semibold transition-colors ${
        active ? "bg-emerald-500 text-neutral-950" : "text-neutral-400 hover:text-white"
      }`}
    >
      {icon}
      {label}
    </button>
  );
}
