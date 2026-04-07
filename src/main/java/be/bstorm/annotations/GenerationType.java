package be.bstorm.annotations;

/**
 * Enumération définissant la stratégie de génération d'une clé primaire.
 *
 * <p><b>Concept : enum</b><br>
 * Une {@code enum} est un type spécial dont les valeurs possibles sont connues
 * à l'avance et listées explicitement. C'est plus sûr qu'utiliser des entiers
 * ou des chaînes, car le compilateur empêche toute valeur invalide.
 *
 * <p><b>Utilisation :</b><br>
 * Cette enum est passée en paramètre de l'annotation {@link Id} pour indiquer
 * si la base de données génère automatiquement la clé primaire, ou si c'est
 * l'application qui fournit la valeur.
 *
 * <pre>
 * {@code
 * // Cas 1 : la DB génère l'ID toute seule (ex: SERIAL / AUTO_INCREMENT)
 * @Id(generation = GenerationType.GENERATED)
 * private Integer id;
 *
 * // Cas 2 : l'application fournit l'ID manuellement (ex: ISBN d'un livre)
 * @Id(generation = GenerationType.NOT_GENERATED)
 * private String isbn;
 * }
 * </pre>
 */
public enum GenerationType {

    /**
     * La clé primaire est <b>auto-générée par la base de données</b>.
     * Dans ce cas, l'INSERT n'inclut pas la colonne PK dans la liste des colonnes ;
     * la valeur assignée par la DB est ensuite récupérée via {@code getGeneratedKeys()}.
     *
     * <p>Exemples DB : {@code SERIAL} (PostgreSQL), {@code AUTO_INCREMENT} (MySQL),
     * {@code IDENTITY} (SQL Server).
     */
    GENERATED,

    /**
     * La clé primaire est <b>fournie manuellement par le code applicatif</b>.
     * Dans ce cas, l'INSERT inclut la colonne PK avec la valeur déjà présente
     * dans l'objet Java.
     *
     * <p>Exemples : ISBN d'un livre, UUID généré en Java, code produit.
     */
    NOT_GENERATED
}
