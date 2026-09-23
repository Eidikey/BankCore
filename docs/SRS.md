
# BankCore — Software Requirements Specification (SRS)

**Versión:** 1.1
**Proyecto:** BankCore
**Tipo:** Aplicación web bancaria simulada
**Moneda base:** MXN
**Base de datos:** PostgreSQL mediante Supabase

---

# 1. Introducción

## 1.1 Propósito

Este documento define los requisitos funcionales y no funcionales de **BankCore**, una aplicación web educativa que simula las operaciones principales de un sistema bancario.

El sistema permitirá a los usuarios crear cuentas bancarias simuladas, consultar saldos, realizar depósitos, retiros y transferencias, consultar movimientos y administrar el estado de sus cuentas.

BankCore **no manejará dinero real** ni tendrá integración con instituciones financieras externas.

Este documento servirá como referencia para:

* Diseño técnico.
* Diseño de base de datos.
* Diseño de API.
* Desarrollo del backend.
* Desarrollo del frontend.
* Pruebas.
* Despliegue.
* Validación del proyecto.

---

# 2. Alcance

## 2.1 Incluido en el MVP

El MVP de BankCore deberá permitir:

* Registro de clientes.
* Inicio y cierre de sesión.
* Gestión de perfil.
* Creación de múltiples cuentas bancarias.
* Consulta de cuentas.
* Consulta de saldo.
* Depósitos simulados.
* Retiros simulados.
* Transferencias entre cuentas BankCore.
* Consulta de movimientos.
* Cierre de cuentas.
* Bloqueo y desbloqueo administrativo de cuentas.
* Gestión de usuarios según roles.
* Auditoría de eventos importantes.
* Control de operaciones duplicadas.
* Protección contra condiciones de carrera.
* Manejo correcto de cantidades monetarias.
* Control de autorización.
* Pruebas automatizadas.
* Despliegue remoto reproducible.

## 2.2 Fuera del MVP

Quedan fuera del alcance inicial:

* Dinero real.
* SPEI.
* Integración con bancos reales.
* Tarjetas bancarias reales.
* Pagos con comercios reales.
* Préstamos.
* Inversiones.
* KYC/AML.
* Integraciones gubernamentales.
* Aplicación móvil nativa.
* Blockchain.
* Inteligencia artificial financiera.
* Múltiples monedas.
* Cuenta de ahorro.
* Intereses.
* Límites diarios.
* Arquitectura de microservicios.
* Event-driven architecture.
* Escalamiento horizontal avanzado.

Estas características podrán considerarse posteriormente.

---

# 3. Definiciones

| Término          | Definición                                                            |
| ---------------- | --------------------------------------------------------------------- |
| Cliente          | Usuario que utiliza los servicios bancarios simulados                 |
| Empleado         | Usuario con permisos operativos y de soporte                          |
| Administrador    | Usuario con permisos administrativos avanzados                        |
| Cuenta           | Cuenta bancaria simulada perteneciente a un cliente                   |
| Movimiento       | Registro de una operación financiera                                  |
| Operación        | Acción financiera ejecutada por el sistema                            |
| ID de operación  | Identificador único de una operación                                  |
| Idempotencia     | Propiedad que evita ejecutar dos veces una misma operación solicitada |
| Idempotency Key  | Clave utilizada para identificar una solicitud financiera única       |
| Saldo            | Cantidad disponible en una cuenta                                     |
| Cuenta activa    | Cuenta que puede realizar operaciones                                 |
| Cuenta bloqueada | Cuenta que no puede realizar operaciones financieras                  |
| Cuenta cerrada   | Cuenta permanentemente inhabilitada                                   |
| Auditoría        | Registro de eventos relevantes del sistema                            |

---

# 4. Actores

## 4.1 Cliente

Puede:

* Registrarse.
* Iniciar sesión.
* Cerrar sesión.
* Consultar su perfil.
* Crear cuentas.
* Consultar sus cuentas.
* Consultar saldos.
* Depositar.
* Retirar.
* Transferir.
* Consultar movimientos.
* Cerrar cuentas con saldo cero.

## 4.2 Empleado

Puede:

* Consultar clientes.
* Consultar cuentas.
* Consultar operaciones.
* Revisar movimientos.
* Bloquear cuentas.
* Desbloquear cuentas.
* Realizar tareas de soporte.

