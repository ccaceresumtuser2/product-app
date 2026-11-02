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
| GET    | `/api/productos/fecha`   | Lista por fecha de creación `?desde=&hasta=` |
| GET    | `/api/productos/{id}`    | Obtiene un producto                          |
| POST   | `/api/productos`         | Crea un producto (responde 201)              |
| PUT    | `/api/productos/{id}`    | Actualiza un producto                        |
| PUT    | `/api/productos?nombre=` | Actualiza un producto buscándolo por nombre  |
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
    "stock": 10,
    "creadoEn": "2026-10-24T09:55:44.603972"
  },
  "message": null
}
```

| Endpoint | `data` | `message` |
|----------|--------|-----------|
| `GET /api/productos`, `/precio`, `/fecha` | lista de productos (`[]` si no hay) | `null` |
| `GET /api/productos/{id}` | el producto | `null` |
| `POST /api/productos` (201) | el producto creado | `"Producto creado"` |
| `PUT /api/productos/{id}`, `PUT /api/productos?nombre=` | el producto actualizado | `"Producto actualizado"` |
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

### Buscar por fecha de creación

`GET /api/productos/fecha?desde=2026-10-01&hasta=2026-10-31` devuelve los productos cuyo `creado_en` cae entre esos días (**ambos incluidos, día completo**), del más antiguo al más reciente. Las fechas van en formato `yyyy-MM-dd` y los dos parámetros son obligatorios. Para un solo día se usa la misma fecha en ambos.

```
curl "http://localhost:8082/api/productos/fecha?desde=2026-10-24&hasta=2026-10-24"
```

```json
{
  "status": "success",
  "data": [
    { "id": 2, "nombre": "Monitor 24\"", "descripcion": null, "precio": 720000.00, "stock": 10, "creadoEn": "2026-10-24T09:55:44.603972" },
    { "id": 3, "nombre": "Audífonos Bluetooth", "descripcion": null, "precio": 150000.00, "stock": 30, "creadoEn": "2026-10-24T09:55:44.603972" }
  ],
  "message": null
}
```

Internamente busca `creado_en >= desde 00:00` y `creado_en < (hasta + 1 día) 00:00`, así un producto creado el 31 a las 23:59 entra en el rango.

| Caso | Respuesta |
|------|-----------|
| Sin productos en el rango | 200 con `data: []` |
| `desde` posterior a `hasta` | 400, `La fecha 'desde' no puede ser posterior a 'hasta'` |
| Fecha con otro formato (ej. `01/10/2026`) | 400, `Valor inválido para el parámetro 'desde'` |
| Falta `desde` o `hasta` | 400, `Falta el parámetro 'hasta'` |

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
- `creadoEn`: **no se envía**. Lo asigna la aplicación al crear el producto, no cambia al actualizar y, si el cliente lo manda en el JSON, se ignora.

### Editar por nombre

`PUT /api/productos?nombre=Teclado` actualiza el producto cuyo nombre es **exactamente** `Teclado`, sin distinguir mayúsculas (`teclado` también sirve) e ignorando espacios al inicio y al final. El cuerpo es el mismo que el de `PUT /api/productos/{id}`, con las mismas validaciones, y puede traer un nombre nuevo para renombrar el producto.

```
curl -X PUT "http://localhost:8082/api/productos?nombre=Teclado"      -H "Content-Type: application/json"      -d '{"nombre":"Teclado","descripcion":"Mecánico","precio":95000,"stock":20}'
```

```json
{
  "status": "success",
  "data": { "id": 1, "nombre": "Teclado", "descripcion": "Mecánico", "precio": 95000, "stock": 20, "creadoEn": "2026-10-24T09:55:44.603972" },
  "message": "Producto actualizado"
}
```

El nombre va como parámetro (`?nombre=`) y no en la ruta, para admitir espacios, comillas o `/`. Si lo escribes a mano en la URL, codifícalo: `Monitor 24"` se escribe `?nombre=Monitor%2024%22`. Swagger y `curl --data-urlencode` lo codifican solos.

