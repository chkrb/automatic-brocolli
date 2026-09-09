from datetime import datetime, timedelta, timezone

from app.database import SessionLocal
from app.services.res import create_reservation


def main():
    with SessionLocal() as session:
        reservation = create_reservation(
            session,
            pos_id=1,
            items=[(1, 5)],
            expires_at=datetime.now(timezone.utc) + timedelta(minutes=5),
        )

        session.commit()

        print(f"Reservation created: {reservation.id}")


if __name__ == "__main__":
    main()