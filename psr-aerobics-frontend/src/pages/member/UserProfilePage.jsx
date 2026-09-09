import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { ROLE_LABELS, ROLE_BADGE_STYLES } from "../../context/mockDataConstants";
import { peopleService } from "../../services/peopleService";
import { authService } from "../../services/authService";
import { User, Phone, Mail, Shield, CheckCircle2, Lock, ChevronRight } from "lucide-react";

const inputClass =
  "w-full bg-neutral-900 border border-neutral-800 rounded-xl px-4 py-2.5 text-white placeholder-neutral-600 outline-none focus:border-emerald-500 transition-colors text-sm";

export default function UserProfilePage() {
  const { user, updateUserProfile } = useAuth();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    documentType: "CC",
    documentNumber: "",
    firstName: "",
    lastName: "",
    phone: "",
  });
  const [loading, setLoading] = useState(!!user?.personId);
  const [loadError, setLoadError] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState("");
  const [successMessage, setSuccessMessage] = useState(null);

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [changingPassword, setChangingPassword] = useState(false);

  useEffect(() => {
    if (!user?.personId) return;
    let cancelled = false;
    async function loadPerson() {
      try {
        const person = await peopleService.getById(user.personId);
        if (cancelled) return;
        setFormData({
          documentType: person.documentType || "CC",
          documentNumber: person.documentNumber || "",
          firstName: person.firstName || "",
          lastName: person.lastName || "",
          phone: person.phone || "",
        });
      } catch (err) {
        if (!cancelled) setLoadError(err.data?.message || err.message || "No se pudo cargar tu información.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    loadPerson();
    return () => {
      cancelled = true;
    };
  }, [user?.personId]);

  function handleChange(field, value) {
    setFormData((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSaveProfile(e) {
    e.preventDefault();
    if (!user?.personId) return;
    setSaving(true);
    setSaveError("");
    try {
      // PersonUpdateRequest no incluye email: el correo no se puede cambiar desde aquí.
      await peopleService.update(user.personId, {
        documentType: formData.documentType,
        documentNumber: formData.documentNumber,
        firstName: formData.firstName,
        lastName: formData.lastName,
        phone: formData.phone,
      });
      updateUserProfile({ fullName: `${formData.firstName} ${formData.lastName}`.trim() });
      setSuccessMessage("Tu información de perfil ha sido actualizada correctamente.");
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err) {
      setSaveError(err.data?.message || err.message || "No se pudo guardar el perfil.");
    } finally {
      setSaving(false);
    }
  }

  async function handleChangePassword(e) {
    e.preventDefault();
    if (!currentPassword || newPassword.length < 8) return;
    setChangingPassword(true);
    setPasswordError("");
    try {
      await authService.changePassword(currentPassword, newPassword);
      setCurrentPassword("");
      setNewPassword("");
      setSuccessMessage("Contraseña modificada con éxito.");
      setTimeout(() => setSuccessMessage(null), 4000);
    } catch (err) {
      setPasswordError(err.data?.message || err.message || "No se pudo cambiar la contraseña.");
    } finally {
      setChangingPassword(false);
    }
  }

  return (
    <div className="min-h-screen w-full bg-neutral-950 px-6 py-8 font-body text-white">
      <div className="max-w-4xl mx-auto space-y-6">
        {/* Encabezado de página */}
        <div className="border-b border-neutral-800 pb-4">
          <h1 className="text-2xl font-bold font-display text-white">Mi Perfil</h1>
          <p className="text-neutral-400 text-sm mt-1">
            Gestiona tus datos personales, información de contacto y preferencias de cuenta
          </p>
        </div>

        {successMessage && (
          <div className="bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 p-4 rounded-2xl text-sm flex items-center gap-2">
            <CheckCircle2 size={18} />
            <span>{successMessage}</span>
          </div>
        )}

        {loadError && (
          <div className="bg-rose-500/10 border border-rose-500/20 text-rose-400 p-4 rounded-2xl text-sm">
            {loadError}
          </div>
        )}

        {/* Tarjeta resumen de usuario con badges en pastillas redondeadas */}
        <div className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-4">
            <div className="w-16 h-16 rounded-full bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 flex items-center justify-center font-bold text-xl">
              {(formData.firstName?.[0] || "") + (formData.lastName?.[0] || "") || "US"}
            </div>
            <div>
              <h2 className="text-xl font-bold text-white">{`${formData.firstName} ${formData.lastName}`.trim() || "—"}</h2>
              <p className="text-xs text-neutral-400 mt-0.5 font-mono">
                Documento: {formData.documentType} {formData.documentNumber}
              </p>
            </div>
          </div>

          <div className="flex flex-wrap gap-2 justify-start sm:justify-end">
            {user?.roles?.map((role) => (
              <span
                key={role}
                className={`text-xs font-semibold px-3 py-1 rounded-full border ${
                  ROLE_BADGE_STYLES[role] || "bg-neutral-800 text-neutral-300 border-neutral-700"
                }`}
              >
                {ROLE_LABELS[role] || role}
              </span>
            ))}
          </div>
        </div>

        {/* Formulario de información personal */}
        <form onSubmit={handleSaveProfile} className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 space-y-6">
          <h3 className="text-base font-bold text-white flex items-center gap-2 border-b border-neutral-800 pb-3">
            <User size={18} className="text-emerald-400" />
            <span>Información Personal</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-neutral-400 font-semibold mb-1 block">Nombres</label>
              <input
                type="text"
                value={formData.firstName}
                onChange={(e) => handleChange("firstName", e.target.value)}
                className={inputClass}
                disabled={loading}
                required
              />
            </div>
            <div>
              <label className="text-xs text-neutral-400 font-semibold mb-1 block">Apellidos</label>
              <input
                type="text"
                value={formData.lastName}
                onChange={(e) => handleChange("lastName", e.target.value)}
                className={inputClass}
                disabled={loading}
                required
              />
            </div>
            <div>
              <label className="text-xs text-neutral-400 font-semibold mb-1 block">Teléfono</label>
              <div className="relative">
                <input
                  type="text"
                  value={formData.phone}
                  onChange={(e) => handleChange("phone", e.target.value)}
                  className={`${inputClass} pl-10`}
                  placeholder="3001234567"
                  disabled={loading}
                />
                <Phone size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-500" />
              </div>
            </div>
            <div>
              <label className="text-xs text-neutral-400 font-semibold mb-1 block">Correo electrónico</label>
              <div className="relative">
                <input
                  type="email"
                  value={user?.email || ""}
                  disabled
                  className={`${inputClass} pl-10 opacity-60 cursor-not-allowed`}
                  placeholder="Sin correo registrado"
                />
                <Mail size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-500" />
              </div>
              <p className="text-neutral-600 text-[11px] mt-1">
                El correo no se puede cambiar desde aquí. Contacta a recepción si necesitas actualizarlo.
              </p>
            </div>
          </div>

          {saveError && (
            <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl">
              {saveError}
            </div>
          )}

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={saving || loading}
              className="bg-emerald-500 disabled:opacity-40 hover:bg-emerald-400 text-neutral-950 font-bold text-sm px-6 py-2.5 rounded-xl transition-colors shadow-lg shadow-emerald-500/20"
            >
              {saving ? "Guardando..." : "Guardar Cambios"}
            </button>
          </div>
        </form>

        {/* Acceso a contacto de emergencia (pantalla dedicada, con CRUD real) */}
        <button
          type="button"
          onClick={() => navigate("/member/emergency-contact")}
          className="w-full bg-neutral-900 border border-neutral-800 hover:border-emerald-500/40 rounded-3xl p-6 flex items-center justify-between transition-colors text-left"
        >
          <div className="flex items-center gap-3">
            <Shield size={18} className="text-emerald-400" />
            <div>
              <p className="text-white font-bold text-sm">Contacto de Emergencia</p>
              <p className="text-neutral-400 text-xs mt-0.5">Ver y editar tu contacto de emergencia</p>
            </div>
          </div>
          <ChevronRight size={18} className="text-neutral-500" />
        </button>

        {/* Cambiar contraseña */}
        <form onSubmit={handleChangePassword} className="bg-neutral-900 border border-neutral-800 rounded-3xl p-6 space-y-4">
          <h3 className="text-base font-bold text-white flex items-center gap-2 border-b border-neutral-800 pb-3">
            <Lock size={18} className="text-emerald-400" />
            <span>Seguridad de la Cuenta</span>
          </h3>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div>
              <label className="text-xs text-neutral-400 font-semibold mb-1 block">Contraseña Actual</label>
              <input
                type="password"
                value={currentPassword}
                onChange={(e) => setCurrentPassword(e.target.value)}
                className={inputClass}
                placeholder="••••••••"
              />
            </div>
            <div>
              <label className="text-xs text-neutral-400 font-semibold mb-1 block">Nueva Contraseña</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className={inputClass}
                placeholder="Mínimo 8 caracteres"
              />
              {newPassword && newPassword.length < 8 && (
                <p className="text-amber-400 text-xs mt-1">La contraseña debe tener al menos 8 caracteres.</p>
              )}
            </div>
          </div>

          {passwordError && (
            <div className="p-3 bg-rose-500/10 border border-rose-500/20 text-rose-400 text-xs rounded-xl">
              {passwordError}
            </div>
          )}

          <div className="flex justify-end pt-2">
            <button
              type="submit"
              disabled={!currentPassword || newPassword.length < 8 || changingPassword}
              className="bg-neutral-800 disabled:opacity-40 hover:bg-neutral-700 text-white font-semibold text-sm px-5 py-2.5 rounded-xl transition-colors"
            >
              {changingPassword ? "Cambiando..." : "Cambiar Contraseña"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
