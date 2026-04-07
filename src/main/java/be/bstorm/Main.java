package be.bstorm;

import be.bstorm.entities.Author;
import be.bstorm.repositories.AuthorRepository;

import java.time.LocalDate;

/**
 * Point d'entrée de démonstration.
 *
 * <p>Ce fichier montre comment utiliser les repositories en pratique.
 * Chaque appel démontre une méthode héritée de {@code BaseRepository}.
 *
 * <p><b>Architecture globale :</b>
 * <pre>
 *  Main.java
 *    │
 *    └── AuthorRepository          (extends BaseRepository)
 *          │
 *          ├── findAll()           ←  SELECT * FROM Author
 *          ├── findById(id)        ←  SELECT * FROM Author WHERE id = ?
 *          ├── save(entity)        ←  INSERT INTO Author (...) VALUES (...)
 *          ├── update(id, entity)  ←  UPDATE Author SET ... WHERE id = ?
 *          ├── delete(id)          ←  DELETE FROM Author WHERE id = ?
 *          ├── count()             ←  SELECT COUNT(*) FROM Author
 *          └── exists(id)          ←  SELECT 1 FROM Author WHERE id = ?
 * </pre>
 */
public class Main {

    static void main() {

        // Instanciation du repository.
        // Le constructeur de BaseRepository lit les annotations de Author par réflexion
        // et prépare tout : nom de table, colonne PK, stratégie de génération...
        AuthorRepository authorRepository = new AuthorRepository();

        // -----------------------------------------------------------------
        // findAll() — récupère tous les auteurs
        // Retourne une List<Author> (vide si la table est vide)
        // -----------------------------------------------------------------
        authorRepository.findAll().forEach(System.out::println);

        // -----------------------------------------------------------------
        // findById() — recherche par clé primaire
        // Retourne un Optional<Author> :
        //   .ifPresentOrElse(action, fallback) permet de gérer les deux cas
        //   sans risque de NullPointerException
        // -----------------------------------------------------------------
        authorRepository.findById(1).ifPresentOrElse(
                System.out::println,                         // cas trouvé
                () -> System.out.println("Auteur introuvable") // cas non trouvé
        );

        // -----------------------------------------------------------------
        // save() — insertion d'un nouvel auteur
        // On crée l'auteur SANS id (null), car la DB va l'auto-incrémenter.
        // save() retourne l'id Integer généré par la base de données.
        // -----------------------------------------------------------------
        Author author = new Author("truc", "muche", LocalDate.now());
        Integer id = authorRepository.save(author);
        System.out.println("Auteur inséré avec l'id : " + id);

        // -----------------------------------------------------------------
        // count() — nombre total de lignes dans la table
        // -----------------------------------------------------------------
        System.out.println("Nombre total d'auteurs : " + authorRepository.count());

        // -----------------------------------------------------------------
        // exists() — vérifie si un id existe sans charger toute l'entité
        // Plus efficace que findById() quand on n'a pas besoin des données
        // -----------------------------------------------------------------
        System.out.println("L'auteur id=" + id + " existe : " + authorRepository.exists(id));

        // -----------------------------------------------------------------
        // update() — modification d'un auteur existant
        // On passe l'id séparément pour éviter toute ambiguïté sur
        // "quel auteur modifier" (l'entité elle-même peut avoir un id null)
        // Retourne true si exactement 1 ligne a été modifiée.
        // -----------------------------------------------------------------
        author.setFirstName("updated");
        boolean updated = authorRepository.update(id, author);
        System.out.println("Mise à jour réussie : " + updated);

        // -----------------------------------------------------------------
        // delete() — suppression par id
        // Retourne true si exactement 1 ligne a été supprimée.
        // -----------------------------------------------------------------
        boolean deleted = authorRepository.delete(id);
        System.out.println("Suppression réussie : " + deleted);
    }
}
