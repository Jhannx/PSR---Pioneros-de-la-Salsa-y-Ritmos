import { useMemo } from "react";
import { getMaxBirthDateForAge } from "../../utils/date";

// Reemplaza el <input type="date"> nativo para fecha de nacimiento (errores-nuevo-front.md, nota
// suelta "Cambiar Selector de fechas"): llegar a un año lejano con el selector nativo obliga a
// desplazarse año por año desde hoy. Acá se elige directo con tres <select>. También impone la
// edad mínima estructuralmente (no se puede ni construir una fecha inválida), en vez de solo
// validarla después del submit.
const MONTHS = [
  "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
  "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
];

function daysInMonth(year, month) {
  return new Date(year, month, 0).getDate();
}

// `value`/`onChange` usan el mismo formato ISO "YYYY-MM-DD" (o "") que ya manejan los tres
// formularios de fecha de nacimiento — solo se reemplaza el input, el resto de cada formulario
// no cambia. `minAge`/`oldestAge` acotan el rango de años mostrado.
export default function BirthDateField({
  value,
  onChange,
  minAge = 10,
  oldestAge = 100,
  selectClassName = "bg-neutral-800 border border-neutral-700 rounded-xl px-2 py-2.5 text-white text-sm outline-none focus:border-emerald-500 transition-colors",
  className = "",
}) {
  const [cutoffYear, cutoffMonth, cutoffDay] = useMemo(
    () => getMaxBirthDateForAge(minAge).split("-").map(Number),
    [minAge]
  );
  const firstYear = cutoffYear - (oldestAge - minAge);

  const years = useMemo(() => {
    const list = [];
    for (let y = cutoffYear; y >= firstYear; y--) list.push(y);
    return list;
  }, [cutoffYear, firstYear]);

  const [y, m, d] = value ? value.split("-").map(Number) : [null, null, null];

  const maxMonth = y === cutoffYear ? cutoffMonth : 12;
  const months = useMemo(
    () => MONTHS.map((name, i) => ({ value: i + 1, name })).filter((month) => month.value <= maxMonth),
    [maxMonth]
  );

  const maxDay =
    y === cutoffYear && m === cutoffMonth ? cutoffDay : y && m ? daysInMonth(y, m) : 31;
  const days = useMemo(() => Array.from({ length: maxDay }, (_, i) => i + 1), [maxDay]);

  function emit(nextYear, nextMonth, nextDay) {
    if (!nextYear || !nextMonth || !nextDay) {
      onChange("");
      return;
    }
    const clampedMonth = nextYear === cutoffYear ? Math.min(nextMonth, cutoffMonth) : nextMonth;
    const dayLimit =
      nextYear === cutoffYear && clampedMonth === cutoffMonth
        ? cutoffDay
        : daysInMonth(nextYear, clampedMonth);
    const clampedDay = Math.min(nextDay, dayLimit);
    const pad = (n) => String(n).padStart(2, "0");
    onChange(`${nextYear}-${pad(clampedMonth)}-${pad(clampedDay)}`);
  }

  return (
    <div className={`grid grid-cols-3 gap-2 ${className}`}>
      <select value={d || ""} onChange={(e) => emit(y, m, Number(e.target.value))} className={selectClassName}>
        <option value="">Día</option>
        {days.map((day) => (
          <option key={day} value={day}>
            {day}
          </option>
        ))}
      </select>
      <select value={m || ""} onChange={(e) => emit(y, Number(e.target.value), d || 1)} className={selectClassName}>
        <option value="">Mes</option>
        {months.map((month) => (
          <option key={month.value} value={month.value}>
            {month.name}
          </option>
        ))}
      </select>
      <select value={y || ""} onChange={(e) => emit(Number(e.target.value), m || 1, d || 1)} className={selectClassName}>
        <option value="">Año</option>
        {years.map((year) => (
          <option key={year} value={year}>
            {year}
          </option>
        ))}
      </select>
    </div>
  );
}
