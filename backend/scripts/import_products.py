import json
from pathlib import Path
from uuid import UUID

from sqlalchemy.dialects.postgresql import insert

from app.database import SessionLocal
from app.models import Product


BASE_DIR = Path(__file__).resolve().parent.parent
PRODUCTS_FILE = BASE_DIR / "products.json"


def load_products():
    with PRODUCTS_FILE.open("r", encoding="utf-8") as f:
        data = json.load(f)

    return data["products"]


def main():
    products = load_products()

    rows = [
        {
            "uuid": UUID(product["uuid"]),
            "added": product["added"],
            "name": product["name"],
            "brand": product["brand"],
            "mrp": product["mrp"],
            "unit": product["unit"],
        }
        for product in products
    ]

    with SessionLocal() as session:
        stmt = insert(Product).values(rows)

        stmt = stmt.on_conflict_do_nothing(
            index_elements=[Product.uuid]
        )

        result = session.execute(stmt)
        session.commit()

        print(f"Loaded {len(rows)} products.")
        print(f"Inserted {result.rowcount} new products.")


if __name__ == "__main__":
    main()