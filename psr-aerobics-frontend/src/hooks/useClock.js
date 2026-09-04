import { useState, useEffect } from "react";

// vista_cola_sala_espera.jsx — reloj de la cabecera, se actualiza cada
// segundo. Puramente informativo: no decide nada de la lógica de cola.
export default function useClock() {
  const [now, setNow] = useState(new Date());

  useEffect(() => {
    const clock = setInterval(() => setNow(new Date()), 1000);
    return () => clearInterval(clock);
  }, []);

  return now;
}
