package be.bstorm.repositories;

import be.bstorm.entities.Book;

/**
 * Repository concret pour l'entité {@link Book}.
 *
 * <p>Hérite de toutes les méthodes CRUD de {@code BaseRepository} :
 * {@code findAll()}, {@code findById()}, {@code save()}, {@code update()},
 * {@code delete()}, {@code count()}, {@code exists()}.
 *
 * <p>Le mapping {@link java.sql.ResultSet} → {@link Book} est géré automatiquement
 * par la méthode {@code buildEntity()} de {@code BaseRepository} via la réflexion.
 * Les champs annotés {@code @NavigationProperty} (comme {@code author}) sont ignorés
 * automatiquement — aucune configuration supplémentaire n'est nécessaire.
 *
 * <p><b>Différence clé avec {@link AuthorRepository} :</b><br>
 * {@code Book} utilise une clé primaire de type {@code String} (l'ISBN),
 * fournie par l'application ({@code GenerationType.NOT_GENERATED}).
 * Le comportement de {@code save()} dans {@code BaseRepository} en tient compte.
 *
 * <p><b>Paramètres de type :</b>
 * <ul>
 *   <li>{@code Book}   → TEntity : le type d'objet que ce repository gère</li>
 *   <li>{@code String} → TId    : le type de la clé primaire (ISBN)</li>
 * </ul>
 */
public class BookRepository extends BaseRepository<Book, String> {
}
