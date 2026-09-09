from sqlalchemy import select

from app.database import SessionLocal
from app.models import POS, ProductInventory, POSInventory


def main():
    with SessionLocal() as session:
        poses = session.scalars(
            select(POS).order_by(POS.id)
        ).all()

        if not poses:
            raise RuntimeError("No POS terminals found.")

        inventories = session.scalars(
            select(ProductInventory).order_by(ProductInventory.product_id)
        ).all()

        pos_count = len(poses)

        for inventory in inventories:
            base = inventory.physical_stock // pos_count
            remainder = inventory.physical_stock % pos_count

            for index, pos in enumerate(poses):
                allocation = base + (1 if index < remainder else 0)

                existing = session.scalar(
                    select(POSInventory).where(
                        POSInventory.pos_id == pos.id,
                        POSInventory.product_id == inventory.product_id,
                    )
                )

                if existing is None:
                    session.add(
                        POSInventory(
                            pos_id=pos.id,
                            product_id=inventory.product_id,
                            allocated_stock=allocation,
                            blocked_stock=0,
                        )
                    )

        session.commit()

        print(
            f"Allocated {len(inventories)} products across "
            f"{pos_count} POS terminals."
        )


if __name__ == "__main__":
    main()