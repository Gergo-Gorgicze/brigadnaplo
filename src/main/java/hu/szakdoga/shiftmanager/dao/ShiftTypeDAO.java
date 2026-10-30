package hu.szakdoga.shiftmanager.dao;

import hu.szakdoga.shiftmanager.model.ShiftType;
import hu.szakdoga.shiftmanager.util.DatabaseManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShiftTypeDAO {

    public List<ShiftType> getAllShiftTypes() {
        List<ShiftType> list = new ArrayList<>();
        String sql = "SELECT id, name, code, start_time, end_time, color_code, is_working_shift, break_minutes FROM shift_types ORDER BY id";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(mapResultSetToShiftType(rs));
            }
        } catch (Exception e) {
            System.err.println("Hiba a műszaktípusok lekérésekor: " + e.getMessage());
        }
        return list;
    }

    public Map<String, ShiftType> getShiftTypesByCodeMap() {
        Map<String, ShiftType> map = new HashMap<>();
        for (ShiftType st : getAllShiftTypes()) {
            map.put(st.getCode(), st);
        }
        return map;
    }

    private ShiftType mapResultSetToShiftType(ResultSet rs) throws Exception {
        return new ShiftType(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("code"),
                LocalTime.parse(rs.getString("start_time")),
                LocalTime.parse(rs.getString("end_time")),
                rs.getString("color_code"),
                rs.getInt("is_working_shift") == 1,
                rs.getInt("break_minutes")
        );
    }
}
