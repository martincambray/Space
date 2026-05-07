CREATE DATABASE IF NOT EXISTS smc_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE smc_db;

-- =========================
-- UTILISATEUR
-- =========================
CREATE TABLE IF NOT EXISTS utilisateur 
(
    id        INT NOT NULL AUTO_INCREMENT,
    mail      VARCHAR(50) NOT NULL,
    password  VARCHAR(255) NOT NULL,
    lastname  VARCHAR(30),
    firstname VARCHAR(30),
    role      VARCHAR(30) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_utilisateur_mail (mail)
);

-- =========================
-- CORPS CÉLESTES
-- =========================
CREATE TABLE IF NOT EXISTS celestial_body (
    id             INT NOT NULL AUTO_INCREMENT,
    name           VARCHAR(30),
    mass           DOUBLE,          -- kg
    radius         DOUBLE,          -- km
    orbital_radius DOUBLE,          -- km depuis le soleil
    ref_coord_x    DOUBLE,
    ref_coord_y    DOUBLE,
    PRIMARY KEY (id)
);

-- =========================
-- TYPES DE MISSION
-- =========================
CREATE TABLE IF NOT EXISTS mission_type (
    id          INT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(30),
    description VARCHAR(255),
    PRIMARY KEY (id)
);

-- =========================
-- VAISSEAUX
-- =========================
CREATE TABLE IF NOT EXISTS spacecraft (
    id            INT NOT NULL AUTO_INCREMENT,
    name          VARCHAR(30),
    description   VARCHAR(255),
    battery_max   DOUBLE,
    fuel_capacity DOUBLE,
    type          VARCHAR(30) NOT NULL,  -- discriminateur JPA : SATELLITE, POD_HABITE, ROVER, UTILITAIRE
    solar_panel_deployed  BOOLEAN,
    o2_level              DOUBLE,
    odometer              DOUBLE,
    maintenance_count     INT,
    available             BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
    );

-- =========================
-- MISSIONS
-- =========================
CREATE TABLE IF NOT EXISTS mission (
    id                INT NOT NULL AUTO_INCREMENT,
    name              VARCHAR(30),
    status            VARCHAR(30) NOT NULL,
    operator_id       INT,
    spacecraft_id     INT,
    type_id           INT,
    departure_body_id INT,
    arrival_body_id   INT,
    departure_date    DATETIME(6),
    arrival_date      DATETIME(6),
    orbital_time      INT, -- minutes
    payload           VARCHAR(255),
    trajectory        TEXT,
    created_at        DATETIME(6),
    PRIMARY KEY (id),

    CONSTRAINT fk_mission_operator       FOREIGN KEY (operator_id)       REFERENCES utilisateur(id),
    CONSTRAINT fk_mission_spacecraft     FOREIGN KEY (spacecraft_id)     REFERENCES spacecraft(id),
    CONSTRAINT fk_mission_type           FOREIGN KEY (type_id)           REFERENCES mission_type(id),
    CONSTRAINT fk_mission_departure_body FOREIGN KEY (departure_body_id) REFERENCES celestial_body(id),
    CONSTRAINT fk_mission_arrival_body   FOREIGN KEY (arrival_body_id)   REFERENCES celestial_body(id)
);

-- =========================
-- LOG TRAJECTOIRE
-- =========================
CREATE TABLE IF NOT EXISTS trajectory_log (
    id            INT NOT NULL AUTO_INCREMENT,
    mission_id    INT,
    operator_id   INT,
    body_id       INT,
    computed_at   DATETIME(6),
    altitude      DOUBLE,   -- km
    initial_speed DOUBLE,   -- m/s
    mass          DOUBLE,   -- kg
    result_json   TEXT,
    PRIMARY KEY (id),

    CONSTRAINT fk_tlog_mission  FOREIGN KEY (mission_id)  REFERENCES mission(id),
    CONSTRAINT fk_tlog_operator FOREIGN KEY (operator_id) REFERENCES utilisateur(id),
    CONSTRAINT fk_tlog_body     FOREIGN KEY (body_id)     REFERENCES celestial_body(id)
);

-- =========================
-- DONNÉES : CORPS CÉLESTES
-- =========================
INSERT IGNORE INTO celestial_body (name, mass, radius, orbital_radius, ref_coord_x, ref_coord_y) VALUES
  ('Soleil',   1.989e30,  696340.0,           0.0,            0.0,   0.0),
  ('Mercure',  3.301e23,    2439.7,    57900000.0,     57900000.0,   0.0),
  ('Vénus',    4.867e24,    6051.8,   108200000.0,    108200000.0,   0.0),
  ('Terre',    5.972e24,    6371.0,   149600000.0,    149600000.0,   0.0),
  ('Lune',     7.342e22,    1737.4,      384400.0,    149984400.0,   0.0),
  ('Mars',     6.390e23,    3389.5,   227900000.0,    227900000.0,   0.0),
  ('Jupiter',  1.898e27,   71492.0,   778500000.0,    778500000.0,   0.0),
  ('Saturne',  5.683e26,   58232.0,  1432000000.0,   1432000000.0,  0.0),
  ('Uranus',   8.681e25,   25362.0,  2867000000.0,   2867000000.0,  0.0),
  ('Neptune',  1.024e26,   24622.0,  4495060000.0,   4495060000.0,  0.0);

