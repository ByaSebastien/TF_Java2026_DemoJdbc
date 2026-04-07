package be.bstorm.repositories;

import be.bstorm.annotations.*;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static be.bstorm.utils.ConnectionUtils.getConnection;

/**
 * Repository générique qui génère automatiquement les requêtes CRUD via la <b>réflexion</b>.
 *
 * <hr>
 * <h2>Concept : Classe abstraite générique</h2>
 * <p>Cette classe est {@code abstract} car elle ne peut pas être utilisée directement :
 * elle ne sait pas comment transformer une ligne SQL ({@link ResultSet}) en objet Java.
 * Chaque sous-classe concrète doit implémenter {@link #buildEntity(ResultSet)} pour
 * définir ce mapping.
 *
 * <p>{@code <TEntity, TId>} sont des <b>paramètres de type</b> (génériques) :
 * <ul>
 *   <li>{@code TEntity} = le type de l'entité gérée (ex: {@code Author}, {@code Book})</li>
 *   <li>{@code TId}     = le type de la clé primaire (ex: {@code Integer}, {@code String})</li>
 * </ul>
 * Ces types sont précisés lors de l'héritage : {@code extends BaseRepository<Author, Integer>}.
 *
 * <hr>
 * <h2>Concept : Réflexion (java.lang.reflect)</h2>
 * <p>La réflexion permet d'inspecter et manipuler la structure d'une classe <em>au moment
 * de l'exécution</em>, sans connaître le type exact à la compilation. On peut :
 * <ul>
 *   <li>Lister les champs d'une classe : {@code Class.getDeclaredFields()}</li>
 *   <li>Lire/écrire la valeur d'un champ : {@code Field.get(object)}</li>
 *   <li>Détecter la présence d'une annotation : {@code Field.isAnnotationPresent(...)}</li>
 *   <li>Lire la valeur d'une annotation : {@code Field.getAnnotation(...).attribut()}</li>
 * </ul>
 * C'est grâce à la réflexion que ce repository peut fonctionner avec n'importe quelle
 * entité sans qu'on ait à réécrire le code SQL pour chacune.
 *
 * <hr>
 * <h2>Prérequis pour une entité compatible</h2>
 * <ul>
 *   <li>Exactement un champ annoté avec {@link Id}.</li>
 *   <li>Optionnellement {@link Table} sur la classe pour préciser le nom de table.</li>
 *   <li>Optionnellement {@link Column} sur les champs pour préciser les noms de colonnes.</li>
 *   <li>Les propriétés de navigation doivent être annotées avec {@link NavigationProperty}.</li>
 * </ul>
 *
 * @param <TEntity> le type de l'entité (ex: {@code Author})
 * @param <TId>     le type de la clé primaire (ex: {@code Integer})
 */
public abstract class BaseRepository<TEntity, TId> {

    // =========================================================================
    // Attributs — initialisés une seule fois dans le constructeur par réflexion
    // =========================================================================

    /** La classe Java de l'entité (ex: {@code Author.class}). Utilisée pour la réflexion. */
    private final Class<TEntity> entityClass;

    /** Nom de la table en base de données (résolu depuis {@link Table} ou le nom de classe). */
    private final String tableName;

    /** Le champ Java annoté {@link Id} — représente la clé primaire de l'entité. */
    private final Field idField;

    /** Nom de la colonne PK en base de données (résolu depuis {@link Column} ou le nom du champ). */
    private final String idColumnName;

    /**
     * {@code true} si la PK est auto-générée par la DB ({@link GenerationType#GENERATED}),
     * {@code false} si elle est fournie par le code ({@link GenerationType#NOT_GENERATED}).
     */
    private final boolean isGenerated;

    // =========================================================================
    // Constructeur — résolution complète par réflexion à l'instanciation
    // =========================================================================

