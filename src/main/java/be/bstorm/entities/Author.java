package be.bstorm.entities;

import be.bstorm.annotations.Column;
import be.bstorm.annotations.GenerationType;
import be.bstorm.annotations.Id;
import be.bstorm.annotations.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Entité représentant un auteur dans la base de données.
 *
 * <p><b>Concept : entité (Entity)</b><br>
 * Une entité est une classe Java qui représente une table de la base de données.
 * Chaque instance de la classe correspond à une ligne de la table.
 * Ses champs correspondent aux colonnes.
 *
 * <p><b>Annotations utilisées :</b>
 * <ul>
 *   <li>{@link Table} : indique le nom de la table en DB ("Author")</li>
 *   <li>{@link Id} : indique quelle colonne est la clé primaire</li>
 *   <li>{@link Column} : précise le nom de la colonne en DB si différent du champ Java</li>
 * </ul>
 *
 * <p><b>Table DB correspondante :</b>
 * <pre>
 * CREATE TABLE Author (
 *     id        SERIAL PRIMARY KEY,   -- auto-généré par PostgreSQL
 *     firstName VARCHAR(100),
 *     lastName  VARCHAR(100),
 *     birthDate DATE
 * );
 * </pre>
 */
@Table(name = "Author") // mappe cette classe sur la table "Author" en base de données
public class Author {

    /**
     * Clé primaire auto-incrémentée par la base de données (SERIAL).
     * {@code GenerationType.GENERATED} indique à {@code BaseRepository} que
     * cette valeur sera assignée par la DB lors d'un INSERT (ne pas l'inclure
     * dans la liste des colonnes à insérer).
     */
    @Id(generation = GenerationType.GENERATED)
    @Column(name = "id")        // colonne "id" en base de données
    private Integer id;

    /** Prénom de l'auteur — colonne "firstName" en base de données. */
    @Column(name = "firstName")
    private String firstName;

    /** Nom de famille de l'auteur — colonne "lastName" en base de données. */
    @Column(name = "lastName")
    private String lastName;

    /** Date de naissance — colonne "birthDate" en base de données. */
    @Column(name = "birthDate")
    private LocalDate birthDate;

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    /**
     * Constructeur vide requis par certains frameworks de réflexion.
     * Permet d'instancier un Author sans aucune valeur initiale.
     */
    public Author() {}

    /**
     * Constructeur utilisé pour créer un nouvel auteur <b>avant</b> insertion en DB.
     * L'id n'est pas précisé car il sera assigné par la base de données.
     *
     * @param firstName prénom
     * @param lastName  nom de famille
     * @param birthDate date de naissance
     */
    public Author(String firstName, String lastName, LocalDate birthDate) {
        this();
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
    }

    /**
     * Constructeur utilisé pour reconstruire un auteur <b>après</b> lecture depuis la DB.
     * L'id est connu car il a été lu depuis le ResultSet.
     *
     * @param id        identifiant récupéré depuis la base de données
     * @param firstName prénom
     * @param lastName  nom de famille
     * @param birthDate date de naissance
     */
    public Author(Integer id, String firstName, String lastName, LocalDate birthDate) {
        this(firstName, lastName, birthDate);
        this.id = id;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    /**
     * Retourne l'identifiant. Peut être {@code null} pour un auteur non encore inséré en DB.
     */
    public Integer getId() { return id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    // -------------------------------------------------------------------------
    // equals, hashCode, toString
    // -------------------------------------------------------------------------

    /**
     * Deux auteurs sont égaux si tous leurs champs sont identiques.
     *
     * <p><b>Concept : equals & hashCode</b><br>
     * Ces méthodes sont utilisées lors des comparaisons ({@code ==} compare les références,
     * {@code equals} compare les valeurs). Il est obligatoire de redéfinir les deux ensemble
     * pour respecter le contrat Java (si a.equals(b), alors a.hashCode() == b.hashCode()).
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Author author = (Author) o;
        return Objects.equals(id, author.id)
                && Objects.equals(firstName, author.firstName)
                && Objects.equals(lastName, author.lastName)
                && Objects.equals(birthDate, author.birthDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, firstName, lastName, birthDate);
    }

    /**
     * Représentation textuelle pratique pour le débogage.
     * Utilisée par {@code System.out.println(author)}.
     */
    @Override
    public String toString() {
        return "Author{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", birthDate=" + birthDate +
                '}';
    }
}
