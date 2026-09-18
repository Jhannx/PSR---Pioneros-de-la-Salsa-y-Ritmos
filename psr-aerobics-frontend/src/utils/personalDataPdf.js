// Descarga de datos personales en PDF (docs/correcciones-pendientes-18-09.md #2): reemplaza el
// volcado de JSON crudo que había antes en UserProfilePage.jsx. `jspdf` se importa de forma
// dinámica desde `buildPersonalDataPdf` — nadie más en la app lo necesita, así que no tiene por
// qué entrar en el bundle inicial, solo cuando alguien realmente pulsa "Descargar".
//
// El documento se arma con lo que ya autoriza la política de tratamiento de datos a entregarle a
// "la propia persona titular" (numeral 6) y el registro de qué autorizó y cuándo (numeral 10):
// perfil, contacto de emergencia, saldo y consentimientos — los mismos cuatro `fetch` que ya hacía
// la descarga en JSON, solo que acá se formatean como un documento legible en vez de volcarse tal
// cual.
import { formatCOP } from "./currency";
import { formatBirthDate } from "./date";

const PAGE_MARGIN = 42;
const PAGE_WIDTH = 595.28; // A4 en puntos
const PAGE_HEIGHT = 841.89;
const CONTENT_WIDTH = PAGE_WIDTH - PAGE_MARGIN * 2;
const FOOTER_TEXT =
  "Generado desde tu cuenta en PSR Aeróbicos, en ejercicio del derecho de acceso a tus datos " +
  "personales (Ley 1581 de 2012, artículo 8, literal a).";

// Fecha/hora completa, con año — a diferencia de `formatDateTimeShort` (utils/date.js), que omite
// el año a propósito para pantallas de auditoría donde siempre es "este año". Un documento que la
// persona puede guardar y volver a abrir meses después sí necesita el año.
function formatFullDateTime(isoString) {
  if (!isoString) return null;
  try {
    return new Date(isoString).toLocaleString("es-CO", {
      day: "2-digit",
      month: "long",
      year: "numeric",
      hour: "numeric",
      minute: "2-digit",
    });
  } catch {
    return null;
  }
}

// Documento que sí distingue null (dato inexistente en la cuenta) de una sección entera fallida
// (`available === false`, la llamada al backend se cayó) — el mensaje que se muestra es distinto
// a propósito: "No registrado" no es lo mismo que "No disponible".
function buildPdfState({ profile, contacts, balance, consents }) {
  return {
    profile: { available: profile.status === "fulfilled", value: profile.value ?? null },
    contacts: {
      available: contacts.status === "fulfilled",
      value: Array.isArray(contacts.value) ? contacts.value : [],
    },
    balance: { available: balance.status === "fulfilled", value: balance.value ?? null },
    consents: { available: consents.status === "fulfilled", value: consents.value ?? null },
  };
}

