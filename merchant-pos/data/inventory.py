from functools import reduce
from typing import Any

from .catalog import Catalog
from .product import Product

class Inventory:
    def __init__(self, catalog: Catalog):
        self.catalog = catalog

        self.selling_prices: dict[Product, int] = {}
        self.quantities: dict[Product, int] = {}

        # XXX: here, there's gonna be a database where we get metrics such as
        # remaining stock and stuff
        import random
        for product in self.catalog.products:
            self.selling_prices[product] = random.randint(product.mrp // 2, product.mrp)
            self.quantities[product] = random.randint(0, 1000)

    def to_retailer_status(self, spec_version: int = 1) -> bytes:
        products_data_array = reduce(
            lambda accum, x: accum + x,
            map(
                lambda product: product.to_retailer_status_product(
                    self.catalog.products,
                    self.selling_prices[product],
                    self.quantities[product],
                    spec_version,
                ),
                filter(lambda product: self.quantities[product], self.catalog.products),
            ),
        )
        products_data_sentinel = b"\0\0\0\0"

        return (
            self.catalog.version.to_bytes(2, "little")
            + spec_version.to_bytes(2, "little")
            + products_data_array
            + products_data_sentinel
        )
