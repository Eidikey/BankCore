
# BankCore — Product Requirements Document (PRD)

**Versión:** 1.0
**Tipo:** Proyecto educativo
**Moneda base:** MXN

---

## 1. Resumen del producto

**BankCore** es una aplicación web educativa que simula las operaciones fundamentales de una institución bancaria.

El sistema permitirá registrar clientes, autenticarlos, crear y administrar cuentas bancarias, realizar depósitos y retiros simulados, transferir fondos entre cuentas BankCore, consultar movimientos y mantener un historial auditable de las operaciones.

El sistema **no manejará dinero real**, no se conectará con bancos externos y no realizará transferencias reales como SPEI.

El objetivo principal es construir una aplicación full-stack que permita al equipo aprender y aplicar conceptos de:

* Desarrollo backend con Java.
* Desarrollo de APIs REST.
* Persistencia de datos.
* Bases de datos relacionales.
* Autenticación y autorización.
* Transacciones y concurrencia.
* Validación de reglas de negocio.
* Auditoría.
* Pruebas automatizadas.
* Despliegue de una aplicación web.

---

# 2. Objetivos

## 2.1 Objetivo general

Desarrollar una plataforma bancaria simulada que permita ejecutar operaciones financieras de forma consistente, segura y auditable dentro de un entorno educativo.

## 2.2 Objetivos específicos

* Permitir el registro y autenticación de usuarios.
* Permitir que un cliente administre múltiples cuentas bancarias.
* Permitir depósitos y retiros simulados.
* Permitir transferencias entre cuentas BankCore.
* Mantener un historial de movimientos.
* Implementar diferentes roles y permisos.
* Implementar bloqueo temporal después de intentos fallidos de autenticación.
* Implementar bloqueo y desbloqueo administrativo de cuentas.
* Garantizar consistencia de saldos ante operaciones concurrentes.
* Mantener trazabilidad de operaciones mediante auditoría.
* Crear pruebas automatizadas para las operaciones críticas.
* Permitir ejecutar y desplegar el sistema de forma reproducible.

---

# 3. Alcance

## 3.1 Incluido en MVP

### Usuarios

* Registro de clientes.
* Inicio de sesión.
* Cierre de sesión.
* Perfil básico.
* Control de acceso basado en roles.
* Bloqueo temporal por intentos fallidos.

### Cuentas

* Creación de cuentas.
* Consulta de cuentas.
* Consulta de saldo.
* Múltiples cuentas por cliente.
* Bloqueo y desbloqueo administrativo.
* Cierre permanente de cuentas.
* Conservación del historial de cuentas cerradas.

### Operaciones

* Depósitos simulados.
* Retiros simulados.
* Transferencias internas entre cuentas BankCore.
* Consulta de movimientos.

### Administración

* Consulta de clientes.
* Consulta de cuentas.
* Revisión de operaciones.
* Bloqueo y desbloqueo de cuentas.
* Gestión de usuarios y permisos según rol.
* Consulta de auditoría.

### Calidad y seguridad

* Validación de operaciones.
* Control de concurrencia.
* Atomicidad de transferencias.
* Precisión monetaria.
* Auditoría.
* Pruebas automatizadas.

---

# 4. Fuera del alcance del MVP

Las siguientes funcionalidades no forman parte del MVP:

* Dinero real.
* Integración con bancos reales.
* SPEI.
* Tarjetas bancarias reales.
* Pagos reales con tarjeta.
* Préstamos.
* Créditos.
* Inversiones.
* Intereses.
* Cuentas de ahorro.
* Múltiples monedas.
* KYC/AML real.
* Integraciones gubernamentales.
* Aplicación móvil nativa.
* Blockchain.
* Inteligencia artificial financiera.
* Arquitectura de microservicios.
* Event-driven architecture.
* API Gateway.
* Escalamiento horizontal.
* Límites diarios de operación en la primera versión.

Estas características podrán considerarse en versiones posteriores.

---

