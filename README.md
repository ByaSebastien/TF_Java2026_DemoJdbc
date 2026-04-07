# DemoJdbc — Du SQL brut vers un mini-ORM maison

---

## Table des matières

1. [JDBC — Qu'est-ce que c'est et pourquoi ça existe ?](#1-jdbc--quest-ce-que-cest-et-pourquoi-ça-existe-)
2. [Le problème du JDBC brut](#2-le-problème-du-jdbc-brut)
3. [Le pattern Repository — pourquoi séparer l'accès aux données ?](#3-le-pattern-repository--pourquoi-séparer-laccès-aux-données-)
4. [Ce qu'on a construit — le BaseRepository](#4-ce-quon-a-construit--le-baserepository)
5. [Les annotations — pourquoi en avait-on besoin ?](#5-les-annotations--pourquoi-en-avait-on-besoin-)
6. [La réflexion — le moteur invisible](#6-la-réflexion--le-moteur-invisible)
7. [Ce qu'il reste à faire à la main — et pourquoi c'est normal](#7-ce-quil-reste-à-faire-à-la-main--et-pourquoi-cest-normal)
8. [Ce que cette expérience vous a vraiment appris](#8-ce-que-cette-expérience-vous-a-vraiment-appris)

---

## 1. JDBC — Qu'est-ce que c'est et pourquoi ça existe ?

### Le problème de départ

Une application Java et une base de données relationnelle parlent des langages fondamentalement différents.

- Java parle d'**objets** : `Author`, `Book`, listes, méthodes...
- Une base de données parle de **tables**, de **lignes** et de **colonnes** via le langage **SQL**.

Il faut donc un **traducteur** entre ces deux mondes. C'est exactement le rôle de **JDBC** *(Java Database Connectivity)*.

### Ce qu'est JDBC

JDBC est une **API standard** incluse dans Java (depuis 1997) qui définit un ensemble d'interfaces permettant à n'importe quelle application Java de communiquer avec n'importe quelle base de données relationnelle.

```
Application Java
      │
      │  (appels JDBC standard)
      ▼
  JDBC API          ← vous écrivez votre code contre cette interface
      │
      │  (implémentation spécifique)
      ▼
Driver PostgreSQL   ← fourni par PostgreSQL (dépendance Maven)
      │
      │  (protocole réseau propriétaire)
      ▼
Base de données PostgreSQL
```

> **L'idée clé :** vous écrivez votre code contre l'interface `JDBC`, pas contre PostgreSQL directement.  
> Si demain vous changez de base de données (MySQL, Oracle, SQLite...), vous changez uniquement le **driver** — votre code Java reste identique.

### Les 4 étapes fondamentales de JDBC

Toute interaction avec la base de données suit toujours ce même cycle :

```java
// 1. Ouvrir une connexion à la base de données
Connection conn = DriverManager.getConnection(url, user, password);

// 2. Préparer la requête SQL (les "?" sont des paramètres, jamais concaténés directement)
PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM Author WHERE id = ?");

// 3. Remplir les paramètres et exécuter
pstmt.setInt(1, 42);
ResultSet rs = pstmt.executeQuery();

// 4. Lire les résultats ligne par ligne
while (rs.next()) {
    String firstName = rs.getString("firstName");
    // ...
}

// 5. Fermer toutes les ressources (ou utiliser try-with-resources)
rs.close();
pstmt.close();
conn.close();
```

### Pourquoi utiliser des `?` et jamais la concaténation ?

```java
// ❌ DANGEREUX — injection SQL possible
String query = "SELECT * FROM Author WHERE name = '" + userInput + "'";
// Si userInput = "'; DROP TABLE Author; --"  → catastrophe

// ✅ SÛR — le driver échappe automatiquement les valeurs
String query = "SELECT * FROM Author WHERE name = ?";
pstmt.setString(1, userInput); // sécurisé quoi que contienne userInput
```

Un `PreparedStatement` **prépare** la structure SQL à l'avance et remplace les `?` de manière sécurisée. C'est la base de toute interaction JDBC robuste.

### Le try-with-resources — pourquoi c'est obligatoire

`Connection`, `PreparedStatement` et `ResultSet` sont des **ressources physiques** (connexions réseau, curseurs en mémoire côté serveur). Si on oublie de les fermer :

- Les connexions restent ouvertes → la base de données sature
- Les curseurs s'accumulent → fuite mémoire côté serveur

Le `try-with-resources` garantit la fermeture automatique, même en cas d'exception :

```java
try (Connection conn = getConnection();
     PreparedStatement pstmt = conn.prepareStatement(query);
     ResultSet rs = pstmt.executeQuery()) {
    // utilisation...
} // conn, pstmt et rs sont fermés automatiquement ici, quoi qu'il arrive
```

> Ces classes implémentent l'interface `AutoCloseable`, ce qui est la condition pour être utilisables dans un `try-with-resources`.

---

## 2. Le problème du JDBC brut

Regardons à quoi ressemblait notre `AuthorRepository` **avant** le refactoring :

```java
public int save(Author author) {
    try (Connection conn = getConnection()) {
        String query = "INSERT INTO Author (firstName, lastName, birthDate) VALUES (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
        pstmt.setString(1, author.getFirstName());
        pstmt.setString(2, author.getLastName());
        pstmt.setDate(3, Date.valueOf(author.getBirthDate()));
        pstmt.executeUpdate();
        ResultSet rs = pstmt.getGeneratedKeys();
        if (!rs.next()) throw new RuntimeException("Insert failed");
        return rs.getInt(1);
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
}
```

Et idem pour `findAll`, `findById`, `update`, `delete`... Le même schéma répété ad nauseam.

### Ce qui pose problème

| Problème | Impact |
|---|---|
| **Répétition** | Chaque méthode réécrit la même structure try/catch/close |
| **Fragilité** | Changer le nom d'une colonne en DB oblige à modifier le code Java à la main |
| **Non-scalable** | Pour chaque nouvelle entité (`Book`, `Customer`...), on recopie tout |
| **Couplage fort** | La logique métier et les détails SQL sont mélangés |

> Ce n'est pas que JDBC soit mauvais — c'est qu'écrire du **JDBC brut** pour chaque entité est **fastidieux et source d'erreurs**.  
> La question devient : *peut-on factoriser tout ce code répétitif ?*

---

## 3. Le pattern Repository — pourquoi séparer l'accès aux données ?

### Le principe

Le **pattern Repository** consiste à isoler tout le code d'accès à la base de données dans des classes dédiées, séparées du reste de l'application.

```
┌─────────────────────────────────────────┐
│  Couche métier (Main, Services...)      │
│  → parle d'objets Java (Author, Book)   │
│  → ignore totalement SQL et JDBC        │
└──────────────────┬──────────────────────┘
                   │ appelle
┌──────────────────▼──────────────────────┐
│  Couche Repository (AuthorRepository)   │
│  → parle SQL et JDBC                    │
│  → traduit : objet Java ↔ ligne SQL     │
└──────────────────┬──────────────────────┘
                   │ se connecte
┌──────────────────▼──────────────────────┐
│  Base de données                        │
└─────────────────────────────────────────┘
```

### Pourquoi c'est important

- **Lisibilité** : le code métier ne voit que `authorRepository.findById(1)`, pas une seule ligne SQL
- **Testabilité** : on peut remplacer le vrai repository par un "faux" pour les tests
- **Interchangeabilité** : si on passe de PostgreSQL à MySQL, seule la couche Repository change

Ce pattern est universellement utilisé dans les applications professionnelles, quelle que soit la technologie sous-jacente.

---

## 4. Ce qu'on a construit — le BaseRepository

### La question centrale

> *Puisque toutes les entités ont les mêmes opérations (findAll, findById, save, update, delete, count, exists), peut-on écrire ces opérations UNE SEULE FOIS pour toutes les entités ?*

La réponse est **oui** — à condition de résoudre deux problèmes :

1. **Comment connaître le nom de la table et des colonnes** sans le coder en dur pour chaque entité ?
2. **Comment lire les valeurs des champs** d'un objet dont on ne connaît pas le type exact ?

Notre réponse à ces deux questions constitue l'architecture du `BaseRepository`.

### La solution en deux parties

```
┌─────────────────────────────────────────────────────────┐
│                    ENTITÉ (Author, Book)                  │
│                                                           │
│  @Table(name="Author")     ← dit à quel table map        │
│  @Id(generation=GENERATED) ← dit quelle est la PK        │
│  @Column(name="firstName") ← dit quel est le nom colonne │
│  @NavigationProperty       ← dit quoi ignorer            │
│                                                           │
│  → Les annotations = le "contrat" entre entité et DB     │
└─────────────────────────────┬───────────────────────────┘
                              │ lues par la réflexion
┌─────────────────────────────▼───────────────────────────┐
│                  BaseRepository<TEntity, TId>            │
│                                                           │
│  Lit les annotations au démarrage → génère les requêtes  │
│  Appelle buildEntity() → obtient les objets              │
│                                                           │
│  → La réflexion = le "moteur" qui lit le contrat         │
└─────────────────────────────────────────────────────────┘
```

### Le résultat concret

Avant notre refactoring, `AuthorRepository` faisait **149 lignes** de code SQL/JDBC répétitif.

Après :

```java
public class AuthorRepository extends BaseRepository<Author, Integer> {

    @Override
    protected Author buildEntity(ResultSet rs) throws SQLException {
        return new Author(
            rs.getInt("id"),
            rs.getString("firstName"),
            rs.getString("lastName"),
            rs.getDate("birthDate").toLocalDate()
        );
    }
}
```

**~10 lignes.** Toutes les méthodes CRUD sont héritées, toutes les requêtes SQL sont générées automatiquement.

Et pour créer le repository d'une nouvelle entité `Customer` ? Même chose : annoter l'entité + implémenter `buildEntity`. C'est tout.

---

## 5. Les annotations — pourquoi en avait-on besoin ?

### Le problème sans annotations

Si `BaseRepository` ne connaît pas le nom de la table ni des colonnes, il ne peut pas générer les requêtes SQL. Sans mécanisme de configuration, on est obligé de passer ces informations au constructeur :

```java
// Sans annotations : fragile et verbeux
public AuthorRepository() {
    super("Author", "id", true); // facile à se tromper, refactoring dangereux
}
```

### La solution : les annotations comme métadonnées

Une **annotation** est une façon d'**attacher des informations supplémentaires** directement sur un élément du code (classe, champ, méthode) — sans modifier son comportement intrinsèque.

```java
@Table(name = "Author")      // ← information : "cette classe correspond à la table Author"
public class Author {

    @Id(generation = GENERATED)  // ← information : "ce champ est la PK auto-générée"
    @Column(name = "id")         // ← information : "la colonne DB s'appelle 'id'"
    private Integer id;

    @NavigationProperty          // ← information : "ce champ n'a pas de colonne DB"
    private Author author;
}
```

Ces annotations ne font **rien par elles-mêmes** — elles sont de simples étiquettes. C'est `BaseRepository` qui les lit et leur donne du sens.

### Pourquoi ce design est puissant

> Les annotations constituent un **contrat déclaratif** entre l'entité et le repository.
> 
> Au lieu de dire *"comment"* faire (logique impérative), on déclare *"ce que c'est"* (métadonnées descriptives).

Si demain le nom de la table change en base, il suffit de modifier `@Table(name = "authors")` dans l'entité — une seule ligne, à un seul endroit, et toutes les requêtes de tous les repositories s'adaptent automatiquement.

---

## 6. La réflexion — le moteur invisible

### Qu'est-ce que la réflexion ?

La **réflexion** (*reflection*) est la capacité d'un programme à **s'inspecter lui-même** à l'exécution.

Sans réflexion, le code doit tout savoir à la compilation. Avec la réflexion, le code peut poser des questions sur sa propre structure au moment où il s'exécute :

> *"Quels champs a cette classe ? L'un d'eux est-il annoté @Id ? Quel est son nom ? Quelle est sa valeur dans cet objet ?"*

### Ce que ça permet concrètement

```java
// Sans réflexion : on doit savoir à l'avance que c'est un Author avec ces champs précis
pstmt.setString(1, author.getFirstName());
pstmt.setString(2, author.getLastName());

// Avec réflexion : on découvre les champs à l'exécution, pour N'IMPORTE quelle entité
for (Field field : entityClass.getDeclaredFields()) {
    pstmt.setObject(index++, field.get(entity)); // fonctionne pour Author, Book, Customer...
}
```

### Pourquoi c'est la clé de la généricité

C'est grâce à la réflexion que `BaseRepository` peut :

- **Détecter** le champ `@Id` sans savoir à l'avance s'il s'appelle `id`, `isbn` ou `customerId`
- **Lire** la valeur de n'importe quel champ d'un objet, même `private`
- **Construire** dynamiquement des requêtes SQL pour n'importe quelle entité
- **Fonctionner** identiquement pour `Author` (PK Integer générée) et `Book` (PK String manuelle)

> La réflexion est le mécanisme qui transforme du code générique en comportement spécifique à l'exécution.

---

## 7. Ce qu'il reste à faire à la main — et pourquoi c'est normal

Notre `BaseRepository` automatise la **construction des requêtes SQL** et leur **exécution**.  
Mais il reste une chose qu'on fait encore manuellement dans chaque repository : `buildEntity`.

```java
@Override
protected Author buildEntity(ResultSet rs) throws SQLException {
    return new Author(
        rs.getInt("id"),
        rs.getString("firstName"),   // ← encore du code manuel
        rs.getString("lastName"),
        rs.getDate("birthDate").toLocalDate()
    );
}
```

### Pourquoi ce code reste manuel chez nous

Pour automatiser ce mapping aussi, il faudrait que `BaseRepository` sache comment **construire** un objet Java depuis un `ResultSet` — c'est-à-dire trouver le bon constructeur, lui passer les bons arguments, dans le bon ordre, avec les bons types.

C'est techniquement faisable par réflexion, mais ça pose des questions complexes :

- Comment choisir entre plusieurs constructeurs ?
- Comment convertir une `java.sql.Date` en `LocalDate` automatiquement ?
- Que faire si un champ est `null` en base ?
- Comment gérer des types complexes, des enums, des relations ?

### Ce que ça implique

Ce mapping "objet ↔ résultat SQL" est **le problème central** de la persistance de données en Java. Ce n'est pas anodin — c'est un problème sur lequel des équipes entières ont travaillé pendant des années pour produire des solutions robustes, avec des règles précises, des conventions et des mécanismes de configuration avancés.

Notre `buildEntity` est en réalité la partie **la plus difficile à généraliser** — et c'est précisément là que réside toute la complexité des grands frameworks de persistance.

> **Retenez ceci :** la frontière entre ce que *nous* automatisons et ce que *nous* faisons encore à la main est exactement la frontière entre JDBC brut et les outils qui vont plus loin.  
> Vous avez reproduit manuellement les mécanismes fondamentaux qui se cachent derrière ces outils. Vous savez maintenant *pourquoi* ils existent.

---

## 8. Ce que cette expérience vous a vraiment appris

### Les concepts maîtrisés

| Concept | Ce que vous avez fait |
|---|---|
| **JDBC** | Connexion, PreparedStatement, ResultSet, transactions manuelles |
| **Généricité** | `BaseRepository<TEntity, TId>` — un seul code pour N entités |
| **Annotations** | Créer vos propres annotations avec `@Target` et `@Retention` |
| **Réflexion** | Lire la structure d'une classe et les valeurs de ses champs à l'exécution |
| **Pattern Repository** | Séparer la logique d'accès aux données du reste de l'application |
| **Pattern Template Method** | `BaseRepository` définit l'algorithme, `AuthorRepository` fournit la pièce manquante |
| **Transactions** | `setAutoCommit(false)`, `commit()`, `rollback()` |

### La vraie leçon

Vous n'avez pas juste appris à faire du JDBC.

Vous avez **réinventé à la main** les fondations de ce que font les grands frameworks de persistance.

En passant du JDBC brut à votre `BaseRepository`, vous avez résolu exactement les mêmes problèmes que ces frameworks ont résolus :

- ✅ Éliminer le code SQL répétitif
- ✅ Mapper automatiquement les champs Java aux colonnes SQL via des métadonnées
- ✅ Gérer les différentes stratégies de clé primaire
- ✅ Ignorer les propriétés qui n'existent pas en base
- ✅ Fournir un ensemble standard d'opérations CRUD réutilisables

La différence ? Les frameworks vont **beaucoup plus loin** — ils automatisent aussi le `buildEntity`, gèrent les relations entre entités, le cache, les migrations de schéma, les requêtes complexes...

Mais maintenant que vous avez construit les fondations vous-même, quand vous découvrirez ces frameworks, vous ne verrez plus de la magie. Vous verrez des **solutions à des problèmes que vous connaissez déjà**.

---

## Structure du projet

```
src/main/java/be/bstorm/
│
├── annotations/                    ← Les "étiquettes" qui décrivent les entités
│   ├── GenerationType.java         Enum : GENERATED / NOT_GENERATED
│   ├── Id.java                     @Id → marque la clé primaire
│   ├── Column.java                 @Column → nom de colonne en DB
│   ├── NavigationProperty.java     @NavigationProperty → champ à ignorer en SQL
│   └── Table.java                  @Table → nom de table en DB
│
├── entities/                       ← Les objets Java qui représentent les tables
│   ├── Author.java                 Entité avec PK auto-générée (Integer)
│   └── Book.java                   Entité avec PK manuelle (String ISBN)
│
├── repositories/                   ← L'accès à la base de données
│   ├── BaseRepository.java         Le cœur générique — toutes les requêtes CRUD
│   ├── AuthorRepository.java       10 lignes : juste buildEntity()
│   └── BookRepository.java         10 lignes : juste buildEntity()
│
├── utils/
│   └── ConnectionUtils.java        Centralise les paramètres de connexion JDBC
│
└── Main.java                       Démonstration de toutes les opérations
```

## Configuration requise

- Java 25+
- PostgreSQL avec une base `demo_jdbc`
- Modifier `ConnectionUtils.java` si vos identifiants diffèrent des valeurs par défaut (`postgres` / `postgres`)

```sql
-- Schéma minimal pour faire tourner les exemples
CREATE TABLE Author (
    id        SERIAL PRIMARY KEY,
    firstName VARCHAR(100),
    lastName  VARCHAR(100),
    birthDate DATE
);

CREATE TABLE Book (
    isbn        VARCHAR(20) PRIMARY KEY,
    title       VARCHAR(200),
    description TEXT,
    authorId    INTEGER REFERENCES Author(id)
);
```

