# Operador y administrador

| Operación | USER | OPERADOR | ADMIN |
| --- | --- | --- | --- |
| Consultar y comparar | Sí | Sí | Sí |
| Agregar/modificar cortes y locales | No | No | Sí |
| Dar de baja cortes y locales | No | No | Sí |
| Cargar precios nuevos | No | Sí | No |
| Editar/eliminar precios publicados | No | No | No |

La gestión está en /gestion.html, separada del comparador público. ADMIN gestiona cortes y locales con bajas lógicas. OPERADOR solo ve el apartado de carga de precios; una nueva publicación conserva los valores anteriores. El administrador puede consultar ese historial. No se cambian roles ni contraseñas existentes y no hace falta migrar el esquema.

## Crear una cuenta de operador en IntelliJ

1. Abrí Run → Edit Configurations y elegí la configuración que realmente ejecutás (Application/Spring Boot o Gradle bootRun).
2. Abrí el editor de Environment variables y creá estas filas:

| Nombre | Valor que debés ingresar |
| --- | --- |
| ASADITO_OPERADOR_EMAIL | Un email nuevo para el operador |
| ASADITO_OPERADOR_PASSWORD | Una contraseña propia de al menos 10 caracteres y máximo 72 bytes UTF-8 |

Usá valores sin comillas. No pegues los textos explicativos de esta tabla como valores. No guardes contraseñas en configuraciones compartidas o versionadas. El email debe ser distinto del ADMIN y no estar registrado como USER.

3. Guardá con Apply, detené la ejecución anterior y reiniciá.
4. Abrí `/gestion.html` en la dirección de tu aplicación e ingresá con esa cuenta. La página pública `/` queda dedicada al comparador, calculadora y reportes. Aparecerá “Panel de operador”, con el apartado Precios y el botón Publicar precio.
5. Retirá ambas variables luego de la creación y reiniciá; la cuenta queda persistida con BCrypt.

Para una nueva cuenta ADMIN el procedimiento es el mismo con ASADITO_ADMIN_EMAIL y ASADITO_ADMIN_PASSWORD. Al ingresar como ADMIN aparece “Administrar catálogo”, con apartados Cortes y Locales para agregar, editar y dar de baja; Precios es de consulta.

## Si la cuenta ya existe

Las variables crean cuentas: no cambian contraseñas ni roles existentes. Si un email ya pertenece a otro rol, el arranque informa el conflicto. Elegí otro email para crear la cuenta de operador. No se convierte automáticamente un USER en OPERADOR ni se reemplaza un ADMIN.

Para cambiar una contraseña existente hace falta un procedimiento de recuperación controlado; cambiar la variable y reiniciar no la restablece. No almacenés contraseñas en texto plano.

## Seguridad y prueba manual

Spring Security valida método y ruta, independientemente de los botones:

- POST/PUT/DELETE /api/admin/productos y /api/admin/locales (con ID para modificar/eliminar): solo ADMIN.
- POST /api/admin/precios: solo OPERADOR.
- PUT/DELETE /api/admin/precios/{id}: denegados para todos los roles.
- Toda escritura necesita CSRF. Un USER no puede realizar ninguna operación de gestión.

Probá un ADMIN creando, editando y dando de baja un corte/local. Ingresá como OPERADOR: solo debe aparecer Precios con Publicar precio. Intentar crear cortes/locales como OPERADOR, publicar precios como ADMIN o modificar/eliminar precios con cualquiera de los dos roles debe devolver 403.

## Apartados

- /gestion.html#productos: ADMIN carga y mantiene cortes.
- /gestion.html#locales: ADMIN carga comercios, direcciones y coordenadas.
- /gestion.html#precios: OPERADOR carga valores por kilo y fecha para cortes/locales existentes. ADMIN consulta el historial sin acciones de escritura.

El acceso directo a otro apartado no otorga permisos. Para el operador se mantiene el apartado Precios aunque se intente abrir un hash de cortes/locales. Las búsquedas y filtros no modifican registros. El administrador debe crear un corte y un local antes de que el operador pueda publicar precios. Dar de baja un corte/local oculta sus precios de las consultas públicas y preserva sus registros históricos.
