-- Brigádnapló adatbázis séma (SQLite)

CREATE TABLE IF NOT EXISTS teams (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS shift_types (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    code TEXT NOT NULL UNIQUE,
    start_time TEXT NOT NULL,
    end_time TEXT NOT NULL,
    color_code TEXT NOT NULL,
    is_working_shift INTEGER NOT NULL DEFAULT 1,
    break_minutes INTEGER NOT NULL DEFAULT 45
);

CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name TEXT NOT NULL,
    last_name TEXT NOT NULL,
    team_id INTEGER,
    contract_start TEXT NOT NULL,
    birth_date TEXT NOT NULL,
    children_count INTEGER NOT NULL DEFAULT 0,
    has_disability INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (team_id) REFERENCES teams(id)
);

CREATE TABLE IF NOT EXISTS work_schedule (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    shift_type_id INTEGER NOT NULL,
    date TEXT NOT NULL,
    notes TEXT,
    UNIQUE (user_id, date),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (shift_type_id) REFERENCES shift_types(id)
);

CREATE TABLE IF NOT EXISTS time_off (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    type TEXT NOT NULL CHECK (type IN ('SZABADSAG', 'BETEGSZABADSAG', 'FIZETETLEN')),
    reason TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS app_settings (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT
);

CREATE INDEX IF NOT EXISTS idx_schedule_date ON work_schedule(date);
CREATE INDEX IF NOT EXISTS idx_time_off_user ON time_off(user_id, start_date, end_date);

-- A 4 folyamatos termelési brigád
INSERT OR IGNORE INTO teams (id, name, description) VALUES (1, 'A Brigád', '1. számú folyamatos termelési brigád');
INSERT OR IGNORE INTO teams (id, name, description) VALUES (2, 'B Brigád', '2. számú folyamatos termelési brigád');
INSERT OR IGNORE INTO teams (id, name, description) VALUES (3, 'C Brigád', '3. számú folyamatos termelési brigád');
INSERT OR IGNORE INTO teams (id, name, description) VALUES (4, 'D Brigád', '4. számú folyamatos termelési brigád');

-- Műszaktípusok (12 órás műszakok 45 perc munkaközi szünettel, Mt. 103. §; távolléti kódok)
INSERT OR IGNORE INTO shift_types (id, name, code, start_time, end_time, color_code, is_working_shift, break_minutes)
VALUES (1, 'Nappal (06-18)', 'N', '06:00:00', '18:00:00', '#F57C00', 1, 45);
INSERT OR IGNORE INTO shift_types (id, name, code, start_time, end_time, color_code, is_working_shift, break_minutes)
VALUES (2, 'Éjszaka (18-06)', 'É', '18:00:00', '06:00:00', '#1976D2', 1, 45);
INSERT OR IGNORE INTO shift_types (id, name, code, start_time, end_time, color_code, is_working_shift, break_minutes)
VALUES (3, 'Pihenőnap', 'P', '00:00:00', '00:00:00', '#4CAF50', 0, 0);
INSERT OR IGNORE INTO shift_types (id, name, code, start_time, end_time, color_code, is_working_shift, break_minutes)
VALUES (4, 'Szabadság', 'SZ', '00:00:00', '00:00:00', '#9C27B0', 0, 0);
INSERT OR IGNORE INTO shift_types (id, name, code, start_time, end_time, color_code, is_working_shift, break_minutes)
VALUES (5, 'Betegszabadság / Táppénz', 'B', '00:00:00', '00:00:00', '#E53935', 0, 0);
INSERT OR IGNORE INTO shift_types (id, name, code, start_time, end_time, color_code, is_working_shift, break_minutes)
VALUES (6, 'Fizetés nélküli szabadság', 'F', '00:00:00', '00:00:00', '#757575', 0, 0);

-- Korábbi adatbázisokban a nappalos műszak még „Délelőtt” néven, D kóddal szerepelt
UPDATE shift_types SET code = 'N' WHERE code = 'D';
UPDATE shift_types SET name = 'Nappal (06-18)' WHERE code = 'N';
