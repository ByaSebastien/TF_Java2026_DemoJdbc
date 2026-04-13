create table Author (
    id int primary key generated always as identity,
    firstName varchar(50) not null,
    lastName varchar(50) not null,
    birthDate date not null
);

create table Book (
    isbn varchar(13) primary key check ( length(isbn) = 10 or length(isbn) = 13 ),
    title varchar(50) not null,
    description varchar(150) null,
    author_id int references Author(id)
);

-- AUTEURS
insert into Author (firstName, lastName, birthDate) values
('Robert', 'Martin', '1952-12-05'),
('Martin', 'Fowler', '1963-12-18'),
('Eric', 'Evans', '1965-01-01'),
('Kent', 'Beck', '1961-03-31'),
('Thomas', 'Cormen', '1956-01-01'),
('Andrew', 'Tanenbaum', '1944-03-16'),
('Donald', 'Knuth', '1938-01-10'),
('Bjarne', 'Stroustrup', '1950-12-30'),
('Steve', 'McConnell', '1961-01-01'),
('Brian', 'Kernighan', '1942-01-01');


-- LIVRES (titres et descriptions en français)
insert into Book (isbn, title, description, author_id) values
('9780132350884', 'Code propre', 'Guide des bonnes pratiques pour écrire du code lisible et maintenable', 1),
('9780137081073', 'Refactoring', 'Améliorer la structure du code existant', 2),
('9780321125217', 'Domain Driven Design', 'Conception logicielle centrée sur le domaine métier', 3),
('9780321146533', 'Test Driven Development', 'Développement piloté par les tests', 4),
('9780262033848', 'Introduction aux algorithmes', 'Ouvrage de référence sur les algorithmes', 5),
('9780131429383', 'Systèmes d exploitation modernes', 'Concepts fondamentaux des OS', 6),
('9780201896831', 'Art de programmer', 'Approche approfondie de la programmation', 7),
('9780321563842', 'Langage C++', 'Guide du langage C++ par son créateur', 8),
('9780735619678', 'Code complet', 'Techniques avancées de construction logicielle', 9),
('9780131103627', 'Langage C', 'Introduction classique au langage C', 10);