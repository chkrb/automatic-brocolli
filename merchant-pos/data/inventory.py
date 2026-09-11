from functools import reduce
import json
import requests
from typing import Any

from .catalog import Catalog
from .product import Product

class Inventory:
    def __init__(self, catalog: Catalog):
        self.catalog = catalog

        self.selling_prices: dict[Product, int] = {}
        self.quantities: dict[Product, int] = {}

        for product in self.catalog.products:
            # TODO: add "product.selling_price" to backend.
            self.selling_prices[product] = product.mrp
            response = json.loads(
                requests.get(
                    f"{self.catalog.api_url}/products/{product.backend_id}/inventory"
                ).text
            )
            self.quantities[product] = int(
                response["pos"][self.catalog.pos_id - 1]["available_stock"]
            )

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
