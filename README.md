# RoomiesFind: microservicios

Backend de RoomiesFind en un monorepositorio Maven (Java 21, Spring Boot). Cada responsabilidad vive en su propio microservicio y todos comparten una sola base de datos PostgreSQL, porque es una demo y no necesita escalabilidad estricta.

## La regla de comunicación

| Quién habla con quién | Protocolo | Dónde está |
|---|---|---|
| **Entre módulos** (microservicios y orquestador) | **gRPC sobre HTTP/2** | `contratos/src/main/proto`, archivos `*Api.java` |
| **Con APIs de terceros** (Claude, Gemini) | **API REST** | `servicios/ia/.../cliente/rest` |
| Navegador con el orquestador | REST/JSON y WebSocket | `infraestructura/.../rest`, `infraestructura/.../chat` |
| Eventos del chat entre instancias | Redis pub/sub | Solo dentro de `servicios/chat` |

Ningún microservicio expone HTTP y ningún módulo llama a otro por REST. El único código que usa un cliente REST está en `servicios/ia/.../cliente/rest`.

```text
                 Frontend Angular
                        │  REST/JSON
                        ▼
        ┌───────────────────────────────┐
        │  infraestructura (orquestador)│
        └───┬─────────┬─────────────┬───┘
            │ gRPC    │ gRPC        │ gRPC       ← HTTP/2
            ▼         ▼             ▼
       ┌─────────┐ ┌─────────┐ ┌────────────────┐  gRPC  ┌────┐
       │usuarios │ │perfiles │ │ compatibilidad ├───────►│ ia │
       └────┬────┘ └────┬────┘ └───┬────────┬───┘        └─┬──┘
            │           │          │  gRPC  │              │ REST
            ▲───────────┼──────────┼────────┘              ▼
            ▼           ▼          ▼                 Claude, Gemini
       ┌───────────────────────────────┐
       │     PostgreSQL (una sola)     │
       └───────────────────────────────┘
```

## Módulos

| Módulo | Responsabilidad | Tablas | Puerto gRPC |
|---|---|---|---|
| `contratos` | Archivos `.proto`: el contrato entre módulos | | |
| `comun` | Utilidades compartidas: servidor y canal gRPC, errores, conexión | | |
| `servicios/usuarios` | Cuentas, contraseñas y sesiones (JWT) | `usuarios` | 9091 |
| `servicios/perfiles` | Preferencias, barrios y fotos | `perfiles`, `zonas`, `fotos_perfil` | 9092 |
| `servicios/compatibilidad` | Historial de puntajes y explicaciones guardadas | `recomendaciones`, `explicaciones_ia` | 9093 |
| `servicios/ia` | Redacta explicaciones con Claude y Gemini de respaldo | | 9094 |
| `servicios/chat` | Conversaciones y mensajes en tiempo real, con Redis | `conversaciones`, `mensajes` | 9095 |
| `servicios/moderacion` | Bloqueos, reportes y alertas de cuentas muy reportadas | `bloqueos`, `reportes`, `alertas_cuenta` | 9096 |
| `infraestructura` | Orquestador: puerta de entrada, registro, tablas y Docker | crea el esquema | HTTP 3001 |

Cada servicio solo consulta sus propias tablas. Si necesita datos de otro, los pide por gRPC.

## Capas de un microservicio

```text
servicios/perfiles/src/main/java/com/roomiesfind/perfiles/
├── PerfilesApplication.java
├── api/PerfilesApi.java      ← la API: lo único expuesto, por gRPC
├── controller/               ← valida la entrada y coordina
├── service/                  ← reglas de negocio
├── repository/               ← acceso a sus tablas
├── model/                    ← filas de la base
└── dto/                      ← datos entre capas
```

Una petición entra por `*Api.java`, que traduce de protobuf a DTO, y sigue hacia `controller`, `service` y `repository`. La capa interna no conoce gRPC. `compatibilidad` agrega `cliente/` para llamar a otros servicios por gRPC, e `ia` agrega `cliente/rest/` para las APIs de terceros.

## Orquestador

`infraestructura` es lo único que ve el frontend. Conserva exactamente las rutas y respuestas del backend anterior, así que Angular no cambia.

Crear una cuenta toca dos servicios, así que ya no cabe en una sola transacción. `RegistroOrquestador` lo resuelve con compensación:

1. Pide a `perfiles` que valide los datos y la foto.
2. Pide a `usuarios` que cree la cuenta.
3. Pide a `perfiles` que cree el perfil. Si falla, pide a `usuarios` que elimine la cuenta.

