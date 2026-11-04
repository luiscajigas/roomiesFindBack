# Roomies API

API Node.js/Express con PostgreSQL. Esta carpeta puede desplegarse como repositorio independiente en Render.

## Render

1. Crea un Web Service conectado a este repositorio.
2. Usa `npm install` como Build Command y `npm start` como Start Command.
3. En Environment configura:
   - `DATABASE_URL`: Internal Database URL del PostgreSQL creado en Render.
   - `JWT_SECRET`: secreto largo, aleatorio y privado.
   - `FRONTEND_URL`: dominio HTTPS de Vercel, sin barra final.
   - `ANTHROPIC_API_KEY`: clave de Claude para las explicaciones con IA (se crea en https://console.anthropic.com).
   - `ANTHROPIC_MODEL` (opcional): modelo de Claude; por defecto `claude-opus-5-5`.
   - `GEMINI_API_KEY` (opcional): clave de Gemini, usada como respaldo si Claude falla (gratis en https://aistudio.google.com/apikey).
   - `GEMINI_MODEL` (opcional): modelo de Gemini; por defecto `gemini-3.8-flash`.
   - `UPLOADS_DIR` (opcional): directorio para servir fotos antiguas que se guardaron en disco antes de usar almacenamiento en PostgreSQL.
   - `NODE_ENV`: `production`.
4. Render asigna `PORT` automáticamente. No configures la URL interna de Postgres en el frontend.
5. Comprueba `/api/salud` en el dominio público del servicio.

## Explicaciones con IA

`POST /api/ia/explicar` recibe el score y las listas de coincidencias y conflictos que el Web Worker del frontend ya calculó con reglas objetivas, y devuelve una explicación en lenguaje natural generada con Claude, o con Gemini como respaldo. La IA solo interpreta el resultado; nunca decide la compatibilidad.

Para minimizar llamadas al proveedor, si ya existe una explicación generada para la misma pareja con el mismo score, el endpoint devuelve la guardada en `explicaciones_ia` (`desdeCache: true`) sin volver a llamar a la IA. Si no hay ninguna clave configurada o fallan todos los proveedores, responde `503` con un mensaje claro y el registro queda en estado `pendiente`.

Si Claude falla después de sus reintentos, se usa Gemini. El cliente de Gemini reintenta con espera creciente cuando responde 429 (cuota) o 503 (alta demanda), y prueba un modelo de respaldo si el configurado ya no existe (404). Los modelos se pueden ajustar con `ANTHROPIC_MODEL` y `GEMINI_MODEL`.

El backend crea las tablas al iniciar: solo necesita una base vacía en `DATABASE_URL`. No hay que ejecutar `schema.sql` a mano, no existen usuarios demo ni se necesita un archivo seed.

## Desarrollo local

Configura `DATABASE_URL` y `JWT_SECRET` en `.env` usando `.env.example` como referencia, instala dependencias con `npm install` e inicia con `npm start`. El backend local permite el origen `http://localhost:4300`.

El frontend se configura con el dominio público de este servicio en `src/environments/environment.prod.ts`, añadiendo `/api` al final. Después agrega el dominio de Vercel a `FRONTEND_URL` en Render y vuelve a desplegar el backend.

Las fotos nuevas se guardan en la tabla `fotos_perfil` de PostgreSQL (máximo 5 MB; JPG, PNG, WEBP o GIF), por lo que persisten después de cerrar sesión, reiniciar o desplegar el backend y se pueden mostrar en otras cuentas. `perfiles.foto_url` contiene la ruta del endpoint de imagen. El directorio `uploads/` se conserva únicamente para servir archivos de instalaciones anteriores; los archivos locales que ya se hayan perdido no se pueden recuperar y tendrán que volver a subirse.