    /**
     * Initialise le repository en lisant les annotations de l'entité par réflexion.
     * Ce travail est fait <b>une seule fois</b> lors de la création du repository,
     * puis mis en cache dans les champs finaux ci-dessus.
     *
     * <p><b>Concept : récupérer TEntity à l'exécution</b><br>
     * Les génériques Java sont effacés à la compilation (<em>type erasure</em>), mais
     * l'information reste accessible via {@code getGenericSuperclass()} lorsqu'une
     * sous-classe concrète déclare explicitement le type :
     * <pre>
     * {@code
     * // Dans AuthorRepository.java :
     * public class AuthorRepository extends BaseRepository<Author, Integer>
     *                                                       ^^^^^^  ^^^^^^^
     *                                                    TEntity   TId
     * // → getGenericSuperclass() retourne "BaseRepository<Author, Integer>"
     * // → getActualTypeArguments()[0] retourne Author.class
     * }
     * </pre>
     */
    @SuppressWarnings("unchecked") // cast (Class<TEntity>) vérifié manuellement
    protected BaseRepository() {

        // ------------------------------------------------------------------
        // Étape 1 : récupérer la classe réelle de TEntity via la réflexion
        // ------------------------------------------------------------------
        // getClass()              → la classe concrète (ex: AuthorRepository)
        // getGenericSuperclass()  → le parent avec ses paramètres de type (ex: BaseRepository<Author, Integer>)
        // (ParameterizedType)     → on caste pour accéder aux paramètres de type
        // getActualTypeArguments()[0] → le 1er paramètre = TEntity = Author.class
        Type genericSuperclass = getClass().getGenericSuperclass();
        ParameterizedType paramType = (ParameterizedType) genericSuperclass;
        this.entityClass = (Class<TEntity>) paramType.getActualTypeArguments()[0];

        // ------------------------------------------------------------------
        // Étape 2 : déterminer le nom de la table
        // ------------------------------------------------------------------
        // Priorité : @Table(name="xxx") > @Table (→ nom de classe) > nom de classe
        if (entityClass.isAnnotationPresent(Table.class)) {
            String declared = entityClass.getAnnotation(Table.class).name();
            // Si name="" (valeur par défaut), on prend le nom simple de la classe
            this.tableName = declared.isBlank() ? entityClass.getSimpleName() : declared;
        } else {
            // Pas d'annotation @Table → on utilise le nom simple de la classe
            this.tableName = entityClass.getSimpleName();
        }

        // ------------------------------------------------------------------
        // Étape 3 : localiser le champ @Id (clé primaire)
        // ------------------------------------------------------------------
        // getDeclaredFields() retourne TOUS les champs de la classe (même private),
        // mais PAS ceux des classes parentes.
        Field foundId = null;
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                if (foundId != null) {
                    // Protection : une entité ne peut avoir qu'une seule PK
                    throw new IllegalStateException(
                            entityClass.getSimpleName() + " a plus d'un champ @Id.");
                }
                foundId = field;
            }
        }
        if (foundId == null) {
            throw new IllegalStateException(
                    "Aucun champ @Id trouvé sur " + entityClass.getSimpleName());
        }

        this.idField = foundId;
        // setAccessible(true) est indispensable pour lire un champ "private"
        // par réflexion (sans cela une IllegalAccessException serait levée)
        this.idField.setAccessible(true);

        // ------------------------------------------------------------------
        // Étape 4 : résoudre le nom de colonne de la PK et la stratégie de génération
        // ------------------------------------------------------------------
        this.idColumnName = resolveColumnName(idField);
        this.isGenerated = idField.getAnnotation(Id.class).generation() == GenerationType.GENERATED;
    }

    // =========================================================================
    // Méthodes privées utilitaires (réflexion)
    // =========================================================================

    /**
     * Résout le nom de colonne DB pour un champ donné.
     *
     * <p>Logique : si le champ possède {@code @Column(name="xxx")} avec un nom non vide,
     * on l'utilise ; sinon on prend le nom Java du champ.
     *
     * @param field le champ Java à analyser
     * @return le nom de la colonne correspondante en base de données
     */
    private String resolveColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            String declared = field.getAnnotation(Column.class).name();
            // name="" → valeur par défaut de l'annotation → utilise le nom du champ
            return declared.isBlank() ? field.getName() : declared;
        }
        // Pas d'@Column → le nom Java et le nom DB sont identiques
        return field.getName();
    }

    /**
     * Retourne tous les champs "mappés" de l'entité, c'est-à-dire tous les champs
     * sauf ceux annotés {@link NavigationProperty} (qui n'ont pas de colonne en DB).
     *
     * <p>{@code peek(f -> f.setAccessible(true))} rend chaque champ accessible
     * en lecture/écriture par réflexion, même s'il est {@code private}.
     *
     * @return liste des champs à inclure dans les requêtes SQL
     */
    private List<Field> getMappedFields() {
        return Arrays.stream(entityClass.getDeclaredFields())
                // On exclut les propriétés de navigation (@NavigationProperty)
                .filter(f -> !f.isAnnotationPresent(NavigationProperty.class))
                // On rend chaque champ accessible avant de le retourner
                .peek(f -> f.setAccessible(true))
                .toList();
    }

    /**
     * Champs à utiliser dans la clause INSERT.
     *
     * <p>Règle : on prend tous les champs mappés,
     * <b>sauf la PK si elle est auto-générée</b> (la DB l'assignera elle-même).
     * Si la PK n'est pas générée (ex: ISBN), elle est incluse dans l'INSERT.
     *
     * @return liste des champs à insérer
     */
    private List<Field> getInsertFields() {
        return getMappedFields().stream()
                // Exclure le champ @Id SEULEMENT si isGenerated=true
                .filter(f -> !(f.isAnnotationPresent(Id.class) && isGenerated))
                .toList();
    }

    /**
     * Champs à utiliser dans la clause SET d'un UPDATE.
     *
     * <p>Règle : tous les champs mappés <b>sauf la PK</b>.
     * La PK ne doit jamais être modifiée — elle sert uniquement dans la clause WHERE.
     *
     * @return liste des champs à mettre à jour
     */
    private List<Field> getUpdateFields() {
        return getMappedFields().stream()
                // On exclut toujours la PK du SET (quelle que soit la stratégie de génération)
                .filter(f -> !f.isAnnotationPresent(Id.class))
                .toList();
    }

    // =========================================================================
    // CRUD — Create, Read, Update, Delete
    // =========================================================================

    /**
     * Récupère toutes les lignes de la table.
     *
     * <p>Requête générée : {@code SELECT * FROM <table>}
     *
     * <p><b>Concept : try-with-resources</b><br>
     * {@code try (Connection conn = ...; PreparedStatement pstmt = ...; ResultSet rs = ...)}
     * garantit que {@code conn.close()}, {@code pstmt.close()} et {@code rs.close()}
     * sont appelés automatiquement en fin de bloc, même en cas d'exception.
     * C'est possible car ces types implémentent {@link AutoCloseable}.
     *
     * @return liste (potentiellement vide) de toutes les entités
     */
    public List<TEntity> findAll() {
        String query = "SELECT * FROM " + tableName;
        System.out.println("[SQL] " + query);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            List<TEntity> list = new ArrayList<>();

            // rs.next() avance d'une ligne dans le ResultSet et retourne false quand il n'y en a plus
            while (rs.next()) {
                list.add(buildEntity(rs)); // délégation au sous-classe pour le mapping
            }
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("findAll a échoué sur la table " + tableName, e);
        }
    }

    /**
     * Recherche une entité par sa clé primaire.
     *
     * <p>Requête générée : {@code SELECT * FROM <table> WHERE <pk> = ?}
     *
     * <p><b>Concept : Optional</b><br>
     * {@link Optional} est un conteneur qui peut soit contenir une valeur ({@code Optional.of(entity)}),
     * soit être vide ({@code Optional.empty()}). Il force l'appelant à gérer explicitement
     * le cas "introuvable", évitant ainsi les {@code NullPointerException} silencieuses.
     *
     * <p><b>Concept : PreparedStatement et les {@code ?}</b><br>
     * Les {@code ?} dans la requête sont des <em>paramètres positionnels</em>.
     * On les remplace par {@code pstmt.setObject(position, valeur)}.
     * Utiliser des paramètres plutôt que la concaténation de chaînes protège contre
     * les injections SQL.
     *
     * @param id l'identifiant à rechercher
     * @return {@code Optional.of(entity)} si trouvé, {@code Optional.empty()} sinon
     */
    public Optional<TEntity> findById(TId id) {
        String query = "SELECT * FROM " + tableName + " WHERE " + idColumnName + " = ?";
        System.out.println("[SQL] " + query);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            // Remplacement du 1er "?" par la valeur de id
            // setObject() accepte n'importe quel type Java et le convertit automatiquement
            pstmt.setObject(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // On a trouvé une ligne → on la transforme en entité et on l'enveloppe dans Optional
                    return Optional.of(buildEntity(rs));
                }
                // Aucune ligne → on retourne un Optional vide (pas de null !)
                return Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("findById a échoué sur " + tableName + " pour id=" + id, e);
        }
    }

    /**
     * Insère une nouvelle entité dans la base de données et retourne sa clé primaire.
     *
     * <p>Requête générée (avec PK générée) :
     * {@code INSERT INTO <table> (col1, col2, …) VALUES (?, ?, …)}
     *
     * <p>Requête générée (avec PK non générée) :
     * {@code INSERT INTO <table> (pk, col1, col2, …) VALUES (?, ?, ?, …)}
     *
     * <p><b>Concept : construction dynamique de requête</b><br>
     * On itère sur les champs de l'entité (via la réflexion) pour construire
     * la liste des colonnes et la liste des {@code ?} correspondants.
     * {@link StringBuilder} est utilisé car les concaténations répétées avec
     * {@code String +} créent de nombreux objets intermédiaires inutiles.
     *
     * <p><b>Concept : RETURN_GENERATED_KEYS</b><br>
     * Quand on demande {@code Statement.RETURN_GENERATED_KEYS} au {@link PreparedStatement},
     * la DB retourne la valeur auto-générée après l'INSERT (accessible via {@code getGeneratedKeys()}).
     *
     * @param entity l'entité à insérer (la PK peut être null si elle est auto-générée)
     * @return la clé primaire de la ligne insérée
     */
    @SuppressWarnings("unchecked") // cast (TId) vérifié implicitement par le type générique
    public TId save(TEntity entity) {
        // Récupération des champs à insérer (PK exclue si auto-générée)
        List<Field> fields = getInsertFields();

        // Construction dynamique de : "col1, col2, col3" et "?, ?, ?"
        StringBuilder cols = new StringBuilder();
        StringBuilder placeholders = new StringBuilder();
        for (Field f : fields) {
            cols.append(resolveColumnName(f)).append(", ");
            placeholders.append("?, ");
        }
        // Suppression de la dernière ", " (ex: "col1, col2, " → "col1, col2")
        cols.setLength(cols.length() - 2);
        placeholders.setLength(placeholders.length() - 2);

        String query = "INSERT INTO " + tableName + " (" + cols + ") VALUES (" + placeholders + ")";
        System.out.println("[SQL] " + query);

        // On demande à la DB de retourner la clé générée, seulement si nécessaire
        int generatedKeysFlag = isGenerated ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS;

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query, generatedKeysFlag)) {

            // Remplissage des "?" : pour chaque champ, on lit sa valeur via la réflexion
            int index = 1;
            for (Field f : fields) {
                // f.get(entity) : lit la valeur du champ "f" dans l'objet "entity"
                // Exemple : pour le champ "firstName" de l'Author → retourne "Jean"
                pstmt.setObject(index++, f.get(entity));
            }
            pstmt.executeUpdate(); // exécute l'INSERT

            if (isGenerated) {
                // La DB a généré la clé → on la récupère depuis le ResultSet des clés générées
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new RuntimeException("L'INSERT a réussi mais aucune clé générée n'a été retournée.");
                    }
                    return (TId) rs.getObject(1); // cast vers TId (ex: Integer)
                }
            } else {
                // La PK était déjà dans l'entité → on la lit directement
                return (TId) idField.get(entity);
            }

        } catch (SQLException | IllegalAccessException e) {
            throw new RuntimeException("save a échoué sur " + tableName, e);
        }
    }

    /**
     * Met à jour la ligne correspondant à {@code id} avec les valeurs de {@code entity}.
     *
     * <p>Requête générée : {@code UPDATE <table> SET col1 = ?, col2 = ?, … WHERE <pk> = ?}
     *
     * <p><b>Concept : transaction</b><br>
     * Par défaut, JDBC est en mode "auto-commit" (chaque requête est validée immédiatement).
     * Ici on désactive ce mode pour gérer manuellement la transaction :
     * <ul>
     *   <li>{@code conn.setAutoCommit(false)} : démarre la transaction</li>
     *   <li>{@code conn.commit()} : valide les changements si tout s'est bien passé</li>
     *   <li>{@code conn.rollback()} : annule les changements en cas d'erreur ou d'incohérence</li>
     * </ul>
     * Ici, si l'UPDATE touche plus d'une ligne (ce qui ne devrait jamais arriver sur une PK),
     * on fait un rollback par sécurité.
     *
     * @param id     la clé primaire de la ligne à modifier
     * @param entity l'entité contenant les nouvelles valeurs
     * @return {@code true} si exactement une ligne a été modifiée
     */
    public boolean update(TId id, TEntity entity) {
        List<Field> fields = getUpdateFields(); // tous les champs sauf la PK

        // Construction du SET : "col1 = ?, col2 = ?, col3 = ?"
        StringBuilder setClause = new StringBuilder();
        for (Field f : fields) {
            setClause.append(resolveColumnName(f)).append(" = ?, ");
        }
        setClause.setLength(setClause.length() - 2); // suppression de la dernière ", "

        String query = "UPDATE " + tableName + " SET " + setClause + " WHERE " + idColumnName + " = ?";
        System.out.println("[SQL] " + query);

        try (Connection conn = getConnection()) {

            try (PreparedStatement pstmt = conn.prepareStatement(query)) {
                // Remplissage des "?" du SET (champs 1 à N)
                int index = 1;
                for (Field f : fields) {
                    pstmt.setObject(index++, f.get(entity));
                }
                // Remplissage du dernier "?" du WHERE (position N+1)
                pstmt.setObject(index, id);

                return pstmt.executeUpdate() == 1;
            }

        } catch (SQLException | IllegalAccessException e) {
            throw new RuntimeException("update a échoué sur " + tableName + " pour id=" + id, e);
        }
    }

    /**
     * Supprime la ligne correspondant à l'identifiant donné.
     *
     * <p>Requête générée : {@code DELETE FROM <table> WHERE <pk> = ?}
     *
     * @param id la clé primaire de la ligne à supprimer
     * @return {@code true} si exactement une ligne a été supprimée
     */
    public boolean delete(TId id) {
        String query = "DELETE FROM " + tableName + " WHERE " + idColumnName + " = ?";
        System.out.println("[SQL] " + query);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setObject(1, id);
            // executeUpdate() retourne le nombre de lignes affectées
            return pstmt.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException("delete a échoué sur " + tableName + " pour id=" + id, e);
        }
    }

    // =========================================================================
    // Méthodes supplémentaires
    // =========================================================================

    /**
     * Retourne le nombre total de lignes dans la table.
     *
     * <p>Requête générée : {@code SELECT COUNT(*) FROM <table>}
     *
     * <p>{@code COUNT(*)} est une fonction d'agrégation SQL qui compte toutes les lignes.
     * Elle retourne toujours exactement une ligne avec une seule colonne numérique.
     *
     * @return nombre de lignes (0 si la table est vide)
     */
    public long count() {
        String query = "SELECT COUNT(*) FROM " + tableName;
        System.out.println("[SQL] " + query);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            // COUNT(*) retourne toujours une ligne → rs.next() est toujours vrai
            return rs.next() ? rs.getLong(1) : 0L;

        } catch (SQLException e) {
            throw new RuntimeException("count a échoué sur " + tableName, e);
        }
    }

    /**
     * Vérifie si une ligne avec l'identifiant donné existe dans la table.
     *
     * <p>Requête générée : {@code SELECT 1 FROM <table> WHERE <pk> = ?}
     *
     * <p>{@code SELECT 1} est plus performant que {@code SELECT *} car la DB
     * n'a pas besoin de lire toutes les colonnes — elle retourne juste le
     * chiffre 1 pour chaque ligne correspondante (ce qui confirme l'existence).
     *
     * @param id l'identifiant à rechercher
     * @return {@code true} si une ligne avec cet id existe, {@code false} sinon
     */
    public boolean exists(TId id) {
        String query = "SELECT 1 FROM " + tableName + " WHERE " + idColumnName + " = ?";
        System.out.println("[SQL] " + query);

        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setObject(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                // rs.next() retourne true si au moins une ligne a été trouvée
                return rs.next();
            }

        } catch (SQLException e) {
            throw new RuntimeException("exists a échoué sur " + tableName + " pour id=" + id, e);
        }
    }

    // =========================================================================
    // Méthode abstraite à implémenter dans chaque repository concret
    // =========================================================================

    /**
     * Transforme la ligne courante du {@link ResultSet} en une instance de l'entité.
     *
     * <p><b>Pourquoi cette méthode est-elle abstraite ?</b><br>
     * {@code BaseRepository} sait <em>quand</em> appeler cette méthode (après chaque
     * {@code rs.next()}), mais il ne sait pas <em>comment</em> lire les colonnes
     * et construire l'objet — car cela dépend de chaque entité spécifique.
     * C'est le <em>pattern Template Method</em> : la classe de base définit l'algorithme
     * global, les sous-classes fournissent les étapes spécifiques.
     *
     * <p><b>Exemple d'implémentation dans AuthorRepository :</b>
     * <pre>
     * {@code
     * @Override
     * protected Author buildEntity(ResultSet rs) throws SQLException {
     *     return new Author(
     *         rs.getInt("id"),
     *         rs.getString("firstName"),
     *         rs.getString("lastName"),
     *         rs.getDate("birthDate").toLocalDate()
     *     );
     * }
     * }
     * </pre>
     *
     * @param rs le {@link ResultSet} positionné sur la ligne courante (après {@code rs.next()})
     * @return une instance de {@code TEntity} construite depuis les données de la ligne
     * @throws SQLException si la lecture d'une colonne échoue
     */
    protected abstract TEntity buildEntity(ResultSet rs) throws SQLException;
}