# 5. Usuarios y roles

BankCore tendrá tres roles principales:

| Rol        | Descripción                                                                   |
| ---------- | ----------------------------------------------------------------------------- |
| `CLIENTE`  | Usuario que utiliza los servicios bancarios simulados.                        |
| `EMPLEADO` | Usuario interno encargado de soporte y operaciones administrativas limitadas. |
| `ADMIN`    | Usuario con permisos administrativos y de configuración.                      |

## 5.1 Cliente

Puede:

* Registrarse.
* Iniciar sesión.
* Cerrar sesión.
* Consultar su perfil.
* Crear cuentas.
* Consultar sus cuentas.
* Consultar saldos.
* Depositar fondos simulados.
* Retirar fondos simulados.
* Transferir fondos entre cuentas BankCore.
* Consultar movimientos.
* Cerrar sus propias cuentas cuando cumplan las condiciones.
* Consultar cuentas cerradas que le pertenecieron.

No puede:

* Bloquear o desbloquear cuentas manualmente.
* Administrar otros usuarios.
* Modificar permisos.
* Acceder a información de otros clientes.

## 5.2 Empleado

Puede:

* Consultar clientes.
* Consultar cuentas.
* Consultar operaciones.
* Bloquear cuentas.
* Desbloquear cuentas.
* Brindar soporte.
* Consultar información necesaria para atención al cliente.

No puede realizar acciones exclusivas del administrador.

## 5.3 Administrador

Puede:

* Administrar usuarios.
* Administrar empleados.
* Gestionar permisos.
* Consultar operaciones.
* Bloquear y desbloquear cuentas.
* Consultar auditoría.
* Administrar configuraciones permitidas.
* Realizar acciones administrativas del sistema.

---

# 6. Modelo de clientes y cuentas

## 6.1 Relación

La relación entre clientes y cuentas será:

**Cliente 1:N Cuentas**

Un cliente puede tener múltiples cuentas.

## 6.2 Límite de cuentas

Un cliente puede tener un máximo de:

**5 cuentas activas simultáneamente.**

Las cuentas cerradas **no cuentan** para este límite.

### Ejemplo

Un cliente tiene:

* Cuenta A → `ACTIVA`
* Cuenta B → `ACTIVA`
* Cuenta C → `ACTIVA`
* Cuenta D → `ACTIVA`
* Cuenta E → `ACTIVA`

No puede crear otra cuenta.

Si cierra la Cuenta C:

* Cuenta A → `ACTIVA`
* Cuenta B → `ACTIVA`
* Cuenta C → `CERRADA`
* Cuenta D → `ACTIVA`
* Cuenta E → `ACTIVA`

Ahora tiene solamente 4 cuentas activas y puede crear una nueva.

La cuenta cerrada continúa existiendo para efectos históricos y de auditoría.

---

# 7. Tipo de cuenta

El MVP tendrá un único tipo de cuenta:

```text
CUENTA_CORRIENTE
```

El sistema deberá almacenar el tipo de cuenta como un atributo independiente, aunque inicialmente solamente exista un tipo.

Esto permitirá incorporar posteriormente otros tipos sin rediseñar completamente el modelo.

### Fuera del MVP

* Cuenta de ahorro.
* Cuenta de inversión.
* Cuenta de crédito.
* Cuenta con intereses.

---

# 8. Identificación de cuentas

Cada cuenta tendrá **dos identificadores**.

## 8.1 ID interno

Identificador técnico utilizado internamente por el sistema.

Características:

* No se muestra como identificador principal al usuario.
* Será utilizado para relaciones internas.
* No dependerá del formato del número de cuenta.
* Se recomienda utilizar UUID.

Ejemplo:

```text
550e8400-e29b-41d4-a716-446655440000
```

## 8.2 Número de cuenta

Identificador público utilizado por los clientes.

Características:

* Exactamente 10 dígitos.
* Solamente caracteres numéricos.
* Único en el sistema.
* Se almacena como texto (`String`), no como número.
* Tendrá un prefijo inicial `10`.

