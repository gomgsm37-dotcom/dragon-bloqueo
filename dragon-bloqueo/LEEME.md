# Dragon G.S.M – Ventas en cuotas con bloqueo de equipos

El sistema tiene dos partes:

- **Panel** (web, se usa desde el celular o la PC): cargás la venta con el IMEI, las cuotas y los pagos. Desde ahí bloqueás, desbloqueás o liberás el equipo.
- **App "Dragon Protección"** (Android): se instala en el celular que vendés, antes de entregarlo. Cada 15 minutos le pregunta al panel cómo tiene que estar ese IMEI.

Si una cuota vence y pasan los días de gracia sin que registres el pago, **el celular se bloquea solo**, aunque no tengas el panel abierto. Cuando registrás el pago, se desbloquea.

Con el equipo bloqueado, el cliente solo puede llamar a emergencias (911) y al local. Todas las demás apps quedan suspendidas.

---

## Paso 1 – Firebase (la base de datos, gratis)

1. Entrá a **console.firebase.google.com** con tu cuenta de Google.
2. **Crear un proyecto** → nombre `dragon-bloqueo` → podés desactivar Google Analytics → Crear.
3. Menú de la izquierda → **Compilación → Realtime Database → Crear base de datos**.
   - Ubicación: Estados Unidos. Modo: **bloqueado**.
   - Pestaña **Reglas**: borrá lo que hay, pegá el contenido del archivo `firebase-reglas.json` y tocá **Publicar**.
   - Copiá la dirección que aparece arriba (algo como `https://dragon-bloqueo-default-rtdb.firebaseio.com`).
4. Menú → **Compilación → Authentication → Comenzar → Correo electrónico/contraseña → Habilitar → Guardar**.
   - Pestaña **Usuarios → Agregar usuario**: tu email y una contraseña. Con ese usuario vas a entrar al panel. Podés crear otros usuarios para Lorena, Mariana, Dragon y Gabriel.
5. Tocá el engranaje ⚙ → **Configuración del proyecto**. Copiá la **Clave de API web** (empieza con `AIza`).
   - Si no aparece, en esa misma pantalla tocá el ícono `</>` (agregar app web), poné cualquier nombre y registrala. Después vas a ver la clave.

## Paso 2 – GitHub (arma la app y publica el panel, gratis)

1. Creá una cuenta en **github.com**.
2. Arriba a la derecha **+ → New repository**. Nombre: `dragon-bloqueo`. Dejalo en **Public**. Tocá **Create repository**.
3. Tocá **uploading an existing file**. Descomprimí el zip en la PC y **arrastrá todo lo que hay adentro de la carpeta** (incluida la carpeta `.github`). Tocá **Commit changes**.
   - Si la carpeta `.github` no se subió: **Add file → Create new file**, escribí como nombre `.github/workflows/build.yml`, pegá el contenido de ese archivo y guardá.
4. **Settings → Pages → Source: GitHub Actions**.
5. Pestaña **Actions** → "Compilar app y publicar panel" → **Run workflow**. Tarda unos 5 minutos. Cuando termina sale un tilde verde ✔.
   - Si la primera vez falló el último paso (deploy), es porque Pages no estaba activado todavía. Volvé a correrlo.
6. Tu panel queda en: **https://TU-USUARIO.github.io/dragon-bloqueo/**
   - Guardalo en la pantalla de inicio del celular y en el escritorio de la PC.

## Paso 3 – Entrar al panel

1. Abrí el link del panel.
2. La primera vez te pide la **Clave de API web** y la **dirección de Realtime Database** (del Paso 1).
3. Entrá con el email y la contraseña que creaste.
4. En **Ajustes**, revisá los nombres y teléfonos de los 3 locales.

## Paso 4 – Instalar la protección en un celular

> **Primero probalo con un celular tuyo de prueba.**

1. Reseteá el celular de fábrica. Tiene que quedar en la pantalla de bienvenida.
2. En el panel: **Instalar → Generar código QR**. Si querés, cargá el Wi-Fi del local así se conecta solo.
3. En la pantalla de bienvenida del celular, **tocá 6 veces seguidas** en un lugar vacío. Se abre el lector de QR.
4. Escaneá el QR. El celular descarga la app y se configura como "equipo administrado".
5. Terminá la configuración con la cuenta de Google del cliente.
6. Abrí la app **Dragon Protección** y fijate que diga **"Protección activa: Sí"**. Ahí también ves el IMEI.
7. En el panel: **Nueva venta**, cargá los datos con ese IMEI.
8. Prueba: tocá **Bloquear ahora**. En hasta 15 minutos el celular se bloquea (o reiniciá el celular para que revise al toque). Después tocá **Desbloquear**. Desde la pantalla de bloqueo, el botón "Ya pagué – verificar" lo revisa en el momento.