## 4.3 Administrador

Puede:

* Realizar las operaciones de un empleado.
* Administrar usuarios.
* Administrar empleados.
* Administrar permisos.
* Consultar auditorías.
* Administrar configuraciones relevantes.
* Gestionar operaciones administrativas.

---

# 5. Requisitos funcionales

## RF-001 — Registro

El sistema deberá permitir registrar nuevos clientes.

El registro deberá solicitar como mínimo la información necesaria para:

* Identificación del usuario.
* Datos básicos.
* Credenciales de acceso.

Las contraseñas nunca deberán almacenarse en texto plano.

---

## RF-002 — Inicio de sesión

El sistema deberá permitir que un usuario autenticado inicie sesión utilizando sus credenciales.

El sistema deberá:

* Validar las credenciales.
* Crear una sesión válida.
* Registrar el evento de autenticación.
* Aplicar las restricciones de seguridad correspondientes.

---

## RF-003 — Cierre de sesión

El sistema deberá permitir cerrar una sesión individual.

Cerrar una sesión no deberá cerrar automáticamente otras sesiones activas del mismo usuario.

---

## RF-004 — Bloqueo por intentos fallidos

Después de **3 intentos consecutivos fallidos de autenticación**, el usuario deberá quedar temporalmente bloqueado durante **15 minutos**.

Durante este periodo:

* No podrá iniciar sesión.
* Los intentos realizados durante el bloqueo no deberán reiniciar innecesariamente el temporizador.
* El evento deberá quedar registrado en auditoría.

El bloqueo de autenticación será independiente del estado de las cuentas bancarias.

---

## RF-005 — Sesiones concurrentes

El sistema deberá permitir múltiples sesiones simultáneas para un mismo usuario.

Cada sesión deberá poder:

* Expirar.
* Cerrarse individualmente.
* Revocarse individualmente.

La implementación concreta de sesiones queda pendiente del diseño técnico.

Se consideran como alternativas:

* JWT.
* Sesiones persistentes almacenadas en base de datos.
* Sesiones utilizando Redis.

---

# 6. Gestión de clientes

## RF-006 — Perfil

El cliente deberá poder consultar y modificar los datos de su perfil que sean permitidos por el sistema.

Los cambios relevantes deberán poder registrarse en auditoría.

---

# 7. Gestión de cuentas

## RF-007 — Creación de cuenta

Un cliente podrá tener múltiples cuentas bancarias.

El sistema permitirá un máximo de:

**5 cuentas activas simultáneamente por cliente.**

Las cuentas cerradas no deberán contar para este límite.

Ejemplo:

```text
4 cuentas activas
1 cuenta cerrada
----------------
Puede crear otra cuenta activa
```

---

## RF-008 — Tipos de cuenta

El MVP tendrá un único tipo de cuenta bancaria.

La implementación deberá conservar un campo que permita agregar nuevos tipos posteriormente.

El tipo inicial podrá representarse conceptualmente como:

```text
CUENTA_CORRIENTE
```

La incorporación futura de otros tipos no deberá requerir rediseñar completamente el modelo de cuentas.

---

## RF-009 — Identificador interno

Cada cuenta deberá tener un identificador interno único.

Este identificador será utilizado para relaciones internas del sistema.

Se recomienda que sea un UUID, aunque la implementación definitiva se determinará durante el diseño técnico.

---

## RF-010 — Número público de cuenta

Cada cuenta deberá tener un número de cuenta visible para el cliente.

Características:

* Exactamente 10 dígitos numéricos.
* Almacenado como cadena.
* Único.
* Utilizado por los clientes para identificar cuentas.
* No deberá utilizarse como mecanismo de seguridad.

La composición propuesta para MVP es:

```text
10 + 8 dígitos aleatorios
```

Ejemplo:

```text
1028473916
```

El sistema deberá garantizar su unicidad.

---

# 8. Estados de cuenta

Cada cuenta deberá encontrarse en uno de los siguientes estados:

```text
ACTIVA
BLOQUEADA
CERRADA
```

## RF-011 — Cuenta activa

Una cuenta `ACTIVA` podrá:

* Consultar saldo.
* Recibir depósitos.
* Realizar retiros.
* Realizar transferencias.
* Recibir transferencias.

---

