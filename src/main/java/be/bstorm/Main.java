package be.bstorm;

import be.bstorm.entities.Author;
import be.bstorm.repositories.AuthorRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static be.bstorm.utils.ConnectionUtils.getConnection;

public class Main {
    static void main() {

        AuthorRepository authorRepository = new AuthorRepository();

//        List<Author> authors = authorRepository.findAll();
//
//        authors.forEach(System.out::println);
//
//        System.out.print("Entrez l'id de l'auteur à rechercher : ");
//        int id = new Scanner(System.in).nextInt();
//
//        Author author = authorRepository.findById(id).orElseThrow();
//        System.out.println(author);

        Author author = new Author("truc","muche", LocalDate.now());

        int id = authorRepository.save(author);

        System.out.println(id);
    }
}
