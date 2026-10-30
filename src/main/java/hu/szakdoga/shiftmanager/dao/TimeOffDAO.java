package hu.szakdoga.shiftmanager.dao;

import hu.szakdoga.shiftmanager.model.TimeOffRequest;
import hu.szakdoga.shiftmanager.model.User;
import hu.szakdoga.shiftmanager.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A jóváhagyott távollétek (szabadság, betegség, fizetés nélküli szabadság) adatelérése.
 */
public class TimeOffDAO {
    private final UserDAO userDAO = new UserDAO();

    /** Az adott időszakkal átfedő összes távollét. */
    public List<TimeOffRequest> getForPeriod(LocalDate from, LocalDate to) {
        List<TimeOffRequest> list = new ArrayList<>();
        String sql = "SELECT id, user_id, start_date, end_date, type, reason FROM time_off " +
                     "WHERE start_date <= ? AND end_date >= ? ORDER BY start_date";

        Map<Integer, User> users = new HashMap<>();
        for (User u : userDAO.getAllUsers()) {
            users.put(u.getId(), u);
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, to.toString());
            ps.setString(2, from.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = users.get(rs.getInt("user_id"));
                    if (user != null) {
                        list.add(new TimeOffRequest(
                                rs.getInt("id"),
                                user,
                                LocalDate.parse(rs.getString("start_date")),
                                LocalDate.parse(rs.getString("end_date")),
                                rs.getString("type"),
                                rs.getString("reason")));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Hiba a távollétek lekérésekor: " + e.getMessage());
        }
        return list;
    }

    public List<TimeOffRequest> getForYear(int year) {
        return getForPeriod(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    public List<TimeOffRequest> getForMonth(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        return getForPeriod(start, start.plusMonths(1).minusDays(1));
    }

    public boolean save(int userId, LocalDate start, LocalDate end, String type, String reason) {
        String sql = "INSERT INTO time_off (user_id, start_date, end_date, type, reason) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, start.toString());
            ps.setString(3, end.toString());
            ps.setString(4, type);
            ps.setString(5, reason);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.err.println("Hiba a távollét rögzítésekor: " + e.getMessage());
            return false;
        }
    }
}
