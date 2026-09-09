import os

os.chdir(os.path.dirname(__file__))

from fastapi import FastAPI
from sqlalchemy import text

from fastapi.middleware.cors import CORSMiddleware

from app.database import engine

from sqlalchemy import select
from app.database import SessionLocal
from app.models import Product

from sqlalchemy import select

from app.models import Product, ProductInventory, POSInventory, POS

app = FastAPI(title="Automatic Brocolli Cat Backend")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
def root():
    return {"status": "alive"}


@app.get("/health/db")
def database_health():
    with engine.connect() as connection:
        result = connection.execute(text("SELECT 1"))
        value = result.scalar()

    return {
        "database": "connected",
        "result": value,
    }

@app.get("/products")
def get_products():
    with SessionLocal() as session:
        products = session.scalars(
            select(Product).order_by(Product.id)
        ).all()

        return [
            {
                "id": product.id,
                "uuid": str(product.uuid),
                "name": product.name,
                "brand": product.brand,
                "mrp": product.mrp,
                "unit": product.unit,
            }
            for product in products
        ]

@app.get("/products/{product_id}/inventory")
def get_product_inventory(product_id: int):
    with SessionLocal() as session:
        product = session.get(Product, product_id)

        if product is None:
            return {"error": "Product not found"}

        global_inventory = session.scalar(
            select(ProductInventory).where(
                ProductInventory.product_id == product_id
            )
        )

        pos_rows = session.scalars(
            select(POSInventory)
            .where(POSInventory.product_id == product_id)
            .order_by(POSInventory.pos_id)
        ).all()

        return {
            "product_id": product.id,
            "physical_stock": (
                global_inventory.physical_stock
                if global_inventory
                else 0
            ),
            "pos": [
                {
                    "pos_id": row.pos_id,
                    "allocated_stock": row.allocated_stock,
                    "blocked_stock": row.blocked_stock,
                    "available_stock": (
                        row.allocated_stock - row.blocked_stock
                    ),
                }
                for row in pos_rows
            ],
        }
