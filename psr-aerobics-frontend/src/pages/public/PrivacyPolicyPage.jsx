import { useNavigate } from "react-router-dom";
import { ArrowLeft, ShieldCheck } from "lucide-react";

// Página pública de la Política de Tratamiento de Datos Personales — docs/pendiente-implementar-
// sistema.md §Frontend #1: hoy este documento vivía solo en docs/politica-tratamiento-datos.md,
// en ningún lugar que el sistema pudiera mostrar. El contenido de acá es una transcripción fiel de
// ese archivo; cualquier cambio a la política se hace ahí primero y se refleja aquí después.
//
// Pública a propósito (fuera de ProtectedRoute, como /register o /confirmar-correo): quien todavía
// no se ha registrado necesita poder leerla antes de aceptar el checkbox de registro.
//
// [FECHA] / [FECHA DE PUBLICACIÓN] se dejan literales — docs/pendiente-implementar-sistema.md
// (sección "Ya resuelto"): la fecha se fija el día real del despliegue, no antes.
export default function PrivacyPolicyPage() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen w-full bg-neutral-950 text-white font-body">
      <div className="max-w-3xl mx-auto px-4 sm:px-6 py-10">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-neutral-400 hover:text-white text-xs bg-neutral-900 border border-neutral-800 px-3 py-2 rounded-xl transition-colors mb-8"
        >
          <ArrowLeft size={14} /> Volver
        </button>

        <div className="flex items-center gap-3 mb-2">
          <ShieldCheck className="text-emerald-400 shrink-0" size={28} />
          <h1 className="text-2xl sm:text-3xl font-bold font-display text-white">
            Política de Tratamiento de Datos Personales
          </h1>
        </div>
        <p className="text-neutral-500 text-sm mb-10">
          Versión: 1.0 · Última actualización: [FECHA] · Marco normativo: Ley 1581 de 2012 y Decreto
          1377 de 2013 (República de Colombia).
        </p>

        <div className="space-y-10 text-neutral-300 text-sm leading-relaxed">
          <Section n="1" title="Responsable del tratamiento">
            <table className="w-full text-left border border-neutral-800 rounded-xl overflow-hidden mb-4">
              <tbody className="divide-y divide-neutral-800">
                <Row label="Razón social" value="Pioneros de la Salsa y Ritmos" />
                <Row label="NIT" value="9017205958" />
                <Row
                  label="Domicilio"
                  value="Calle 20 # 25-39, segundo piso, Floristería Jaiber, Centro, Sincelejo, Sucre"
                />
              </tbody>
            </table>
            <p>
              El responsable del tratamiento de los datos personales descritos en este documento es
              Pioneros de la Salsa y Ritmos (en adelante, «PSR»). Esta política regula el
              tratamiento de los datos personales recopilados a través del sistema de control de
              ingreso utilizado para el programa de Aeróbicos, uno de los programas ofrecidos por
              PSR.
            </p>
            <p className="mt-3">
              <strong className="text-white">Canal de contacto</strong> (para consultas sobre esta
              política y para el ejercicio de los derechos descritos en el numeral 10):{" "}
              <a href="mailto:escuelapsr@gmail.com" className="text-emerald-400 hover:text-emerald-300 underline">
                escuelapsr@gmail.com
              </a>
            </p>
          </Section>

          <Section n="2" title="Datos personales que se recopilan">
            <Sub title="2.1 Datos de identificación">
              Tipo y número de documento de identidad, nombres, apellidos, teléfono y fecha de
              nacimiento.
            </Sub>
            <Sub title="2.2 Datos de la cuenta de acceso">
              Correo electrónico (opcional) y credenciales de acceso. Las contraseñas se almacenan
              cifradas mediante un algoritmo de hash; PSR nunca almacena ni puede consultar la
              contraseña de un usuario en texto plano.
            </Sub>
            <Sub title="2.3 Datos de asistencia y uso del servicio">
              Fecha y hora de ingreso a cada clase, método utilizado para el ingreso, la clase
              específica a la que se asistió y los montos cobrados por cada sesión. Estos datos, en
              conjunto, constituyen un historial de hábitos y frecuencia de asistencia de la persona
              titular.
            </Sub>
            <Sub title="2.4 Datos financieros">
              Montos abonados y reembolsados, método de pago utilizado (identificado únicamente por
              su nombre, por ejemplo «efectivo» o «transferencia», sin que PSR almacene números de
              tarjeta, cuentas bancarias ni ningún instrumento de pago), y fecha de cada movimiento.
            </Sub>
            <Sub title="2.5 Datos sensibles de salud">
              De manera opcional y con consentimiento diferenciado, la persona puede suministrar
              observaciones de salud relevantes para la práctica segura de la actividad física. El
              tratamiento de este dato sensible se rige por las condiciones especiales descritas en
              el numeral 4.
            </Sub>
            <Sub title="2.6 Datos de contacto de emergencia">
              La persona registrada puede suministrar el nombre y teléfono de hasta cinco personas de
              contacto para ser localizadas en caso de emergencia durante la actividad. Estos son
              datos personales de un tercero; su tratamiento se rige por lo descrito en el numeral 5.
            </Sub>
            <Sub title="2.7 Registros de auditoría">
              Con fines de seguridad y control interno, el sistema conserva un registro de
              modificaciones sobre información relevante, incluyendo qué cambió, el valor anterior,
              el valor nuevo, quién lo hizo y cuándo.
            </Sub>
            <Sub title="2.8 Datos que PSR no recopila">
              PSR no recopila datos biométricos, números de tarjetas o cuentas bancarias, datos de
              geolocalización ni fotografías, y no utiliza cookies de rastreo ni herramientas de
              analítica de terceros.
            </Sub>
          </Section>

          <Section n="3" title="Finalidades del tratamiento">
            <p className="mb-3">Los datos personales recopilados se utilizan exclusivamente para:</p>
            <ol className="list-decimal list-inside space-y-1.5 mb-3">
              <li>Registrar e identificar a las personas usuarias del programa.</li>
              <li>Controlar el acceso y la asistencia a las clases.</li>
              <li>Gestionar los pagos, saldos y devoluciones asociados al servicio.</li>
              <li>
                Comunicar asuntos relacionados con la cuenta, tales como la bienvenida al sistema, la
                recuperación de contraseña y la confirmación de cambios.
              </li>
              <li>
                Contactar a la persona designada como contacto de emergencia, exclusivamente cuando
                sea necesario por una emergencia relacionada con la actividad.
              </li>
              <li>
                Conocer condiciones de salud relevantes para la práctica segura del ejercicio, cuando
                la persona decida suministrarlas.
              </li>
              <li>
                Mantener trazabilidad de las operaciones realizadas sobre la información, con fines
                de seguridad y control interno.
              </li>
              <li>Cumplir con obligaciones legales, contables y tributarias derivadas del registro de pagos.</li>
            </ol>
            <p>
              PSR no utiliza los datos personales con fines de mercadeo, publicidad ni perfilamiento
              comercial, y no los cede ni vende a terceros con fines publicitarios.
            </p>
          </Section>

          <Section n="4" title="Autorización general y datos sensibles de salud">
            <Sub title="4.1 Autorización general">
              Al registrarse en el sistema, la persona titular autoriza el tratamiento de sus datos
              personales de identificación, cuenta, asistencia e información financiera para las
              finalidades descritas en el numeral 3.
            </Sub>
            <Sub title="4.2 Datos de salud: consentimiento diferenciado y expreso">
              De conformidad con el artículo 5 de la Ley 1581 de 2012, los datos relativos a la salud
              son datos sensibles. Su suministro es opcional y no condiciona el acceso al servicio.
              Se solicitan mediante un mecanismo de consentimiento separado y expreso, distinto de la
              autorización general de registro, mediante el siguiente texto:
              <Quote>
                ¿Desea proporcionar información de salud relevante para la práctica segura de la
                actividad?
                <br />
                ☐ Sí, autorizo expresamente su tratamiento. Entiendo que proporcionar esta
                información es opcional.
              </Quote>
              Si la persona no marca la casilla, el registro continúa con normalidad.
            </Sub>
          </Section>

          <Section n="5" title="Datos de terceros: contacto de emergencia">
            <p>
              El nombre y teléfono del contacto de emergencia son datos personales de una persona
              distinta de quien se registra en el sistema. Al momento de suministrar estos datos, se
              presenta el siguiente texto:
            </p>
            <Quote>
              Si proporciona datos de una persona como contacto de emergencia, usted declara que está
              autorizado para suministrarlos para esta finalidad, o que dispone de una base legítima
              para hacerlo.
            </Quote>
            <p>
              La finalidad de este tratamiento es exclusivamente contactar a la persona designada
              cuando sea necesario por una emergencia relacionada con la actividad. La persona
              designada como contacto de emergencia puede solicitar el acceso, la actualización o la
              supresión de sus datos a través del canal indicado en el numeral 10.
            </p>
          </Section>

          <Section n="6" title="Acceso interno a los datos">
            <p className="mb-3">
              Dentro de PSR, el acceso a los datos personales está restringido según el rol de quien
              opera el sistema:
            </p>
            <table className="w-full text-left border border-neutral-800 rounded-xl overflow-hidden">
              <tbody className="divide-y divide-neutral-800">
                <Row label="Administración" value="Acceso completo: identificación, salud, contactos de emergencia, información financiera, auditoría y gestión de cuentas." />
                <Row label="Recepción" value="Personas, contactos de emergencia, observaciones de salud, check-in e información financiera." />
                <Row label="Instructores" value="Datos de identificación de las personas y su check-in. Sin acceso a información financiera." />
                <Row label="La propia persona titular" value="Únicamente su propia información: perfil, saldo, historial e información general de tarifas." />
                <Row label="Cuenta técnica de ingreso" value="Búsqueda por documento y registro de llegada, sin acceso a otra información." />
              </tbody>
            </table>
          </Section>

          <Section n="7" title="Transferencia y transmisión internacional de datos">
            <p>
              PSR podrá utilizar proveedores tecnológicos para el almacenamiento de la información y
              el envío de comunicaciones, algunos de los cuales pueden estar ubicados fuera de
              Colombia. En caso de que dichos proveedores impliquen transferencia o transmisión
              internacional de datos personales, PSR informará las categorías de datos involucradas,
              el país de destino y actuará conforme a lo dispuesto en la Ley 1581 de 2012.{" "}
              <span className="text-amber-400">
                [PENDIENTE: completar esta sección una vez definidos los proveedores tecnológicos.]
              </span>
            </p>
          </Section>

          <Section n="8" title="Conservación y eliminación de datos">
            <Sub title="8.1 Plazos de conservación">
              Los datos personales tratados en virtud de esta política se conservan por diez (10)
              años. Para los movimientos financieros, este plazo corresponde a la obligación
              establecida en el artículo 60 del Código de Comercio y el artículo 28 de la Ley 962 de
              2005.
            </Sub>
            <Sub title="8.2 Mecanismos de baja">
              <p className="mb-2">PSR cuenta con dos mecanismos para dar de baja a una persona:</p>
              <ul className="list-disc list-inside space-y-1.5">
                <li>
                  <strong className="text-white">Desactivación:</strong> mecanismo reversible. Los
                  datos de la persona se conservan, pero la cuenta deja de operar en el sistema.
                </li>
                <li>
                  <strong className="text-white">Eliminación definitiva:</strong> mecanismo
                  irreversible, disponible únicamente para administración y sujeto a confirmación
                  explícita. Al eliminar definitivamente a una persona, sus datos de identificación
                  se eliminan de forma permanente. Los movimientos financieros y de asistencia que
                  haya generado se conservan sin ninguna referencia a su identidad, de forma que el
                  registro económico permanece pero la identidad no.
                </li>
              </ul>
            </Sub>
          </Section>

          <Section n="9" title="Medidas de seguridad">
            <p className="mb-3">
              PSR implementa las siguientes medidas técnicas para proteger los datos personales bajo
              su tratamiento:
            </p>
            <ul className="list-disc list-inside space-y-1.5">
              <li>Almacenamiento de contraseñas mediante cifrado unidireccional, sin conservarlas en texto plano.</li>
              <li>Enlaces de recuperación de contraseña y de confirmación de cambio de correo almacenados de forma cifrada, de un solo uso y con expiración en plazos breves.</li>
              <li>Autenticación mediante tokens de sesión con expiración.</li>
              <li>Control de acceso a la información según el rol de cada usuario del sistema, restringido por defecto.</li>
              <li>Control de propiedad, de forma que cada persona con cuenta accede únicamente a su propia información.</li>
              <li>Conexión cifrada con la base de datos.</li>
              <li>Restricción de los orígenes autorizados para consumir el sistema.</li>
              <li>Registro de auditoría sobre los cambios realizados en información crítica.</li>
            </ul>
          </Section>

          <Section n="10" title="Derechos de los titulares">
            <p className="mb-3">
              De conformidad con la Ley 1581 de 2012, toda persona titular de datos personales tiene
              derecho a:
            </p>
            <ul className="list-disc list-inside space-y-1.5 mb-3">
              <li>Conocer los datos personales que PSR tiene sobre ella.</li>
              <li>Actualizar y rectificar sus datos cuando sean inexactos o estén desactualizados.</li>
              <li>Solicitar la supresión de sus datos, cuando no exista un deber legal o contractual que obligue a conservarlos.</li>
              <li>Revocar la autorización otorgada en cualquier momento.</li>
              <li>Presentar quejas ante la Superintendencia de Industria y Comercio por infracciones a la normativa de protección de datos personales.</li>
            </ul>
            <p>
              Estos derechos, así como cualquier consulta sobre esta política, pueden ejercerse a
              través del canal de contacto indicado en el numeral 1. PSR dará respuesta dentro de los
              términos establecidos por la Ley 1581 de 2012 y el Decreto 1377 de 2013.
            </p>
          </Section>

          <Section n="11" title="Menores de edad">
            <p>
              El servicio está dirigido exclusivamente a personas mayores de 18 años. El sistema no
              está diseñado para recibir ni tratar datos personales de menores de edad.
            </p>
          </Section>

          <Section n="12" title="Declaración de participación voluntaria">
            <p>
              Esta declaración es independiente del tratamiento de datos personales descrito en los
              numerales anteriores. Se presenta en el mismo momento del registro, junto con la
              autorización general, mediante el siguiente texto:
            </p>
            <Quote>
              Entiendo que la actividad física conlleva riesgo de lesión y participo de manera
              voluntaria.
            </Quote>
          </Section>

          <Section n="13" title="Vigencia">
            <p>Esta política rige a partir de [FECHA DE PUBLICACIÓN].</p>
          </Section>
        </div>
      </div>
    </div>
  );
}

