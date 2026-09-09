from sqlalchemy.dialects.postgresql import insert

from app.database import SessionLocal
from app.models import POS


POSITIONS = [
    {"name": "POS A"},
    {"name": "POS B"},
    {"name": "POS C"},
    {"name": "POS D"},
]


def main():
    with SessionLocal() as session:
        stmt = insert(POS).values(POSITIONS)
        stmt = stmt.on_conflict_do_nothing()

        session.execute(stmt)
        session.commit()

        rows = session.query(POS).order_by(POS.id).all()

        print("POS terminals:")
        for pos in rows:
            print(f"{pos.id}: {pos.name}")


if __name__ == "__main__":
    main()