| Caso | Respuesta |
|------|-----------|
| Un producto con ese nombre | 200, producto actualizado |
| Ninguno (o solo coincide una parte, ej. `Tecla`) | 404, `Producto 'Tecla' no encontrado` |
| Varios con el mismo nombre | 409, `Hay 2 productos llamados 'X'; use PUT /api/productos/{id}` |
| Cuerpo inválido | 400, `Datos inválidos` con detalle en `errors` |

La columna `nombre` no es única en la base. Si hay nombres repetidos, editar por nombre sería ambiguo, así que la API responde 409 en vez de elegir uno: en ese caso hay que usar el `id`.

### Errores

| Caso | Código | `message` |
|------|--------|-----------|
| Producto inexistente | 404 | `Producto 999 no encontrado` |
| Nombre repetido al editar por nombre | 409 | `Hay 2 productos llamados 'X'; use PUT /api/productos/{id}` |
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
├── exception/                         # Manejo de errores (400, 404, 409, 503) en formato ApiError
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
    stock       INTEGER      NOT NULL,
    creado_en   TIMESTAMP    NOT NULL DEFAULT now()
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
  - `listarPorFechaCreacion` pasa al repositorio `[desde 00:00, hasta + 1 día 00:00)` para incluir el último día completo; si `desde > hasta` lanza `IllegalArgumentException` y **no** consulta.
  - `obtener` devuelve el producto o lanza `RecursoNoEncontradoException`.
  - `crear` pone `id = null` antes de guardar (se verifica con `ArgumentCaptor`).
  - `actualizar` copia los campos y guarda; si no existe, **no** llama a `save` (`verify(repository, never())`).
  - `actualizarPorNombre` busca con `findByNombreIgnoreCase` (quitando espacios); si no hay coincidencias lanza `RecursoNoEncontradoException`, si hay varias lanza `ConflictoException`, y en ambos casos **no** guarda.
  - `eliminar` borra el producto; si no existe, **no** llama a `delete`.

#### 2. Controller — `ProductoControllerTest` (`@WebMvcTest` + Mockito)

- `@MockitoBean ProductoService service`: Spring registra un mock de Mockito en lugar del servicio real.
- `MockMvc` simula peticiones HTTP sin levantar un servidor.
- Casos probados: códigos HTTP (200, 201, 400, 404, 409, 503), el formato estándar de respuesta (`status`, `data`, `message`) y de error (`status`, `code`, `message`, `errors`) con `jsonPath`, y que con datos inválidos, parámetros faltantes o JSON mal formado el servicio no se llama (`verifyNoInteractions`).

#### 3. Repository — `ProductoRepositoryTest` (`@DataJpaTest`)

- Prueba las consultas reales contra H2. No usa mocks: mockear el repositorio aquí no probaría nada.
- Cada test corre en una transacción que se revierte al terminar, así los datos no se mezclan entre tests.
- Casos probados: `save` genera id, `findByNombreContainingIgnoreCase` ignora mayúsculas y devuelve vacío si no hay coincidencias, `findByPrecioBetweenOrderByPrecioAsc` incluye los extremos del rango y ordena por precio, `save` asigna `creadoEn`, `buscarCreadosEntre` incluye `desde`, excluye `hasta` y ordena por fecha (las fechas se fijan con SQL directo porque `creado_en` no es actualizable desde la entidad), y `findByNombreIgnoreCase` es coincidencia exacta (no parcial), ignora mayúsculas y devuelve todos los repetidos.

#### 4. Integración — `ProductoIntegrationTest` (`@SpringBootTest`)

- Levanta la aplicación completa, sin mocks: Controller → Service → Repository → H2.
- Recorre el CRUD completo por HTTP (crear, obtener, filtrar por nombre, por rango de precio y por fecha de creación, actualizar por id, actualizar por nombre y renombrar, 404 al buscar el nombre anterior, eliminar, 404 después de eliminar) y revisa la base con el repositorio.
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
