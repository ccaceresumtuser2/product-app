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
| GET    | `/api/productos/precio`  | Lista por rango de precio `?min=&max=`       |
| GET    | `/api/productos/{id}`    | Obtiene un producto                          |
| POST   | `/api/productos`         | Crea un producto (responde 201)              |
| PUT    | `/api/productos/{id}`    | Actualiza un producto                        |
| DELETE | `/api/productos/{id}`    | Elimina un producto                          |

### Formato de respuesta

Todas las respuestas usan el mismo envoltorio JSON, tanto las exitosas como las de error.

**Éxito** (200 / 201): `data` trae el contenido y `message` un texto opcional (`null` si no aplica).

```json
{
  "status": "success",
  "data": {
    "id": 1,
    "nombre": "Mouse",
    "descripcion": "Inalámbrico",
    "precio": 15990,
    "stock": 10
  },
  "message": null
}
```

| Endpoint | `data` | `message` |
|----------|--------|-----------|
| `GET /api/productos`, `GET /api/productos/precio` | lista de productos (`[]` si no hay) | `null` |
| `GET /api/productos/{id}` | el producto | `null` |
| `POST /api/productos` (201) | el producto creado | `"Producto creado"` |
| `PUT /api/productos/{id}` | el producto actualizado | `"Producto actualizado"` |
| `DELETE /api/productos/{id}` | `null` | `"Producto eliminado"` |

**Error** (400 / 404): `code` repite el código HTTP y `errors` trae el detalle por campo (lista vacía si no aplica).

```json
{
  "status": "error",
  "code": 404,
  "message": "Producto 999 no encontrado",
  "errors": []
}
```

### Buscar por rango de precio

`GET /api/productos/precio?min=10000&max=20000` devuelve los productos con precio entre `min` y `max` (**ambos incluidos**), ordenados del más barato al más caro. Los dos parámetros son obligatorios.

```
curl "http://localhost:8082/api/productos/precio?min=10000&max=20000"
```

```json
{
  "status": "success",
  "data": [
    { "id": 1, "nombre": "Mouse", "descripcion": "Inalámbrico", "precio": 15990, "stock": 10 },
    { "id": 4, "nombre": "Audífonos", "descripcion": "Bluetooth", "precio": 19990, "stock": 7 }
  ],
  "message": null
}
```

Si no hay productos en el rango, `data` es `[]`. Si `min` es mayor que `max` responde 400:

```json
{
  "status": "error",
  "code": 400,
  "message": "El precio mínimo no puede ser mayor que el máximo",
  "errors": []
}
```

### Crear y actualizar

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

| Caso | Código | `message` |
|------|--------|-----------|
| Producto inexistente | 404 | `Producto 999 no encontrado` |
| Validación de `POST` / `PUT` | 400 | `Datos inválidos` (detalle en `errors`) |
| Rango de precio invertido | 400 | `El precio mínimo no puede ser mayor que el máximo` |
| Falta un parámetro | 400 | `Falta el parámetro 'max'` |
| Parámetro con tipo incorrecto | 400 | `Valor inválido para el parámetro 'min'` |
| JSON mal formado | 400 | `El cuerpo de la petición no es un JSON válido` |
| Base de datos caída o sin responder | 503 | `Base de datos no disponible, intente más tarde` |

Datos inválidos (400), con un elemento en `errors` por cada campo que falla:

```json
{
  "status": "error",
  "code": 400,
  "message": "Datos inválidos",
  "errors": [
    { "campo": "nombre", "mensaje": "El nombre es obligatorio" },
    { "campo": "precio", "mensaje": "El precio no puede ser negativo" }
  ]
}
```

## Estructura

```
src/main/java/com/app/productos/
├── ProductosApplication.java
├── config/OpenApiConfig.java          # Información de la API en Swagger
├── controller/ProductoController.java # Endpoints REST
├── exception/                         # Manejo de errores (404 y 400) en formato ApiError
├── model/Producto.java                # Entidad JPA -> tabla productos
├── repository/ProductoRepository.java # Acceso a datos
├── response/                          # ApiResponse (éxito) y ApiError (error)
└── service/ProductoService.java       # Lógica de negocio
src/main/resources/application.properties  # Puerto, conexión y Swagger

src/test/java/com/app/productos/
├── ProductosApplicationTests.java              # Arranque del contexto
├── ProductoIntegrationTest.java                # Integración completa (H2)
├── ConexionBaseDatosTest.java                  # Conexión a la base: disponible y caída (timeout, 503)
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
spring.datasource.url=jdbc:postgresql://localhost:5432/trigg_db?connectTimeout=5&socketTimeout=30
spring.datasource.username=postgres
```

### Timeout sin conexión a la base

Si PostgreSQL está caído o no responde, la API no se queda esperando: corta a los ~5 segundos y responde **503** en el formato estándar de error.

```json
{
  "status": "error",
  "code": 503,
  "message": "Base de datos no disponible, intente más tarde",
  "errors": []
}
```