Ejemplo:

```text
1084930219
```

### Composición inicial

```text
10 + 8 dígitos aleatorios
```

El sistema deberá verificar que el número generado no exista antes de asignarlo.

La base de datos tendrá una restricción `UNIQUE` sobre el número de cuenta.

### Importante

El número de cuenta **no será utilizado como clave primaria técnica**.

Esto permite modificar posteriormente el formato del número de cuenta sin romper las relaciones internas del sistema.

---

# 9. Estados de cuenta

Una cuenta podrá tener los siguientes estados:

```text
ACTIVA
BLOQUEADA
CERRADA
```

## 9.1 ACTIVA

Permite:

* Consultar saldo.
* Depositar.
* Retirar.
* Transferir fondos.
* Recibir transferencias.

## 9.2 BLOQUEADA

Una cuenta bloqueada:

* No puede retirar.
* No puede depositar.
* No puede realizar transferencias salientes.
* No puede recibir transferencias.
* Puede ser consultada según los permisos del usuario.

Solamente `EMPLEADO` o `ADMIN` pueden bloquear o desbloquear una cuenta.

Toda acción de bloqueo/desbloqueo debe quedar registrada en auditoría.

## 9.3 CERRADA

Una cuenta cerrada:

* No puede realizar operaciones.
* No puede recibir depósitos.
* No puede recibir transferencias.
* No puede realizar transferencias.
* No puede retirarse dinero.
* No puede reactivarse.
* No debe eliminarse físicamente de la base de datos.

Para cerrar una cuenta:

```text
balance == $0.00 MXN
```

El cierre es permanente.

La información histórica y los movimientos de la cuenta deben conservarse.

Una cuenta cerrada deja de ocupar uno de los 5 lugares disponibles del cliente.

---

# 10. Saldo y dinero

## 10.1 Saldo

En el MVP cada cuenta tendrá un único saldo:

```text
balance
```

No existirán saldos pendientes o retenidos.

Por lo tanto:

```text
saldo contable = saldo disponible
```

## 10.2 Precisión

Java deberá utilizar:

```java
BigDecimal
```

Nunca deberán utilizarse `float` o `double` para representar dinero.

La base de datos utilizará:

```sql
NUMERIC(15,2)
```

## 10.3 Moneda

La moneda base del sistema será:

```text
MXN
```

## 10.4 Monto mínimo

Toda operación monetaria deberá ser de al menos:

```text
$1.00 MXN
```

## 10.5 Monto máximo

Toda operación individual tendrá un máximo configurable de:

```text
$50,000.00 MXN
```

Este límite será configurable desde la configuración del backend.

Los límites diarios quedan fuera del MVP inicial.

---

# 11. Depósitos

BankCore contará con un sistema de **depósitos simulados**.

No representa dinero físico real.

## Reglas

Para realizar un depósito:

1. La cuenta debe existir.
2. La cuenta debe pertenecer al usuario autenticado o la operación debe estar autorizada por el rol correspondiente.
3. La cuenta debe estar `ACTIVA`.
4. El monto debe ser mayor o igual a `$1.00`.
5. El monto no debe superar `$50,000.00`.
6. El saldo debe actualizarse correctamente.
7. Debe registrarse un movimiento.
8. Debe generarse un identificador único de operación.

---

# 12. Retiros

BankCore permitirá realizar retiros simulados.

## Reglas

Para realizar un retiro:

1. La cuenta debe existir.
2. La cuenta debe pertenecer al usuario autenticado o la operación debe estar autorizada por el rol correspondiente.
3. La cuenta debe estar `ACTIVA`.
4. El monto debe ser mayor o igual a `$1.00`.
5. El monto no debe superar `$50,000.00`.
6. Debe existir saldo suficiente.
7. El saldo no puede quedar negativo.
8. Debe registrarse un movimiento.
9. Debe generarse un identificador único de operación.

