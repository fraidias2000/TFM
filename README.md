Requisitos
- Java JDK 21.
- Node.js y npm.
- Docker Desktop abierto.
- Maven

Paso 1 Iniciar PostgreSQL con docker
    Ir a la carpeta backend\HablaBarBackend y ejecutar: docker compose up -d db
        La base de datos estará disponible con estos datos de desarrollo
            Host	localhost
            Puerto	5432
            Base de datos	hablabar
            Usuario	hablabar
            Contraseña	hablabar_dev

Paso 2 Iniciar el backend
    Ir a la carpeta backend y ejecutar: mvn spring-boot:run
        Deja la terminal abierta. Comprueba el arranque accediendo a: http://localhost:8080/api/v1/health
    Para compilar, ejecutar las pruebas y generar el archivo JAR, con PostgreSQL iniciado: .\mvnw.cmd clean package

Paso 3 Iniciar frontend
    Abrir otra terminal en la carpeta de frontend y ejecutar:
        1º npm.cmd ci (primera vez que ejecutas para instalar dependencias)
        2º npm.cmd run dev -- --host 0.0.0.0
