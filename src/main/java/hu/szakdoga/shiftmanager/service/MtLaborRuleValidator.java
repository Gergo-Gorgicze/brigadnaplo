package hu.szakdoga.shiftmanager.service;

import hu.szakdoga.shiftmanager.model.ShiftType;
import hu.szakdoga.shiftmanager.model.WorkSchedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A Munka Törvénykönyve (Mt. 2012. évi I. törvény) beosztási szabályainak ellenőrzése.
 */
public class MtLaborRuleValidator {

    public static class ValidationResult {
        private final boolean valid;
        private final List<String> errors;

        public ValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }

        public String getErrorMessage() {
            return String.join("\n", errors);
        }
    }

    /**
     * Ellenőrzi egy dolgozó beosztásának törvényi megfelelőségét.
     */
    public ValidationResult validateUserSchedule(List<WorkSchedule> userSchedules) {
        List<String> errors = new ArrayList<>();

        if (userSchedules == null || userSchedules.isEmpty()) {
            return new ValidationResult(true, errors);
        }

        // Dátum szerint sorba rendezés
        List<WorkSchedule> sorted = userSchedules.stream()
                .sorted(Comparator.comparing(WorkSchedule::getDate))
                .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            WorkSchedule current = sorted.get(i);
            ShiftType currentShift = current.getShiftType();

            // 1. Napi maximum 12 órás munkaidő ellenőrzése (Mt. 99. §)
            if (currentShift != null && currentShift.isWorkingShift() && currentShift.getNetWorkingHours() > 12.0) {
                errors.add(String.format("Mt. 99. § sértés [%s]: A napi munkaidő nem haladhatja meg a 12 órát!", current.getDate()));
            }

            // 2. Két műszak közötti minimális 11 óra pihenőidő ellenőrzése (Mt. 104. §)
            if (i > 0) {
                WorkSchedule prev = sorted.get(i - 1);
                ShiftType prevShift = prev.getShiftType();

                if (prevShift != null && currentShift != null && prev.getDate().plusDays(1).isEqual(current.getDate())) {
                    // Ha az előző nap Éjszakás (18-06) volt, az a mai nap reggel 06:00-kor ért véget
                    if ("É".equals(prevShift.getCode())) {
                        if ("N".equals(currentShift.getCode())) {
                            errors.add(String.format("Mt. 104. § súlyos pihenőidő sértés [%s]: Éjszakai műszak (18-06) után nem osztható be közvetlenül nappalos műszakra (06-18)! Pihenőidő: 0 óra (min. 11 óra szükséges).",
                                    current.getDate()));
                        }
                    }
                }
            }
        }

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /**
     * Több dolgozó vegyes beosztásának ellenőrzése: dolgozónként csoportosít, és a hibaüzenetek
     * elé odaírja a dolgozó nevét. (Az egész havi tervet így lehet egyszerre validálni, anélkül,
     * hogy két különböző dolgozó egymást követő napjai összekeverednének.)
     */
    public ValidationResult validateAll(List<WorkSchedule> schedules) {
        List<String> errors = new ArrayList<>();
        if (schedules == null || schedules.isEmpty()) {
            return new ValidationResult(true, errors);
        }

        Map<Integer, List<WorkSchedule>> byUser = new LinkedHashMap<>();
        for (WorkSchedule ws : schedules) {
            if (ws.getUser() == null) continue;
            byUser.computeIfAbsent(ws.getUser().getId(), k -> new ArrayList<>()).add(ws);
        }

        for (List<WorkSchedule> userSchedules : byUser.values()) {
            ValidationResult r = validateUserSchedule(userSchedules);
            String name = userSchedules.get(0).getUser().getFullName();
            for (String err : r.getErrors()) {
                errors.add(name + ": " + err);
            }
        }
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