El sistema debe evitar que dos retiros concurrentes permitan gastar el mismo saldo.

---

# 13. Transferencias

BankCore permitirá transferencias exclusivamente entre cuentas internas de BankCore.

No existirán transferencias hacia bancos externos en el MVP.

## Datos mínimos

Una transferencia deberá identificar:

* Cuenta origen.
* Cuenta destino.
* Monto.
* Fecha/hora.
* Usuario que inició la operación.
* Identificador único de operación.

## Reglas

Para realizar una transferencia:

1. La cuenta origen debe existir.
2. La cuenta destino debe existir.
3. La cuenta origen debe pertenecer al cliente autenticado.
4. Ambas cuentas deben estar `ACTIVA`.
5. El monto debe ser mayor o igual a `$1.00`.
6. El monto no debe superar `$50,000.00`.
7. La cuenta origen debe tener saldo suficiente.
8. La cuenta destino debe poder recibir la transferencia.
9. El saldo de origen debe disminuir.
10. El saldo de destino debe aumentar.
11. Deben registrarse los movimientos correspondientes.
12. La operación debe tener un identificador único.

## Atomicidad

Una transferencia debe ejecutarse como una sola operación transaccional.

Debe cumplirse:

```text
DEBITAR ORIGEN
      +
ACREDITAR DESTINO
      +
REGISTRAR MOVIMIENTO
```

Todo debe confirmarse correctamente o nada debe aplicarse.

Nunca debe existir un estado en el que el dinero se descuente del origen pero no llegue al destino.

---

# 14. Identificador de operación

Cada operación financiera deberá tener un identificador único.

Se aplicará a:

* Depósitos.
* Retiros.
* Transferencias.

Este identificador permitirá:

* Consultar operaciones.
* Evitar duplicados.
* Facilitar auditoría.
* Rastrear errores.
* Relacionar movimientos con operaciones.

La implementación concreta del identificador se definirá durante el diseño técnico.

---

# 15. Movimientos e historial

Cada cuenta tendrá un historial de movimientos.

Un movimiento deberá contener, como mínimo:

* Identificador del movimiento.
* Tipo de operación.
* Monto.
* Fecha y hora.
* Cuenta involucrada.
* Identificador de operación.
* Saldo resultante cuando corresponda.

Tipos iniciales:

```text
DEPOSITO
RETIRO
TRANSFERENCIA_ENTRANTE
TRANSFERENCIA_SALIENTE
```

Los movimientos de cuentas cerradas deberán conservarse.

Los clientes solamente podrán consultar movimientos de cuentas a las que tengan acceso.

---

# 16. Autenticación

El sistema deberá implementar autenticación de usuarios.

## Requisitos

* Las contraseñas nunca deben almacenarse en texto plano.
* Debe utilizarse un mecanismo seguro de hashing.
* Debe existir validación de credenciales.
* Debe existir control de acceso.
* Debe existir protección contra intentos repetidos de autenticación.
* Las acciones relevantes deberán quedar registradas.

---

# 17. Bloqueo temporal de autenticación

Después de:

```text
3 intentos consecutivos fallidos
```

la cuenta de autenticación del usuario quedará temporalmente bloqueada durante:

```text
15 minutos
```

## Reglas

* El bloqueo debe comenzar después del tercer intento fallido.
* Los intentos realizados durante el periodo de bloqueo no deben reiniciar innecesariamente el temporizador.
* Debe registrarse un evento de auditoría.
* Después del periodo de bloqueo, el usuario podrá volver a intentar autenticarse.
* Un inicio de sesión exitoso deberá restablecer el contador de intentos fallidos.

### Importante

El bloqueo de autenticación y el estado de una cuenta bancaria son conceptos diferentes.

```text
Bloqueo de autenticación
        ≠
Cuenta bancaria BLOQUEADA
```

---

# 18. Sesiones

El MVP permitirá múltiples sesiones concurrentes para un mismo usuario.

Por ejemplo, un usuario podrá iniciar sesión desde:

