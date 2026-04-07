package be.bstorm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque un champ comme étant une <b>propriété de navigation</b>.
 *
 * <p><b>Qu'est-ce qu'une propriété de navigation ?</b><br>
 * Dans un modèle objet, une entité peut contenir une référence vers une autre
 * entité liée. Par exemple, un {@code Book} peut contenir un objet {@code Author}
 * complet (chargé depuis la DB). Cette référence s'appelle une "navigation property"
 * car elle permet de "naviguer" d'une entité à l'autre dans le code.
 *
 * <p><b>Problème sans cette annotation :</b><br>
 * Ce champ {@code Author author} n'a <em>pas</em> de colonne correspondante dans
 * la table {@code Book}. Si {@code BaseRepository} essayait de l'inclure dans un
 * INSERT ou un UPDATE, la requête SQL échouerait.
 *
 * <p><b>Solution :</b><br>
 * En annotant le champ avec {@code @NavigationProperty}, on indique à
 * {@code BaseRepository} de l'ignorer complètement lors de la construction
 * des requêtes SQL (INSERT, UPDATE, SELECT).
 *
 * <p><b>Exemple :</b>
 * <pre>
 * {@code
 * public class Book {
 *     @Column(name = "authorId")
 *     private Integer authorId;    // ✅ vraie colonne DB → incluse dans les requêtes
 *
 *     @NavigationProperty
 *     private Author author;       // ❌ pas une colonne DB → ignorée dans les requêtes
 * }
 * }
 * </pre>
 *
 * <p><b>Note :</b> c'est une annotation "marqueur" (marker annotation) : elle ne
 * possède aucun attribut, sa seule présence suffit à déclencher le comportement.
 */
@Target(ElementType.FIELD)           // applicable uniquement sur un champ
@Retention(RetentionPolicy.RUNTIME)  // lisible à l'exécution par la réflexion
public @interface NavigationProperty {
    // Pas d'attribut : la simple présence de l'annotation suffit
}