## Uso diario

- **Registrar pago**: abrís la venta → monto → Registrar. Si queda al día, se desbloquea solo.
- **Bloquear ahora**: bloquea aunque no esté vencida.
- **Desbloquear**: si la cuota sigue vencida, te pide hasta qué fecha le das plazo. Pasada esa fecha se vuelve a bloquear solo.
- **WhatsApp recordatorio**: abre WhatsApp con el mensaje listo.
- **Liberar equipo**: cuando termina de pagar. Se sacan todas las restricciones y la app se puede desinstalar. **No se puede volver atrás.**
- **Bloquear si no se conecta en X días** (en Ajustes): si el cliente deja el celular sin internet para esquivar el bloqueo, se bloquea igual pasado ese tiempo.

## Qué hace y qué no hace

- Mientras se paga: no deja resetear el celular desde Ajustes, no deja desinstalar la app, no deja instalar apps fuera de Play Store, no deja cambiar la fecha y hora y no deja activar la depuración USB.
- Funciona en **Android 8 o más nuevo**. **No funciona en iPhone.**
- El bloqueo no es inmediato: el celular revisa cada 15 minutos.

## Protección anti-recovery (FRP – el "hoja verde")

Para que no se borre reseteando por recovery (con los botones), el sistema graba en el equipo una **cuenta de Google tuya como "llave maestra"**. Si el cliente resetea por recovery, el celular arranca pidiendo esa cuenta y **no se puede usar sin ella**. Es lo mismo que pasa con un celular robado que estaba vinculado a otra cuenta.

- La cuenta configurada es **gomgsm37@gmail.com**. La podés cambiar en **Ajustes → Protección anti-recovery**.
- El cliente **no usa esa cuenta** en el día a día: usa la suya normal. La llave maestra solo aparece después de un recovery.
- Al **liberar** el equipo (cuando termina de pagar), la llave se quita sola y el celular queda 100% del cliente.
- Importante: esa cuenta tiene que existir y vos tenés que saber su contraseña. Si la perdés, no podés destrabar un equipo reseteado. Anotala bien.
- Requiere Android 11 o más nuevo. En equipos más viejos la app igual frena el reseteo desde Ajustes, pero no el recovery.
- Aun así, flashear el equipo con una PC (box de reparación) puede saltear el FRP en algunos modelos. Es el límite real: ninguna protección por software es 100%.

## Televisores (Android TV / Google TV)

El mismo sistema sirve para vender **televisores en cuotas**, pero solo en teles con **Android TV o Google TV** (Philips, Sony, TCL, Hisense, el Chromecast con Google TV, etc.). En **Samsung (Tizen) y LG (webOS) no funciona**.

Diferencias con el celular:
- Las teles **no tienen IMEI**. La app les da un "ID del equipo". Al cargar la venta, marcás la casilla **"Es un televisor / tablet sin chip"** y pegás ese ID.
- **No se instala con QR** (la tele no es táctil). Se instala con una PC por cable USB (ADB) o con el método de cada marca. Es un paso más técnico; si querés te paso los comandos cuando tengas una tele para probar.
- Para ver el ID: después de instalar, abrís la app **Dragon Protección** en la tele y copiás el "ID del equipo".
- El bloqueo, los pagos y el FRP funcionan igual. La tele casi siempre está enchufada con wifi, así que el bloqueo es muy efectivo.

## Contrato

Hacé firmar al cliente una cláusula como esta (consultala con un abogado o contador):

> "El comprador acepta que el equipo IMEI ______ tiene instalado un sistema de protección que puede restringir su uso si una cuota no se abona dentro de los ___ días de su vencimiento. Mientras esté restringido podrá realizar llamadas de emergencia. El uso se restablecerá al regularizar el pago. Al cancelar el total, el vendedor liberará el equipo y quitará el sistema."

## Archivos

- `panel/index.html` → el panel.
- `app/` → código de la app Android.
- `.github/workflows/build.yml` → arma la app y publica el panel en GitHub.
- `firebase-reglas.json` → reglas de seguridad para Firebase.
- `app/dragon.keystore` → firma de la app. **No lo borres**: si cambia, los celulares ya instalados no aceptan actualizaciones.
