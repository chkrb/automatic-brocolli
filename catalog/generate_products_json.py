#!/usr/bin/env python3

import csv
import os
import json
import sys
import uuid

os.chdir(os.path.dirname(sys.argv[0]))

PRODUCT_VERSION = 1

csv_reader = csv.reader(
    open("./datasets/blinkit-products-selected-columns.csv").read().split("\n")
)

# Indices based on CSV above.
NAME = 0
BRAND = 1
MRP = 2
QUANTITY = 4
AVAILABLE = 7

products = []

next(csv_reader)  # skip header
for values in csv_reader:
    if not values:
        continue

    products.append({
        "uuid": str(uuid.uuid4()),
        "added": PRODUCT_VERSION,
        "name": values[NAME] + " (" + values[QUANTITY] + ")",
        "brand": values[BRAND],
        "mrp": int(values[MRP]),
        "unit": "",
    })


for target in [
    "./products.json",
    "../customer-app/app/src/main/res/raw/products.json",
    "../merchant-pos/spec/products.json",
]:
    json.dump({"version": PRODUCT_VERSION, "products": products}, open(target, "w"))
