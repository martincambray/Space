# Space Mission Control — Dossier de Présentation Orale

> Application full-stack de gestion et simulation de missions spatiales  
> Projet de fin de formation · Java · Angular · Spring Boot · MySQL

---

## Table des matières

1. [Vue d'ensemble du projet](#1-vue-densemble-du-projet)
2. [Architecture globale](#2-architecture-globale)
3. [Base de données — MySQL](#3-base-de-données--mysql)
4. [Backend — Spring Boot (Java)](#4-backend--spring-boot-java)
5. [Sécurité — Spring Security & JWT](#5-sécurité--spring-security--jwt)
6. [Simulation orbitale — Le moteur physique](#6-simulation-orbitale--le-moteur-physique)
7. [Frontend — Angular](#7-frontend--angular)
8. [Déploiement — Docker](#8-déploiement--docker)
9. [Tests & Qualité](#9-tests--qualité)
10. [Tableau récapitulatif des technologies](#10-tableau-récapitulatif-des-technologies)
11. [Parcours de formation — De Java à Full Stack](#11-parcours-de-formation--de-java-à-full-stack)

---

## 1. Vue d'ensemble du projet

**Space Mission Control (SMC)** est une console web de gestion et de simulation de missions spatiales. Elle permet à une agence spatiale fictive de :

- Gérer une flotte de vaisseaux spatiaux (satellites, pods habités, rovers, utilitaires)
- Planifier et suivre des missions interplanétaires
- Simuler des trajectoires orbitales en 2D basées sur un vrai moteur physique N-corps
- Contrôler les vaisseaux à distance via des actions (propulsion, EVA, scan…)
- Administrer les utilisateurs et leurs droits d'accès

Le projet répond à un cahier des charges complet : **authentification sécurisée**, **CRUD sur toutes les entités**, **calcul scientifique**, **visualisation graphique**, et **déploiement conteneurisé**.

---

## 2. Architecture globale

```
┌─────────────────────────────────────────────────────────────┐
│                        CLIENT (Port 4200)                    │
│                    Angular 21 + TypeScript                   │
│              Canvas 2D · RxJS · Standalone Components        │
└──────────────────────────┬──────────────────────────────────┘
                           │  HTTP REST (JSON) + JWT Bearer
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                     BACKEND (Port 8080)                      │
│                   Spring Boot 3.5 (Java 21)                  │
│   Spring Security · Spring Data JPA · JJWT · Commons Math   │
└──────────────────────────┬──────────────────────────────────┘
                           │  JDBC (Hibernate ORM)
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                   BASE DE DONNÉES (Port 3316)                │
│                         MySQL 8.0                            │
│         6 tables · Données de référence pré-chargées        │
└─────────────────────────────────────────────────────────────┘

         Ensemble orchestré par Docker Compose
```

### Structure des dossiers

| Dossier | Contenu |
|---|---|
| `space_app/` | Backend Spring Boot (Maven, Java 21) |
| `space_app_Angular/` | Frontend Angular 21 (npm, TypeScript) |
| `space_doc/` | Scripts SQL, UML, docs API (PDF) |
| `docker-compose.yml` | Orchestration des 2 services |

---

## 3. Base de données — MySQL

### Technologie

| Élément | Valeur |
|---|---|
| SGBD | **MySQL 8.0** |
| ORM | **Hibernate / JPA** (via Spring Data) |
| Initialisation | Script `space_doc/space_db.sql` |
| Base de test | **H2 in-memory** (pas besoin de MySQL pour `mvn test`) |

### Schéma relationnel

```
utilisateur ──────────────────────────────────┐
  id, mail*, password, lastname, firstname     │ (opérateur)
  role (ADMIN|OPERATEUR), suspended           │
                                               │
spacecraft ────────────────┐                  │
  id, name, description     │                  │
  type (SATELLITE|POD_HABITE│                  │
        |ROVER|UTILITAIRE)  │                  │
  battery_max, fuel_capacity│                  │
  available ─────────────── ┤                  │
                             │ mission ──────────┤
celestial_body               │   id, name        │
  id, name, mass, radius     │   status          │
  orbital_radius             ├── spacecraft_id   │
  ref_coord_x/y ─────────── ┤   operator_id ────┘
                             │   type_id
mission_type ────────────── ┤   departure_body_id
  id, name, description      │   arrival_body_id
                             │   departure_date, arrival_date
                             │   orbital_time, payload, trajectory
                             │
trajectory_log ──────────────┘
  id, mission_id, operator_id, body_id
  computed_at, altitude, initial_speed, result_json
```

### Données pré-chargées (dans le script SQL)

- **7 utilisateurs** : 1 ADMIN (`admin.admin@smc.fr`) + 6 opérateurs (membres de l'équipe)
- **10 corps célestes** : Soleil, Mercure, Vénus, Terre, Lune, Mars, Jupiter, Saturne, Uranus, Neptune
- **3 types de mission** : Mise en Orbite, Mission Lunaire, Interplanétaire
- **4 vaisseaux** de référence (un de chaque type)
- **4 missions exemples** dans les 4 statuts possibles

### Héritage JPA — Table unique (SINGLE_TABLE)

La table `spacecraft` utilise une stratégie d'héritage JPA particulière : une seule table pour tous les types de vaisseaux, avec une colonne discriminante `type`. C'est la stratégie la plus performante en lecture.

```
spacecraft (table unique)
  ├── SATELLITE      → champ spécifique : solar_panel_deployed
  ├── POD_HABITE     → champs spécifiques : o2_level, solar_panel_deployed
  ├── ROVER          → champ spécifique : odometer
  └── UTILITAIRE     → champ spécifique : maintenance_count
```

---

## 4. Backend — Spring Boot (Java)

### Stack technique

| Technologie | Version | Rôle |
|---|---|---|
| **Java** | 21 LTS | Langage principal |
| **Spring Boot** | 3.5.x | Framework applicatif |
| **Spring Web** | (inclus) | API REST (`@RestController`) |
| **Spring Data JPA** | (inclus) | Accès BDD via interfaces |
| **Spring Security** | 6.x | Authentification & autorisation |
| **Hibernate** | 6.x | ORM (mapping objet-relationnel) |
| **JJWT** | 0.12.6 | Génération/validation tokens JWT |
| **Apache Commons Math** | 3.6.1 | Intégrateur ODE (simulation orbitale) |
| **MySQL Connector** | latest | Pilote JDBC MySQL |
| **Maven** | 3.x | Build & gestion des dépendances |

### Organisation des packages

```
space/
├── MODEL/          Entités JPA (Utilisateur, Mission, Spacecraft…)
├── DAO/            Interfaces Spring Data JPA (IDAO*)
├── DTO/
│   ├── request/    Objets de requête avec validation @Valid
│   └── response/   Objets de réponse (pattern factory convert())
├── REST/           9 contrôleurs @RestController
├── SERVICE/        Services métier (MoteurPhysique, TableauDeBord…)
├── CONFIG/         Sécurité (SecurityConfig, JWT, Filter)
├── ACTIONS/        13 classes Consumer<Double> (une par action)
├── ENUM/           4 enums (TYPE_ACTION, MISSION_STATUS…)
└── EXCEPTION/      ActionNotSupportedException
```

### Les 9 endpoints REST

| Contrôleur | Route | Rôles |
|---|---|---|
| `AuthController` | `POST /api/auth` | Public (login) |
| `MissionController` | `/api/mission` | CRUD, ADMIN pour suppression |
| `SpacecraftController` | `/api/spacecraft` | CRUD ADMIN |
| `CelestialBodyController` | `/api/celestial-body` | CRUD ADMIN |
| `UtilisateurController` | `/api/utilisateur` | CRUD ADMIN + self-update |
| `MissionTypeController` | `/api/mission-type` | CRUD ADMIN |
| `TrajectoryController` | `/api/trajectory` | Calcul + lecture orbites |
| `ActionController` | `/api/action` | Exécution d'actions |
| `TrajectoryLogsController` | `/api/trajectory-logs` | Historique calculs |

### Pattern DTO (Data Transfer Object)

Pour ne jamais exposer directement les entités JPA en dehors du backend, toutes les réponses HTTP passent par des DTOs avec une méthode de conversion statique :

```java
// Exemple : MissionResponse.convert(Mission m)
public static MissionResponse convert(Mission m) {
    MissionResponse r = new MissionResponse();
    r.setId(m.getId());
    r.setOperatorMail(m.getOperator().getMail());
    r.setSpacecraftName(m.getSpacecraft().getName());
    // ...
    return r;
}
```

### Machines à états — Mission

Les missions suivent un cycle de vie strict avec des transitions validées :

```
PLANNED ──→ IN_PROGRESS ──→ COMPLETED (état terminal)
                       └──→ CANCELLED  (état terminal)
```

Quand une mission passe en état terminal, le vaisseau est automatiquement libéré (`available = true`).

### Pattern ActionRegistry

Un registre central définit quelle action est supportée par quel type de vaisseau. C'est le **pattern Strategy** + **Factory** :

```
TYPE_ACTION          SATELLITE  POD_HABITE  ROVER  UTILITAIRE
──────────────────────────────────────────────────────────────
CHANGER_PROPULSION      ✓           ✓
CHANGER_DIRECTION       ✓           ✓
OUVRIR_PANNEAU          ✓           ✓
TRANSMISSION_DONNEES    ✓                     ✓
SCAN_SURFACE            ✓
MODE_ECO                ✓                     ✓
COLLECTE_DONNEES                    ✓         ✓
EVA                                 ✓
GESTION_O2                          ✓
DEPLACEMENT                                   ✓       ✓
MAINTENANCE                                           ✓
PROD_ELEC                                             ✓
PHOTO                                         ✓
```

---

## 5. Sécurité — Spring Security & JWT

### Principe général

L'API est **stateless** : le serveur ne garde aucune session. Chaque requête est authentifiée via un **JSON Web Token (JWT)** transporté dans l'en-tête HTTP.

```
Client                            Serveur
  │                                  │
  │── POST /api/auth (mail+mdp) ────▶│
  │                                  │── Vérifie BCrypt
  │◀── { token: "eyJ..." } ─────────│── Génère JWT signé HMAC-SHA256
  │                                  │
  │── GET /api/mission               │
  │   Authorization: Bearer eyJ... ─▶│
  │                                  │── JwtHeaderFilter décode le token
  │                                  │── Crée le SecurityContext
  │◀── [{ id: 1, ... }] ────────────│── Contrôleur répond
```

### Composants de sécurité

| Classe | Rôle |
|---|---|
| `SecurityConfig` | Configuration globale : routes publiques, CORS, sessions stateless |
| `JwtUtils` | Génération et validation des tokens (expiration 8h, HMAC-SHA256) |
| `JwtHeaderFilter` | Filtre HTTP : extrait le token, valide, charge l'utilisateur |
| `JpaUserDetailsService` | Charge l'utilisateur depuis la BDD par son mail |

### Les 2 rôles

| Rôle | Capacités |
|---|---|
| `ADMIN` | Accès total : CRUD utilisateurs, vaisseaux, corps célestes, gestion missions |
| `OPERATEUR` | Créer des missions, consulter les données, exécuter des actions |

---

## 6. Simulation orbitale — Le moteur physique

C'est la partie la plus ambitieuse du projet sur le plan technique.

### Principe

Le moteur simule des trajectoires orbitales dans un système solaire N-corps (chaque planète attire le vaisseau gravitationnellement). Il utilise une bibliothèque de calcul scientifique Java.

### Technologies utilisées

| Élément | Détail |
|---|---|
| **Apache Commons Math 3** | Librairie de mathématiques pour Java |
| **Intégrateur ODE** | `DormandPrince853Integrator` — méthode Runge-Kutta d'ordre 8(5,3) à pas adaptatif |
| **Modèle physique** | Gravitation newtonienne N-corps (Soleil + 9 planètes + vaisseau) |
| **Transfert de Hohmann** | Manœuvre orbitale interplanétaire (2 impulsions) |

### Fonctionnement simplifié

1. La mission fournit les **conditions initiales** (position et vitesse au départ)
2. L'intégrateur résout l'**équation différentielle** des forces gravitationnelles à chaque pas de 60 secondes
3. La trajectoire calculée est **mise en cache** par mission (objet `Orbit`)
4. Quand une action modifie la vitesse du vaisseau (propulsion, direction), seul le **segment affecté** est recalculé — pas toute l'orbite

### Les 3 classes du moteur

```
MoteurPhysique.java     — Intégration numérique N-corps (DormandPrince853)
TableauDeBord.java      — Orchestrateur : cache, actions, appels au moteur
OrbitalCalculator.java  — Calculs képlériens simples (vitesse circulaire, période)
```

---

## 7. Frontend — Angular

### Stack technique

| Technologie | Version | Rôle |
|---|---|---|
| **Angular** | 21.2.0 | Framework SPA (Single Page Application) |
| **TypeScript** | 5.9.x | Langage (JavaScript typé) |
| **RxJS** | 7.8.0 | Programmation réactive (Observables) |
| **Angular Router** | (inclus) | Navigation entre pages (SPA) |
| **Angular Forms** | (inclus) | Formulaires réactifs (validation) |
| **Angular HttpClient** | (inclus) | Requêtes HTTP vers l'API |
| **Canvas 2D API** | (natif browser) | Dessin de la simulation orbitale |
| **Vitest** | 4.0.8 | Tests unitaires (remplace Jasmine/Karma) |
| **npm** | 11.11.0 | Gestionnaire de paquets |

### Architecture moderne : Standalone Components

Angular 21 utilise une architecture **sans NgModule** : chaque composant est autonome et déclare ses propres dépendances. C'est la pratique recommandée aujourd'hui.

```typescript
// Pas de NgModule, chaque composant s'auto-déclare
@Component({
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `...`
})
export class MissionComposant { ... }
```

### Routing et guards

```
/login          → LoginComposant      (guard: noAuthGuard — redirige si déjà connecté)
/menu           → MenuComposant       (guard: authGuard — redirige si non connecté)
/spacecraft     → SpacecraftComposant (guard: authGuard)
/celestial-body → CelestialBodyComposant
/mission        → MissionComposant
/simulation     → SimulationComposant
/profil         → ProfilComposant
```

Tous les composants sont **lazy-loadés** (`loadComponent()`), ce qui améliore les performances au premier chargement.

### Les intercepteurs HTTP

Deux intercepteurs fonctionnels wrappent automatiquement toutes les requêtes :

```
Requête sortante                        Réponse entrante
     │                                        │
     ▼ api-url-interceptor                    │
  Ajoute le préfixe http://localhost:8080     │
     │                                        │
     ▼ jwt-header-interceptor                 │
  Ajoute Authorization: Bearer <token>    ◀── Si 401/403 → logout + /login
```

### Les composants principaux

| Composant | Fonctionnalité |
|---|---|
| `LoginComposant` | Formulaire de connexion (validation réactive, toggle password) |
| `MenuComposant` | Tableau de bord navigation |
| `MissionComposant` | Créer/filtrer/trier des missions, gérer les types (ADMIN) |
| `SpacecraftComposant` | CRUD vaisseaux, upload image en base64 |
| `CelestialBodyComposant` | CRUD corps célestes |
| `ProfilComposant` | Modifier son propre profil |
| `SimulationComposant` | **Visualisation Canvas 2D** : orbites, zoom molette, drag, animation |

### Services Angular (pattern Singleton)

Tous les services sont `providedIn: 'root'` — une seule instance partagée dans toute l'application :

```
AuthService          — Gestion du JWT en sessionStorage
MissionService       — Appels API /api/mission
SpacecraftService    — Appels API /api/spacecraft
CelestialBodyService — Appels API /api/celestial-body
UtilisateurService   — Appels API /api/utilisateur
TrajectoryService    — Appels API /api/trajectory
RoleService          — Lecture du rôle de l'utilisateur connecté
```

---

## 8. Déploiement — Docker

### Principe

L'application entière se lance en **une seule commande** grâce à Docker Compose :

```bash
docker compose up --build
```

### Services Docker

```yaml
services:

  boot:                          # Backend Spring Boot
    build: ./space_app
    ports: 8080:8080
    healthcheck: TCP :8080
    environment:
      - MYSQL_URL, MYSQL_USER, MYSQL_PASS

  angular:                       # Frontend (servi par Nginx)
    build: ./space_app_Angular
    ports: 4200:80               # Nginx sert le build Angular
    depends_on:
      boot: { condition: service_healthy }
```

Le frontend Angular est **compilé en production** (`ng build`) puis servi par **Nginx** dans Docker — c'est plus performant qu'un serveur de dev.

---

## 9. Tests & Qualité

### Backend — JUnit 5 + Mockito + JaCoCo

| Outil | Rôle |
|---|---|
| **JUnit 5** | Framework de tests Java (annotations `@Test`, `@BeforeEach`…) |
| **Mockito** | Simulation d'objets (mocks) pour isoler les composants |
| **Spring Boot Test** | Contexte Spring en test (`@SpringBootTest`, `MockMvc`) |
| **H2** | Base de données en mémoire pour les tests (remplace MySQL) |
| **JaCoCo** | Mesure de couverture de code — seuil minimum : **60%** |

Suites de tests existantes :
- `MoteurPhysiqueTest` — Tests purs du simulateur orbital
- `SecurityIntegrationTest` — Tests JWT + Spring Security end-to-end
- `JwtUtilsTest`, `ActionRegistryTest`, `MissionServiceTest`
- `SpacecraftControllerTest`, `UtilisateurControllerTest`
- `DtoConvertTest` — Validation des conversions entité → DTO

### Frontend — Vitest

| Outil | Rôle |
|---|---|
| **Vitest** | Framework de tests moderne (remplace Jasmine/Karma dans Angular récent) |
| **jsdom** | Simulation du DOM navigateur en environnement Node.js |

### Qualité du code

| Pratique | Détail |
|---|---|
| **Validation `@Valid`** | Toutes les requêtes entrantes sont validées (Jakarta Validation) |
| **DTOs** | Séparation claire entre entités JPA et données exposées à l'API |
| **Prettier** | Formatage automatique du code TypeScript/Angular |
| **Guards Angular** | Protection des routes côté frontend |
| **Pattern Factory** | `Spacecraft.of(type)` instancie le bon sous-type |

---

## 10. Tableau récapitulatif des technologies

### Backend

| Technologie | Catégorie | Version |
|---|---|---|
| Java | Langage | **21 LTS** |
| Spring Boot | Framework applicatif | **3.5.x** |
| Spring Web (REST) | API HTTP | inclus Spring Boot |
| Spring Data JPA | Persistance / ORM | inclus Spring Boot |
| Spring Security | Sécurité | **6.x** |
| Hibernate | ORM SQL | **6.x** |
| JJWT | Authentification JWT | **0.12.6** |
| Apache Commons Math 3 | Calcul scientifique / ODE | **3.6.1** |
| MySQL Connector/J | Pilote JDBC | latest |
| Maven | Build & dépendances | 3.x |
| JUnit 5 | Tests unitaires | inclus Spring Boot |
| Mockito | Mocks (tests) | inclus Spring Boot |
| JaCoCo | Couverture de code | **0.8.14** |
| H2 Database | BDD tests in-memory | inclus Spring Boot |

### Frontend

| Technologie | Catégorie | Version |
|---|---|---|
| Angular | Framework SPA | **21.2.0** |
| TypeScript | Langage | **5.9.x** |
| RxJS | Programmation réactive | **7.8.0** |
| Angular Router | Navigation SPA | inclus Angular |
| Angular Forms | Formulaires réactifs | inclus Angular |
| Angular HttpClient | Requêtes HTTP | inclus Angular |
| Canvas 2D API | Dessin navigateur | API native |
| Vitest | Tests unitaires | **4.0.8** |
| npm | Gestionnaire de paquets | 11.11.0 |

### Infrastructure

| Technologie | Catégorie | Version |
|---|---|---|
| MySQL | SGBD relationnel | **8.0** |
| Docker | Conteneurisation | latest |
| Docker Compose | Orchestration | latest |
| Nginx | Serveur web (prod) | latest |

---

## 11. Parcours de formation — De Java à Full Stack

Ce projet illustre une progression pédagogique complète.

### Les grandes étapes apprises

```
Java fondamental
  └─▶ Orienté objet (classes, héritage, interfaces, polymorphisme)
       └─▶ Collections, Generics, Lambdas, Stream API
            └─▶ Persistance avec JPA / Hibernate (mapping objet-relationnel)
                 └─▶ Spring Boot (IoC, injection de dépendances, REST)
                      └─▶ Sécurité (Spring Security, JWT, BCrypt)
                           └─▶ Tests (JUnit, Mockito, couverture JaCoCo)
                                └─▶ Angular / TypeScript (frontend SPA)
                                     └─▶ Docker (déploiement conteneurisé)
```

### Concepts démontrés dans ce projet

| Concept | Où c'est visible |
|---|---|
| **Héritage & polymorphisme** | `Spacecraft` (abstraite) → `Satellite`, `Rover`, `PodHabite`, `Utilitaire` |
| **Interfaces** | `IDAOSpacecraft extends JpaRepository<Spacecraft, Integer>` |
| **Enums** | `TYPE_ACTION`, `MISSION_STATUS`, `SPACECRAFT_TYPE`, `TYPE_COMPTE` |
| **Pattern Factory** | `Spacecraft.of(SPACECRAFT_TYPE)` |
| **Pattern Strategy** | `ActionRegistry` + 13 classes `Consumer<Double>` |
| **Injection de dépendances** | `@Autowired`, `@Service`, `@Repository` Spring |
| **ORM / SQL** | JPA + Hibernate → génération SQL automatique |
| **API REST** | 9 contrôleurs, verbes HTTP (GET, POST, PUT, PATCH, DELETE) |
| **JWT / Sécurité** | Filter HTTP, token signé, rôles ADMIN/OPERATEUR |
| **Programmation réactive** | RxJS `Observable` dans Angular (appels HTTP asynchrones) |
| **Composants Angular** | Architecture standalone, routing, lazy-loading |
| **Guards de navigation** | `authGuard`, `noAuthGuard` côté frontend |
| **Formulaires réactifs** | `ReactiveFormsModule`, validators, messages d'erreur |
| **Canvas 2D** | Dessin de la simulation orbitale dans le navigateur |
| **Tests automatisés** | JUnit 5, Mockito, H2, JaCoCo 60% seuil, Vitest |
| **Docker** | Conteneurisation, images multi-services, health checks |
| **Calcul scientifique** | Intégrateur ODE DormandPrince853, gravitation N-corps |

---

*Document généré pour la soutenance de fin de formation · Space Mission Control · SMC*
