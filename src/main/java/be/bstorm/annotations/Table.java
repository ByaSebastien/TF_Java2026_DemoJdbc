package be.bstorm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associe une classe entité à une table spécifique dans la base de données.
 *
 * <p><b>Comportement par défaut (sans cette annotation) :</b><br>
 * Si une entité ne porte pas {@code @Table}, {@code BaseRepository} utilise
 * automatiquement le <em>nom simple de la classe</em> comme nom de table.
 * Exemple : la classe {@code Author} → table {@code "Author"}.
 *
 * <p><b>Quand utiliser {@code @Table} ?</b><br>
 * Lorsque le nom de la table en base de données est différent du nom de la classe Java.
 * Exemples courants :
 * <ul>
 *   <li>Convention pluriel : classe {@code Author} → table {@code "authors"}</li>
 *   <li>Convention snake_case : classe {@code BookAuthor} → table {@code "book_author"}</li>
 *   <li>Préfixe de schéma : {@code "dbo.Author"}</li>
 * </ul>
 *
 * <p><b>Exemples :</b>
 * <pre>
 * {@code
 * // Table DB "Author" = nom de la classe → @Table optionnel mais explicite
 * @Table(name = "Author")
 * public class Author { ... }
 *
 * // Table DB "books" (pluriel) ≠ nom de la classe → @Table obligatoire
 * @Table(name = "books")
 * public class Book { ... }
 * }
 * </pre>
 *
 * <p><b>@Target(ElementType.TYPE)</b><br>
 * Cette annotation se place sur une <em>classe</em> (ou interface, enum),
 * contrairement à {@link Column} et {@link Id} qui se placent sur des champs.
 */
@Target(ElementType.TYPE)            // applicable sur une classe/interface/enum
@Retention(RetentionPolicy.RUNTIME)  // lisible à l'exécution par la réflexion
public @interface Table {

    /**
     * Nom de la table dans la base de données.
     *
     * <p>Si non renseigné (ou vide), le nom simple de la classe Java sera utilisé.
     *
     * @return le nom de la table DB, ou {@code ""} pour utiliser le nom de la classe
     */
    String name() default "";
}