## RF-012 — Cuenta bloqueada

Una cuenta `BLOQUEADA`:

* No podrá realizar operaciones financieras.
* No podrá recibir transferencias.
* No podrá recibir depósitos.
* No podrá realizar retiros.
* No podrá transferir fondos.

Un empleado o administrador autorizado podrá bloquear o desbloquear una cuenta.

Las acciones de bloqueo y desbloqueo deberán registrarse en auditoría.

---

## RF-013 — Cuenta cerrada

Una cuenta `CERRADA`:

* No podrá realizar operaciones.
* No podrá recibir depósitos.
* No podrá recibir transferencias.
* No podrá realizar retiros.
* No podrá transferir.
* No podrá reactivarse.
* No podrá eliminarse físicamente.

Una cuenta solamente podrá cerrarse cuando su saldo sea:

```text
0.00 MXN
```

El historial de la cuenta deberá conservarse.

---

## RF-014 — Cierre de cuenta

El cliente podrá solicitar el cierre de una cuenta cuando:

```text
saldo == 0.00 MXN
```

Una vez cerrada, la cuenta deberá permanecer permanentemente en estado `CERRADA`.

---

# 9. Requisitos monetarios

## RF-015 — Precisión monetaria

Las cantidades monetarias deberán manejarse utilizando precisión decimal.

En Java deberá utilizarse:

```java
BigDecimal
```

No deberán utilizarse:

```java
float
double
```

para representar dinero.

La base de datos deberá utilizar:

```sql
NUMERIC(15,2)
```

---

## RF-016 — Monto mínimo

Toda operación financiera deberá tener como monto mínimo:

```text
$1.00 MXN
```

---

## RF-017 — Monto máximo

Toda operación financiera individual deberá tener como límite máximo:

```text
$50,000.00 MXN
```

El límite deberá ser configurable desde el backend.

---

# 10. Depósitos

## RF-018 — Depósito simulado

El cliente podrá realizar depósitos simulados mediante la aplicación.

El depósito representará una operación ficticia.

No existirá conexión con:

* Cajeros reales.
* Bancos.
* Procesadores de pago.
* Sistemas financieros externos.

---

## RF-019 — Validación de depósito

Para realizar un depósito:

* La cuenta deberá existir.
* La cuenta deberá pertenecer al cliente autenticado.
* La cuenta deberá estar `ACTIVA`.
* El monto deberá ser mayor o igual a $1.00 MXN.
* El monto no deberá superar $50,000.00 MXN.

---

## RF-020 — Registro de depósito

Cada depósito exitoso deberá:

1. Actualizar el saldo.
2. Crear un movimiento.
3. Generar un ID de operación único.
4. Registrar fecha y hora.
5. Registrar el saldo resultante.
6. Generar auditoría cuando corresponda.

Estas acciones deberán mantener consistencia transaccional.

---

# 11. Retiros

## RF-021 — Retiro simulado

El cliente podrá realizar retiros simulados.

---

## RF-022 — Validación de retiro

Para realizar un retiro:

* La cuenta deberá existir.
* Deberá pertenecer al cliente autenticado.
* Deberá estar `ACTIVA`.
* El monto deberá ser mínimo $1.00 MXN.
* El monto máximo será $50,000.00 MXN.
* El saldo deberá ser suficiente.

---

## RF-023 — Protección contra retiros concurrentes

El sistema deberá impedir situaciones como:

```text
Saldo inicial: $1,000

Solicitud A: retirar $800
Solicitud B: retirar $800
```

No deberá ser posible terminar con:

```text
Saldo: -$600
```

ni procesar ambas operaciones utilizando incorrectamente el mismo saldo inicial.

La implementación concreta de:

* Transacciones.
* Bloqueos.
* Nivel de aislamiento.
* Actualizaciones atómicas.

será definida durante el diseño técnico.

---

# 12. Transferencias

## RF-024 — Transferencias internas

El sistema permitirá transferencias únicamente entre cuentas BankCore.

No se permitirán transferencias hacia bancos externos durante el MVP.

---

## RF-025 — Cuenta origen

La cuenta origen deberá:

* Existir.
* Pertenecer al cliente autenticado.
* Estar `ACTIVA`.
* Tener saldo suficiente.

---

## RF-026 — Cuenta destino

La cuenta destino deberá:

