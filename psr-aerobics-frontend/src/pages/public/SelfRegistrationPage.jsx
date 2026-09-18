import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { CheckCircle2, CreditCard, KeyRound, Mail, Cake, LogIn, ArrowLeft } from "lucide-react";
import Field from "../../components/forms/Field";
import BirthDateField from "../../components/forms/BirthDateField";
import RegistrationConfirmModal from "../../components/shared/RegistrationConfirmModal";
import RegistrationConsent from "../../components/shared/RegistrationConsent";
import RegistrationHealthAndContacts from "../../components/shared/RegistrationHealthAndContacts";
import { peopleService } from "../../services/peopleService";
import { isValidDocumentNumber, isValidPhone } from "../../utils/validation";

const inputClass =
  "w-full bg-neutral-800 border border-neutral-700 rounded-xl px-4 py-3 text-white text-base placeholder-neutral-600 outline-none focus:border-emerald-500";

export default function SelfRegistrationPage({ activityName = "Aeróbicos" }) {
  const navigate = useNavigate();
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [documentType, setDocumentType] = useState("CC");
  const [documento, setDocumento] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [birthDate, setBirthDate] = useState("");
  const [passwordMode, setPasswordMode] = useState("documento"); // documento | propia
  const [password, setPassword] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  // Panel de confirmación antes del envío final (mejoras-frontend.md §1) — un typo en el
  // documento o el teléfono acá nadie más lo revisa antes de guardarlo, a diferencia del alta
  // que hace recepción.
  const [showConfirm, setShowConfirm] = useState(false);
  // Aceptación general de la política + declaración de participación voluntaria
  // (docs/pendiente-implementar-sistema.md §Frontend #2, política numeral 12) — ver
  // RegistrationConsent.jsx para el porqué de dos casillas separadas.
  const [acceptedPolicy, setAcceptedPolicy] = useState(false);
  const [acceptedVoluntary, setAcceptedVoluntary] = useState(false);

  // Salud y contactos de emergencia, opcionales (NOTAS-PARA-FRONTEND.md Bloque 2, C1) — antes
  // esto solo se podía cargar estando logueado (docs/pendientes-back.md C1, resuelto). Ver
  // RegistrationHealthAndContacts.jsx para el contrato completo del objeto.
  const [healthAndContacts, setHealthAndContacts] = useState({
    healthObservations: "",
    healthDataConsent: false,
    emergencyContacts: [],
  });

  // Front-only, ver docs/errores-13sep-front.md #3 — no reemplaza la validación real del backend.
  const documentoValid = isValidDocumentNumber(documento);
  const phoneValid = isValidPhone(phone);
  // Si hay observaciones de salud escritas, hace falta su propio consentimiento (numeral 4.2) —
  // sin esto el backend responde 400. Los contactos no necesitan este chequeo: `handleAddContactClick`
  // en RegistrationHealthAndContacts.jsx nunca deja agregar uno sin `thirdPartyConsent: true`.
  const healthConsentPending =
    healthAndContacts.healthObservations.trim().length > 0 && !healthAndContacts.healthDataConsent;

  const canSubmit =
    firstName.trim() &&
    lastName.trim() &&
    documentoValid &&
    phoneValid &&
    birthDate &&
    acceptedPolicy &&
    acceptedVoluntary &&
    !healthConsentPending &&
    (passwordMode === "documento" || password.trim().length >= 8);

  async function handleSubmit() {
    if (!canSubmit) return;
    setLoading(true);
    setErrorMessage("");
    try {
      await peopleService.autoRegister({
        documentType,
        documentNumber: documento.trim(),
        firstName: firstName.trim(),
        lastName: lastName.trim(),
        phone: phone.trim(),
        email: email.trim() || undefined,
        birthDate,
        password: passwordMode === "documento" ? null : password,
        dataProcessingConsent: acceptedPolicy,
        voluntaryParticipationConsent: acceptedVoluntary,
        // Opcionales — se omiten por completo si no hay nada, en vez de mandar
        // healthObservations: "" o emergencyContacts: [] (NOTAS-PARA-FRONTEND.md Bloque 2, C1).
        ...(healthAndContacts.healthObservations.trim()
          ? {
              healthObservations: healthAndContacts.healthObservations.trim(),
              healthDataConsent: true,
            }
          : {}),
        ...(healthAndContacts.emergencyContacts.length > 0
          ? { emergencyContacts: healthAndContacts.emergencyContacts }
          : {}),
      });
      setShowConfirm(false);
      setSubmitted(true);
    } catch (err) {
      setShowConfirm(false);
      setErrorMessage(err.data?.message || err.message || "No fue posible registrar la cuenta.");
    } finally {
      setLoading(false);
    }
  }

  if (submitted) {
    return (
      <div className="min-h-screen w-full bg-neutral-950 flex items-start justify-center px-4 py-10">
        <div className="w-full max-w-sm">
          <div className="bg-neutral-900 rounded-3xl p-6 border border-neutral-800 text-center">
            <div className="w-16 h-16 rounded-full bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-4">
              <CheckCircle2 className="text-emerald-400" size={32} />
            </div>
            <h1 className="text-xl font-bold text-white">¡Listo!</h1>
            <p className="text-neutral-400 text-sm mt-2">
              Ya quedaste registrado. La próxima vez que escanees el QR de asistencia, puedes marcar directo.
            </p>

            <div className="mt-6 bg-neutral-800 rounded-2xl p-4 text-left">
              <p className="text-white font-semibold">
                {firstName} {lastName}
              </p>
              <p className="text-neutral-500 text-sm mt-0.5">Documento: {documentType} {documento}</p>
              <p className="text-neutral-500 text-sm">{phone}</p>
            </div>

            <button
              onClick={() => navigate("/login")}
              className="w-full mt-6 flex items-center justify-center gap-2 bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-3 rounded-xl transition-colors"
            >
              <LogIn size={18} /> Volver a Iniciar Sesión
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-start justify-center px-4 py-10 relative">
      <button
        onClick={() => navigate("/login")}
        className="absolute top-6 left-4 flex items-center gap-2 text-neutral-400 hover:text-white text-xs bg-neutral-900 border border-neutral-800 px-3 py-2 rounded-xl transition-colors"
      >
        <ArrowLeft size={14} /> Volver a Iniciar Sesión
      </button>

      <div className="w-full max-w-sm mt-12">
        <div className="text-center mb-6">
          <p className="text-neutral-500 text-sm font-semibold tracking-wide uppercase">{activityName}</p>
          <h1 className="text-2xl font-bold text-white mt-1">Crea tu perfil</h1>
          <p className="text-neutral-400 text-sm mt-1">
            Completa tus datos para poder ingresar a las clases
          </p>
        </div>

        {errorMessage && (
          <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl text-center">
            {errorMessage}
          </div>
        )}

        <div className="bg-neutral-900 rounded-3xl p-6 border border-neutral-800 flex flex-col gap-4">
          <Field label="Nombre">
            <input
              value={firstName}
              onChange={(e) => setFirstName(e.target.value)}
              placeholder="Rosa"
              className={inputClass}
            />
          </Field>

          <Field label="Apellidos">
            <input
              value={lastName}
              onChange={(e) => setLastName(e.target.value)}
              placeholder="Martínez López"
              className={inputClass}
            />
          </Field>

          <div className="grid grid-cols-3 gap-2">
            <Field label="Tipo Doc.">
              <select
                value={documentType}
                onChange={(e) => setDocumentType(e.target.value)}
                className={inputClass}
              >
                <option value="CC">CC</option>
                <option value="TI">TI</option>
                <option value="CE">CE</option>
                <option value="PA">PA</option>
              </select>
            </Field>

            <div className="col-span-2">
              <Field label="Número de Documento" icon={<CreditCard size={14} />}>
                <input
                  inputMode="numeric"
                  value={documento}
                  onChange={(e) => setDocumento(e.target.value.replace(/\D/g, ""))}
                  placeholder="1001234567"
                  className={inputClass}
                />
              </Field>
              {documento && !documentoValid && (
                <p className="text-amber-400 text-xs mt-1">Mínimo 6 dígitos.</p>
              )}
            </div>
          </div>

          <Field label="Teléfono">
            <input
              type="tel"
              inputMode="tel"
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              placeholder="300 123 4567"
              className={inputClass}
            />
          </Field>
          {phone && !phoneValid && <p className="text-amber-400 text-xs -mt-2">Mínimo 10 dígitos.</p>}

          <Field label="Correo electrónico (opcional)" icon={<Mail size={14} />}>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="correo@ejemplo.com"
              className={inputClass}
            />
          </Field>

          <Field label="Fecha de nacimiento" icon={<Cake size={14} />}>
            {/* minAge explícito (no solo el default del componente): el backend rechaza menores
                de edad en el alta (RegisterService.MINIMUM_AGE), acá se ve desde el selector. */}
            <BirthDateField value={birthDate} onChange={setBirthDate} minAge={18} selectClassName={inputClass} />
          </Field>

          <div>
            <label className="text-neutral-300 text-sm font-semibold flex items-center gap-1.5 mb-1.5">
              <KeyRound size={14} />
              Tu contraseña
            </label>
            <div className="flex bg-neutral-800 rounded-lg p-1 text-xs font-semibold mb-2">
              <button
                type="button"
                onClick={() => setPasswordMode("documento")}
                className={`flex-1 px-3 py-2 rounded-md transition-colors ${
                  passwordMode === "documento" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400"
                }`}
              >
                Usar mi documento
              </button>
              <button
                type="button"
                onClick={() => setPasswordMode("propia")}
                className={`flex-1 px-3 py-2 rounded-md transition-colors ${
                  passwordMode === "propia" ? "bg-emerald-500 text-neutral-950" : "text-neutral-400"
                }`}
              >
                Elegir la mía
              </button>
            </div>
            {passwordMode === "documento" ? (
              <p className="text-neutral-600 text-xs">
                Será tu documento{documento ? `: ${documento}` : ""}. La puedes cambiar después desde tu perfil.
              </p>
            ) : (
              <>
                <input
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Escribe tu contraseña (mínimo 8 caracteres)"
                  className={inputClass}
                />
                {password && password.length < 8 && (
                  <p className="text-amber-400 text-xs mt-1">La contraseña debe tener al menos 8 caracteres.</p>
                )}
              </>
            )}
          </div>

          <RegistrationConsent
            acceptedPolicy={acceptedPolicy}
            onAcceptedPolicyChange={setAcceptedPolicy}
            acceptedVoluntary={acceptedVoluntary}
            onAcceptedVoluntaryChange={setAcceptedVoluntary}
          />

          <RegistrationHealthAndContacts
            value={healthAndContacts}
            onChange={setHealthAndContacts}
            inputClass={inputClass}
          />
        </div>

        <button
          onClick={() => setShowConfirm(true)}
          disabled={!canSubmit || loading}
          className="w-full mt-6 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 active:scale-[0.98] transition-all text-neutral-950 font-bold text-lg py-5 rounded-2xl shadow-lg shadow-emerald-500/20"
        >
          {loading ? "Registrando..." : "Enviar solicitud"}
        </button>
      </div>

      {showConfirm && (
        <RegistrationConfirmModal
          data={{ firstName, lastName, documentType, documento, phone, email, birthDate }}
          submitting={loading}
          onConfirm={handleSubmit}
          onClose={() => setShowConfirm(false)}
        />
      )}
    </div>
  );
}