// `id` sigue el mismo valor que `anchor` en constants/policyExcerpts.js — así los paneles de
// consentimiento y los checkboxes de registro pueden enlazar directo a la sección exacta
// (`/politica-privacidad#numeral-5`, etc.) en vez de mandar solo a la página completa.
function Section({ n, title, children }) {
  return (
    <section id={`numeral-${n}`} className="scroll-mt-6">
      <h2 className="text-lg font-bold text-white mb-3 flex items-baseline gap-2">
        <span className="text-emerald-400">{n}.</span> {title}
      </h2>
      <div className="space-y-3">{children}</div>
    </section>
  );
}

function Sub({ title, children }) {
  return (
    <div>
      <h3 className="text-white font-semibold text-sm mb-1.5">{title}</h3>
      <div className="space-y-2">{children}</div>
    </div>
  );
}

function Quote({ children }) {
  return (
    <blockquote className="border-l-2 border-emerald-500/40 bg-neutral-900/60 rounded-r-lg pl-4 pr-3 py-2.5 my-2 text-neutral-200 text-sm">
      {children}
    </blockquote>
  );
}

function Row({ label, value }) {
  return (
    <tr>
      <td className="px-4 py-2.5 text-neutral-500 text-xs font-semibold w-1/3 align-top">{label}</td>
      <td className="px-4 py-2.5 text-neutral-200">{value}</td>
    </tr>
  );
}
