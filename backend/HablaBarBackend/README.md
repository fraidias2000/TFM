# HablaBar Backend

Esqueleto inicial del backend de HablaBar. Se plantea como **monolito modular**: una sola aplicación Spring Boot desplegable como unidad, organizada en módulos funcionales para mantener separadas sus responsabilidades.

## Tecnologías

- Java 21 y Spring Boot 4.1.2.
- Spring MVC para la API REST y WebSocket para las comunicaciones en tiempo real.
- Spring Data JPA y PostgreSQL.
- Spring Security para proteger la API; la autenticación JWT se añadirá en el módulo `auth`.

## Módulos

- `auth`: registro, inicio de sesión y validación de la identidad.
- `users`: perfil, alias y preferencias de avisos.
- `chats`: creación, consulta, capacidad y caducidad de conversaciones.
- `participants`: altas y bajas de participantes y autorización de acceso.
- `messages`: persistencia y consulta de mensajes.
- `location`: recepción y validación de ubicaciones temporales y cálculo de distancias.
- `notifications`: avisos en tiempo real a usuarios conectados.
- `shared`: configuración común, salud de servicio y errores transversales.

El paquete principal es `es.hablabar`. Cada módulo contiene directamente sus controladores, servicios, repositorios, entidades y DTO cuando se implementen. Por ejemplo, `chats` contendrá `ChatController`, `ChatService`, `ChatRepository`, `Chat`, `CreateChatRequest` y `ChatResponse`, sin subcarpetas de capas. Un módulo debe acceder a otro mediante sus servicios públicos, evitando compartir repositorios entre módulos.

## Primer arranque

1. Necesitas Java 21 y PostgreSQL (puedes iniciarlo con Docker Desktop). Para ejecutar Maven, instala Maven o copia a esta carpeta los archivos `mvnw`, `mvnw.cmd` y la carpeta `.mvn` del ZIP que descargaste de Spring Initializr. El wrapper no sustituye a Java.
2. Si quieres cambiar las credenciales de PostgreSQL, copia `.env.example` como `.env`; Docker Compose leerá ese archivo. No lo subas al repositorio. Spring Boot no carga `.env` automáticamente, así que conserva los valores por defecto de `application.properties` o configura las variables de entorno también en la terminal de VS Code.
3. Inicia PostgreSQL desde esta carpeta:

   ```powershell
   docker compose up -d db
   ```

4. Ejecuta el backend desde esta carpeta. Si ya tienes el wrapper del ZIP de Initializr:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

   Si tienes Maven instalado:

   ```powershell
   mvn spring-boot:run
   ```

5. Comprueba `http://localhost:8080/api/v1/health`. Debe responder con el estado `UP`.

Para ejecutar las pruebas de contexto, PostgreSQL debe estar iniciado. La configuración JPA `update` es solo de desarrollo local; antes de un despliegue se sustituirá por migraciones versionadas.

## Integración con el frontend

El backend escucha en el puerto `8080`. CORS permite el frontend local de Vite en `http://localhost:8443` y `http://127.0.0.1:8443`. Si Vite utiliza otro origen, cambia `CORS_ALLOWED_ORIGINS`.

## Alcance de este esqueleto

Incluye la estructura modular, la configuración de desarrollo, un endpoint de salud y un test de arranque. Los endpoints de negocio, entidades y reglas de proximidad se implementarán de forma incremental. Los endpoints protegidos requieren autenticación; se habilitarán al completar el módulo `auth` y no se han dejado rutas funcionales abiertas.
