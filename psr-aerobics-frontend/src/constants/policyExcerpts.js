// Citas literales de la Política de Tratamiento de Datos Personales — fuente única, para que el
// texto legal nunca viva escrito dos veces. Lo usan `PrivacyPolicyPage.jsx` (la página completa),
// `RegistrationConsent.jsx` (registro) y `MissingConsentPanel.jsx` (el panel que aparece cuando
// falta un consentimiento puntual, ej. al agregar un contacto o guardar salud sin haberlo dado
// antes). Cambiar el texto de la política se hace acá una sola vez.
//
// `anchor` apunta a la sección correspondiente de `/politica-privacidad#<anchor>` — los
// `id` de cada <Section> en PrivacyPolicyPage.jsx usan el mismo valor.
export const POLICY_EXCERPTS = {
  dataProcessing: {
    numeral: "4.1",
    anchor: "numeral-4",
    heading: "Autorización general",
    quote:
      "Al registrarse en el sistema, la persona titular autoriza el tratamiento de sus datos personales de identificación, cuenta, asistencia e información financiera para las finalidades descritas en el numeral 3.",
    checkboxLabel: "He leído y acepto la Política de Tratamiento de Datos Personales de PSR.",
  },
  voluntaryParticipation: {
    numeral: "12",
    anchor: "numeral-12",
    heading: "Declaración de participación voluntaria",
    quote: "Entiendo que la actividad física conlleva riesgo de lesión y participo de manera voluntaria.",
    checkboxLabel: "Entiendo que la actividad física conlleva riesgo de lesión y participo de manera voluntaria.",
  },
  health: {
    numeral: "4.2",
    anchor: "numeral-4",
    heading: "Datos de salud: consentimiento diferenciado y expreso",
    quote:
      "¿Desea proporcionar información de salud relevante para la práctica segura de la actividad? Sí, autorizo expresamente su tratamiento. Entiendo que proporcionar esta información es opcional.",
    checkboxLabel: "Sí, autorizo expresamente su tratamiento. Entiendo que proporcionar esta información es opcional.",
  },
  thirdParty: {
    numeral: "5",
    anchor: "numeral-5",
    heading: "Datos de terceros: contacto de emergencia",
    quote:
      "Si proporciona datos de una persona como contacto de emergencia, usted declara que está autorizado para suministrarlos para esta finalidad, o que dispone de una base legítima para hacerlo.",
    checkboxLabel:
      "Declaro que estoy autorizado para suministrar estos datos para esta finalidad, o que dispongo de una base legítima para hacerlo.",
  },
};
