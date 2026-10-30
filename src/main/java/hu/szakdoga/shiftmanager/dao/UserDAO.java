package hu.szakdoga.shiftmanager.dao;

import hu.szakdoga.shiftmanager.model.Team;
import hu.szakdoga.shiftmanager.model.User;
import hu.szakdoga.shiftmanager.util.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT u.id, u.first_name, u.last_name, u.contract_start, u.birth_date, " +
                     "u.children_count, u.has_disability, t.id AS team_id, t.name AS team_name, t.description " +
                     "FROM users u LEFT JOIN teams t ON u.team_id = t.id " +
                     "ORDER BY u.team_id, u.last_name, u.first_name";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                int teamId = rs.getInt("team_id");
                Team team = rs.wasNull() ? null : new Team(teamId, rs.getString("team_name"), rs.getString("description"));
                list.add(new User(
                        rs.getInt("id"),
                        rs.getString("first_name"),
                        rs.getString("last_name"),
                        team,
                        LocalDate.parse(rs.getString("contract_start")),
                        LocalDate.parse(rs.getString("birth_date")),
                        rs.getInt("children_count"),
                        rs.getInt("has_disability") == 1
                ));
            }
        } catch (Exception e) {
            System.err.println("Hiba a dolgozók lekérésekor: " + e.getMessage());
        }
        return list;
    }

    public int countUsers() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}
