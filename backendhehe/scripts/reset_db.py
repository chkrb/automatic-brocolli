import argparse
import random

from sqlalchemy import delete, func, select

from app.database import SessionLocal
from app.models import (
    POS,
    POSInventory,
    Product,
    ProductInventory,
    Reservation,
    ReservationItem,
)


MIN_STOCK = 10
MAX_STOCK = 1000


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", type=int)
    args = parser.parse_args()

    rng = random.Random(args.seed)

    with SessionLocal() as session:
        session.execute(delete(ReservationItem))
        session.execute(delete(Reservation))
        session.execute(delete(POSInventory))
        session.execute(delete(ProductInventory))

        products = session.scalars(
            select(Product).order_by(Product.id)
        ).all()

        poses = session.scalars(
            select(POS).order_by(POS.id)
        ).all()

        if not poses:
            raise RuntimeError("No POS terminals found.")

        pos_count = len(poses)

        for product in products:
            stock = rng.randint(MIN_STOCK, MAX_STOCK)

            session.add(
                ProductInventory(
                    product_id=product.id,
                    physical_stock=stock,
                    version=0,
                )
            )

            base = stock // pos_count
            remainder = stock % pos_count

            for index, pos in enumerate(poses):
                allocation = base + (1 if index < remainder else 0)

                session.add(
                    POSInventory(
                        pos_id=pos.id,
                        product_id=product.id,
                        allocated_stock=allocation,
                        blocked_stock=0,
                    )
                )

        session.commit()

    print(f"Reset complete. Seed: {args.seed}")


if __name__ == "__main__":
    main()