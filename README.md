# productos-app

API REST para gestionar productos, construida con Spring Boot y PostgreSQL. Expone un CRUD sobre la tabla `productos` de la base `trigg_db` y lo documenta con Swagger.

## Tecnologías

- Java 21
- Spring Boot 4.1.2 (Web MVC, Data JPA, Validation)
- PostgreSQL
- springdoc-openapi 3.2.0 (Swagger UI)
- Lombok
- Maven Wrapper (no hace falta tener Maven instalado)
- Tests: JUnit 5, Mockito, MockMvc, H2 (base en memoria)
- CI: GitHub Actions

## Requisitos

- JDK 21
- PostgreSQL corriendo en `localhost:5432` con:
  - base de datos: `trigg_db`
  - usuario: `postgres`
  - tabla `productos` ya creada (ver [Base de datos](#base-de-datos))

## Ejecutar

Windows:

```
.\mvnw.cmd spring-boot:run
```

Linux / macOS:

```
./mvnw spring-boot:run
```

La aplicación queda en `http://localhost:8082`.

Para generar el `.jar` y ejecutarlo:

```
.\mvnw.cmd -DskipTests package
java -jar target/productos-app-1.0.jar
```

## Swagger

- Swagger UI: http://localhost:8082/swagger-ui.html
- OpenAPI (JSON): http://localhost:8082/v3/api-docs

Desde Swagger UI se pueden probar todos los endpoints.

## Endpoints

| Método | Ruta                     | Descripción                                  |
|--------|--------------------------|----------------------------------------------|
| GET    | `/api/productos`         | Lista productos. Filtro opcional `?nombre=`  |
| GET    | `/api/productos/{id}`    | Obtiene un producto                          |
| POST   | `/api/productos`         | Crea un producto (responde 201)              |
| PUT    | `/api/productos/{id}`    | Actualiza un producto                        |
| DELETE | `/api/productos/{id}`    | Elimina un producto (responde 204)           |

Ejemplo de cuerpo para `POST` / `PUT`:

```json
{
  "nombre": "Mouse",
  "descripcion": "Inalámbrico",
  "precio": 15990,
  "stock": 10
}
```

Validaciones:

- `nombre`: obligatorio, máximo 100 caracteres
- `descripcion`: opcional, máximo 255 caracteres
- `precio`: obligatorio, mayor o igual a 0
- `stock`: obligatorio, mayor o igual a 0

### Errores

Los errores se devuelven en formato `ProblemDetail` (RFC 9457).

Producto inexistente (404):

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "Producto 999 no encontrado",
  "instance": "/api/productos/999"
}
```

Datos inválidos (400):

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Datos inválidos",
  "errores": {
    "nombre": "El nombre es obligatorio"
  }
}
```

## Estructura

```
src/main/java/com/app/productos/
├── ProductosApplication.java
├── config/OpenApiConfig.java          # Información de la API en Swagger
├── controller/ProductoController.java # Endpoints REST
├── exception/                         # Manejo de errores (404 y 400)
├── model/Producto.java                # Entidad JPA -> tabla productos
├── repository/ProductoRepository.java # Acceso a datos
└── service/ProductoService.java       # Lógica de negocio
src/main/resources/application.properties  # Puerto, conexión y Swagger

src/test/java/com/app/productos/
├── ProductosApplicationTests.java              # Arranque del contexto
├── ProductoIntegrationTest.java                # Integración completa (H2)
├── controller/ProductoControllerTest.java      # Capa web (Mockito)
├── repository/ProductoRepositoryTest.java      # Capa de datos (H2)
└── service/ProductoServiceTest.java            # Capa de negocio (Mockito)
src/test/resources/application-test.properties  # Perfil test: H2 en memoria

.github/workflows/ci.yml                        # CI: corre los tests en GitHub
```

## Base de datos

La aplicación usa `spring.jpa.hibernate.ddl-auto=none`, es decir, **no crea ni modifica tablas**. Esto es intencional: en `trigg_db` la tabla `productos` ya existe y la usan otros objetos (por ejemplo, la vista `productos_ventas_view` y triggers), así que Hibernate no debe alterar su estructura.

La tabla necesita al menos estas columnas:

```sql
CREATE TABLE productos (
    id          BIGSERIAL PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    descripcion VARCHAR(255),
    precio      NUMERIC      NOT NULL,
    stock       INTEGER      NOT NULL
);
```

Para cambiar los datos de conexión, edita `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/trigg_db
spring.datasource.username=postgres
```

## Tests

Los tests **no necesitan PostgreSQL**: usan una base H2 en memoria (modo compatible con PostgreSQL), configurada en el perfil `test` ([src/test/resources/application-test.properties](src/test/resources/application-test.properties)). En ese perfil Hibernate sí crea la tabla a partir de la entidad (`spring.jpa.hibernate.ddl-auto=create-drop`).

Ejecutar todos los tests:

```
.\mvnw.cmd test
```

Ejecutar solo una clase:

```
.\mvnw.cmd test -Dtest=ProductoServiceTest
```

### Estrategia: un test por capa

```
HTTP ─▶ Controller ─▶ Service ─▶ Repository ─▶ Base de datos
        └─ WebMvcTest ┘  └─ Mockito ┘  └─ DataJpaTest ┘
        └──────────── SpringBootTest (integración) ────────────┘
```

Cada capa se prueba aislada, reemplazando la capa de abajo por un **mock de Mockito**. Un mock es un objeto falso: con `when(...).thenReturn(...)` se define qué responde y con `verify(...)` se comprueba cómo fue llamado. Así cada test valida solo la lógica de su capa y corre rápido.

| Capa | Clase de test | Tipo | Qué se levanta | Qué se mockea |
|------|---------------|------|----------------|---------------|
| Service | `service/ProductoServiceTest` | Unitario puro | Nada de Spring (`@ExtendWith(MockitoExtension.class)`) | `ProductoRepository` con `@Mock` |
| Controller | `controller/ProductoControllerTest` | Slice web | Solo MVC: controller, validación, `GlobalExceptionHandler` (`@WebMvcTest`) | `ProductoService` con `@MockitoBean` |
| Repository | `repository/ProductoRepositoryTest` | Slice JPA | Solo JPA + H2 (`@DataJpaTest`) | Nada (usa base real en memoria) |
| Todas | `ProductoIntegrationTest` | Integración | Aplicación completa + H2 (`@SpringBootTest` + `@AutoConfigureMockMvc`) | Nada |

#### 1. Service — `ProductoServiceTest` (unitario con Mockito)

- `@Mock ProductoRepository repository`: repositorio falso.
- `@InjectMocks ProductoService service`: servicio real que recibe el mock por constructor.
- Casos probados:
  - `listar` usa `findAll()` sin filtro (o filtro en blanco) y `findByNombreContainingIgnoreCase` con filtro.
  - `obtener` devuelve el producto o lanza `RecursoNoEncontradoException`.
  - `crear` pone `id = null` antes de guardar (se verifica con `ArgumentCaptor`).
  - `actualizar` copia los campos y guarda; si no existe, **no** llama a `save` (`verify(repository, never())`).
  - `eliminar` borra el producto; si no existe, **no** llama a `delete`.

#### 2. Controller — `ProductoControllerTest` (`@WebMvcTest` + Mockito)

- `@MockitoBean ProductoService service`: Spring registra un mock de Mockito en lugar del servicio real.
- `MockMvc` simula peticiones HTTP sin levantar un servidor.
- Casos probados: códigos HTTP (200, 201, 204, 400, 404), el JSON de respuesta (`jsonPath`), el formato `ProblemDetail` de los errores, y que con datos inválidos el servicio no se llama (`verifyNoInteractions`).

#### 3. Repository — `ProductoRepositoryTest` (`@DataJpaTest`)

- Prueba las consultas reales contra H2. No usa mocks: mockear el repositorio aquí no probaría nada.
- Cada test corre en una transacción que se revierte al terminar, así los datos no se mezclan entre tests.
- Casos probados: `save` genera id, y `findByNombreContainingIgnoreCase` ignora mayúsculas y devuelve vacío si no hay coincidencias.

#### 4. Integración — `ProductoIntegrationTest` (`@SpringBootTest`)

- Levanta la aplicación completa, sin mocks: Controller → Service → Repository → H2.
- Recorre el CRUD completo por HTTP (crear, obtener, filtrar, actualizar, eliminar, 404 después de eliminar) y revisa la base con el repositorio.
- Comprueba también que un POST inválido no guarda nada.

`ProductosApplicationTests.contextLoads` solo verifica que el contexto de Spring arranca con el perfil `test`.

## CI (GitHub Actions)

El workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) ejecuta los tests automáticamente en GitHub en cada `push` a `main`/`master` y en cada pull request:

1. Descarga el código.
2. Instala Java 21 (Temurin), con caché de dependencias Maven.
3. Ejecuta `./mvnw -B test`.
4. Sube los reportes de `target/surefire-reports/` como artefacto `reportes-tests`, incluso si algún test falla.

No necesita PostgreSQL porque los tests usan H2 (perfil `test`). El resultado se ve en la pestaña **Actions** del repositorio y como ✅/❌ en cada commit y pull request.
