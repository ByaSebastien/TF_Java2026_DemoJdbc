package be.bstorm.entities;

import be.bstorm.annotations.Column;
import be.bstorm.annotations.GenerationType;
import be.bstorm.annotations.Id;
import be.bstorm.annotations.NavigationProperty;
import be.bstorm.annotations.Table;

import java.util.Objects;

/**
 * Entité représentant un livre dans la base de données.
 *
 * <p><b>Particularités de cette entité par rapport à {@link Author} :</b>
 * <ol>
 *   <li><b>Clé primaire non générée :</b> l'ISBN est un identifiant universel fourni
 *       par l'éditeur. C'est l'application (pas la DB) qui fournit cette valeur.
 *       → {@code @Id(generation = GenerationType.NOT_GENERATED)}</li>
 *   <li><b>Propriété de navigation :</b> le champ {@code author} est un objet Java
 *       complet ({@link Author}), mais il n'existe pas de colonne "author" en base.
 *       La DB stocke uniquement la clé étrangère {@code authorId}.
 *       → {@code @NavigationProperty}</li>
 * </ol>
 *
 * <p><b>Table DB correspondante :</b>
 * <pre>
 * CREATE TABLE Book (
 *     isbn        VARCHAR(20) PRIMARY KEY,  -- fourni par l'application
 *     title       VARCHAR(200),
 *     description TEXT,
 *     authorId    INTEGER REFERENCES Author(id)  -- clé étrangère
 * );
 * -- Pas de colonne "author" : c'est une navigation property côté Java uniquement
 * </pre>
 */
@Table(name = "Book") // mappe cette classe sur la table "Book" en base de données
public class Book {

    /**
     * ISBN (International Standard Book Number) — clé primaire du livre.
     *
     * <p>{@code GenerationType.NOT_GENERATED} : l'ISBN est une valeur externe
     * (fournie par l'éditeur), pas auto-incrémentée par la DB. L'INSERT
     * devra donc inclure cette colonne avec sa valeur.
     */
    @Id(generation = GenerationType.NOT_GENERATED)
    @Column(name = "isbn")      // colonne "isbn" en base de données
    private String isbn;

    /** Titre du livre — colonne "title" en base de données. */
    @Column(name = "title")
    private String title;

    /** Description du livre — colonne "description" en base de données. */
    @Column(name = "description")
    private String description;

    /**
     * Clé étrangère vers la table Author — colonne "authorId" en base de données.
     * C'est la <em>vraie</em> colonne DB qui établit la relation entre Book et Author.
     */
    @Column(name = "authorId")
    private Integer authorId;

    /**
     * Propriété de navigation : l'objet {@link Author} complet associé à ce livre.
     *
     * <p><b>Important :</b> ce champ n'a <em>aucune</em> colonne correspondante dans
     * la table Book. Il est rempli manuellement par le code applicatif
     * (en chargeant l'auteur via son id). L'annotation {@code @NavigationProperty}
     * indique à {@code BaseRepository} de l'ignorer totalement dans les requêtes SQL.
     *
     * <p>Exemple d'utilisation :
     * <pre>
     * {@code
     * Book book = bookRepository.findById("978-2-07-036024-5").orElseThrow();
     * // book.getAuthor() est null à ce stade — pas chargé automatiquement
     *
     * Author author = authorRepository.findById(book.getAuthorId()).orElseThrow();
     * book.setAuthor(author); // on "remplit" la navigation property manuellement
     * }
     * </pre>
     */
    @NavigationProperty
    private Author author;

    // -------------------------------------------------------------------------
    // Constructeurs
    // -------------------------------------------------------------------------

    /** Constructeur vide. */
    public Book() {}

    /**
     * Constructeur utilisé pour créer ou reconstruire un livre depuis la DB.
     * La navigation property {@code author} n'est pas incluse : elle ne vient pas de la DB.
     *
     * @param isbn        identifiant ISBN
     * @param title       titre
     * @param description description
     * @param authorId    clé étrangère vers l'auteur
     */
    public Book(String isbn, String title, String description, Integer authorId) {
        this.isbn = isbn;
        this.title = title;
        this.description = description;
        this.authorId = authorId;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getAuthorId() { return authorId; }
    public void setAuthorId(Integer authorId) { this.authorId = authorId; }

    /** Retourne la navigation property (peut être {@code null} si non chargée). */
    public Author getAuthor() { return author; }
    public void setAuthor(Author author) { this.author = author; }

    // -------------------------------------------------------------------------
    // equals, hashCode, toString
    // -------------------------------------------------------------------------

    /**
     * Deux livres sont égaux si leur ISBN est identique (l'ISBN est unique au monde).
     * On ne compare pas les autres champs car l'ISBN suffit à identifier un livre.
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(isbn, book.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }

    @Override
    public String toString() {
        return "Book{" +
                "isbn='" + isbn + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", authorId=" + authorId +
                ", author=" + author +
                '}';
    }
}
