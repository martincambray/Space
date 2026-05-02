# \[ **Space : Projet Final** \]

Console de gestion et planification de mission spatiale

## \[ Feuille de route \]
- [x] Pitch projet
- [x] Choix design front
- [ ] UML back \[refactor UML en cour\]
- [X] Choix librairie utilisées
- [X] Definition API
- [ ] Front
- [ ] Back

## \[ Politique commit \]
Definir la procédure pour partage de travail : nom des branches, scope du travail

## \[ Environnement de developemment \]
Techno utilisées :
- Spring boot : Back
- Mysql : Database
- Spring Boot : Test (Necessaire) ?
- HTML/CSS/JS : Front

| LIBRAIRIE | VERSION | SCOPE | COMMENT | MAEVEN |
|-|-|-|-|-|
| [EJML](https://ejml.org/wiki/index.php?title=Main_Page) | v.0.44.0 | Runtime | Manipulation de matrice Back|  [mvnrepo](https://mvnrepository.com/artifact/org.ejml/ejml-all/0.44.0) |
| [Three.js](https://threejs.org/) | ? | Runtime | Animation Front | ? |
| [Apache Common Math](https://commons.apache.org/proper/commons-math/userguide/ode.html) | v.3.6.1  | Runtime | ODE solveur Back | [mvnrepo](https://mvnrepository.com/artifact/org.apache.commons/commons-math3/3.6.1) |
| [jjwt](https://github.com/jwtk/jjwt) | v.0.13.0 | Runtime | Necessaire ? | [mvnrepo](https://mvnrepository.com/artifact/io.jsonwebtoken/jjwt-api) |
| spring-boot-starter-parent |  v.3.5.13 | Runtime | Spring boot | [mvnrepo](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-parent/3.5.13) |
| spring-boot-starter-web | Gestion Boot | Runtime | | [mvnrepo](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-web) |
| spring-boot-starter-data-jpa | Gestion Boot  | Runtime | | [mvnrepo](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-data-jpa) |
| spring-boot-starter-security | Gestion Boot  | Runtime | Necessaire ?| [mvnrepo](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-security)|
| spring-boot-devtools | Gestion Boot  | Runtime | | [mvnrepo](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-devtools)|
| spring-boot-starter-test | Gestion Boot | Test | | [mvnrepo](https://mvnrepository.com/artifact/org.springframework.boot/spring-boot-starter-test)|
| spring-security-test | Gestion Boot | Test | | [mvnrepo](https://mvnrepository.com/artifact/org.springframework.security/spring-security-test) |
| mysql-connector-j | Gestion Boot | Runtime | | [mvnrepo](https://mvnrepository.com/artifact/com.mysql/mysql-connector-j)|
| h2 | Gestion Boot | Test | Necessaire avec Spring Data JPA ? | [mvnrepo](https://mvnrepository.com/artifact/com.h2database/h2) |
| [jacoco](https://www.eclemma.org/jacoco/) | v.0.8.14 | Test | Coverage report ; Necessaire ? | [mvnrepo](https://mvnrepository.com/artifact/org.jacoco/jacoco-maven-plugin/0.8.14) |

Dockerfile :
```dockerfile
FROM
COPY
RUN
```

Image Docker devellopement

## \[ Liens ressources \]
- [Pitch projet](https://1drv.ms/f/c/747579825db81177/IgCKnbV9m7oPTa9rjsK56n47AX81FtHhe9t_7v0hsxMjpwk?e=SVcNDi)
- [UML back](https://lucid.app/lucidchart/ed78ac48-5230-44cf-9933-ff06c6be1fd6/edit?viewport_loc=2654%2C464%2C5439%2C2724%2C0_0&invitationId=inv_a9c266b7-a1cc-4444-b12a-d0f1f6fee16c)
- [Diagramme projet](https://lucid.app/lucidchart/f398e127-983e-49c3-b7a6-45bb57b0628a/edit?invitationId=inv_f0c64e7e-f75e-41aa-af23-ba7bdac2ead5)

