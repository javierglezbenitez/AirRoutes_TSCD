# ✈️ Air Routes — Plataforma Escalable para Búsqueda de Vuelos Inmediatos

[![Java](https://img.shields.io/badge/Java-17-orange?logo=java)](https://java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![Neo4j](https://img.shields.io/badge/Neo4j-Graph%20DB-blue?logo=neo4j)](https://neo4j.com)
[![AWS](https://img.shields.io/badge/AWS-EC2%20%7C%20S3-yellow?logo=amazonaws)](https://aws.amazon.com)
[![Docker](https://img.shields.io/badge/Docker-Containerized-2496ED?logo=docker)](https://docker.com)
[![CI/CD](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-black?logo=githubactions)](https://github.com/features/actions)

---

## 📌 Descripción

**Air Routes** es una plataforma escalable diseñada para encontrar vuelos de forma rápida y eficiente ante imprevistos. El sistema procesa, organiza y expone datos de vuelos mediante una arquitectura modular orientada a la nube, combinando **AWS**, **Neo4j** y **Docker**.

El proyecto está diseñado para funcionar en dos modos: **local** (para desarrollo y pruebas) y **remoto en AWS** (para producción), sin cambios en el código.

---

## 🏗️ Arquitectura del sistema

```
Datos crudos de vuelos
        │
        ▼
┌──────────────┐        ┌─────────────────────┐
│   DATALAKE   │        │     ORCHESTRATOR     │
│  Local / S3  │◄──────►│  Coordina Datalake   │
└──────┬───────┘        │  + Datamart          │
       │                └─────────────────────┘
       ▼
┌──────────────┐
│   DATAMART   │  ← Transforma datos · Carga diaria en Neo4j
│ Local / EC2  │
└──────┬───────┘
       │
       ▼
┌──────────────┐        ┌──────────────┐
│     API      │◄──────►│    Neo4j     │  ← Base de datos de grafos
│  Spring Boot │        │ Local / EC2  │
│  EC2         │        └──────────────┘
└──────┬───────┘
       │ REST
       ▼
┌──────────────┐
│     GUI      │  ← Interfaz web HTML dinámica
└──────────────┘
```

---

## 📦 Módulos

### 1. Datalake
Punto de entrada de los datos crudos de vuelos. Mantiene una copia íntegra sin transformaciones para futuros reprocesamientos.

| Modo | Almacenamiento |
|------|---------------|
| Local | Sistema de archivos |
| Remoto | Amazon S3 |

### 2. Datamart
Transforma los datos crudos en información estructurada lista para consulta. Realiza una carga diaria completa (elimina los datos del día anterior y carga los nuevos) en **Neo4j**, una base de datos orientada a grafos que modela relaciones complejas entre vuelos, aerolíneas y rutas.

| Modo | Infraestructura |
|------|----------------|
| Local | Docker + Neo4j |
| Remoto | EC2 + Docker + Neo4j |

### 3. Orchestrator
Coordinador del sistema. Ejecuta simultáneamente el Datalake y el Datamart, controla la secuencia de ejecución y gestiona errores para mantener la consistencia del pipeline.

### 4. API
Desarrollada en **Spring Boot**, desplegada en EC2. Expone endpoints REST optimizados para búsquedas rápidas sobre Neo4j, independientemente de si la base de datos está en local o en la nube.

### 5. GUI
Interfaz web con páginas HTML dinámicas. Permite al usuario lanzar consultas que son procesadas por la API y recibir resultados en tiempo real.

---

## 🛠️ Stack tecnológico

| Categoría | Tecnología |
|-----------|-----------|
| Backend | Java 17 · Spring Boot |
| Base de datos | Neo4j (grafos) |
| Cloud | AWS EC2 · AWS S3 |
| Contenedores | Docker |
| Frontend | HTML · JavaScript |
| CI/CD | GitHub Actions · Playwright |
| Testing | Tests unitarios e integración por módulo |

---

## ☁️ Infraestructura AWS

```
┌─────────────────────────────────────────┐
│                  AWS                    │
│                                         │
│   ┌─────────┐       ┌───────────────┐  │
│   │   S3    │       │  EC2 Instancia│  │
│   │Datalake │       │      API      │  │
│   │ Bucket  │       │  Spring Boot  │  │
│   └─────────┘       └──────┬────────┘  │
│                             │           │
│                    ┌────────▼────────┐  │
│                    │  EC2 Instancia  │  │
│                    │    Datamart     │  │
│                    │  Neo4j Docker   │  │
│                    └─────────────────┘  │
└─────────────────────────────────────────┘
```

---

## 🔄 CI/CD con GitHub Actions

Cada módulo tiene pruebas automáticas integradas en un pipeline de GitHub Actions:

1. Se ejecutan **tests unitarios y de integración** tras cada cambio
2. Se valida la correcta ejecución en cada módulo
3. Solo si **todas las pruebas pasan** se realiza el push al repositorio

---

## 📂 Estructura del repositorio

```
📦 AirRoutes_TSCD
├── 📁 .github/workflows    # Pipelines CI/CD con GitHub Actions
├── 📁 Api                  # API REST con Spring Boot
├── 📁 Datalake             # Módulo de ingesta de datos (local / S3)
├── 📁 Datamart             # Transformación y carga en Neo4j (local / EC2)
├── 📁 GUI                  # Interfaz web HTML dinámica
├── 📁 Orchestrator         # Coordinador del pipeline
├── docker-neo4j.yml        # Configuración Docker para Neo4j
├── pom.xml                 # Dependencias Maven
└── AirRoutes.pdf           # Documentación del proyecto
```

---

## 🚀 Instalación y uso

### Modo local

```bash
# Clonar el repositorio
git clone https://github.com/javierglezbenitez/AirRoutes_TSCD.git
cd AirRoutes_TSCD

# Levantar Neo4j con Docker
docker-compose -f docker-neo4j.yml up -d

# Ejecutar el Orchestrator (lanza Datalake + Datamart)
cd Orchestrator
java -jar orchestrator.jar --mode=local

# Arrancar la API
cd Api
./mvnw spring-boot:run

# Abrir la GUI en el navegador
open GUI/index.html
```

### Modo AWS

```bash
# Configurar credenciales AWS
aws configure

# Ejecutar en modo remoto (S3 + EC2)
cd Orchestrator
java -jar orchestrator.jar --mode=aws
```

---


## 👥 Autores

**Javier González Benítez** · **Jorge González Benítez**

Grado en Ciencia e Ingeniería de Datos — ULPGC 2025

[![LinkedIn](https://img.shields.io/badge/LinkedIn-javiergonzalez--benitez-blue?logo=linkedin)](https://www.linkedin.com/in/javier-gonzalez-benitez-78052838b)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-jorgegonzalez--benitez-blue?logo=linkedin)](https://www.linkedin.com/in/jorge-gonz%C3%A1lez-a2612738b/)

---

## 📜 Licencia

Este proyecto se distribuye con fines educativos.

