from typing import Any
import json
import requests

from .product import Product

class Catalog:
    def __init__(self, api_url: str, pos_id: int):
        self.api_url = api_url
        self.pos_id = pos_id

        catalog_response = requests.get(f"{self.api_url}/products")
        catalog = {
            # TODO: add `version` to backend.
            "version": 1,
            "products": list(
                map(
                    # TODO: add `products.added` to backend.
                    lambda x: {**x, "added": 1},
                    json.loads(catalog_response.text),
                )
            ),
        }

        self.version = int(catalog["version"])
        self.products = list(map(lambda x: Product(x), catalog["products"]))

        assert 0 < self.version < (1 << 16)  # version is a unsigned 16-bit (non-zero) number
