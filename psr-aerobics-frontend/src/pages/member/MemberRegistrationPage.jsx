import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { CheckCircle2, CreditCard, KeyRound, Mail, Cake, ArrowLeft } from "lucide-react";
import Field from "../../components/forms/Field";
import BirthDateField from "../../components/forms/BirthDateField";
import RegistrationConsent from "../../components/shared/RegistrationConsent";
import RegistrationHealthAndContacts from "../../components/shared/RegistrationHealthAndContacts";
import { peopleService } from "../../services/peopleService";
import { queueService } from "../../services/queueService";

const inputClass =
  "w-full bg-neutral-900 border border-neutral-800 rounded-xl px-4 py-3 text-white text-base placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors";

export default function MemberRegistrationPage({ activityName = "Aeróbicos" }) {
  const navigate = useNavigate();
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [documentType, setDocumentType] = useState("CC");
  const [documento, setDocumento] = useState("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  // Faltaba por completo (encontrado al conectar NOTAS-PARA-FRONTEND.md): esta pantalla llamaba a
  // `autoRegister` sin `birthDate` ni los consentimientos de registro, los tres obligatorios del
  // lado del backend (`birthDate` @NotNull, los otros dos obligatorios en la ruta de
  // autoregistro) — cualquier envío daba 400. La pantalla tampoco está enlazada desde ningún lado
  // de la UI todavía, así que nadie lo había notado.
  const [birthDate, setBirthDate] = useState("");
  const [acceptedPolicy, setAcceptedPolicy] = useState(false);
  const [acceptedVoluntary, setAcceptedVoluntary] = useState(false);
  const [passwordMode, setPasswordMode] = useState("documento"); // documento | propia
  const [password, setPassword] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [queuePosition, setQueuePosition] = useState(null);

  // Salud y contactos de emergencia, opcionales (NOTAS-PARA-FRONTEND.md Bloque 2, C1).
  const [healthAndContacts, setHealthAndContacts] = useState({
    healthObservations: "",
    healthDataConsent: false,
    emergencyContacts: [],
  });
  const healthConsentPending =
    healthAndContacts.healthObservations.trim().length > 0 && !healthAndContacts.healthDataConsent;

  const canSubmit =
    firstName.trim() &&
    lastName.trim() &&
    documento.trim() &&
    phone.trim() &&
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

      setSubmitted(true);
    } catch (err) {
      setErrorMessage(err.data?.message || err.message || "No fue posible registrar la cuenta.");
    } finally {
      setLoading(false);
    }
  }

  if (submitted) {
    return (
      <div className="min-h-screen w-full bg-neutral-950 flex items-start justify-center px-4 py-8 font-body">
        <div className="w-full max-w-md text-center">
          <div className="w-16 h-16 rounded-2xl bg-emerald-500/15 border-2 border-emerald-500 flex items-center justify-center mx-auto mb-4">
            <CheckCircle2 className="text-emerald-400" size={28} />
          </div>
          <h1 className="text-2xl font-bold text-white mb-1">¡Listo, {firstName}!</h1>
          <p className="text-neutral-400 text-sm mb-6">
            {queuePosition != null ? "Ya quedaste registrado y estás en la fila de hoy" : "Ya quedaste registrado"}
          </p>

          <div className="bg-neutral-900 border border-neutral-800 rounded-2xl p-5 text-left mb-6">
            <p className="text-white font-semibold">
              {firstName} {lastName}
            </p>
            <p className="text-neutral-500 text-sm mt-0.5">Documento: {documentType} {documento}</p>
            <p className="text-neutral-500 text-sm">{phone}</p>
          </div>

          {queuePosition != null ? (
            <p className="text-neutral-400 text-sm">
              Turno número <span className="text-white font-bold">{queuePosition}</span> — revisa tu estado en pantalla
            </p>
          ) : (
            <p className="text-amber-400 text-sm">
              No pudimos ingresarte a la fila automáticamente. Pide en recepción que te agreguen.
            </p>
          )}

          <button
            onClick={() => navigate("/login")}
            className="w-full mt-6 bg-emerald-500 hover:bg-emerald-400 text-neutral-950 font-bold py-3 rounded-xl transition-colors"
          >
            Ir a Iniciar Sesión
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 flex items-start justify-center px-4 py-6 font-body">
      <div className="w-full max-w-md space-y-4">
        <button
          onClick={() => navigate("/login")}
          className="flex items-center gap-2 text-neutral-400 hover:text-white text-xs bg-neutral-900 border border-neutral-800 px-3 py-2 rounded-xl transition-colors"
        >
          <ArrowLeft size={14} /> Volver a Iniciar Sesión
        </button>

        <div className="text-center">
          <p className="text-neutral-500 text-xs font-semibold tracking-wider uppercase">{activityName}</p>
          <h1 className="text-2xl font-bold text-white mt-0.5">Crea tu perfil de miembro</h1>
          <p className="text-neutral-400 text-xs mt-1">Completa tus datos para ingresar a la clase</p>
        </div>

        {errorMessage && (
          <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl text-center">
            {errorMessage}
          </div>
        )}

        <div className="bg-neutral-900/80 border border-neutral-800 rounded-3xl p-5 space-y-4 shadow-xl">
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
                  autoFocus
                  inputMode="numeric"
                  value={documento}
                  onChange={(e) => setDocumento(e.target.value.replace(/\D/g, ""))}
                  placeholder="1001234567"
                  className={inputClass}
                />
              </Field>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-3">
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
          </div>

          <div className="grid grid-cols-2 gap-3">
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
            <Field label="Correo (opcional)" icon={<Mail size={14} />}>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="correo@ejemplo.com"
                className={inputClass}
              />
            </Field>
          </div>

          <Field label="Fecha de nacimiento" icon={<Cake size={14} />}>
            {/* minAge explícito (no solo el default del componente): el backend rechaza menores
                de edad en el alta (RegisterService.MINIMUM_AGE), acá se ve desde el selector. */}
            <BirthDateField value={birthDate} onChange={setBirthDate} minAge={18} selectClassName={inputClass} />
          </Field>

          <div className="border-t border-neutral-800 pt-3">
            <label className="text-neutral-300 text-xs font-semibold flex items-center gap-1.5 mb-2">
              <KeyRound size={14} />
              Tu contraseña de acceso
            </label>
            <div className="flex bg-neutral-800 rounded-xl p-1 text-xs font-semibold mb-2">
              <button
                type="button"
                onClick={() => setPasswordMode("documento")}
                className={`flex-1 px-3 py-2 rounded-lg transition-colors ${
                  passwordMode === "documento" ? "bg-emerald-500 text-neutral-950 font-bold" : "text-neutral-400"
                }`}
              >
                Usar mi documento
              </button>
              <button
                type="button"
                onClick={() => setPasswordMode("propia")}
                className={`flex-1 px-3 py-2 rounded-lg transition-colors ${
                  passwordMode === "propia" ? "bg-emerald-500 text-neutral-950 font-bold" : "text-neutral-400"
                }`}
              >
                Elegir la mía
              </button>
            </div>

            {passwordMode === "documento" ? (
              <p className="text-neutral-500 text-xs">
                Tu contraseña será tu documento{documento ? `: ${documento}` : ""}. Podrás cambiarla después.
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
          onClick={handleSubmit}
          disabled={!canSubmit || loading}
          className="w-full bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 active:scale-[0.98] transition-all text-neutral-950 font-bold text-base py-3.5 rounded-2xl shadow-lg shadow-emerald-500/20"
        >
          {loading ? "Guardando..." : "Guardar y entrar a la fila"}
        </button>
      </div>
    </div>
  );
}