| Configuración | Valor | Qué limita |
|---------------|-------|------------|
| `connectTimeout` (en la URL) | 5 s | Abrir la conexión TCP con PostgreSQL |
| `socketTimeout` (en la URL) | 30 s | Esperar la respuesta de una consulta ya enviada |
| `spring.datasource.hikari.connection-timeout` | 5000 ms | Esperar una conexión libre del pool (por defecto son 30 s) |

Además, el dialecto está fijado (`spring.jpa.database-platform=PostgreSQLDialect`), así que la aplicación **arranca aunque la base esté caída**: responde 503 mientras tanto y vuelve a funcionar sola cuando PostgreSQL vuelve, sin reiniciar. Si el dialecto no estuviera fijado, Hibernate necesitaría consultar la base al arrancar y la aplicación no levantaría.

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

Maven guarda las librerías en `~/.m2/repository` y solo las descarga la primera vez. Si ya están descargadas, se puede usar el modo offline (`-o`), que no consulta internet y es más rápido:

```
.\mvnw.cmd -o test
```

Si falta alguna librería, el modo offline falla; en ese caso se ejecuta una vez sin `-o` para descargarla.

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
  - `listarPorRangoPrecio` consulta `findByPrecioBetweenOrderByPrecioAsc`; si `min > max` lanza `IllegalArgumentException` y **no** consulta.
  - `obtener` devuelve el producto o lanza `RecursoNoEncontradoException`.
  - `crear` pone `id = null` antes de guardar (se verifica con `ArgumentCaptor`).
  - `actualizar` copia los campos y guarda; si no existe, **no** llama a `save` (`verify(repository, never())`).
  - `eliminar` borra el producto; si no existe, **no** llama a `delete`.

#### 2. Controller — `ProductoControllerTest` (`@WebMvcTest` + Mockito)

- `@MockitoBean ProductoService service`: Spring registra un mock de Mockito en lugar del servicio real.
- `MockMvc` simula peticiones HTTP sin levantar un servidor.
- Casos probados: códigos HTTP (200, 201, 400, 404), el formato estándar de respuesta (`status`, `data`, `message`) y de error (`status`, `code`, `message`, `errors`) con `jsonPath`, y que con datos inválidos, parámetros faltantes o JSON mal formado el servicio no se llama (`verifyNoInteractions`).

#### 3. Repository — `ProductoRepositoryTest` (`@DataJpaTest`)

- Prueba las consultas reales contra H2. No usa mocks: mockear el repositorio aquí no probaría nada.
- Cada test corre en una transacción que se revierte al terminar, así los datos no se mezclan entre tests.
- Casos probados: `save` genera id, `findByNombreContainingIgnoreCase` ignora mayúsculas y devuelve vacío si no hay coincidencias, y `findByPrecioBetweenOrderByPrecioAsc` incluye los extremos del rango y ordena por precio.

#### 4. Integración — `ProductoIntegrationTest` (`@SpringBootTest`)

- Levanta la aplicación completa, sin mocks: Controller → Service → Repository → H2.
- Recorre el CRUD completo por HTTP (crear, obtener, filtrar por nombre y por rango de precio, actualizar, eliminar, 404 después de eliminar) y revisa la base con el repositorio.
- Comprueba también que un POST inválido no guarda nada.

#### 5. Conexión a la base — `ConexionBaseDatosTest` (`@SpringBootTest`)

Dos grupos de tests (`@Nested`), sin mocks y sin necesitar PostgreSQL:

- **`BaseDisponible`** (H2): obtiene una conexión del pool y la valida con un ping real (`isValid`), y comprueba que el pool usa el `connection-timeout` de 5000 ms configurado.
- **`BaseCaida`**: apunta a `10.255.255.1`, una IP que no responde, para simular PostgreSQL caído. Verifica que la aplicación arranca igual, que `GET /api/productos/1` responde 503 en el formato estándar y que lo hace en menos de 10 s (sin timeouts esperaría 30 s o más).

`ProductosApplicationTests.contextLoads` solo verifica que el contexto de Spring arranca con el perfil `test`.

## CI (GitHub Actions)

El workflow [.github/workflows/ci.yml](.github/workflows/ci.yml) ejecuta los tests automáticamente en GitHub en cada `push` a `main`, `master` o `developer`, y en cada pull request:

1. Descarga el código.
2. Instala Java 21 (Temurin), con caché de dependencias Maven (`cache: maven`): la primera ejecución descarga las librerías y las guarda; las siguientes las restauran desde la caché y no las vuelven a bajar mientras el `pom.xml` no cambie.
3. Ejecuta `./mvnw -B test`.
4. Sube los reportes de `target/surefire-reports/` como artefacto `reportes-tests`, incluso si algún test falla.

No necesita PostgreSQL porque los tests usan H2 (perfil `test`). El resultado se ve en la pestaña **Actions** del repositorio y como ✅/❌ en cada commit y pull request.
