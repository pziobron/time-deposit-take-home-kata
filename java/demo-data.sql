-- Optional local demo data, applied manually after Flyway creates the tables.
-- Fixed IDs and ON CONFLICT make reloading safe for existing demo records.
INSERT INTO "timeDeposits" (id, "planType", balance, days) VALUES
    (1, 'basic', 1200.123, 31),
    (2, 'student', 1200.0, 365),
    (3, 'premium', 1200.0, 46),
    (4, 'premium', 1200.0, 45)
ON CONFLICT (id) DO NOTHING;

INSERT INTO withdrawals (id, "timeDepositId", amount, date) VALUES
    (1, 1, 50.25, DATE '2026-11-01')
ON CONFLICT (id) DO NOTHING;

SELECT setval(pg_get_serial_sequence('"timeDeposits"', 'id'), (SELECT MAX(id) FROM "timeDeposits"));
SELECT setval(pg_get_serial_sequence('withdrawals', 'id'), (SELECT MAX(id) FROM withdrawals));