* Existir.
* Estar `ACTIVA`.

La cuenta destino será identificada mediante su número público de 10 dígitos.

---

## RF-027 — Monto de transferencia

La transferencia deberá cumplir:

```text
$1.00 MXN <= monto <= $50,000.00 MXN
```

El saldo de la cuenta origen deberá ser suficiente.

---

## RF-028 — Atomicidad de transferencia

Una transferencia deberá ejecutarse como una única operación lógica.

Deberá incluir:

1. Débito de la cuenta origen.
2. Crédito de la cuenta destino.
3. Registro de los movimientos.
4. Registro de la operación.

Todas las operaciones deberán confirmarse juntas.

Si alguna falla, ninguna deberá quedar aplicada.

Conceptualmente:

```text
BEGIN TRANSACTION

debitar origen
acreditar destino
crear movimientos
crear operación

COMMIT
```

Si ocurre un error:

```text
ROLLBACK
```

---

## RF-029 — ID de operación

Cada transferencia deberá tener un identificador único.

El identificador deberá permitir rastrear la operación completa.

---

# 13. Idempotencia

## RF-030 — Idempotency Key

Las operaciones financieras que puedan ser reintentadas deberán soportar mecanismos de idempotencia.

El mecanismo preferido será mediante el header:

```http
Idempotency-Key: <clave-única>
```

---

## RF-031 — Procesamiento idempotente

El backend deberá:

* Identificar solicitudes repetidas.
* Asociar la clave al usuario y operación correspondiente.
* Evitar ejecutar dos veces una misma operación.
* Evitar procesamiento concurrente duplicado.
* Retornar el resultado anterior cuando corresponda.
* Rechazar usos incorrectos de una clave.

La base de datos deberá proporcionar las restricciones necesarias para garantizar la unicidad dentro del alcance definido.

---

## RF-032 — Transferencias duplicadas

El sistema deberá detectar y rechazar transferencias duplicadas provocadas por:

* Reintentos del cliente.
* Doble clic.
* Reenvío de solicitudes.
* Problemas de red.
* Reintentos automáticos.

Una misma operación financiera no deberá ejecutarse dos veces.

---

# 14. Movimientos

## RF-033 — Historial de movimientos

Cada cuenta deberá tener un historial de movimientos.

Los movimientos deberán conservarse aunque la cuenta sea cerrada.

---

## RF-034 — Información de movimiento

Cada movimiento deberá contener, como mínimo:

* Tipo de operación.
* Monto.
* Fecha y hora.
* Cuenta involucrada.
* ID de operación.
* Saldo resultante cuando corresponda.

---

## RF-035 — Consulta de movimientos

Un cliente solamente podrá consultar movimientos de cuentas a las que tenga acceso.

No podrá consultar movimientos pertenecientes a cuentas de otros clientes.

---

# 15. Dashboard

## RF-036 — Dashboard del cliente

El sistema deberá proporcionar un dashboard que muestre como mínimo:

* Saldo total.
* Número de cuentas activas.
* Cuentas del cliente.
* Movimientos recientes.
* Transferencias recientes.
* Acciones rápidas.
* Estado de las cuentas.

El dashboard deberá funcionar correctamente con múltiples cuentas.

---

# 16. Autorización

## RF-037 — Separación de roles

El sistema deberá diferenciar al menos:

```text
CLIENTE
EMPLEADO
ADMIN
```

Los permisos deberán validarse en el backend.

No será suficiente ocultar funcionalidades únicamente en el frontend.

---

## RF-038 — Control de acceso

Cada endpoint protegido deberá verificar:

1. Autenticación.
2. Identidad.
3. Rol cuando corresponda.
4. Propiedad del recurso cuando corresponda.
5. Permisos específicos.

---

# 17. Auditoría

## RF-039 — Eventos auditables

El sistema deberá registrar como mínimo:

* Inicio de sesión exitoso.
* Inicio de sesión fallido.
* Bloqueo por autenticación.
* Cierre de sesión.
* Creación de cuenta.
* Depósitos.
* Retiros.
* Transferencias.
* Bloqueo de cuenta.
* Desbloqueo de cuenta.
* Cierre de cuenta.
* Cambios importantes.
* Acciones administrativas.

---

## RF-040 — Información de auditoría

Los eventos deberán registrar como mínimo:

* Tipo de evento.
* Fecha y hora.
* Usuario involucrado.
* Recurso afectado.
* ID de operación cuando corresponda.

Los eventos de auditoría no deberán depender exclusivamente de información enviada por el frontend.

---

# 18. Persistencia

## RF-041 — Base de datos

El sistema utilizará:

```text
PostgreSQL
```

La infraestructura inicial podrá utilizar:

```text
Supabase
```

Supabase funcionará como proveedor de PostgreSQL administrado.

La lógica de negocio deberá permanecer en el backend de BankCore.

El frontend no deberá conectarse directamente a la base de datos.

Arquitectura conceptual:

```text
Frontend
    |
   HTTPS
    |
Backend API
    |
PostgreSQL / Supabase
```

---

# 19. Integridad de datos

## RF-042 — Integridad financiera

El sistema deberá mantener consistencia entre:

* Saldos.
* Movimientos.
* Operaciones.
* Transferencias.

No deberán existir operaciones parcialmente aplicadas.

---

## RF-043 — Consistencia de movimientos

Una operación financiera exitosa deberá generar los movimientos correspondientes.

No deberá existir un movimiento financiero registrado si la operación correspondiente no fue aplicada correctamente.

---

# 20. Concurrencia

## RF-044 — Operaciones concurrentes

El backend deberá soportar solicitudes simultáneas sin producir:

* Saldos incorrectos.
* Doble procesamiento.
* Pérdida de operaciones.
* Movimientos inconsistentes.
* Transferencias parciales.

---

## RF-045 — Protección del saldo

Las operaciones que modifiquen un saldo deberán utilizar mecanismos apropiados para evitar condiciones de carrera.

Los mecanismos concretos serán determinados en el diseño técnico.

---

## RF-046 — Transferencias concurrentes

Dos o más transferencias simultáneas deberán mantener la integridad de las cuentas involucradas.

El sistema deberá garantizar que no se utilice incorrectamente un saldo obsoleto.

---

# 21. Manejo de errores

## RF-047 — Errores de negocio

El backend deberá devolver respuestas consistentes para errores como:

* Credenciales incorrectas.
* Usuario bloqueado.
* Cuenta inexistente.
* Cuenta bloqueada.
* Cuenta cerrada.
* Saldo insuficiente.
* Monto inválido.
* Monto fuera de límites.
* Cuenta destino inválida.
* Operación duplicada.
* Falta de permisos.
* Recurso no encontrado.

---

## RF-048 — Respuestas HTTP

La API deberá utilizar códigos HTTP apropiados.

Ejemplos:

```text
200 OK
201 Created
400 Bad Request
401 Unauthorized
403 Forbidden
404 Not Found
409 Conflict
422 Unprocessable Entity
500 Internal Server Error
```

La asignación definitiva de códigos se establecerá durante el diseño de la API.

---

# 22. Seguridad

## RF-049 — Contraseñas

Las contraseñas deberán almacenarse utilizando un algoritmo seguro de hashing.

Nunca deberán almacenarse:

```text
password = "123456"
```

en texto plano.

---

## RF-050 — Protección de operaciones

Las operaciones financieras deberán requerir autenticación y autorización.

El backend nunca deberá confiar exclusivamente en datos proporcionados por el frontend.

---

## RF-051 — Protección de datos

La aplicación deberá utilizar HTTPS en ambientes desplegados.

Los secretos no deberán almacenarse directamente en el repositorio.

Ejemplos:

```text
DATABASE_URL
JWT_SECRET
API_KEYS
```

deberán manejarse mediante variables de entorno o mecanismos equivalentes.

---

# 23. Requisitos no funcionales

## RNF-001 — Lenguaje

El backend deberá desarrollarse obligatoriamente en:

```text
Java
```

---

## RNF-002 — Arquitectura

El sistema deberá separar como mínimo:

```text
Frontend
Backend
Base de datos
```

El frontend no tendrá acceso directo a la base de datos.

---

## RNF-003 — Mantenibilidad

El código deberá:

* Mantener responsabilidades separadas.
* Evitar duplicación innecesaria.
* Mantener nombres descriptivos.
* Utilizar una estructura consistente.
* Contar con documentación técnica suficiente.

---

## RNF-004 — Seguridad

Las operaciones sensibles deberán validarse en el backend.

