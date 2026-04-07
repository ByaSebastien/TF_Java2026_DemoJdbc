package be.bstorm.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Classe utilitaire responsable de la création des connexions à la base de données.
 *
 * <p><b>Concept : classe utilitaire</b><br>
 * Une classe utilitaire regroupe des méthodes statiques liées à une même responsabilité.
 * On ne l'instancie jamais — on appelle directement ses méthodes via le nom de la classe.
 *
 * <p><b>Concept : JDBC (Java Database Connectivity)</b><br>
 * JDBC est l'API standard de Java pour communiquer avec une base de données relationnelle.
 * Les étapes typiques sont :
 * <ol>
 *   <li>Obtenir une {@link Connection} (cette classe)</li>
 *   <li>Créer un {@link java.sql.PreparedStatement} avec la requête SQL</li>
 *   <li>Exécuter la requête et lire le {@link java.sql.ResultSet}</li>
 *   <li>Fermer toutes les ressources (idéalement avec try-with-resources)</li>
 * </ol>
 */
public class ConnectionUtils {

    // -------------------------------------------------------------------------
    // Paramètres de connexion PostgreSQL
    // -------------------------------------------------------------------------

    /**
     * URL JDBC de la base de données.
     * Format : jdbc:<driver>://<hôte>:<port>/<nom_de_la_base>
     * Ici : PostgreSQL sur localhost, port 5432, base "demo_jdbc".
     */
    private static final String URL = "jdbc:postgresql://localhost:5432/demo_jdbc";

    /** Nom d'utilisateur PostgreSQL. */
    private static final String USER = "postgres";

    /** Mot de passe PostgreSQL. */
    private static final String PASSWORD = "postgres";

    // -------------------------------------------------------------------------
    // Méthode principale
    // -------------------------------------------------------------------------

    /**
     * Crée et retourne une nouvelle connexion à la base de données.
     *
     * <p><b>Important :</b> chaque appel ouvre une nouvelle connexion physique.
     * Il faut absolument la fermer après usage (via try-with-resources) pour
     * éviter les fuites de ressources.
     *
     * <pre>
     * {@code
     * // Bonne pratique : try-with-resources ferme la connexion automatiquement
     * try (Connection conn = ConnectionUtils.getConnection()) {
     *     // utiliser conn...
     * } // conn.close() est appelé automatiquement ici
     * }
     * </pre>
     *
     * @return une {@link Connection} active vers la base de données
     * @throws SQLException si la connexion échoue (base indisponible, mauvais mot de passe…)
     */
    public static Connection getConnection() throws SQLException {
        // DriverManager.getConnection() est le point d'entrée JDBC standard.
        // Il utilise l'URL pour choisir le bon pilote (driver) PostgreSQL
        // qui doit être présent dans les dépendances Maven (pom.xml).
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
