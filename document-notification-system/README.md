# Document Notification System

Sistema de notificación de documentos implementado con arquitectura hexagonal y Domain-Driven Design (DDD).

## 📋 Tabla de Contenidos

- [Arquitectura](#arquitectura)
- [Tecnologías](#tecnologías)
- [Prerequisitos](#prerequisitos)
- [Configuración del Entorno](#configuración-del-entorno)
- [Construcción del Proyecto](#construcción-del-proyecto)
- [Generación de Clases Avro](#generación-de-clases-avro)
- [Ejecución de Servicios](#ejecución-de-servicios)
- [Estructura del Proyecto](#estructura-del-proyecto)

## 🏗️ Arquitectura

El proyecto sigue **Arquitectura Hexagonal (Ports & Adapters)** con principios de **Domain-Driven Design (DDD)**:

- **domain-core**: Lógica de negocio pura (sin dependencias de frameworks)
- **application-service**: Casos de uso, puertos (input/output), DTOs
- **dataaccess**: Adaptadores JPA, repositorios
- **application-api**: Controladores REST, manejo de excepciones
- **container**: Aplicación Spring Boot, configuración de beans
- **messaging**: Publicadores/consumidores Kafka (en desarrollo)

## 🛠️ Tecnologías

- **Java 19**
- **Spring Boot 3.3.2**
- **Maven** (Multi-módulo)
- **PostgreSQL** (Base de datos)
- **Apache Kafka** (Mensajería asíncrona)
- **Apache Avro** (Serialización de eventos)
- **Lombok** (Reducción de boilerplate)

## 📦 Prerequisitos

- Java 19 o superior
- Maven 3.6+
- Docker (para PostgreSQL y Kafka)
- PowerShell (Windows)

## ⚙️ Configuración del Entorno

### 1. Base de Datos PostgreSQL

```powershell
# Document Service (Puerto 5434)
docker run -d --name document-postgres `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=admin `
  -e POSTGRES_DB=postgres `
  -p 5434:5432 `
  postgres:15

# Customer Service (Puerto 5435) - Si es necesario
docker run -d --name customer-postgres `
  -e POSTGRES_USER=postgres `
  -e POSTGRES_PASSWORD=admin `
  -e POSTGRES_DB=postgres `
  -p 5435:5432 `
  postgres:15
```

### 2. Kafka Cluster (Opcional - En desarrollo)

```powershell
cd infraestructure/docker-compose
docker-compose -f zookeeper.yml -f kafka_cluster.yml -f common.yml up -d
```

## 🔨 Construcción del Proyecto

### Compilar todo el proyecto

```powershell
# Desde la raíz del proyecto
mvn clean install
```

### Compilar un servicio específico

```powershell
# Document Service
cd document-service
mvn clean install

# Customer Service
cd customer-service
mvn clean install
```

### Compilar sin ejecutar tests

```powershell
mvn clean install -DskipTests
```

## 📝 Generación de Clases Avro

Las clases Java para eventos Kafka se generan automáticamente desde schemas Avro (`.avsc`).

### Generar clases desde schemas Avro

```powershell
# Navegar al módulo kafka-model
cd infraestructure/kafka/kafka-model

# Generar clases Java
mvn clean generate-sources
```

### Ubicación de archivos

- **Schemas Avro**: `infraestructure/kafka/kafka-model/src/main/resources/avro/*.avsc`
- **Clases generadas**: `infraestructure/kafka/kafka-model/src/main/java/com/document/notification/system/kafka/document/avro/model/`

### Eventos disponibles

1. **DocumentCreatedEventAvroModel** - Documento creado
2. **DocumentGeneratedEventAvroModel** - Documento generado
3. **DocumentSentEventAvroModel** - Documento enviado
4. **DocumentCancelledEventAvroModel** - Documento cancelado

### Modificar schemas Avro

1. Editar archivos `.avsc` en `src/main/resources/avro/`
2. Regenerar clases: `mvn generate-sources`
3. Las clases Java se actualizarán automáticamente

## 🚀 Ejecución de Servicios

### Document Service

```powershell
# Asegúrate de que PostgreSQL esté corriendo (puerto 5434)
cd document-service/document-container
mvn spring-boot:run
```

**Puerto**: 8181  
**Endpoint base**: http://localhost:8181/documents  
**API Version**: `application/vnd.api.v1+json`

### Customer Service

```powershell
# Asegúrate de que PostgreSQL esté corriendo (puerto 5435)
cd customer-service/customer-container
mvn spring-boot:run
```

### Verificar servicios

```powershell
# Document Service
curl http://localhost:8181/actuator/health

# Customer Service  
curl http://localhost:8182/actuator/health
```

## 📂 Estructura del Proyecto

```
document-notification-system/
├── common/                          # Módulos compartidos
│   ├── common-domain/               # Entidades base, value objects
│   └── common-application-api/      # DTOs y utilidades comunes
├── document-service/                # Servicio de documentos
│   ├── document-domain/
│   │   ├── document-domain-core/    # Lógica de negocio pura
│   │   └── document-application-service/  # Casos de uso
│   ├── document-dataaccess/         # Adaptadores de persistencia
│   ├── document-application-api/    # REST Controllers
│   ├── document-messaging/          # Kafka publishers/listeners
│   └── document-container/          # Spring Boot App
├── customer-service/                # Servicio de clientes
│   └── [estructura similar a document-service]
├── generator-service/               # Servicio de generación (placeholder)
└── infraestructure/                 # Infraestructura
    ├── docker-compose/              # Docker Compose files
    └── kafka/
        ├── kafka-config-data/       # Configuración Kafka
        └── kafka-model/             # Schemas Avro y clases generadas
```

## 🔧 Comandos Útiles

### Maven

```powershell
# Limpiar compilación
mvn clean

# Compilar sin tests
mvn install -DskipTests

# Ejecutar tests
mvn test

# Ver árbol de dependencias
mvn dependency:tree

# Actualizar dependencias
mvn versions:display-dependency-updates
```

### Docker

```powershell
# Ver contenedores activos
docker ps

# Ver logs de PostgreSQL
docker logs document-postgres -f

# Detener y eliminar contenedor
docker stop document-postgres
docker rm document-postgres

# Acceder a PostgreSQL
docker exec -it document-postgres psql -U postgres -d postgres
```

## 📊 Base de Datos

### Document Service

- **Host**: localhost
- **Puerto**: 5434
- **Base de datos**: postgres
- **Schema**: document
- **Usuario**: postgres
- **Contraseña**: admin

### Customer Service

- **Host**: localhost
- **Puerto**: 5435
- **Base de datos**: postgres
- **Schema**: customer
- **Usuario**: postgres
- **Contraseña**: admin

## 🔍 Troubleshooting

### Error: Puerto ya en uso

```powershell
# Verificar qué proceso usa el puerto
netstat -ano | findstr :8181

# Detener el proceso (reemplaza PID)
taskkill /PID <PID> /F
```

### Error: PostgreSQL no conecta

```powershell
# Verificar que el contenedor esté corriendo
docker ps | findstr postgres

# Reiniciar contenedor
docker restart document-postgres
```

### Error al generar clases Avro

```powershell
# Limpiar y regenerar
cd infraestructure/kafka/kafka-model
mvn clean
mvn generate-sources
```

## 📚 Recursos Adicionales

- [AGENTS.md](./AGENTS.md) - Guía detallada para agentes de IA
- [commands/](./commands/) - Scripts y comandos útiles

## 👤 Autor

**Ivan Camilo Rincon Saavedra**

## 📄 Licencia

Este proyecto es privado y de uso interno.

