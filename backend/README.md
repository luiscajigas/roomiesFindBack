# Roomies API

API Node.js/Express con PostgreSQL. Esta carpeta puede desplegarse como repositorio independiente en Render.

## Render

1. Crea un Web Service conectado a este repositorio.
2. Usa `npm install` como Build Command y `npm start` como Start Command.
3. En Environment configura:
   - `DATABASE_URL`: Internal Database URL del PostgreSQL creado en Render.
   - `JWT_SECRET`: secreto largo, aleatorio y privado.
   - `FRONTEND_URL`: dominio HTTPS de Vercel, sin barra final.
   - `UPLOADS_DIR` (opcional): directorio para fotos; en Render configúralo con la ruta de un Persistent Disk, por ejemplo `/var/data/roomies-uploads`.
   - `NODE_ENV`: `production`.
4. Render asigna `PORT` automáticamente. No configures la URL interna de Postgres en el frontend.
5. Comprueba `/api/salud` en el dominio público del servicio.

Antes de registrar el primer usuario, conecta pgAdmin a PostgreSQL usando los datos External y ejecuta `schema.sql` en la base recién creada. No existen usuarios demo ni se necesita un archivo seed.

## Desarrollo local

Configura `DATABASE_URL` y `JWT_SECRET` en `.env` usando `.env.example` como referencia, instala dependencias con `npm install` e inicia con `npm start`. El backend local permite el origen `http://localhost:4300`.

El frontend se configura con el dominio público de este servicio en `src/environments/environment.prod.ts`, añadiendo `/api` al final. Después agrega el dominio de Vercel a `FRONTEND_URL` en Render y vuelve a desplegar el backend.

Las fotos de perfil se suben desde el dispositivo y se guardan en el directorio local `uploads/` (máximo 5 MB; JPG, PNG, WEBP o GIF). En Render, el sistema de archivos normal no es persistente: adjunta un Persistent Disk al backend, configura `UPLOADS_DIR` con su punto de montaje y conserva ese disco para que las fotos sigan disponibles después de reiniciar o desplegar el servicio. La ruta de la foto se guarda en `perfiles.foto_url`.
