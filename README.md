# Paso 3. Diagnóstico: identificación de antipatrones

## 1. Antipatrón principal: God Object 

La clase `GestorPedidos` presenta características del antipatrón **God Object**, porque concentra diferentes responsabilidades que deberían estar distribuidas en componentes especializados. Aunque tiene un único método público, `procesarPedido()`, este se encarga de controlar prácticamente todo el proceso de creación de pedidos. Además, los seis métodos privados auxiliares (`obtenerHistorialCliente`, `formatearFactura`, `calcularImpuestoRegional`, `reintentarNotificacion`, `purgarPedidosVencidos` y `construirCuerpoCorreo`) permanecen dentro de la misma clase y amplían su cantidad de responsabilidades. Esto hace que el archivo alcance 340 líneas y que cualquier modificación en diferentes áreas del sistema pueda afectar a `GestorPedidos`.

Se identifican seis conjuntos principales de responsabilidades:

- **Validación de stock (líneas 31-44):** comprueba que el pedido contenga productos y consulta directamente la base de datos para verificar las cantidades disponibles.
- **Validación de cliente y mora (líneas 46-65):** consulta el tipo de cliente, verifica sus deudas pendientes y aplica una excepción según el horario.
- **Cálculo de precio (líneas 67-96):** calcula el subtotal, determina los descuentos según el tipo de cliente y obtiene los valores de impuestos y total.
- **Persistencia (líneas 98-113):** registra el pedido, inserta sus detalles y actualiza las existencias del inventario mediante instrucciones SQL.
- **Notificación (líneas 115-129):** construye manualmente el asunto y el cuerpo del correo y ejecuta el envío mediante `EmailService`.
- **Registro y seguimiento (líneas 29, 33, 41, 50, 59, 62, 127 y 131):** registra eventos, rechazos, errores y confirmaciones mediante el sistema de logs.

Estas responsabilidades representan diferentes razones para modificar la clase. Por ejemplo, un cambio en las reglas de descuentos, una modificación en la estructura de la base de datos o un cambio en el formato del correo obligarían a intervenir el mismo componente. Esto demuestra que `GestorPedidos` concentra demasiadas funciones y no mantiene una separación adecuada de responsabilidades.

## 2. Antipatrón secundario: Spaghetti Code 

El método `procesarPedido()` también presenta características del antipatrón **Spaghetti Code**, porque combina diferentes operaciones y estructuras de control dentro de un mismo flujo de ejecución. Las consultas SQL se encuentran mezcladas con las reglas de negocio, las condiciones determinan diferentes caminos de ejecución y los resultados de las validaciones afectan directamente las operaciones posteriores. Esto hace que para comprender completamente el procesamiento de un pedido sea necesario seguir las distintas condiciones, retornos anticipados y operaciones que se ejecutan en cada escenario.

Se identifican las siguientes evidencias:

- **Condiciones de validación entrelazadas (líneas 31-65):** primero se comprueba el stock y posteriormente se valida el cliente y su situación de mora. Estas operaciones incluyen consultas SQL, estructuras condicionales y retornos anticipados que modifican el recorrido del método dependiendo de cada resultado.
- **Anidamiento de condiciones (líneas 46-65):** la validación de mora presenta tres niveles de anidamiento: primero se identifica al cliente moroso, después se comprueba si tiene una deuda pendiente y finalmente se determina si se encuentra antes o después del horario de corte de las 20:00.
- **Reglas de descuento mezcladas con cálculos (líneas 67-96):** el cálculo del subtotal, la consulta de pedidos anteriores y las condiciones para determinar los descuentos VIP y FRECUENTE se ejecutan dentro del mismo bloque de procesamiento.
- **Dependencia entre operaciones (líneas 98-129):** después de calcular los valores, el método registra el pedido, inserta sus detalles, actualiza el inventario y construye el correo. Además, el envío de la notificación utiliza un bloque `try-catch` que permite continuar aunque se produzca un error.
- **Manejo de diferentes resultados:** dependiendo de las condiciones encontradas, el método puede rechazar el pedido anticipadamente, permitir una excepción para un cliente moroso o continuar hasta confirmar y notificar el pedido.

Esta organización dificulta seguir el comportamiento del método porque sus diferentes caminos de ejecución están relacionados con consultas, reglas y operaciones de distintas responsabilidades. Por ejemplo, para modificar la validación de mora es necesario comprender sus condiciones y sus retornos; para cambiar los descuentos hay que revisar las consultas y los cálculos relacionados; y para modificar la confirmación del pedido se debe considerar la persistencia y la notificación posterior.

Por lo tanto, el código presenta un flujo de procesamiento poco modular, en el que las decisiones y operaciones se encuentran concentradas en un mismo procedimiento.

## 3. Mezcla de niveles de abstracción

Otra evidencia del problema de diseño es que `procesarPedido()` trabaja simultáneamente con diferentes niveles de abstracción:

- **Acceso a datos:** ejecuta consultas SQL directamente mediante `JdbcTemplate`, como las consultas de stock (líneas 37-39) y precios (líneas 70-72).
- **Reglas de negocio:** determina si un cliente puede realizar un pedido, si tiene suficiente stock y qué descuento le corresponde.
- **Cálculos:** realiza operaciones de subtotal, descuentos, impuestos y total (líneas 68-96).
- **Persistencia:** ejecuta inserciones y actualizaciones directamente sobre las tablas de la base de datos (líneas 98-113).
- **Presentación y comunicación:** construye el contenido del correo mediante `StringBuilder` y se comunica con el servicio de notificaciones (líneas 115-129).
- **Seguimiento:** registra información del proceso mediante instrucciones de logging distribuidas por todo el método.

