package com.jhanp.psraerobicsbackend.security;

/**
 * Las expresiones de {@code @PreAuthorize}, con nombre.
 * <p>
 * Escritas una vez aquí en vez de repetir el SpEL en cada método: si mañana recepción deja de poder
 * desactivar personas, se cambia {@link #DESK} y no hay que ir a buscar cadenas sueltas por todo el
 * proyecto. Además el nombre dice el porqué, que la expresión sola no dice.
 * <p>
 * <b>hasAuthority y no hasRole.</b> Las autoridades son las constantes de
 * {@link com.jhanp.psraerobicsbackend.modules.role.enums.RoleName} sin prefijo {@code ROLE_};
 * {@code hasRole('ADMIN')} buscaría {@code ROLE_ADMIN} y nunca encontraría nada.
 * <p>
 * <b>Sobre el multirol.</b> Los roles no son modos entre los que se alterna: el token lleva TODAS las
 * autoridades del usuario a la vez, porque {@code CustomUserDetails.getAuthorities()} mapea todas sus
 * filas de usuario_rol. Alguien que sea ADMIN y PROFESOR pasa por las dos puertas en la misma sesión,
 * sin cambiar nada ni volver a entrar.
 */
public final class Authorities {

    private Authorities() {
    }

    /** Administración pura: dinero, configuración, roles y auditoría. */
    public static final String ADMIN = "hasAuthority('ADMIN')";

    /** Estación autónoma de entrada; no implica permisos de mostrador ni administración. */
    public static final String PSR_TERMINAL = "hasAuthority('PSR_TERMINAL')";

    /**
     * Todo el personal. Es el permiso de LECTURA del día a día: horarios, sesiones, cola y personas.
     * Amplio a propósito — en la práctica el gimnasio lo llevan una o dos personas, y que alguien no
     * pueda ni mirar el horario porque su rol no lo contempla solo estorba.
     */
    public static final String STAFF = "hasAnyAuthority('ADMIN', 'TEACHER', 'RECEPTIONIST')";

    /**
     * El mostrador: registrar personas, cobrar y ver lo que se cobró. Deja fuera al profesor, que no
     * tiene por qué tocar dinero.
     */
    public static final String DESK = "hasAnyAuthority('ADMIN', 'RECEPTIONIST')";

    /**
     * Operar la clase: abrir y cerrar sesiones, hacer check-in, confirmar asistencia.
     * <p>
     * Los tres roles, y es deliberado: si crear la sesión fuera solo de ADMIN y arrancarla solo de
     * PROFESOR, un flujo tan corriente como "abro la sala y empiezo" exigiría dos cuentas.
     */
    public static final String CLASS_OPS = "hasAnyAuthority('ADMIN', 'TEACHER', 'RECEPTIONIST')";

    /**
     * El miembro. Solo para endpoints que actúan sobre sí mismo y no reciben a quién afectan: el
     * sujeto sale del token. Para los que sí llevan un id en la ruta está {@link #OR_SELF}.
     */
    public static final String MEMBER = "hasAuthority('MEMBER')";

    /** Consulta puntual de una persona por documento desde la estación de entrada. */
    public static final String PERSON_LOOKUP = "hasAnyAuthority('ADMIN', 'TEACHER', 'RECEPTIONIST', 'PSR_TERMINAL')";

    /** Operación de fila permitida a la terminal y al personal que ya la operaba. */
    public static final String TERMINAL_QUEUE = "hasAnyAuthority('ADMIN', 'TEACHER', 'RECEPTIONIST', 'PSR_TERMINAL')";

    /**
     * Sufijo que abre un endpoint por persona a esa misma persona, además de a quien lo tuviera por
     * rol. Se concatena, no reemplaza:
     * <pre>
     *   &#64;PreAuthorize(Authorities.STAFF + Authorities.OR_SELF)
     * </pre>
     * El personal sigue viendo a todo el mundo; el miembro entra solo por la segunda condición.
     * <p>
     * Usa {@code #id} como nombre de la variable de ruta. Para los endpoints que la llaman
     * {@code personId} está {@link #OR_SELF_PERSON_ID}: SpEL resuelve el nombre real del parámetro del
     * método, así que no hay una sola expresión que valga para los dos.
     */
    public static final String OR_SELF = " or @ownership.isSelf(authentication, #id)";

    /** Igual que {@link #OR_SELF}, para los métodos cuyo parámetro se llama {@code personId}. */
    public static final String OR_SELF_PERSON_ID = " or @ownership.isSelf(authentication, #personId)";

    /**
     * Para los endpoints que llevan en la ruta el id de un CONTACTO de emergencia y no el de la
     * persona.
     * <p>
     * No sirve {@link #OR_SELF} aquí: compararía el id de persona de quien llama contra un id de
     * contacto —dos tablas distintas cuyos identificadores pueden coincidir por casualidad—, y un
     * miembro podría editar el contacto de otro. Este resuelve antes de qué persona es el contacto.
     */
    public static final String OR_OWNS_CONTACT = " or @ownership.ownsContact(authentication, #contactId)";
}
