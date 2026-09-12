import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, QrCode } from "lucide-react";
import QRCode from "qrcode";

// Canal de check-in por QR: el celular de la persona escanea esto con su propia cámara (no la
// del kiosko), lo que abre el navegador directo en /member/home, donde ya está el botón de
// marcar asistencia (MemberHomePage.jsx, handleMarkAttendance → checkinPropio). Si no tiene
// sesión iniciada, ProtectedRoute la manda a /login y LoginPage la trae de vuelta acá tras
// loguear (state.from). No hace falta backend nuevo ni escanear nada dentro de esta app — el QR
// solo codifica una URL.
const CHECKIN_URL = `${window.location.origin}/member/home`;

export default function LobbyQrCheckinPage() {
  const navigate = useNavigate();
  const canvasRef = useRef(null);
  const [error, setError] = useState("");

  useEffect(() => {
    QRCode.toCanvas(canvasRef.current, CHECKIN_URL, { width: 360, margin: 2 }).catch((err) => {
      setError(err.message || "No fue posible generar el código QR.");
    });
  }, []);

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex flex-col items-center justify-center px-6 py-10 font-body relative">
      <button
        onClick={() => navigate("/lobby/panel")}
        className="absolute top-6 left-6 flex items-center gap-2 text-neutral-400 hover:text-white text-xs bg-neutral-900 border border-neutral-800 px-3 py-2 rounded-xl transition-colors"
      >
        <ArrowLeft size={14} /> Volver al Panel
      </button>

      <div className="w-full max-w-lg text-center">
        <div className="w-16 h-16 rounded-2xl bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-6">
          <QrCode className="text-emerald-400" size={28} />
        </div>
        <h1 className="text-2xl font-bold text-white mb-2">Marca tu asistencia por QR</h1>
        <p className="text-neutral-400 text-sm mb-6">
          Escanea este código con la cámara de tu celular para marcar tu asistencia.
        </p>

        <div className="bg-white rounded-3xl p-6 inline-block shadow-xl">
          {error ? (
            <p className="text-rose-500 text-sm max-w-[280px]">{error}</p>
          ) : (
            <canvas ref={canvasRef} />
          )}
        </div>

        <p className="text-neutral-600 text-xs mt-6">
          Si tu celular no tiene sesión iniciada, primero te pedirá ingresar.
        </p>
      </div>
    </div>
  );
}