-- =========================
-- TYPES DE MISSION
-- =========================
INSERT IGNORE INTO mission_type (name, description) VALUES
('Mise en Orbite',  'Orbite basse terrestre (~500 km)'),
('Mission Lunaire', 'Transfert Terre-Lune'),
('Interplanétaire', 'Transfert vers Mars ou autre planète');

-- =========================
-- UTILISATEURS
-- =========================
INSERT IGNORE INTO utilisateur (mail, password, lastname, firstname, role) VALUES

('admin.admin@smc.fr',
 '$2a$10$ZlMe5Sp64e0APHE8kli8vOPle1EkddBPxXXayQdcEgUt0hqYJlW0C',
 'Admin', 'Admin', 'ADMIN'),

('fabian.labonne@smc.fr',
 '$2a$10$Nh8dPgTTT7qD6/UQSgKZ9e6aKvnviCnMmTUFW7J2Q7qLnOqPIn22i',
 'Labonne', 'Fabian', 'OPERATEUR'),

('martin.cambray@smc.fr',
 '$2a$10$BGWibM67YQATYxSAnwmk5OKBDmf9vP6y390aEl1z8vH636VxHPYFm',
 'Cambray', 'Martin', 'OPERATEUR'),

('hugo.chittaro@smc.fr',
 '$2a$10$2chja0/cf0Wf6916vTturOG4cusXtLTIcNceCbQRcm4Nd9UQ7WA/a',
 'Chittaro', 'Hugo', 'OPERATEUR'),

('eric.ea@smc.fr',
 '$2a$10$F2aSEcSy90K/vxGesLyIDuDzgISYa6YdvVPg3MVbicjdwh0mAXJne',
 'Ea', 'Eric', 'OPERATEUR'),

('audric.olivier@smc.fr',
 '$2a$10$F3.pSQ0unnXUtvjq.sl0OuowAAmQSMR.QNNLHn6aH0L9B2Nzv9zoe',
 'Olivier', 'Audric', 'OPERATEUR'),

('mathias.dieu@smc.fr',
 '$2a$10$5.PJbpufpoLqkpllqGbgkOZ9GIWv.XlcLmg4UY8pSepv1RVi0A53u',
 'Dieu', 'Mathias', 'OPERATEUR');

-- =========================
-- VAISSEAUX (physiquement plausibles)
-- =========================
INSERT IGNORE INTO spacecraft (name, description, battery_max, fuel_capacity, type) VALUES
('Satellite',         'Observation en orbite',  20000.0,   2000.0, 'SATELLITE'),
('Pod Habité',        'Transport équipage',     18000.0, 120000.0, 'POD_HABITE'),
('Rover',             'Exploration surface',     6000.0,      0.0, 'ROVER'),
('Module utilitaire', 'Cargo orbital',           2000.0,      0.0, 'UTILITAIRE');

-- =========================
-- MISSIONS (cohérentes)
-- =========================
INSERT IGNORE INTO mission (name, status, operator_id, spacecraft_id, type_id,
                     departure_body_id, arrival_body_id,
                     departure_date, arrival_date, orbital_time, payload, created_at)
VALUES

-- Planifiée (durée estimée)
('Mission planned', 'PLANNED',
 2, 1, 1, 4, 4,
 '2027-03-15 06:00:00', NULL, 90,
 'Satellite mise en orbite', '2026-03-14 10:30:00'),

-- En cours
('Mission in progress', 'IN_PROGRESS',
 3, 2, 2, 4, 5,
 '2026-02-20 14:00:00', NULL, NULL,
 'Mission lunaire habitée', '2026-01-10 09:00:00'),

-- Terminée (durée cohérente)
('Mission completed', 'COMPLETED',
 4, 3, 3, 4, 6,
 '2025-02-28 03:00:00', '2025-09-15 12:00:00', 288000,
 'Rover martien', '2025-01-05 08:00:00'),

-- Annulée
('Mission cancelled', 'CANCELLED',
 5, 4, 1, 4, 4,
 '2026-05-01 10:00:00', NULL, NULL,
 'Mission cargo annulée', '2025-11-20 14:00:00');

-- =========================
-- TRAJECTOIRE
-- =========================
INSERT IGNORE INTO trajectory_log (mission_id, operator_id, body_id,
                             computed_at, altitude, initial_speed, mass, result_json)
VALUES
(1, 2, 4,
 '2026-03-14 11:00:00',
 550.0,
 7600.0,
 4300.0,
 '{"orbitalRadius":6921000,"orbitalSpeed":7590,"orbitalPeriod":5760}');

-- =========================
-- CHECK DATA
-- =========================
SELECT 'utilisateur'    AS table_name, COUNT(*) FROM utilisateur
UNION ALL
SELECT 'celestial_body', COUNT(*) FROM celestial_body
UNION ALL
SELECT 'mission_type', COUNT(*) FROM mission_type
UNION ALL
SELECT 'spacecraft', COUNT(*) FROM spacecraft
UNION ALL
SELECT 'mission', COUNT(*) FROM mission
UNION ALL
SELECT 'trajectory_log', COUNT(*) FROM trajectory_log;