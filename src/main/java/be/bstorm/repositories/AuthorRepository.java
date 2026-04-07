package be.bstorm.repositories;

import be.bstorm.entities.Author;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

/**
 * Repository concret pour l'entité {@link Author}.
 *
 * <p><b>Concept : héritage et spécialisation</b><br>
 * En étendant {@code BaseRepository<Author, Integer>}, cette classe hérite
 * automatiquement de toutes les méthodes CRUD :
 * <ul>
 *   <li>{@code findAll()}  → SELECT * FROM Author</li>
 *   <li>{@code findById(id)} → SELECT * FROM Author WHERE id = ?</li>
 *   <li>{@code save(author)} → INSERT INTO Author (...) VALUES (...)</li>
 *   <li>{@code update(id, author)} → UPDATE Author SET ... WHERE id = ?</li>
 *   <li>{@code delete(id)} → DELETE FROM Author WHERE id = ?</li>
 *   <li>{@code count()} → SELECT COUNT(*) FROM Author</li>
 *   <li>{@code exists(id)} → SELECT 1 FROM Author WHERE id = ?</li>
 * </ul>
 *
 * <p><b>Ce que cette classe DOIT implémenter :</b><br>
 * La seule méthode {@code abstract} de {@code BaseRepository} est {@link #buildEntity(ResultSet)}.
 * C'est ici qu'on décrit comment transformer une ligne SQL en objet {@link Author}.
 *
 * <p><b>Ce que cette classe N'A PAS BESOIN d'implémenter :</b><br>
 * Tout le reste est géré par {@code BaseRepository} via la réflexion et les annotations
 * de l'entité ({@code @Table}, {@code @Id}, {@code @Column}).
 *
 * <p><b>Paramètres de type :</b>
 * <ul>
 *   <li>{@code Author}  → TEntity : le type d'objet que ce repository gère</li>
 *   <li>{@code Integer} → TId    : le type de la clé primaire (id SERIAL)</li>
 * </ul>
 */
public class AuthorRepository extends BaseRepository<Author, Integer> {

    /**
     * Transforme la ligne courante du {@link ResultSet} en un objet {@link Author}.
     *
     * <p>Cette méthode est appelée automatiquement par {@code BaseRepository}
     * après chaque {@code rs.next()} (dans {@code findAll} et {@code findById}).
     *
     * <p><b>Lecture des colonnes :</b>
     * <ul>
     *   <li>{@code rs.getInt("id")}       → lit la colonne "id" comme int</li>
     *   <li>{@code rs.getString("...")}   → lit une colonne texte</li>
     *   <li>{@code rs.getDate("birthDate").toLocalDate()} → lit une date SQL
     *       et la convertit en {@link LocalDate} (type Java moderne)</li>
     * </ul>
     *
     * @param rs le ResultSet positionné sur la ligne courante
     * @return un objet {@link Author} rempli avec les données de la ligne
     * @throws SQLException si une colonne demandée n'existe pas ou est inaccessible
     */
    @Override
    protected Author buildEntity(ResultSet rs) throws SQLException {
        Integer id          = rs.getInt("id");
        String firstName    = rs.getString("firstName");
        String lastName     = rs.getString("lastName");
        // java.sql.Date.toLocalDate() convertit la date SQL en LocalDate Java
        LocalDate birthDate = rs.getDate("birthDate").toLocalDate();

        // On utilise le constructeur complet (avec id) car on lit depuis la DB
        return new Author(id, firstName, lastName, birthDate);
    }
}
