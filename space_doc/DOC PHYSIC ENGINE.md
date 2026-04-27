Here is the full documentation updated for Cartesian coordinates:

---

# `MoteurPhysique` - Doc

## 1. Responsabilité

`MoteurPhysique` est le moteur global de simulation pour ce projet. Pour le moment, celui-ci est uniquement (une seule instance via singleton) pour des raisons de simplicité, mais est voué à évoluer en fonction des performances du systèmes (voir la section [Evolution](#Evolution)). Il est responsable des calculs et des mises à jour des orbites des `Spacecraft` dans un repère Galiléen héliocentrique, utilisant des coordonnées Cartésiennes en 2D, en prenant en compte l'influence des objets présent dans le système solaire (Soleil, Jupiter, Terre, etc...).

Ce que ne fait **PAS** de le moteur physique :
- Stocker l'orbite de façons persistent, c'est le rôle de `Orbit`
- Prendre la décision de recalculer un orbite, c'est le role de `ActionRegistry` / `TableauDeBord`
- Mettre à jour les consommable des `Spacecraft`, c'est le role de `Spacecraft.updateConsummable()`

---
## 2. Séquence d'exécution et position dans le système

```
TableauDeBord
     │
     ▼
ActionRegistry ──► *Action.run()  ──► [modification delta_v du Spacecraft]
     │
     ▼
MoteurPhysique.eulerOrbitInit(state, t_start, thetaWindow) MoteurPhysique.perturbateOrbit(state, currentOrbit, t_start, thetaWindow)
     │
     ├── lit:  List<CelestialBody>  (perturbation)
     ├── lit:  Spacecraft.mass, Spacecraft.deltaV
     ├── lit:  vecteur du Spacecraft [x, y, vx, vy]
     │
     ▼
   Orbit  (List<double[]> of [x, y] points)
```

---

## 3. Structure des données

Le `Moteur Physique` réalise ses calcul dans un **référentiel 2D Cartésien héliocentré**

| Symbole               | Commentaire                                                                                       | Unité |
| --------------------- | ------------------------------------------------------------------------------------------------- | ----- |
| $$x$$                 | Position sur l'axe X                                                                              | m     |
| $$y$$                 | Position sur l'axe Y                                                                              | m     |
| $$v_x$$               | Vitesse sur l'axe X                                                                               | m/s   |
| $$v_y$$               | Vitesse sur l'axe X                                                                               | m/s   |
| $$\theta$$            | Position angulaire centrée sur l'orbite                                                           | rad   |
| $${\theta}_{window}$$ | Condition d'arrêt pour le calcul `Orbit` <br>$0$  : position actuelle<br>$2\pi$ : orbite complète | rad   |

Le vecteur transmis à l'ODE est donc le suivant (mapping direct à `Orbit.trajectoire`), chaque intégration renvoie une paire de `[x, y]`

```
y = [x, y, vx, vy]
```

Afin d'éviter de recalculer l'entièreté de `Orbit` à chaque demande de mise à jour, `StepHandler` utilise la position angulaire $\theta$ définie ([Pourquoi utiliser atan2(y,x)](https://stackoverflow.com/questions/283406/what-is-the-difference-between-atan-and-atan2-in-c)) :


$$
\theta = \arctan(y,x)
$$

```
θ = atan2(y, x)
```

et la compare à la condition d'arrêt ${\theta}_{window}$

Ici et dans le reste de cette doc, `StepHandler` fait référence à l'interface de Apache Common Math

---

## 4. Attributs et Méthodes

### 4.1 Attributs
```java
public class MoteurPhysique {

    // Contante gravité
    private static final double G = 6.674e-11;

	// Resolution temporel
    private double dt;

    // Ensemble des corps du système solaire
    private final List<CelestialBody> celestialBodies;

    // ODE integrator de Apache Common Math
    // ClassicalRungeKuttaIntegrator
    // DormandPrince853Integrator
    private FirstOrderIntegrator integrator;
}
```

---

### 4.2. Méthodes

#### `eulerOrbitInit`

```java
public Orbit eulerOrbitInit(double x0, double y0,
                             double vx0, double vy0)
```

**Rôle:** Initialise une nouvelle `Orbit`

**Input:**

| Paramètre         | Type     | Commentaire                   |
| ----------------- | -------- | ----------------------------- |
| $$x_0,y_0$$       | `double` | Position initale (m)          |
| $$v_{x0},v_{y0}$$ | `double` | Vecteur vitesse initial (m/s) |


**Processus:**
1. Création du vecteur résultat initiale :$y_0 = \begin{bmatrix}x_0\\y_0\\v_{x0}\\v_{y0}\end{bmatrix}$
2. Exécution de `integrate(y0, t_start, thetaWindow)` avec $t_{start} = 0$, $\theta_{window} = 2\pi$ 
3. Renvoie une nouvelle `Orbit`

**Output:** `Orbit` complète

---

#### 4.2.2 `perturbateOrbit`

```java
public Orbit perturbateOrbit(Orbit currentOrbit,
                              double[] currentStateVector,
                              double t_start, double thetaWindow)
````

**Rôle :** Met à jour une `Orbit` existante en la perturbant suivant la méthode de _Cowell_ (cf. [Méthode de Cowell](https://en.wikipedia.org/wiki/Orbit_modeling#Cowell's_method)) à l'initialisation ou après l'exécution d'une action si nécessaire (exemple : `ChangerPropulsionAction` modifie le $\Delta v$)

**Entrées :**

|Paramètre|Type|Commentaire|
|---|---|---|
|`currentOrbit`|`Orbit`|L'orbite à mettre à jour|
|`currentStateVector`|`double[]`|`[x, y, vx, vy]` au moment de la perturbation|
|`t_start`|`double`|instant temporel où la perturbation survient (s)|
|`thetaWindow`|`double`|borne supérieure pour le recalcul de l'orbite (rad)|

**Processus :**

1. Reprend l’intégration à partir de `currentStateVector` à `t_start`
2. Exécute le solveur d’EDO pendant `dtWindow` secondes
3. Remplace uniquement le segment `[t_start, t_start + dtWindow]` de `currentOrbit.trajectoire` — tout ce qui précède `t_start` reste inchangé


**Output :** L’`Orbit` mise à jour (même objet, seul le segment futur est remplacé)

---

#### 4.2.3 `buildODE` _(private)_

```java
private FirstOrderDifferentialEquations buildODE(double spacecraftMass)
```

**Rôle :** Construit l’EDO Apache Commons Math qui encode les équations du mouvement newtonien en coordonnées cartésiennes.

**Équations du mouvement :**

En coordonnées cartésiennes, l’accélération sur le vaisseau due à tous les corps est une somme vectorielle directe ([Méthode de Cowell](https://en.wikipedia.org/wiki/Orbit_modeling#Cowell's_method))

$$\ddot{x} = \sum_k \frac{G \cdot m_k \cdot (x_k - x)}{d_k^3}$$

$$\ddot{y} = \sum_k \frac{G \cdot m_k \cdot (y_k - y)}{d_k^3}$$

où $d_k = \sqrt{(x_k - x)^2 + (y_k - y)^2}$ est la distance entre le vaisseau et le corps $k$.

Le Soleil est inclus dans `celestialBodies` à la position `(0, 0)` comme terme dominant. Le vecteur dérivé d’état retourné par l’EDO est :

```
ẏ = [vx, vy, ẍ, ÿ]
```

**Output :** Une instance de `FirstOrderDifferentialEquations` prête à être passée à l’intégrateur.

---

### 4.2.4 `integrate` _(private)_

```java
private List<double[]> integrate(double[] y0, double t_start, double dtWindow)
```

**Objectif :** Exécute l’intégrateur d’EDO en utilisant Apache Commons Math, en collectant un point `[x, y]` par étape.

**Processus :**

1. Configure un `StepHandler` qui enregistre `[y[0], y[1]]` (c.-à-d. `x` et `y`) à chaque étape
2. Intègre de `t_start` à `t_start + dtWindow` avec un pas `dt`
3. Retourne la liste collectée de points `[x, y]`

Puisque le vecteur d’état est déjà en coordonnées cartésiennes, le `StepHandler` lit simplement les indices 0 et 1 directement — aucune trigonométrie impliquée.

---

### 4.2.5 `computeAcceleration` _(private)_

```java
private double[] computeAcceleration(double x, double y, double spacecraftMass)
```

**Rôle :** Pour une position donnée du vaisseau `(x, y)`, somme les contributions d’accélération gravitationnelle de tous les objets `CelestialBody` dans `celestialBodies` et retourne `[ax, ay]` — le vecteur d’accélération total en composantes cartésiennes.

**Processus :** Pour chaque corps $k$ à la position $(x_k, y_k)$ :

```java
double dx = body.coords[0] - x;
double dy = body.coords[1] - y;
double dist = Math.sqrt(dx*dx + dy*dy);
double factor = G * body.mass / (dist * dist * dist);
ax += factor * dx;
ay += factor * dy;
```

---

## 7. Résumé des I/O

| Type       | Description                       | Commentaire                                                                                                     |
| ---------- | --------------------------------- | --------------------------------------------------------------------------------------------------------------- |
| **Input**  | `SpacecraftState` (ou `double[]`) | `[x, y, vx, vy]` + masse + delta_v                                                                              |
| **Input**  | `double t_start`                  | Temps de simulation de départ (s)                                                                               |
| **Input**  | `double dtWindow`                 | Durée à calculer vers l’avant (s)                                                                               |
| **Input**  | `List<CelestialBody>`             | Perturbateurs gravitationnels (stockés en interne)                                                              |
| **Output** | `Orbit`                           | Orbite mise à jour ou nouvellement créée avec une `List<double[]>` de points de trajectoire cartésiens `[x, y]` |

## 8. Dépendences

| Dependence                                     | Commentaire                                             |
| ---------------------------------------------- | ------------------------------------------------------- |
| `Apache Commons Math` — `FirstOrderIntegrator` | ODE (RK4 / DormandPrince)                               |
| `CelestialBody`                                | Corps du système solaire, perturbateur gravitationnel   |
| `Orbit`                                        | Données de sortie                                       |
| `Spacecraft`                                   | Caractéristique du vaisseau : masse et vecteur associés |

