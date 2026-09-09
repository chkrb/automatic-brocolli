import hashlib

from sqlalchemy.dialects.postgresql import insert

from app.database import SessionLocal
from app.models import Product, ProductInventory


MIN_STOCK = 10
MAX_STOCK = 1000


def generate_quantity(product: Product) -> int:
    digest = hashlib.sha256(str(product.uuid).encode()).digest()
    number = int.from_bytes(digest[:8], "big")

    return MIN_STOCK + number % (MAX_STOCK - MIN_STOCK + 1)


def main():
    with SessionLocal() as session:
        products = session.query(Product).order_by(Product.id).all()

        rows = [
            {
                "product_id": product.id,
                "physical_stock": generate_quantity(product),
                "version": 0,
            }
            for product in products
        ]

        stmt = insert(ProductInventory).values(rows)

        stmt = stmt.on_conflict_do_nothing(
            index_elements=[ProductInventory.product_id]
        )

        session.execute(stmt)
        session.commit()

        print(f"Processed {len(products)} products.")


if __name__ == "__main__":
    main()