* Computadora.
* Navegador diferente.
* Otro dispositivo.

Un nuevo inicio de sesión no invalidará automáticamente las sesiones anteriores.

El sistema deberá permitir:

* Expiración de sesiones.
* Cierre de sesión.
* Revocación de sesiones cuando sea necesario por seguridad.

El mecanismo concreto de sesiones/tokens se definirá en el SRS y diseño técnico.

---

# 19. Autorización

El backend deberá validar permisos en cada operación protegida.

No deberá confiar únicamente en restricciones implementadas en el frontend.

Ejemplo:

```text
CLIENTE
   ↓
solicita bloquear cuenta
   ↓
BACKEND
   ↓
rechaza por falta de permisos
```

Las reglas de autorización deben aplicarse independientemente de la interfaz utilizada para acceder al sistema.

---

# 20. Auditoría

BankCore deberá mantener un registro de eventos importantes.

Eventos mínimos:

* Inicio de sesión exitoso.
* Inicio de sesión fallido.
* Bloqueo temporal por autenticación.
* Cierre de sesión.
* Creación de cuenta.
* Depósito.
* Retiro.
* Transferencia.
* Bloqueo de cuenta.
* Desbloqueo de cuenta.
* Cierre de cuenta.
* Cambios importantes de información.
* Acciones administrativas.

Como mínimo, un evento de auditoría deberá registrar:

* Tipo de evento.
* Fecha y hora.
* Usuario involucrado.
* Recurso afectado.
* Identificador de operación cuando corresponda.

---

# 21. Dashboard

El cliente contará con un dashboard que mostrará información relevante.

Debe incluir:

* Saldo total de las cuentas activas.
* Cantidad de cuentas activas.
* Cuentas disponibles.
* Movimientos recientes.
* Transferencias recientes.
* Acciones rápidas.
* Estado de las cuentas.

El dashboard debe estar diseñado considerando que un cliente puede tener múltiples cuentas.

---

# 22. Creación y cierre de cuentas

## 22.1 Creación

Al crear una cuenta:

1. El usuario debe estar autenticado.
2. Debe tener menos de 5 cuentas activas.
3. Debe generarse un ID interno.
4. Debe generarse un número de cuenta de 10 dígitos.
5. El número debe ser único.
6. La cuenta inicia con saldo `$0.00`.
7. La cuenta inicia en estado `ACTIVA`.
8. Debe registrarse la creación en auditoría.

## 22.2 Cierre

Un cliente podrá solicitar el cierre de una cuenta cuando:

```text
balance == $0.00
```

Al cerrarse:

* El estado cambia a `CERRADA`.
* La cuenta no puede reactivarse.
* No puede recibir operaciones.
* Se conserva su información.
* Se conserva su historial.
* Se conserva su auditoría.
* Deja de contar para el límite de 5 cuentas activas.

---

# 23. Concurrencia e integridad

El sistema deberá proteger las operaciones financieras contra condiciones de carrera.

Ejemplo:

```text
Saldo = $1,000

Solicitud A → retirar $800
Solicitud B → retirar $800
```

El sistema no debe permitir que ambas operaciones se ejecuten exitosamente si no existe saldo suficiente para ambas.

Debe garantizarse:

* No saldo negativo.
* No pérdida de operaciones.
* No duplicación de operaciones.
* Consistencia del saldo.
* Atomicidad de transferencias.
* Consistencia entre movimientos y saldo.
* Integridad ante operaciones simultáneas.

Los mecanismos concretos se definirán durante el diseño técnico.

---

# 24. Requisitos de UX

La aplicación deberá:

* Ser responsive.
* Tener navegación clara.
* Mostrar estados de cuenta de forma comprensible.
* Mostrar cantidades monetarias claramente.
* Confirmar operaciones sensibles.
* Mostrar mensajes de error comprensibles.
* Evitar exponer información innecesaria.
* Diferenciar claramente cuentas activas, bloqueadas y cerradas.
* Mostrar identificadores de operación después de operaciones financieras exitosas.

