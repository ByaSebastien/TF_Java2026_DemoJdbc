package be.bstorm.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marque un champ Java comme étant la <b>clé primaire</b> de l'entité.
 *
 * <p><b>Concept : annotation personnalisée</b><br>
 * Une annotation (préfixée par {@code @}) est une étiquette qu'on place sur un
 * élément du code (classe, champ, méthode…). Elle ne fait rien en elle-même :
 * c'est le code qui la <em>lit</em> (ici {@code BaseRepository} via la réflexion)
 * qui lui donne son comportement.
 *
 * <p><b>@Target(ElementType.FIELD)</b><br>
 * Restreint l'usage de cette annotation aux <em>champs</em> uniquement.
 * Tenter de la placer sur une classe ou une méthode serait une erreur de compilation.
 *
 * <p><b>@Retention(RetentionPolicy.RUNTIME)</b><br>
 * Indique que l'annotation doit être conservée jusqu'à l'exécution du programme
 * (et non effacée à la compilation). C'est indispensable pour pouvoir la lire
 * via la réflexion avec {@code field.isAnnotationPresent(Id.class)}.
 *
 * <p><b>Utilisation :</b>
 * <pre>
 * {@code
 * // ID auto-incrémenté par la base (valeur par défaut)
 * @Id(generation = GenerationType.GENERATED)
 * private Integer id;
 *
 * // ID fourni par l'application (ex: ISBN)
 * @Id(generation = GenerationType.NOT_GENERATED)
 * private String isbn;
 * }
 * </pre>
 */
@Target(ElementType.FIELD)           // ne peut s'appliquer qu'à un champ
@Retention(RetentionPolicy.RUNTIME)  // lisible à l'exécution par la réflexion
public @interface Id {

    /**
     * Stratégie de génération de la clé primaire.
     *
     * <p>La valeur par défaut est {@link GenerationType#GENERATED}, car c'est
     * le cas le plus courant (identifiant numérique auto-incrémenté par la DB).
     * Il suffit donc d'écrire simplement {@code @Id} pour ce cas.
     *
     * @return la stratégie de génération
     */
    GenerationType generation() default GenerationType.GENERATED;
}
