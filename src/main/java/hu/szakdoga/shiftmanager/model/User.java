package hu.szakdoga.shiftmanager.model;

import hu.szakdoga.shiftmanager.util.AppClock;

import java.time.LocalDate;
import java.time.Period;

/**
 * Munkavállaló (gépkezelő) a brigádjával és a szabadságkeret számításához szükséges adatokkal.
 */
public class User {
    private int id;
    private String firstName;
    private String lastName;
    private Team team;
    private LocalDate contractStart;
    private LocalDate birthDate;
    private int childrenCount;
    private boolean hasDisability;

    public User() {}

    public User(int id, String firstName, String lastName, Team team, LocalDate contractStart,
                LocalDate birthDate, int childrenCount, boolean hasDisability) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.team = team;
        this.contractStart = contractStart;
        this.birthDate = birthDate;
        this.childrenCount = childrenCount;
        this.hasDisability = hasDisability;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getFullName() { return lastName + " " + firstName; }

    public Team getTeam() { return team; }
    public void setTeam(Team team) { this.team = team; }

    public LocalDate getContractStart() { return contractStart; }
    public void setContractStart(LocalDate contractStart) { this.contractStart = contractStart; }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public int getChildrenCount() { return childrenCount; }
    public void setChildrenCount(int childrenCount) { this.childrenCount = childrenCount; }

    public boolean isHasDisability() { return hasDisability; }
    public void setHasDisability(boolean hasDisability) { this.hasDisability = hasDisability; }

    // ==========================================
    // Munka Törvénykönyve (Mt.) szerinti szabadságkeret
    // ==========================================

    /** Alapszabadság (Mt. 116. §): évi 20 munkanap. */
    public int getBaseVacationDays() {
        return 20;
    }

    /** Életkor szerinti pótszabadság a vizsgált évben betöltött életkor alapján (Mt. 117. §). */
    public int calculateAgeBonusDays(int targetYear) {
        if (birthDate == null) return 0;
        int ageInYear = targetYear - birthDate.getYear();

        if (ageInYear >= 45) return 10;
        if (ageInYear >= 43) return 9;
        if (ageInYear >= 41) return 8;
        if (ageInYear >= 39) return 7;
        if (ageInYear >= 37) return 6;
        if (ageInYear >= 35) return 5;
        if (ageInYear >= 33) return 4;
        if (ageInYear >= 31) return 3;
        if (ageInYear >= 28) return 2;
        if (ageInYear >= 25) return 1;
        return 0;
    }

    /** 16 évesnél fiatalabb gyermekek után járó pótszabadság (Mt. 118. §): 1 gyermek 2, 2 gyermek 4, 2-nél több 7 nap. */
    public int calculateChildrenBonusDays() {
        if (childrenCount <= 0) return 0;
        if (childrenCount == 1) return 2;
        if (childrenCount == 2) return 4;
        return 7;
    }

    /** Megváltozott munkaképességű munkavállaló pótszabadsága (Mt. 120. §): 5 munkanap. */
    public int calculateDisabilityBonusDays() {
        return hasDisability ? 5 : 0;
    }

    /** A teljes éves szabadságkeret munkanapokban (Mt. 115-120. §). */
    public int calculateTotalAnnualVacationDays(int targetYear) {
        return getBaseVacationDays()
                + calculateAgeBonusDays(targetYear)
                + calculateChildrenBonusDays()
                + calculateDisabilityBonusDays();
    }

    /** A szabadságkeret órában (1 munkanap = 8 óra). */
    public double calculateTotalAnnualVacationHours(int targetYear) {
        return calculateTotalAnnualVacationDays(targetYear) * 8.0;
    }

    /** Életkor az alkalmazás "mai napján" (demó helyzetben a szimulált dátumon). */
    public int getAge() {
        if (birthDate == null) return 0;
        return Period.between(birthDate, AppClock.today()).getYears();
    }

    @Override
    public String toString() {
        return getFullName() + " (" + (team != null ? team.getName() : "Nincs brigád") + ")";
    }
}