---

# 25. Arquitectura conceptual

La arquitectura general esperada será:

```text
┌────────────────────┐
│      Frontend      │
│     Aplicación Web │
└─────────┬──────────┘
          │ HTTPS
          ▼
┌────────────────────┐
│       Backend      │
│      Java / API    │
└─────────┬──────────┘
          │
          ▼
┌────────────────────┐
│     PostgreSQL     │
│     Base de datos  │
└────────────────────┘
```

La selección definitiva de frameworks y herramientas se documentará posteriormente.

---

# 26. Requisitos no funcionales

## 26.1 Seguridad

* Contraseñas hasheadas.
* Autorización en backend.
* Validación de entradas.
* Protección de secretos.
* Control de sesiones.
* Auditoría.
* Protección contra operaciones duplicadas.
* Protección contra condiciones de carrera.

## 26.2 Integridad

Las operaciones financieras deben mantener estados consistentes.

## 26.3 Disponibilidad

El sistema deberá poder levantarse de forma reproducible en un entorno de desarrollo.

## 26.4 Mantenibilidad

El código deberá:

* Tener estructura clara.
* Separar responsabilidades.
* Utilizar convenciones consistentes.
* Contar con documentación.
* Contar con pruebas automatizadas.

## 26.5 Escalabilidad

La arquitectura inicial deberá permitir crecimiento sin requerir una reescritura completa.

No se implementará microservicios en el MVP.

---

# 27. Pruebas

Se deberán implementar pruebas automatizadas para las funcionalidades críticas.

## Autenticación

* Registro.
* Login exitoso.
* Login fallido.
* Bloqueo después de 3 intentos.
* Expiración del bloqueo.

## Cuentas

* Creación.
* Límite de 5 cuentas activas.
* Liberación de cupo al cerrar una cuenta.
* Cierre con saldo cero.
* Rechazo de cierre con saldo positivo.
* Rechazo de operaciones sobre cuentas cerradas.
* Bloqueo/desbloqueo.

## Dinero

* Depósito válido.
* Depósito inválido.
* Retiro válido.
* Retiro sin saldo suficiente.
* Monto mínimo.
* Monto máximo.
* Precisión monetaria.

## Transferencias

* Transferencia válida.
* Cuenta destino inexistente.
* Cuenta origen sin saldo.
* Cuenta bloqueada.
* Cuenta cerrada.
* Transferencia atómica.
* Operación duplicada.
* Operaciones concurrentes.

---

# 28. Criterios de aceptación del MVP

El MVP se considerará funcional cuando:

* Un cliente pueda registrarse.
* Un cliente pueda iniciar y cerrar sesión.
* Existan los roles Cliente, Empleado y Administrador.
* Un cliente pueda tener hasta 5 cuentas activas.
* Una cuenta cerrada libere su cupo.
* Cada cuenta tenga ID interno y número de cuenta público.
* El número de cuenta tenga exactamente 10 dígitos y sea único.
* Se puedan realizar depósitos simulados.
* Se puedan realizar retiros simulados.
* No sea posible generar saldos negativos.
* Se puedan realizar transferencias internas.
* Las transferencias sean atómicas.
* Se mantenga un historial de movimientos.
* Se conserve el historial de cuentas cerradas.
* Se puedan bloquear y desbloquear cuentas mediante roles autorizados.
* Una cuenta cerrada no pueda reactivarse.
* Se utilice precisión monetaria apropiada.
* Ninguna operación supere el límite configurado de `$50,000.00`.
* Un usuario sea bloqueado temporalmente después de 3 intentos fallidos consecutivos.
* Se permitan múltiples sesiones concurrentes.
* Las acciones importantes sean auditables.
* Existan pruebas automatizadas de las operaciones críticas.
* El proyecto pueda ejecutarse localmente de forma reproducible.
* El proyecto pueda desplegarse en un entorno remoto.

---

