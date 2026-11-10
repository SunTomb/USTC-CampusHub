import sqlite3
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "backend/src/main/resources/db/migration"
TEST = ROOT / "backend/src/test/resources/db/test-migration"
NAME = "V16__woke_tonglu_brand_agreements.sql"
OLD = "蜗壳 CampusHub"
NEW = "蜗壳同路 CampusHub"


class BrandAgreementMigrationTest(unittest.TestCase):
    def setUp(self):
        self.db = sqlite3.connect(":memory:")
        self.db.execute("PRAGMA foreign_keys=ON")
        self.db.executescript("""
            CREATE TABLE role_agreement_versions (
                id INTEGER PRIMARY KEY,
                agreement_code TEXT NOT NULL,
                version TEXT NOT NULL,
                title TEXT NOT NULL,
                content TEXT NOT NULL,
                effective_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                status TEXT NOT NULL DEFAULT 'ACTIVE',
                created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                UNIQUE (agreement_code, version)
            );
            CREATE TABLE role_agreement_consents (
                id INTEGER PRIMARY KEY,
                user_id INTEGER NOT NULL,
                agreement_version_id INTEGER NOT NULL REFERENCES role_agreement_versions(id),
                accepted_at TEXT NOT NULL
            );
            CREATE TABLE role_applications (
                id INTEGER PRIMARY KEY,
                agreement_version_id INTEGER REFERENCES role_agreement_versions(id),
                review_status TEXT NOT NULL
            );
        """)
        seed = (MAIN / "V15__role_agreements.sql").read_text(encoding="utf-8")
        self.db.executescript(seed[seed.index("INSERT INTO role_agreement_versions"):])
        self.db.execute("INSERT INTO role_agreement_consents VALUES (1, 1, 2, '2026-11-01 12:00:00')")
        self.db.execute("INSERT INTO role_applications VALUES (1, 2, 'APPROVED')")
        self.original = self.db.execute("SELECT id, agreement_code, version, title, content FROM role_agreement_versions ORDER BY id").fetchall()
        self.sql = (MAIN / NAME).read_text(encoding="utf-8") if (MAIN / NAME).exists() else ""

    def tearDown(self):
        self.db.close()

    def test_current_versions_only_change_the_brand_name(self):
        self.db.executescript(self.sql)
        current = self.db.execute("SELECT agreement_code, version, title, content FROM role_agreement_versions WHERE status='ACTIVE' ORDER BY agreement_code").fetchall()
        self.assertEqual(len(current), 4)
        originals = {row[1]: row for row in self.original}
        for code, version, title, content in current:
            self.assertEqual(version, "v1.1")
            self.assertEqual(title, originals[code][3].replace(OLD, NEW))
            self.assertEqual(content, originals[code][4].replace(OLD, NEW))
            self.assertNotIn(OLD, content)

    def test_signed_snapshots_and_existing_applications_are_unchanged(self):
        self.db.executescript(self.sql)
        historical = self.db.execute("SELECT id, agreement_code, version, title, content FROM role_agreement_versions WHERE version='v1.0' ORDER BY id").fetchall()
        self.assertEqual(historical, self.original)
        self.assertEqual(self.db.execute("SELECT * FROM role_agreement_consents").fetchall(), [(1, 1, 2, "2026-11-01 12:00:00")])
        self.assertEqual(self.db.execute("SELECT * FROM role_applications").fetchall(), [(1, 2, "APPROVED")])
        self.assertEqual(self.db.execute("SELECT COUNT(*) FROM role_agreement_versions").fetchone()[0], 8)

    def test_reexecution_does_not_create_duplicate_versions(self):
        self.db.executescript(self.sql)
        self.db.executescript(self.sql)
        self.assertEqual(self.db.execute("SELECT COUNT(*) FROM role_agreement_versions").fetchone()[0], 8)
        self.assertEqual(self.db.execute("SELECT COUNT(*) FROM role_agreement_versions WHERE status='ACTIVE'").fetchone()[0], 4)

    def test_main_and_h2_migrations_match(self):
        self.assertTrue((MAIN / NAME).is_file(), "Brand migration is missing")
        self.assertTrue((TEST / NAME).is_file(), "H2 brand migration is missing")
        self.assertEqual((MAIN / NAME).read_bytes(), (TEST / NAME).read_bytes())


if __name__ == "__main__":
    unittest.main(verbosity=2)
