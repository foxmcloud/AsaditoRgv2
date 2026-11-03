# Operador y administrador

| Operación sobre cortes, locales y precios | USER | OPERADOR | ADMIN |
| --- | --- | --- | --- |
| Consultar y comparar | Sí | Sí | Sí |
| Crear | No | Sí | No |
| Modificar | No | Sí | No |
| Dar de baja lógicamente | No | No | Sí |

La eliminación conserva los registros en la base. No se concede al administrador acceso a las altas o modificaciones. Las cuentas existentes ADMIN conservan su rol, pero pasan a tener únicamente permiso de baja sobre el catálogo.

La gestión tiene una página independiente: `/gestion.html`. No hay formularios de alta/edición/baja en la página pública, incluso cuando navega un operador o administrador. El enlace de gestión aparece al iniciar sesión con esos roles. También se puede abrir la dirección directamente para iniciar sesión; un USER recibe un aviso sin acceso a los controles.

## Crear una cuenta de operador en IntelliJ

1. Abrí Run → Edit Configurations y elegí la configuración que realmente ejecutás (Application/Spring Boot o Gradle bootRun).
2. Abrí el editor de Environment variables y creá estas filas:

| Nombre | Valor que debés ingresar |
| --- | --- |
| ASADITO_OPERADOR_EMAIL | Un email nuevo para el operador |
| ASADITO_OPERADOR_PASSWORD | Una contraseña propia de al menos 10 caracteres y máximo 72 bytes UTF-8 |

Usá valores sin comillas. No pegues los textos explicativos de esta tabla como valores. No guardes contraseñas en configuraciones compartidas o versionadas. El email debe ser distinto del ADMIN y no estar registrado como USER.

3. Guardá con Apply, detené la ejecución anterior y reiniciá.
4. Abrí `/gestion.html` en la dirección de tu aplicación e ingresá con esa cuenta. La página pública `/` queda dedicada al comparador, calculadora y reportes. Aparecerá “Panel de operador”, con un resumen, selector de entidad, botón Nuevo registro y botones Editar.
5. Retirá ambas variables luego de la creación y reiniciá; la cuenta queda persistida con BCrypt.

Para una nueva cuenta ADMIN el procedimiento es el mismo con ASADITO_ADMIN_EMAIL y ASADITO_ADMIN_PASSWORD. Al ingresar como ADMIN aparece “Administrar bajas”, con acciones Dar de baja y confirmación. No tiene Nuevo registro ni Editar.

## Si la cuenta ya existe

Las variables crean cuentas: no cambian contraseñas ni roles existentes. Si un email ya pertenece a otro rol, el arranque informa el conflicto. Elegí otro email para crear la cuenta de operador. No se convierte automáticamente un USER en OPERADOR ni se reemplaza un ADMIN.

Para cambiar una contraseña existente hace falta un procedimiento de recuperación controlado; cambiar la variable y reiniciar no la restablece. No almacenés contraseñas en texto plano.

## Seguridad y prueba manual

El frontend consulta /api/auth/me, que devuelve authenticated, email, admin y operador. Los botones responden a esos permisos, pero el backend impone la misma regla:

- POST /api/admin/productos, /locales, /precios: solo OPERADOR.
- PUT /api/admin/productos/{id}, /locales/{id}, /precios/{id}: solo OPERADOR.
- DELETE sobre esas entidades: solo ADMIN.
- Toda escritura sigue necesitando CSRF.

Probá ambas cuentas en sesiones separadas. Crear/editar como ADMIN y eliminar como OPERADOR debe devolver 403, aunque se fabrique la solicitud manualmente. Un usuario común no puede realizar ninguna de estas operaciones. Los dos roles de gestión siguen pudiendo utilizar el comparador, calculadora y PDF.
