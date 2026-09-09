from functools import reduce
from typing import Any

from .catalog import Catalog
from .product import Product

class Order:
    def __init__(self, catalog: Catalog):
        self.catalog = catalog

        self.ordered_stock: dict[bytes, int] = {}

    def clear(self):
        self.ordered_stock.clear()

    def from_order_request(self, data: bytes, spec_version: int = 1):
        uuids = list(map(lambda x: x.uuid, self.catalog.products))

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

            for uuid in uuids:
                for i in range(len(uuid_fragment)):
                    if uuid[i] ^ uuid_fragment[i]:
                        break
                else:
                    self.ordered_stock[uuid] = ordered_stock
                    break