---

## RNF-005 — Consistencia monetaria

Los valores monetarios deberán conservar exactamente dos decimales.

---

## RNF-006 — Atomicidad

Las operaciones financieras deberán ser atómicas.

---

## RNF-007 — Idempotencia

Las operaciones financieras susceptibles a reintentos deberán poder procesarse de forma idempotente.

---

## RNF-008 — Concurrencia

El sistema deberá soportar operaciones concurrentes manteniendo la integridad de los saldos.

---

## RNF-009 — Pruebas

Las funcionalidades críticas deberán contar con pruebas automatizadas.

Como mínimo deberán cubrirse:

* Autenticación.
* Autorización.
* Creación de cuentas.
* Depósitos.
* Retiros.
* Transferencias.
* Idempotencia.
* Cierre de cuentas.
* Bloqueos.
* Concurrencia.

---

## RNF-010 — Reproducibilidad

Un desarrollador nuevo deberá poder levantar el proyecto siguiendo documentación del repositorio.

La configuración local deberá estar documentada.

---

# 24. Modelo conceptual de datos

El modelo conceptual inicial deberá incluir entidades equivalentes a:

```text
User
Account
Transaction / Movement
AuditEvent
Session
```

La estructura exacta de tablas, relaciones, claves, índices y restricciones será definida durante el diseño técnico.

Relación principal:

```text
User 1 ───── N Account

Account 1 ───── N Movement
```

Una transferencia podrá relacionar dos cuentas:

```text
Account ───── Transfer ───── Account
   origen                    destino
```

---

# 25. API

El sistema deberá exponer una API HTTP para la comunicación entre frontend y backend.

Los endpoints definitivos se establecerán durante el diseño técnico.

Conceptualmente deberá existir funcionalidad para:

```text
/auth
/users
/accounts
/movements
/deposits
/withdrawals
/transfers
/audit
```

La estructura exacta de rutas, métodos, DTOs y respuestas queda pendiente.

---

# 26. Validaciones

El backend deberá validar:

* Datos de entrada.
* Autenticación.
* Autorización.
* Propiedad de cuentas.
* Estado de las cuentas.
* Montos.
* Saldo disponible.
* Existencia de recursos.
* Límites.
* Idempotency Keys.
* Reglas de negocio.

Las validaciones del frontend deberán considerarse una ayuda para UX, no un mecanismo de seguridad.

---

# 27. Requisitos de pruebas

## 27.1 Autenticación

Deberá probarse:

* Registro.
* Login correcto.
* Login incorrecto.
* Tres intentos fallidos.
* Bloqueo temporal.
* Login durante bloqueo.
* Expiración del bloqueo.
* Logout.
* Sesiones concurrentes.

## 27.2 Cuentas

Deberá probarse:

* Creación.
* Límite de 5 cuentas activas.
* Creación después de cerrar una cuenta.
* Consulta.
* Bloqueo.
* Desbloqueo.
* Cierre.
* Intento de cierre con saldo distinto de cero.

## 27.3 Dinero

Deberá probarse:

* Depósito válido.
* Retiro válido.
* Saldo insuficiente.
* Monto inferior al mínimo.
* Monto superior al máximo.
* Precisión decimal.

## 27.4 Transferencias

Deberá probarse:

* Transferencia válida.
* Cuenta destino inexistente.
* Cuenta destino bloqueada.
* Cuenta destino cerrada.
* Saldo insuficiente.
* Transferencia hacia cuenta propia.
* Transferencia concurrente.
* Transferencia duplicada.
* Rollback ante errores.

## 27.5 Idempotencia

Deberá probarse:

* Misma clave enviada dos veces.
* Misma clave enviada concurrentemente.
* Misma clave con payload diferente.
* Reintento después de timeout.
* Recuperación del resultado anterior.

---

# 28. Despliegue

El proyecto deberá poder ejecutarse:

### Desarrollo

```text
Local
```

### Producción / demostración

```text
Servidor remoto
     |
Backend
     |
PostgreSQL / Supabase
```

La estrategia definitiva de despliegue queda pendiente del diseño técnico.

---

# 29. Variables de entorno

Los valores sensibles deberán configurarse mediante variables de entorno.

Ejemplo conceptual:

```env
DATABASE_URL=
DATABASE_USERNAME=
DATABASE_PASSWORD=
JWT_SECRET=
```

