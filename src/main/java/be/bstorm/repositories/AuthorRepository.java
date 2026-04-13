package be.bstorm.repositories;

import be.bstorm.entities.Author;

/**
 * Repository concret pour l'entité {@link Author}.
 *
 * <p>Hérite de toutes les méthodes CRUD de {@code BaseRepository} :
 * {@code findAll()}, {@code findById()}, {@code save()}, {@code update()},
 * {@code delete()}, {@code count()}, {@code exists()}.
 *
 * <p>Le mapping {@link java.sql.ResultSet} → {@link Author} est géré automatiquement
 * par la méthode {@code buildEntity()} de {@code BaseRepository} via la réflexion :
 * elle lit les annotations {@code @Column} de l'entité pour savoir quelle colonne
 * SQL correspond à quel champ Java, puis instancie l'objet via le constructeur vide.
 *
 * <p><b>Paramètres de type :</b>
 * <ul>
 *   <li>{@code Author}  → TEntity : le type d'objet que ce repository gère</li>
 *   <li>{@code Integer} → TId    : le type de la clé primaire (id SERIAL)</li>
 * </ul>
 */
public class AuthorRepository extends BaseRepository<Author, Integer> {
}
