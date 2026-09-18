import { useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";
import { CheckCircle2, CreditCard, KeyRound, ArrowLeft, Pencil, Check, Mail, Cake } from "lucide-react";
import Field from "../../components/forms/Field";
import BirthDateField from "../../components/forms/BirthDateField";
import RegistrationConfirmModal from "../../components/shared/RegistrationConfirmModal";
import RegistrationConsent from "../../components/shared/RegistrationConsent";
import RegistrationHealthAndContacts from "../../components/shared/RegistrationHealthAndContacts";
import { peopleService } from "../../services/peopleService";
import { queueService } from "../../services/queueService";
import { isValidDocumentNumber, isValidPhone } from "../../utils/validation";

const inputClass =
  "w-full bg-neutral-900 border border-neutral-800 rounded-xl px-4 py-3 text-white text-base placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function LobbyRegistrationPage({ activityName = "Aeróbicos" }) {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const documentoInicial = searchParams.get("documento") || "";
  const [documentType, setDocumentType] = useState("CC");
  const [documento, setDocumento] = useState(documentoInicial);
  const [editingDocumento, setEditingDocumento] = useState(!documentoInicial);
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [birthDate, setBirthDate] = useState("");
  const [passwordMode, setPasswordMode] = useState("documento"); // documento | propia
  const [password, setPassword] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [queuePosition, setQueuePosition] = useState(null);
  // Panel de confirmación antes del envío final (mejoras-frontend.md §1).
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
  const healthConsentPending =
    healthAndContacts.healthObservations.trim().length > 0 && !healthAndContacts.healthDataConsent;

  const canSubmit =
    documentoValid &&
    firstName.trim() &&
    lastName.trim() &&
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
      const person = await peopleService.autoRegister({
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
        // Opcionales — se omiten por completo si no hay nada (NOTAS-PARA-FRONTEND.md Bloque 2, C1).
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

      try {
        const checkinResult = await queueService.checkin(person.id);
        setQueuePosition(checkinResult?.queuePosition ?? null);
      } catch (checkinErr) {
        console.warn("La persona se registró pero el check-in a la fila falló:", checkinErr);
        setQueuePosition(null);
      }

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
      <div className="min-h-screen w-full bg-neutral-950 flex flex-col items-center justify-center px-6 relative font-body">
        <button
          onClick={() => navigate("/lobby/checkin")}
          className="absolute top-6 left-6 flex items-center gap-2 text-neutral-400 hover:text-white text-xs bg-neutral-900 border border-neutral-800 px-3 py-2 rounded-xl transition-colors"
        >
          <ArrowLeft size={14} /> Volver a Terminal PSR
        </button>

        <div className="w-full max-w-md text-center">
          <div className="w-16 h-16 rounded-2xl bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-6">
            <CheckCircle2 className="text-emerald-400" size={28} />
          </div>
          <h1 className="text-2xl font-bold text-white mb-2">¡Listo, {firstName}!</h1>
          <p className="text-neutral-400 mb-6">
            {queuePosition != null ? "Ya quedaste registrado y estás en la fila de hoy" : "Ya quedaste registrado"}
          </p>

          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-6 text-left">
            <p className="text-white font-semibold">
              {firstName} {lastName}
            </p>
            <p className="text-neutral-500 text-sm mt-0.5">Documento: {documentType} {documento}</p>
            <p className="text-neutral-500 text-sm">{phone}</p>
          </div>

          {queuePosition != null ? (
            <p className="text-neutral-500 text-sm mt-6">
              Turno número <span className="text-white font-bold">{queuePosition}</span> — mira la pantalla de sala de espera
            </p>
          ) : (
            <p className="text-amber-400 text-sm mt-6">
              No pudimos ingresarte a la fila automáticamente. Pide en recepción que te agreguen.
            </p>
          )}

          <button
            onClick={() => navigate("/lobby/checkin")}
            className="w-full mt-6 bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-3.5 rounded-xl transition-colors"
          >
            Finalizar / Volver a Terminal
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex flex-col items-center justify-center px-6 py-10 font-body relative">
      <button
        onClick={() => navigate("/lobby/panel")}
        className="absolute top-6 left-6 flex items-center gap-2 text-neutral-400 hover:text-white text-xs bg-neutral-900 border border-neutral-800 px-3.5 py-2 rounded-xl transition-colors"
      >
        <ArrowLeft size={14} /> Volver al Panel
      </button>

      <div className="w-full max-w-2xl">
        <div className="text-center mb-6">
          <p className="text-neutral-500 text-xs font-semibold tracking-wide uppercase">{activityName} • Terminal PSR</p>
          <h1 className="text-2xl font-bold text-white mt-1">Crea tu perfil en Terminal PSR</h1>
          <p className="text-neutral-400 text-sm mt-1">Completa tus datos para ingresar a la fila</p>
        </div>

        {errorMessage && (
          <div className="mb-4 p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm rounded-xl text-center">
            {errorMessage}
          </div>
        )}

        <div className="bg-neutral-900/60 border border-neutral-800 rounded-3xl p-6">
          <div className="flex items-center gap-2 bg-neutral-800/60 border border-neutral-800 rounded-xl px-4 py-3 mb-5">
            <CreditCard size={16} className="text-neutral-500 shrink-0" />
            <select
              value={documentType}
              onChange={(e) => setDocumentType(e.target.value)}
              className="bg-transparent text-neutral-400 font-medium text-sm outline-none shrink-0 border-r border-neutral-700 pr-2"
            >
              <option value="CC" className="bg-neutral-900">CC</option>
              <option value="TI" className="bg-neutral-900">TI</option>
              <option value="CE" className="bg-neutral-900">CE</option>
              <option value="PA" className="bg-neutral-900">PA</option>
            </select>
            <span className="text-neutral-500 text-sm shrink-0">Documento</span>
            {/*
              Antes había un <input> (mientras se edita) y un <span> (al confirmar) que se
              montaban y desmontaban según `editingDocumento`. El dato nunca se perdía del
              estado, pero visualmente el número desaparecía al confirmar (errores-frontend.md
              #6) — el <span> no llevaba `min-w-0`, así que en pantallas angostas era el único
              elemento de la fila que el flex podía colapsar a 0. El arreglo es que el campo no
              se desmonte nunca: siempre es el mismo <input>, y solo cambia si es editable.
            */}
            <input
              autoFocus={editingDocumento}
              readOnly={!editingDocumento}
              inputMode="numeric"
              value={documento}
              onChange={(e) => setDocumento(e.target.value.replace(/\D/g, ""))}
              onKeyDown={(e) => e.key === "Enter" && documento.trim() && setEditingDocumento(false)}
              placeholder="1001234567"
              className={`ml-auto flex-1 min-w-0 bg-transparent border-b text-white font-semibold text-sm text-right outline-none ${
                editingDocumento ? "border-emerald-500" : "border-transparent cursor-default"
              }`}
            />
            <button
              type="button"
              onClick={() => setEditingDocumento((v) => !v)}
              title={editingDocumento ? "Confirmar documento" : "Corregir documento"}
              className="text-neutral-500 hover:text-emerald-400 shrink-0 p-1"
            >
              {editingDocumento ? <Check size={16} /> : <Pencil size={14} />}
            </button>
          </div>
          {documento && !documentoValid && (
            <p className="text-amber-400 text-xs -mt-3 mb-4 text-right">Mínimo 6 dígitos.</p>
          )}

          <div className="grid grid-cols-2 gap-4 mb-4">
            <Field label="Nombre">
              <input
                autoFocus={!editingDocumento}
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
          </div>

          <div className="grid grid-cols-2 gap-4 mb-4">
            <div>
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
              {phone && !phoneValid && <p className="text-amber-400 text-xs mt-1">Mínimo 10 dígitos.</p>}
            </div>
            <Field label="Correo electrónico (opcional)" icon={<Mail size={14} />}>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="correo@ejemplo.com"
                className={inputClass}
              />
            </Field>
          </div>

          <div className="grid grid-cols-2 gap-4 mb-4">
            <Field label="Fecha de nacimiento" icon={<Cake size={14} />}>
              {/* minAge explícito (no solo el default del componente): el backend rechaza menores
                  de edad en el alta (RegisterService.MINIMUM_AGE), acá se ve desde el selector. */}
              <BirthDateField value={birthDate} onChange={setBirthDate} minAge={18} selectClassName={inputClass} />
            </Field>
          </div>

          <div className="border-t border-neutral-800 mt-4 pt-4">
            <label className="text-neutral-300 text-sm font-semibold flex items-center gap-1.5 mb-2">
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
              <p className="text-neutral-500 text-xs">
                Tu contraseña será tu documento:{" "}
                <span className="text-white font-semibold">{documento}</span>. La puedes cambiar después desde tu
                perfil.
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
          className="w-full mt-6 bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 transition-colors text-neutral-950 font-bold text-lg py-4 rounded-2xl"
        >
          {loading ? "Guardando..." : "Guardar y entrar a la fila"}
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
