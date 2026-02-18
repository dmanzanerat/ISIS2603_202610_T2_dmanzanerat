# Back del proyecto TallerPruebas

## Enlaces de interés

* [BookstoreBack](https://github.com/Uniandes-isis2603/bookstore-back) -> Repositorio de referencia para el Back

* [Jenkins](http://157.253.238.75:8080/jenkins-isis2603/) -> Autentíquese con sus credencias de GitHub
* [SonarQube](http://157.253.238.75:8080/sonar-isis2603/) -> No requiere autenticación

## Regla 0
| **Escenario**                 | **Estado Inicial (BD)**                     | **Acción (Input)**                                | **Resultado Esperado (Output/BD)**                                                                        |
| ----------------------------- | ------------------------------------------- | ------------------------------------------------- | --------------------------------------------------------------------------------------------------------- |
| **Éxito: Crear un bolsillo**  | Existe Cuenta #123 (Saldo: 1000)            | Crear Bolsillo "Ahorro" (Monto: 0) en Cuenta #123 | 1. Se crea el objeto Bolsillo.<br><br>2. Se guarda en la BD.<br><br>3. Retorna la entidad creada.         |
| **Fallo: Cuenta Inexistente** | No existe la cuenta #999                    | Crear Bolsillo en Cuenta #999                     | Lanza Excepción:<br><br>EntityNotFoundException.                                                          |
| **Fallo: Cuenta Bloqueada**   | Existe Cuenta #123 con estado “BLOQUEADA”   | Crear Bolsillo "Ahorro" en Cuenta #123            | Lanza Excepción:<br><br>BusinessLogicException<br><br>("La cuenta está bloqueada"). No guarda nada nuevo. |
| **Fallo: Duplicado**          | Existe Cuenta #123 con un Bolsillo "Ahorro" | Crear Bolsillo "Ahorro" en Cuenta #123            | Lanza Excepción: BusinessLogicException ("El nombre ya existe").<br><br>No guarda nada nuevo.             |


## Regla 1
| Escenario*                                                              | Estado inicial (BD)                                           | Accion (input)                        | Resultado Esperado (Output/BD)                                                                                                                                        |
|-------------------------------------------------------------------------|---------------------------------------------------------------|---------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Éxito: Saldo de la cuenta es igual o superior al monto de transferencia | Existe una cuenta #123 con saldo 5000 y un bolsillo “Ahorros” | Transferir 1500 al bolsillo “Ahorros” | 1. Se resta 1500 al saldo de la cuenta <br> 2.Se suma 1500 al saldo de bolsillo <br> 3. Se suma 1500 al saldo de bolsillo <br> 4. Retorna el monto total del bolsillo 
| Fallo: Saldo es menor al monto de la transferencia                      | Existe una cuenta #123 con saldo 5000 y un bolsillo “Ahorros”| Transferir 6000 al bolsillo “Ahorros"| Lanza excepción: BusinessLogicException (“El monto de la transferencia debe ser menor o igual al saldo de la cuenta”). No se hacen cambios en la BD.                  |
| Fallo: Bolsillo no existe                                               |Existe una cuenta #456 sin ningún bolsillo	Transferir 1500 al bolsillo “Ahorros” |Transferir 1500 al bolsillo “Ahorros” | Lanza excepción: EntityNotFoundException                                                                                                                              |

## Regla 2
| **Escenario**                                                          | **Estado Inicial (BD)**                                                 | **Acción (Input)**                                 | **Resultado Esperado (Output/BD)**                                                                                                                                                          |
| ---------------------------------------------------------------------- | ----------------------------------------------------------------------- | -------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Éxito: Ambas cuentas existen y son distintas                           | Existen la cuenta #123 con saldo 5000 y la cuenta #456 con saldo 1000   | Transferir 1500 de la cuenta #123 a la cuenta #456 | 1. Resta 1500 al saldo de la cuenta #123<br><br>2. Suma 1500 al saldo de la cuenta #456<br><br>3. Guardo los cambios en la BD<br><br>4. Retorna el monto total restante de la cuenta origen |
| Fallo: Saldo de la cuenta origen es menor al monto de la transferencia | Existe la cuenta #123 con saldo 5000 y la cuenta #456 con el saldo 1000 | Transferir 6000 de la cuenta #123 a la cuenta #456 | Lanza excepción: BusinessLogicException (“El monto de la transferencia debe ser menor o igual al saldo de la cuenta”)                                                                       |
| Fallo: Cuenta origen no existe                                         | No existe la cuenta #123 y existe la cuenta #456                        | Transferir 1000 de la cuenta #123 a la cuenta #456 | Lanza excepción: EntityNotFoundException                                                                                                                                                    |
| Fallo: Cuenta destino no existe                                        | Existe la cuenta #123 y no existe la cuenta #456                        | Transferir 1000 de la cuenta #123 a la cuenta #456 | Lanza excepción: EntityNotFoundException                                                                                                                                                    |
| Fallo: Cuenta origen y destino son la misma                            | Existe la cuenta #123                                                   | Transferir 1000 de la cuenta #123 a la cuenta #123 | Lanza excepción: BusinessLogicException (“La cuenta destino debe ser diferente a la cuenta origen”)                                                                                         |