También une los datos de `perfiles` y `usuarios` en una sola respuesta, valida las sesiones con `usuarios` y crea las tablas que falten al arrancar (`schema.sql`).

## Chat en tiempo real

```text
Navegador ⇄ WebSocket ⇄ orquestador ──gRPC──▶ servicios/chat ──▶ PostgreSQL
                             ▲                     │    ▲
                             └── gRPC streaming ◀──┘    └─ Redis pub/sub
```

1. El navegador abre `/ws/chat` y envía su token como primer mensaje, no en la URL.
2. El orquestador valida el token y abre un stream gRPC con `servicios/chat`.
3. Al enviar un mensaje, `servicios/chat` lo guarda en PostgreSQL y publica el evento en Redis.
4. Cada instancia del servicio recibe de Redis los eventos y los entrega a sus streams abiertos. Así el chat funciona igual con varias copias del servicio.

El orquestador no conoce Redis: solo habla con `servicios/chat` por gRPC. Si Redis se cae, los mensajes siguen guardándose y solo se pierde el aviso en vivo hasta que vuelva.

## Moderación

`servicios/moderacion` guarda los bloqueos y los reportes.

- **Bloquear** funciona en los dos sentidos: ninguna de las dos personas ve a la otra en las búsquedas ni en el chat. `servicios/chat` lo consulta por gRPC antes de entregar un mensaje, así que no se puede saltar desde el navegador. Al desbloquear, la conversación vuelve con su historial.
- **Reportar**: cada persona puede reportar a la misma cuenta una sola vez y máximo 10 cuentas al día.
- **Alerta**: con 3 reportes de personas distintas, la cuenta muestra un aviso en su tarjeta y en el chat, y quienes ya conversan con ella reciben un aviso en vivo. El aviso es prudente porque un reporte no prueba nada. No oculta la cuenta ni dice quién reportó.

## Búsqueda de compañeros

`servicios/perfiles` filtra, calcula el puntaje y entrega una página cada vez (`GET /api/perfiles/buscar`). Las reglas del puntaje están en SQL porque hay que conocerlo antes de traer a las personas para poder ordenar por él, y también en el Web Worker del frontend, que además explica las razones. Si cambian, hay que cambiarlas en los dos.

## Ejecutar

Con Docker no hace falta instalar Java:

```bash
cp .env.example .env
cd infraestructura
docker compose --env-file ../.env up --build
```

La API queda en `http://localhost:3001/api`, que es donde la busca el frontend en local. Por defecto usa un Postgres en Docker; para usar Neon, define `DATABASE_URL` en `.env`. Solo se publica el puerto del orquestador.

Sin Docker necesitas Java 21 y un PostgreSQL. Compila con `./mvnw package` y arranca cada `target/*.jar` con estas variables:

| Variable | Dónde | Para qué |
|---|---|---|
| `DATABASE_URL` | usuarios, perfiles, compatibilidad, orquestador | Base compartida |
| `JWT_SECRET` | usuarios | Firma de las sesiones; obligatoria con `NODE_ENV=production` |
| `FRONTEND_URL` | orquestador | Orígenes permitidos (CORS) |
| `ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL` | ia | API REST de Claude |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | ia | API REST de Gemini (respaldo) |
| `GRPC_USUARIOS`, `GRPC_PERFILES`, `GRPC_COMPATIBILIDAD`, `GRPC_IA` | clientes | Direcciones `host:puerto`; por defecto `localhost` |

## Desplegar en un servidor

En un servidor con Docker, con DATABASE_URL externa y DOMINIO definidos en `.env`:

```bash
cd infraestructura
docker compose --env-file ../.env -f docker-compose.yml -f docker-compose.produccion.yml up -d --build
```

Caddy publica la API por HTTPS en ese dominio y obtiene el certificado automáticamente. Solo se exponen los puertos 80 y 443.

## Pruebas

```bash
./mvnw verify
```

## Estado del despliegue

Los microservicios corren en una máquina virtual de Azure con Docker Compose, y la API se publica en `https://roomiesfind.eastus.cloudapp.azure.com/api`. La base de datos está en Neon y el frontend en Vercel.

Los procesos Java consumen unos 170 MB de memoria cada uno, y Redis muy poco. El servidor necesita al menos 2 GB de memoria.

## Si cambias la lista de barrios

Está en dos archivos que deben coincidir: `src/app/data/barrios-pasto.ts` (repositorio del frontend) y `servicios/perfiles/src/main/resources/barrios-pasto.txt`.