Esta mezcla provoca un acoplamiento entre la lógica del negocio, la infraestructura de persistencia y la comunicación externa. Como consecuencia, modificar una de estas partes puede requerir comprender y revisar el flujo completo del método, aumentando la posibilidad de introducir errores en funcionalidades que no están directamente relacionadas con el cambio.

## 4. Dificultad para incorporar nuevos tipos de cliente

Si se necesitara agregar un nuevo tipo de cliente con reglas de descuento propias, sería necesario modificar el bloque de cálculo de descuentos existente, específicamente la estructura condicional comprendida entre las líneas 75 y 93.

Actualmente, las reglas están implementadas mediante condiciones explícitas para los tipos `VIP` y `FRECUENTE`. Por lo tanto, habría que incorporar una nueva condición `else if` y agregar los criterios correspondientes al nuevo tipo de cliente. Si sus reglas necesitaran información adicional, también sería necesario incorporar nuevas consultas o modificar las existentes.

Esto demuestra que la clase no está preparada para extender las reglas de descuento de manera independiente, ya que cada nuevo tipo de cliente obliga a modificar un método que también administra otras responsabilidades del pedido.

## Diagnóstico general

El código de `GestorPedidos` presenta principalmente dos antipatrones: **God Object y Spaghetti Code**. El primero se evidencia en la concentración de seis conjuntos de responsabilidades y seis métodos auxiliares dentro de una única clase de 340 líneas. El segundo se manifiesta en la mezcla de consultas SQL, validaciones, estructuras condicionales, cálculos, persistencia y notificaciones dentro de un mismo flujo de ejecución. Ambos problemas aumentan el acoplamiento, dificultan el mantenimiento y hacen que los cambios en una funcionalidad puedan afectar otras partes del procesamiento de pedidos.

**Conclusión del diagnóstico:** antes de refactorizar, se identifica la necesidad de distribuir las responsabilidades de validación, cálculo de precios, persistencia y notificación en componentes especializados, manteniendo en `GestorPedidos` únicamente la coordinación del proceso de creación del pedido. Esta será la base para justificar las decisiones de refactorización de los siguientes pasos.

---

# Decisiones de diseño

## Decisión de diseño: validación mediante Chain of Responsibility

Se decidió utilizar el patrón **Chain of Responsibility** para organizar la secuencia de validaciones del pedido, debido a que cada validador representa una responsabilidad independiente y puede decidir si permite continuar con el siguiente elemento de la cadena. Por ejemplo, `ValidadorStock` comprueba la disponibilidad de los productos y, si rechaza el pedido, detiene el procesamiento sin ejecutar `ValidadorCliente`. Esto permite mantener el orden de las validaciones y aplicar un mecanismo de corte anticipado.

Se consideró como alternativa utilizar una lista de métodos booleanos o una colección de `Predicate<ContextoPedido>`. Aunque esta alternativa también puede implementar una evaluación secuencial con corte anticipado mediante un ciclo o métodos como `allMatch()`, centraliza el control de ejecución en el componente que recorre la lista y no proporciona por sí misma la misma estructura de delegación entre manejadores. Además, utilizar únicamente valores booleanos dificulta representar de manera uniforme los resultados de rechazo y sus mensajes específicos.

**Decisión final:** se seleccionó Chain of Responsibility porque permite encapsular cada validación en un componente independiente, controlar explícitamente la continuación del proceso y detener la cadena cuando una regla de negocio rechaza el pedido. La alternativa basada en predicados no es incorrecta, pero resulta menos expresiva para representar validadores que deben controlar tanto el resultado como la delegación al siguiente responsable.

## Decisión de diseño: cálculo de descuentos mediante Strategy

Se decidió utilizar el patrón **Strategy** para implementar las reglas de descuento, en lugar de agregar cada tipo de cliente como un eslabón de Chain of Responsibility. La razón es que los descuentos no representan una secuencia de validaciones ni requieren detener o delegar el procesamiento. En este caso, se necesita seleccionar una única política de cálculo según el tipo de cliente y aplicarla sobre el subtotal del pedido. Por ejemplo, un cliente VIP utiliza sus reglas de descuento según el monto de la compra, mientras que un cliente FRECUENTE utiliza la cantidad de pedidos anteriores.

Se consideró utilizar Chain of Responsibility como alternativa, pero este patrón introduce una estructura de manejadores que reciben una solicitud y deciden si la procesan o la delegan al siguiente. Para el cálculo de descuentos, esta delegación no aporta una ventaja significativa, porque no es necesario recorrer diferentes reglas hasta encontrar una que gestione el caso. Por el contrario, un mapa de selección asociado a una fábrica sencilla permite obtener directamente la estrategia correspondiente al tipo de cliente, evitando estructuras condicionales extensas y reduciendo la indirección innecesaria. También puede incorporarse una estrategia de descuento cero para los tipos de cliente que no tengan beneficios especiales.

**Decisión final:** utilizar Strategy junto con un mecanismo sencillo de selección mediante un mapa o fábrica. De esta manera, cada tipo de cliente mantiene su propia política de descuento, facilitando la incorporación de nuevas reglas sin modificar la lógica central de procesamiento del pedido.