package be.bstorm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associe un champ Java à une colonne spécifique de la base de données.
 *
 * <p><b>Pourquoi cette annotation ?</b><br>
 * Par convention, {@code BaseRepository} utilise le nom du champ Java comme
 * nom de colonne SQL. Mais parfois les noms diffèrent (snake_case en DB vs
 * camelCase en Java). {@code @Column} permet de préciser explicitement le
 * nom de la colonne cible.
 *
 * <p><b>Règle de résolution du nom :</b>
 * <ol>
 *   <li>Si {@code @Column(name = "nom")} est présent et non vide → utilise {@code "nom"}</li>
 *   <li>Si {@code @Column} sans {@code name}, ou {@code name = ""} → utilise le nom du champ Java</li>
 *   <li>Si {@code @Column} est absent → utilise le nom du champ Java</li>
 * </ol>
 *
 * <p><b>Exemples :</b>
 * <pre>
 * {@code
 * // La colonne DB s'appelle "first_name" (snake_case),
 * // mais le champ Java s'appelle "firstName" (camelCase)
 * @Column(name = "first_name")
 * private String firstName;
 *
 * // Nom identique en Java et en DB → @Column optionnel
 * @Column(name = "title")   // ou simplement sans @Column
 * private String title;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)           // applicable uniquement sur un champ
@Retention(RetentionPolicy.RUNTIME)  // lisible à l'exécution par la réflexion
public @interface Column {

    /**
     * Nom de la colonne dans la base de données.
     *
     * <p>Si non renseigné (ou vide), le nom du champ Java sera utilisé à la place.
     *
     * @return le nom de la colonne DB, ou {@code ""} pour utiliser le nom du champ
     */
    String name() default "";
}
