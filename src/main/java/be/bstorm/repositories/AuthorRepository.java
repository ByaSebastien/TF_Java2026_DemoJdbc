package be.bstorm.repositories;

import be.bstorm.entities.Author;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static be.bstorm.utils.ConnectionUtils.getConnection;

public class AuthorRepository {

    public List<Author> findAll() {

        try (Connection conn = getConnection()) {

            String query = "select * from Author";

            PreparedStatement pstmt = conn.prepareStatement(query);

            ResultSet rs = pstmt.executeQuery();

            List<Author> authors = new ArrayList<>();

            while (rs.next()) {

                Author author = convert(rs);
                authors.add(author);
            }

            return authors;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Optional<Author> findById(Integer id) {

        try (Connection conn = getConnection()) {

            String query = "select * from Author where id = ?";

            PreparedStatement pstmt = conn.prepareStatement(query);

            pstmt.setInt(1, id);

            ResultSet rs = pstmt.executeQuery();

            Author author = null;

            if (rs.next()) {
                author = convert(rs);
            }

            return Optional.ofNullable(author);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int save(Author author) {

        try (Connection conn = getConnection()) {

            String query = "insert into Author (firstName, lastName, birthDate) values (?, ?, ?)";

            PreparedStatement pstmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);

            pstmt.setString(1, author.getFirstName());
            pstmt.setString(2, author.getLastName());
            pstmt.setDate(3, Date.valueOf(author.getBirthDate()));

            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();

            if (!rs.next()) {
                throw new RuntimeException("Insert failed");
            }

            return rs.getInt(1);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean update(Integer id, Author author) {

        try (Connection conn = getConnection()) {

            conn.setAutoCommit(false);

            String query = "update Author set firstName = ?, lastName = ?, birthDate = ? where id = ?";

            PreparedStatement pstmt = conn.prepareStatement(query);

            pstmt.setString(1, author.getFirstName());
            pstmt.setString(2, author.getLastName());
            pstmt.setDate(3, Date.valueOf(author.getBirthDate()));
            pstmt.setInt(4, id);

            int nbRows = pstmt.executeUpdate();

            if (nbRows > 1) {
                conn.rollback();
            } else if (nbRows == 1) {
                conn.commit();
            }

            return nbRows == 1;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean delete(Integer id) {

        try(Connection conn = getConnection()){

            String query = "delete from Author where id = ?";

            PreparedStatement pstmt = conn.prepareStatement(query);

            pstmt.setInt(1, id);

            return pstmt.executeUpdate() == 1;

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Author convert(ResultSet rs) throws SQLException {

        Integer id = rs.getInt("id");
        String firstName = rs.getString("firstName");
        String lastName = rs.getString("lastName");
        LocalDate birthDate = rs.getDate("birthDate").toLocalDate();

        return new Author(id, firstName, lastName, birthDate);
    }
}