export async function downloadPersonalDataPdf({ user, settled, fileName }) {
  // Importación dinámica: solo se paga el costo de `jspdf` (~150KB) cuando alguien realmente
  // descarga, no en el bundle inicial de toda la app.
  const { jsPDF } = await import("jspdf");
  const doc = new jsPDF({ unit: "pt", format: "a4" });
  const state = buildPdfState(settled);

  let y = PAGE_MARGIN;

  // --- Utilidades de layout, cerradas sobre `doc`/`y` ------------------------------------------

  function newPage() {
    doc.addPage();
    y = PAGE_MARGIN;
  }

  // Asegura que quepan `height` puntos más antes de la siguiente sección; si no, abre página nueva.
  // Sin esto, un párrafo largo (ej. observaciones de salud) podía partirse justo en el borde de la
  // página, con la segunda mitad del texto invisible bajo el margen.
  function ensureSpace(height) {
    if (y + height > PAGE_HEIGHT - PAGE_MARGIN - 24) {
      newPage();
    }
  }

  function sectionTitle(text) {
    ensureSpace(28);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(12);
    doc.setTextColor(20, 20, 20);
    doc.text(text, PAGE_MARGIN, y);
    y += 6;
    doc.setDrawColor(200, 200, 200);
    doc.line(PAGE_MARGIN, y, PAGE_WIDTH - PAGE_MARGIN, y);
    y += 18;
  }

  // `label: value`, con el label en semi-negrita y el valor envuelto a `CONTENT_WIDTH` si hace
  // falta — las observaciones de salud pueden ser un párrafo largo, no una línea corta.
  //
  // Cada línea se dibuja por separado, comprobando espacio ANTES de cada una (`ensureSpace(13)`
  // dentro del loop) — no de una sola vez para todo el bloque. `ensureSpace` solo decide si abrir
  // página nueva ANTES de empezar a dibujar; con un bloque más alto que una página completa (un
  // valor extremadamente largo), esa comprobación única siempre abre página nueva pero después
  // igual intenta dibujar TODAS las líneas ahí, desbordándose por debajo del pie de página en vez
  // de repartirse. Con el límite real de observaciones de salud (2000 caracteres) esto no llegaba
  // a pasar, pero es un defecto real de la función, no un caso inventado — se corrige acá.
  function field(label, value) {
    const text = value === null || value === undefined || value === "" ? "No registrado" : String(value);
    doc.setFont("helvetica", "bold");
    doc.setFontSize(9.5);
    doc.setTextColor(90, 90, 90);
    const labelWidth = doc.getTextWidth(`${label}: `);
    doc.setFont("helvetica", "normal");
    const lines = doc.splitTextToSize(text, CONTENT_WIDTH - labelWidth);

    lines.forEach((line, index) => {
      ensureSpace(13);
      if (index === 0) {
        doc.setFont("helvetica", "bold");
        doc.setTextColor(90, 90, 90);
        doc.text(`${label}:`, PAGE_MARGIN, y);
      }
      doc.setFont("helvetica", "normal");
      doc.setTextColor(20, 20, 20);
      doc.text(line, PAGE_MARGIN + labelWidth, y);
      y += 13;
    });
    y += 4;
  }

  function note(text, { italic = true } = {}) {
    doc.setFont("helvetica", italic ? "italic" : "normal");
    doc.setFontSize(9.5);
    doc.setTextColor(120, 120, 120);
    const lines = doc.splitTextToSize(text, CONTENT_WIDTH);
    lines.forEach((line) => {
      ensureSpace(13);
      doc.text(line, PAGE_MARGIN, y);
      y += 13;
    });
    y += 8;
  }

  function unavailable(what) {
    note(`No fue posible obtener "${what}" en este momento — vuelve a intentar la descarga más tarde.`);
  }

  // --- Encabezado (solo primera página) ---------------------------------------------------------

  doc.setFont("helvetica", "bold");
  doc.setFontSize(18);
  doc.setTextColor(15, 15, 15);
  doc.text("PSR Aeróbicos", PAGE_MARGIN, y);
  y += 22;

  doc.setFont("helvetica", "normal");
  doc.setFontSize(12);
  doc.setTextColor(60, 60, 60);
  doc.text("Mis datos personales", PAGE_MARGIN, y);
  y += 16;

  doc.setFontSize(9);
  doc.setTextColor(120, 120, 120);
  doc.text(`Generado el ${formatFullDateTime(new Date().toISOString())}`, PAGE_MARGIN, y);
  y += 24;

  // --- Datos personales ----------------------------------------------------------------------

  sectionTitle("Datos personales");
  if (state.profile.available) {
    const p = state.profile.value;
    field("Nombre completo", p?.fullName || `${user.firstName || ""} ${user.lastName || ""}`.trim());
    field("Documento", p ? `${p.documentType || ""} ${p.documentNumber || ""}`.trim() : null);
    field("Teléfono", p?.phone);
    field("Fecha de nacimiento", p?.birthDate ? formatBirthDate(p.birthDate) : null);
    field("Observaciones de salud", p?.healthObservations || null);
    field("Estado de la cuenta", p?.active === false ? "Inactiva" : "Activa");
  } else {
    unavailable("perfil");
  }
  y += 8;

  // --- Contacto de emergencia ------------------------------------------------------------------

  sectionTitle("Contacto de emergencia");
  if (state.contacts.available) {
    if (state.contacts.value.length === 0) {
      note("No tienes contactos de emergencia registrados.", { italic: false });
    } else {
      state.contacts.value.forEach((contact, index) => {
        field(`Contacto ${index + 1} — Nombre`, contact.name);
        field(`Contacto ${index + 1} — Teléfono`, contact.phone);
        const updated = formatFullDateTime(contact.updatedAt);
        if (updated) field(`Contacto ${index + 1} — Actualizado`, updated);
        y += 4;
      });
    }
  } else {
    unavailable("contacto de emergencia");
  }
  y += 4;

  // --- Saldo -----------------------------------------------------------------------------------

  sectionTitle("Saldo");
  if (state.balance.available && state.balance.value) {
    const b = state.balance.value;
    const balanceAmount = Number(b.balance || 0);
    field("Total abonado", formatCOP(b.totalPaid));
    field("Total cobrado", formatCOP(b.totalCharged));
    field(
      "Saldo actual",
      balanceAmount < 0 ? `Debes ${formatCOP(Math.abs(balanceAmount))}` : `A favor ${formatCOP(balanceAmount)}`
    );
  } else if (state.balance.available) {
    note("No se encontró información de saldo para esta cuenta.", { italic: false });
  } else {
    unavailable("saldo");
  }
  y += 8;

  // --- Consentimientos ---------------------------------------------------------------------------
  //
  // Numeral 10 de la política: qué autorizó la persona y cuándo. El shape exacto de
  // GET /api/personas/{id}/consentimientos no está confirmado del todo del lado del backend
  // (pendientes-back.md, pregunta 1-bis sobre trazabilidad de terceros) — se renderiza de forma
  // defensiva: lista si es un array (una fila por evento de consentimiento, con lo que traiga cada
  // una), pares clave/valor si es un objeto plano, y nunca se rompe la descarga si la forma es
  // distinta a la esperada.

  sectionTitle("Consentimientos");
  if (state.consents.available && state.consents.value) {
    const c = state.consents.value;
    const hadMissingFlag = Boolean(c.registrationConsentMissing);
    if (hadMissingFlag) {
      note(
        "Esta cuenta se registró antes de que existiera este mecanismo de consentimiento — no hay " +
          "un registro digital de esa aceptación inicial.",
        { italic: false }
      );
    }
    // Independiente del flag de arriba: una cuenta puede no tener el consentimiento de registro
    // (cuentas viejas) y aun así tener otros consentimientos posteriores sí registrados (salud,
    // terceros) — antes esta rama se saltaba entera si `registrationConsentMissing` era `true`,
    // aunque hubiera más datos que sí valía la pena mostrar.
    const entries = Array.isArray(c) ? c : Array.isArray(c.items) ? c.items : Array.isArray(c.consents) ? c.consents : null;
    if (entries && entries.length > 0) {
      entries.forEach((entry, index) => {
        const type = entry.type || entry.consentType || entry.name || `Consentimiento ${index + 1}`;
        const granted = entry.granted ?? entry.accepted ?? entry.value;
        const when = formatFullDateTime(entry.grantedAt || entry.date || entry.timestamp || entry.createdAt);
        const status = granted === false ? "No otorgado" : "Otorgado";
        field(type, when ? `${status} — ${when}` : status);
      });
    } else if (entries && entries.length === 0) {
      if (!hadMissingFlag) note("No hay consentimientos registrados.", { italic: false });
    } else {
      // Objeto plano sin envoltura de array reconocible: se listan sus claves tal cual, mejor que
      // no mostrar nada.
      const keys = Object.keys(c).filter((k) => k !== "registrationConsentMissing");
      if (keys.length > 0) {
        keys.forEach((key) => field(key, typeof c[key] === "boolean" ? (c[key] ? "Sí" : "No") : String(c[key])));
      } else if (!hadMissingFlag) {
        note("Sin información adicional de consentimientos.", { italic: false });
      }
    }
  } else {
    unavailable("consentimientos");
  }

  // --- Pie de página, en todas las páginas generadas --------------------------------------------

  const totalPages = doc.internal.getNumberOfPages();
  for (let page = 1; page <= totalPages; page++) {
    doc.setPage(page);
    doc.setFont("helvetica", "normal");
    doc.setFontSize(7.5);
    doc.setTextColor(150, 150, 150);
    const footerY = PAGE_HEIGHT - PAGE_MARGIN + 14;
    const footerLines = doc.splitTextToSize(FOOTER_TEXT, CONTENT_WIDTH - 60);
    doc.text(footerLines, PAGE_MARGIN, footerY);
    doc.text(`${page} / ${totalPages}`, PAGE_WIDTH - PAGE_MARGIN, footerY, { align: "right" });
  }

  doc.save(fileName);
}
