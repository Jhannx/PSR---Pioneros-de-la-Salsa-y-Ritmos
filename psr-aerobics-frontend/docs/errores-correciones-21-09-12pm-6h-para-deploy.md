# Todo ok hasta el momento a excepcion del checkin

Reception/checkin tanto como admin como profesor, intuyo que tambien como recepcionista al hacer checkin de alguien back no reporta problemas, puedo seguir la cola real
El problema está en que no está funcionando dinamicamente, al yo como otro usuario hacer registro de asistencia la lista de checkin no se actualiza, como si nadie se hubiera registrado, luego al yo agregar alguien manualmente si se agrega con el orden natural inicial y alli si "carga" las anteriores hechas por los usuarios, despues de esto, aun si entro con mi checkin de asistencia automatico sigue sin actualizarse, para q se actualice necesito ingresar a alguien manualmente
Cuando llega la finalizacion de una clase, como admin sigo viendo "finalizar sesion", veo ese boton, pero si no existe no deberia aparecer, este boton no lo ve ni profesores (deberia de verlos, para v2 solo lo ve el profesor que está dando la clase) y recepcionista, entonces deben de verlos ellos y ademas que realmente se sincronice con el tiempo real de la clase
Ademas, el checkin debe de mostrar la hora que se acaba la clase en automatico para tener consciencia sobre ello



Como admin checkin sigue mostrando que el recaudo es cuanto ingreso al sistema, lo cual debe de ser confuso, puesto que si bien ingreso eso al sistema como cobro/abono, es confuso si saber que en la sesion se recaudo x, y que en en el dia ingresó x cantidad, se debe de aclarar esto de alguna forma, para todos los roles que tengan reception checkin



### (Mejora v2: En reportes sucede q la paginacion se carga mal, digamos, hay 5 usuarios con credito a favor, al colocar el filtro se cargó en la pagina 2, sin embargo en la pag 1 no hay ningun usuario)



### Caso: Soy valentina ochoa, tengo 5 dias por adelantado, el sistema lo sabe y me lo dice en home, marco asistencia y "Tu llegada de hoy está registrada!
Tu turno en la fila de hoy: #3
Tu sesión de hoy ya está paga — puedes pasar directo a la clase." donde el problema es llevarlo como "turno de fila", en este caso no debe de mencionarlo, porque es confuso, debe de ser solo el mensaje q puede pasar directo y ya.
Como admin/recepcionista/profesor, me toca confirmar la asistencia de valentina ochoa, sin embargo se le indicó que pase directo, por lo que, no debe de aparecer como "no atendido", sino como "al dia" directamente, ya que tiene saldo a favor, no tengo que estar haciendo mas nada



No deja ingresar "Monto ingresado = 0" para la v2 deberia de existir esta posibilidad para el caso donde alguien deba completamente una sesion



El count de pagos adelantados está mal por lo menos para miembro, ya que menciona 5 dias por adelantado, y si hay 2 sesiones en un dia sigo en teoria teniendo credito disponible, el count o texto es "Clases/Sesiones"



### Segundo caso: Soy ana perez, estoy al dia, sin saldo a favor, ingreso, asigno 1000 pesos de cobro, muestra "saldo a favor 1000", es decir, me salió la sesion gratis, no está haciendo el descuento correctamente, tendria que ser "debe 4k" (esto en el filter de sin atender), pero luego de colocar confirmar si aparecen en la lista de saldo pendiente, es mal? no está mal que salga en saldo pendiente, pero inicialmente una vez se cobre 1000 no puede ponerse a favor 1000, está mal? no, el problema está en que no es claro, el descuento se hace cuando se confirma, cosa que no está mal, porque puede dar primero x monto y luego y monto, pero para el recepcionista queda confuso y como mala ux, se debe de hacer algo al resepcto



### Luego de hacer un test con una sesion de 11:20am - 11:30am finalizó automaticamente, todo ok, luego probé con otra sesion de 11:40 - 11:50 y al colocar el finalizar sesion en el rececption checkin, me aparece "11:20 a. m. - 11:30 a. m." y no a la sesion en curso real



Sin más, no noto inconsistencias en ningun otro lugar
