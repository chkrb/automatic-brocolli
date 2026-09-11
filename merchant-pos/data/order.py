from functools import reduce
import json
import requests
from typing import Any

from .catalog import Catalog
from .inventory import Inventory
from .product import Product

class Order:
    def __init__(self, catalog: Catalog):
        self.catalog = catalog

        self.pending = False
        self.ordered_stock: dict[Product, int] = {}

    def from_order_request(self, data: bytes, spec_version: int = 1):
        data_offset = 0

        while data_offset < len(data):
            header = data[data_offset]
            data_offset += 1

            header_uuid_fragment_bytes = (header & 0b00001111) + 1
            header_ordered_stock_bytes = ((header & 0b00110000) >> 4) + 1

            uuid_fragment = data[
                data_offset : (data_offset + header_uuid_fragment_bytes)
            ]
            data_offset += header_uuid_fragment_bytes

            ordered_stock = int.from_bytes(
                data[data_offset : (data_offset + header_ordered_stock_bytes)], "little"
            )
            data_offset += header_ordered_stock_bytes

            for product in self.catalog.products:
                for i in range(len(uuid_fragment)):
                    if product.uuid[i] ^ uuid_fragment[i]:
                        break
                else:
                    self.ordered_stock[product] = ordered_stock
                    break

        self.pending = True

    def execute(self, inventory: Inventory):
        payload = {
            "pos_id": self.catalog.pos_id,
            "items": [],
        }
        for product in self.ordered_stock:
            payload["items"].append(
                {
                    "product_id": product.backend_id,
                    "quantity": self.ordered_stock[product],
                }
            )

        requests.post(f"{self.catalog.api_url}/orders", json=payload)
        requests.post(f"{self.catalog.api_url}/inventory/rebalance")

        for product in self.ordered_stock:
            response = json.loads(
                requests.get(
                    f"{self.catalog.api_url}/products/{product.backend_id}/inventory"
                ).text
            )
            inventory.quantities[product] = int(
                response["pos"][self.catalog.pos_id - 1]["available_stock"]
            )

        self.ordered_stock.clear()
        self.pending = False
