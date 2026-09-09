from typing import Any

from .product import Product

class Catalog:
    def __init__(self, init: dict[str, Any]):
        self.version = int(init["version"])
        self.products = list(map(lambda x: Product(x), init["products"]))

        assert 0 < self.version < (1 << 16)  # version is a unsigned 16-bit (non-zero) number
