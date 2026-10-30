package hu.szakdoga.shiftmanager.dao;

import hu.szakdoga.shiftmanager.model.ShiftType;
import hu.szakdoga.shiftmanager.model.User;
import hu.szakdoga.shiftmanager.model.WorkSchedule;
import hu.szakdoga.shiftmanager.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleDAO {
    private final UserDAO userDAO = new UserDAO();
    private final ShiftTypeDAO shiftTypeDAO = new ShiftTypeDAO();

    public List<WorkSchedule> getSchedulesForMonth(int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        return getSchedulesForPeriod(start, start.plusMonths(1).minusDays(1));
    }

    public List<WorkSchedule> getSchedulesForYear(int year) {
        return getSchedulesForPeriod(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    public List<WorkSchedule> getSchedulesForPeriod(LocalDate from, LocalDate to) {
        List<WorkSchedule> list = new ArrayList<>();
        String sql = "SELECT id, user_id, shift_type_id, date, notes FROM work_schedule " +
                     "WHERE date >= ? AND date <= ? ORDER BY date";

        Map<Integer, User> users = new HashMap<>();
        for (User u : userDAO.getAllUsers()) users.put(u.getId(), u);
        Map<Integer, ShiftType> shifts = new HashMap<>();
        for (ShiftType st : shiftTypeDAO.getAllShiftTypes()) shifts.put(st.getId(), st);

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from.toString());
            ps.setString(2, to.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = users.get(rs.getInt("user_id"));
                    ShiftType shift = shifts.get(rs.getInt("shift_type_id"));
                    if (user != null && shift != null) {
                        list.add(new WorkSchedule(rs.getLong("id"), user, shift,
                                LocalDate.parse(rs.getString("date")), rs.getString("notes")));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Hiba a beosztások lekérésekor: " + e.getMessage());
        }
        return list;
    }

    /**
     * Beosztások mentése egyetlen tranzakcióban. Ha egy dolgozó adott napjára már van bejegyzés,
     * azt felülírja (UNIQUE(user_id, date) + INSERT OR REPLACE).
     */
    public void saveSchedulesBatch(List<WorkSchedule> schedules) throws Exception {
        if (schedules == null || schedules.isEmpty()) return;
        String sql = "INSERT OR REPLACE INTO work_schedule (user_id, shift_type_id, date, notes) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (WorkSchedule ws : schedules) {
                    ps.setInt(1, ws.getUser().getId());
                    ps.setInt(2, ws.getShiftType().getId());
                    ps.setString(3, ws.getDate().toString());
                    ps.setString(4, ws.getNotes());
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}