No deberán incluirse credenciales reales en Git.

El repositorio deberá incluir un archivo de ejemplo como:

```text
.env.example
```

sin secretos reales.

---

# 30. Control de versiones

El proyecto deberá utilizar Git.

La estructura del repositorio deberá permitir trabajar de manera colaborativa entre los tres integrantes.

Se recomienda establecer:

* Convención de commits.
* Ramas.
* Pull Requests.
* Code Review.
* Protección de la rama principal.

La estrategia concreta se definirá durante la organización técnica del proyecto.

---

# 31. Documentación

El repositorio deberá mantener documentación suficiente para comprender el proyecto.

Estructura propuesta:

```text
docs/
├── PRD.md
├── SRS.md
└── TECHNICAL-DESIGN.md
```

Posteriormente podrán agregarse:

```text
docs/
├── API.md
├── DATABASE.md
├── DEPLOYMENT.md
└── CONTRIBUTING.md
```

---

# 32. Calidad

El proyecto deberá priorizar:

* Correctitud.
* Seguridad.
* Integridad de datos.
* Mantenibilidad.
* Testabilidad.
* Claridad.
* Reproducibilidad.

El objetivo no será únicamente conseguir que la aplicación "funcione", sino construir un sistema que represente correctamente conceptos fundamentales de ingeniería de software backend.

---

# 33. Matriz de requisitos principales

| Requisito                      | Prioridad | MVP |
| ------------------------------ | --------- | --- |
| Registro                       | Alta      | Sí  |
| Login                          | Alta      | Sí  |
| Logout                         | Alta      | Sí  |
| Bloqueo por intentos           | Alta      | Sí  |
| Sesiones concurrentes          | Alta      | Sí  |
| Múltiples cuentas              | Alta      | Sí  |
| Máximo 5 cuentas activas       | Alta      | Sí  |
| Número de cuenta de 10 dígitos | Alta      | Sí  |
| Estados de cuenta              | Alta      | Sí  |
| Depósitos                      | Alta      | Sí  |
| Retiros                        | Alta      | Sí  |
| Transferencias                 | Alta      | Sí  |
| Idempotencia                   | Alta      | Sí  |
| Historial                      | Alta      | Sí  |
| Auditoría                      | Alta      | Sí  |
| Roles                          | Alta      | Sí  |
| Concurrencia                   | Alta      | Sí  |
| Precisión monetaria            | Alta      | Sí  |
| Cierre de cuenta               | Alta      | Sí  |
| Dashboard                      | Media     | Sí  |
| Tarjetas                       | Media     | No  |
| Pagos comerciales              | Media     | No  |
| Notificaciones                 | Baja      | No  |
| Redis                          | Baja      | No  |
| Microservicios                 | Baja      | No  |

---

# 34. Requisitos críticos

Los siguientes requisitos se consideran críticos para la integridad del sistema:

### RC-001 — Integridad monetaria

El sistema nunca deberá perder ni crear dinero ficticio debido a errores de concurrencia o procesamiento.

### RC-002 — Atomicidad

Una transferencia deberá ejecutarse completamente o no ejecutarse.

### RC-003 — Idempotencia

Una misma operación financiera no deberá procesarse dos veces debido a reintentos.

### RC-004 — Autorización

Un cliente no deberá poder operar sobre cuentas de otro cliente.

### RC-005 — Precisión

Las operaciones monetarias deberán utilizar precisión decimal.

### RC-006 — Auditoría

Las operaciones relevantes deberán poder rastrearse.

### RC-007 — Seguridad

Las credenciales y secretos deberán manejarse de forma segura.

---

# 35. Criterios de aceptación del MVP

El MVP podrá considerarse funcional cuando:

* Un cliente pueda registrarse.
* Un cliente pueda iniciar sesión.
* Las contraseñas estén almacenadas de forma segura.
* Exista bloqueo después de 3 intentos fallidos.
* Un cliente pueda tener hasta 5 cuentas activas.
* Las cuentas tengan estados correctamente definidos.
* Cada cuenta tenga un identificador interno y número público.
* Los números públicos tengan exactamente 10 dígitos.
* Se puedan realizar depósitos.
* Se puedan realizar retiros.
* Se valide saldo suficiente.
* Se respeten los límites monetarios.
* Se utilice `BigDecimal`.
* Se utilice `NUMERIC(15,2)` en PostgreSQL.
* Se puedan realizar transferencias internas.
* Las transferencias sean atómicas.
* Las operaciones duplicadas sean detectadas.
* Las operaciones concurrentes mantengan saldos correctos.
* Se registren movimientos.
* Se conserve el historial de cuentas cerradas.
* Las cuentas solamente puedan cerrarse con saldo cero.
* Exista separación entre Cliente, Empleado y Administrador.
* Los empleados y administradores puedan bloquear/desbloquear cuentas.
* Las acciones relevantes queden auditadas.
* El frontend no tenga acceso directo a PostgreSQL.
* Existan pruebas automatizadas para las operaciones críticas.
* El proyecto pueda ejecutarse localmente siguiendo la documentación.
* El proyecto pueda desplegarse en un entorno remoto.

---

# 36. Evolución futura

## Nivel 2 — Funcionalidades bancarias simuladas

Podrán incorporarse:

* Tarjetas virtuales.
* Pagos simulados a comercios.
* Beneficiarios.
* Comprobantes.
* Notificaciones por correo.
* Búsqueda avanzada.
* Exportación de movimientos.
* Límites diarios.

## Nivel 3 — Infraestructura avanzada

Podrán incorporarse:

* Redis.
* Message broker.
* Notificaciones asíncronas.
* Métricas.
* Monitoring.
* Alertas.
* Load testing.

## Nivel 4 — Arquitectura distribuida

Podrán evaluarse:

* Microservicios.
* API Gateway.
* Arquitectura orientada a eventos.
* Escalamiento horizontal.
* Pruebas de resiliencia.

Estas funcionalidades no forman parte del MVP.

---

# 37. Decisiones técnicas pendientes

Las siguientes decisiones deberán realizarse durante el **Technical Design**.

## TD-001 — Framework Java

Determinar el framework principal del backend.

Candidato inicial:

```text
Spring Boot
```

---

## TD-002 — Arquitectura

Definir la arquitectura interna del backend.

Candidato inicial:

```text
Layered Architecture
```

con separación conceptual de:

```text
Controller
Service
Repository
Domain / Entity
DTO
```

---

## TD-003 — Persistencia

Determinar la tecnología de acceso a PostgreSQL.

Opciones:

```text
JPA / Hibernate
JDBC
jOOQ
```

---

## TD-004 — Migraciones

Determinar herramienta de migraciones.

Candidatos:

```text
Flyway
Liquibase
```

---

## TD-005 — Autenticación

Determinar mecanismo de autenticación.

Opciones principales:

```text
JWT
Sesiones persistentes
```

---

## TD-006 — Sesiones

Determinar cómo se almacenarán y revocarán las sesiones.

Opciones:

```text
JWT stateless
PostgreSQL
Redis
```

---

## TD-007 — Concurrencia

Definir:

* Nivel de aislamiento.
* Estrategia de locking.
* Actualizaciones atómicas.
* Manejo de transacciones.
* Protección del saldo.

---

## TD-008 — Idempotencia

Definir:

* Almacenamiento de Idempotency Keys.
* Scope de unicidad.
* Estados de una operación.
* Comportamiento ante reintentos.
* Comportamiento ante solicitudes concurrentes.

---

## TD-009 — API

Definir:

* Endpoints.
* HTTP methods.
* DTOs.
* Respuestas.
* Errores.
* Paginación.
* Versionado.

---

## TD-010 — Testing

Definir:

* Unit tests.
* Integration tests.
* Repository tests.
* API tests.
* Concurrency tests.

---

## TD-011 — Contenedores

Determinar si el entorno utilizará:

```text
Docker
Docker Compose
```

y qué servicios serán contenerizados.

---

## TD-012 — CI/CD

Definir estrategia para:

* Build.
* Tests.
* Linting.
* Validaciones.
* Deploy.

---

## TD-013 — Hosting

Definir plataforma final para:

* Backend.
* Frontend.
* PostgreSQL/Supabase.

---


Este documento representa los requisitos funcionales y no funcionales establecidos para el MVP de BankCore.

Las decisiones de implementación concretas **no forman parte del SRS** y deberán documentarse en:

```text
docs/TECHNICAL-DESIGN.md
```