# 29. Roadmap posterior al MVP

## Nivel 2 — Funcionalidades adicionales

Posibles características:

* Tarjetas virtuales simuladas.
* Pagos simulados a comercios.
* Beneficiarios.
* Comprobantes descargables.
* Notificaciones por correo.
* Búsqueda avanzada de movimientos.
* Exportación de movimientos.
* Límites diarios.

## Nivel 3 — Infraestructura avanzada

Posibles características:

* Redis.
* Message broker.
* Notificaciones asíncronas.
* Métricas.
* Monitoring.
* Alertas.
* Load testing.
* Observabilidad avanzada.

## Nivel 4 — Arquitectura experimental

Posibles características:

* Microservicios.
* API Gateway.
* Arquitectura orientada a eventos.
* Escalamiento horizontal.
* Pruebas de resiliencia.

Estas características no forman parte del MVP.

---

# 30. Decisiones pendientes para el SRS

El PRD define **qué debe hacer BankCore**. El SRS deberá definir con precisión **cómo se implementará**.

Pendientes principales:

* Framework Java.
* Versión de Java.
* Framework backend.
* Estructura del proyecto.
* Diseño detallado de API REST.
* Esquema completo de PostgreSQL.
* Relaciones y restricciones de base de datos.
* Estrategia de migraciones.
* Mecanismo de autenticación.
* Mecanismo de sesiones/tokens.
* Hashing de contraseñas.
* Estrategia de autorización.
* Manejo de errores.
* Validación.
* Estrategia de concurrencia.
* Estrategia de transacciones.
* Generación de números de cuenta.
* Idempotencia.
* Estructura de auditoría.
* Estrategia de testing.
* CI/CD.
* Hosting.
* Dominio.
* HTTPS/TLS.
* Variables de entorno y secretos.
* Backups.
* Logging.
* Monitoring.
* Estructura del frontend.

---

# 31. Resumen de reglas de negocio

| Regla                                   | Valor                            |
| --------------------------------------- | -------------------------------- |
| Relación cliente-cuentas                | 1:N                              |
| Máximo de cuentas activas               | 5                                |
| Cuentas cerradas cuentan para el límite | No                               |
| Tipo de cuenta MVP                      | `CUENTA_CORRIENTE`               |
| Identificadores por cuenta              | ID interno + número público      |
| Formato número de cuenta                | 10 dígitos                       |
| Prefijo inicial                         | `10`                             |
| Número de cuenta único                  | Sí                               |
| Saldo                                   | `balance`                        |
| Precisión Java                          | `BigDecimal`                     |
| Precisión DB                            | `NUMERIC(15,2)`                  |
| Moneda                                  | MXN                              |
| Monto mínimo                            | $1.00                            |
| Monto máximo por operación              | $50,000.00                       |
| Límite diario                           | Fuera del MVP                    |
| Estados                                 | `ACTIVA`, `BLOQUEADA`, `CERRADA` |
| Cierre con saldo                        | Solamente $0.00                  |
| Reactivación de cuenta cerrada          | No                               |
| Historial de cuenta cerrada             | Se conserva                      |
| Transferencias externas                 | No                               |
| SPEI                                    | No                               |
| Dinero real                             | No                               |
| Intentos fallidos de login              | 3                                |
| Bloqueo temporal                        | 15 minutos                       |
| Sesiones simultáneas                    | Permitidas                       |
| Auditoría                               | Obligatoria                      |
| Arquitectura inicial                    | Monolito web/API                 |
| Base de datos propuesta                 | PostgreSQL                       |
| Backend obligatorio                     | Java                             |

---

# 32. Principio general del producto

BankCore deberá priorizar:

1. **Integridad financiera simulada**
2. **Seguridad**
3. **Trazabilidad**
4. **Consistencia**
5. **Claridad**
6. **Mantenibilidad**
7. **Aprendizaje técnico**

El sistema debe comportarse como una simulación bancaria coherente, aunque no represente operaciones financieras reales.
