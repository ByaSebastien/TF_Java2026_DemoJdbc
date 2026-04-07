package be.bstorm.repositories;

import be.bstorm.entities.Book;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Repository concret pour l'entité {@link Book}.
 *
 * <p><b>Différence clé avec {@link AuthorRepository} :</b><br>
 * {@code Book} utilise une clé primaire de type {@code String} (l'ISBN),
 * fournie par l'application ({@code GenerationType.NOT_GENERATED}).
 * Le comportement de {@code save()} dans {@code BaseRepository} en tient compte :
 * il inclut la colonne ISBN dans le INSERT et retourne directement la valeur
 * du champ (sans appeler {@code getGeneratedKeys()}).
 *
 * <p><b>Gestion de la navigation property :</b><br>
 * Le champ {@code author} de {@link Book} est annoté {@code @NavigationProperty}
 * dans l'entité. {@code BaseRepository} l'ignore automatiquement dans toutes
 * les requêtes SQL — aucune configuration supplémentaire n'est nécessaire ici.
 *
 * <p><b>Paramètres de type :</b>
 * <ul>
 *   <li>{@code Book}   → TEntity : le type d'objet que ce repository gère</li>
 *   <li>{@code String} → TId    : le type de la clé primaire (ISBN)</li>
 * </ul>
 */
public class BookRepository extends BaseRepository<Book, String> {

    /**
     * Transforme la ligne courante du {@link ResultSet} en un objet {@link Book}.
     *
     * <p>Note : on lit uniquement les colonnes réelles de la table ({@code isbn},
     * {@code title}, {@code description}, {@code authorId}). La navigation property
     * {@code author} n'est <em>pas</em> chargée ici — c'est au code appelant
     * de la charger si nécessaire via {@code AuthorRepository}.
     *
     * @param rs le ResultSet positionné sur la ligne courante
     * @return un objet {@link Book} rempli avec les données de la ligne
     * @throws SQLException si une colonne demandée n'existe pas ou est inaccessible
     */
    @Override
    protected Book buildEntity(ResultSet rs) throws SQLException {
        String isbn        = rs.getString("isbn");
        String title       = rs.getString("title");
        String description = rs.getString("description");
        Integer authorId   = rs.getInt("authorId"); // clé étrangère, pas l'objet Author

        // Le constructeur sans "author" : la navigation property est null par défaut
        return new Book(isbn, title, description, authorId);
    }
}
