# Roomies API

API Node.js/Express con PostgreSQL. Esta carpeta puede desplegarse como repositorio independiente en Render.

## Render

1. Crea un Web Service conectado a este repositorio.
2. Usa `npm install` como Build Command y `npm start` como Start Command.
3. En Environment configura:
   - `DATABASE_URL`: Internal Database URL del PostgreSQL creado en Render.
   - `JWT_SECRET`: secreto largo, aleatorio y privado.
   - `FRONTEND_URL`: dominio HTTPS de Vercel, sin barra final.
   - `UPLOADS_DIR` (opcional): directorio para servir fotos antiguas que se guardaron en disco antes de usar almacenamiento en PostgreSQL.
   - `NODE_ENV`: `production`.
4. Render asigna `PORT` automáticamente. No configures la URL interna de Postgres en el frontend.
5. Comprueba `/api/salud` en el dominio público del servicio.

Antes de registrar el primer usuario, conecta pgAdmin a PostgreSQL usando los datos External y ejecuta `schema.sql` en la base recién creada. No existen usuarios demo ni se necesita un archivo seed.

## Desarrollo local

Configura `DATABASE_URL` y `JWT_SECRET` en `.env` usando `.env.example` como referencia, instala dependencias con `npm install` e inicia con `npm start`. El backend local permite el origen `http://localhost:4300`.

El frontend se configura con el dominio público de este servicio en `src/environments/environment.prod.ts`, añadiendo `/api` al final. Después agrega el dominio de Vercel a `FRONTEND_URL` en Render y vuelve a desplegar el backend.

Las fotos nuevas se guardan en la tabla `fotos_perfil` de PostgreSQL (máximo 5 MB; JPG, PNG, WEBP o GIF), por lo que persisten después de cerrar sesión, reiniciar o desplegar el backend y se pueden mostrar en otras cuentas. `perfiles.foto_url` contiene la ruta del endpoint de imagen. El directorio `uploads/` se conserva únicamente para servir archivos de instalaciones anteriores; los archivos locales que ya se hayan perdido no se pueden recuperar y tendrán que volver a subirse.
