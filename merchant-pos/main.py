#!/usr/bin/env python3

import cv2
import json
import numpy as np
import os
import qrcode
import requests
import sys
import optparse
import threading
import time
import zxingcpp

from PySide6.QtCore import Qt, QTimer, Slot
from PySide6.QtGui import QFont, QImage, QPixmap
from PySide6.QtWidgets import (
    QApplication,
    QGridLayout,
    QHBoxLayout,
    QLabel,
    QMainWindow,
    QSizePolicy,
    QSpacerItem,
    QVBoxLayout,
    QWidget,
)

os.chdir(os.path.dirname(__file__))

from data.catalog import Catalog
from presentation.pageddata import PagedData
from data.inventory import Inventory
from data.order import Order
from data.product import Product

class Window(QMainWindow):
    def __init__(self, api_url: str, pos_id: int):
        super().__init__()
        self.catalog = Catalog(api_url, pos_id)
        self.inventory = Inventory(self.catalog)
        self.order = Order(self.catalog)
        self.paged_data_retailer_status = PagedData()
        self.paged_data_order_request = PagedData()

        self.qr_page_index = 0
        self.qr_qimages = []
        self.generate_retailer_status_qr()

        self.video_feed = cv2.VideoCapture(0)
        self.order_display_start_time = time.time()
        threading.Thread(target=self.scan_qr_code_thread, daemon=True).start()

        self.init_ui()

    def generate_retailer_status_qr(self):
        # Reference: https://www.qrcode.com/en/about/version.html
        QR_VERSION = 25
        QR_MAX_BYTES = 1273

        self.qr_qimages.clear()
        for paged_data in self.paged_data_retailer_status.get_data_pages_from_data(
            self.inventory.to_retailer_status(), QR_MAX_BYTES
        ):
            qr = qrcode.QRCode(QR_VERSION, qrcode.ERROR_CORRECT_L)
            qr.add_data(paged_data)
            img = qr.make_image().get_image().convert("RGB")
            img = img.resize((img.width // 2, img.height // 2)).convert("L")
            qimg = QImage(
                img.tobytes(),
                img.width,
                img.height,
                img.width,
                QImage.Format.Format_Grayscale8,
            )
            self.qr_qimages.append(qimg)

    def scan_qr_code_thread(self):
        # Inversion is done so as to compensate for QR codes which are drawn in
        # dark mode.
        inverted_qr_code = False

        while True:
            time.sleep(0.1)
            image = self.video_feed.read()[1]
            if image is None:
                raise RuntimeError("no camera feed available")

            if self.order.pending:
                continue

            if inverted_qr_code:
                image_inverted = np.uint8(1.0) - image
                decoded = zxingcpp.read_barcodes(image_inverted)
            else:
                decoded = zxingcpp.read_barcodes(image)

            if not decoded:
                inverted_qr_code = not inverted_qr_code
                continue

            data = self.paged_data_order_request.add_data_page_and_construct(
                decoded[0].bytes
            )
            if data is not None:
                self.paged_data_order_request.clear()
                self.order_display_start_time = time.time()
                self.order.from_order_request(data)

    def init_ui(self):
        self.WINDOW_W = 480
        self.WINDOW_H = 480
        self.setWindowTitle("POSQueue Merchant POS")
        self.resize(self.WINDOW_W, self.WINDOW_H)

        self.central_widget = QWidget()
        self.setCentralWidget(self.central_widget)
        self.main_layout = QVBoxLayout(self.central_widget)
        self.main_layout.setAlignment(Qt.AlignmentFlag.AlignCenter)

        self.qr_label = QLabel()
        self.qr_label.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.main_layout.addWidget(self.qr_label)

        self.summary_container = QWidget()
        self.summary_layout = QVBoxLayout(self.summary_container)
        self.summary_layout.setAlignment(Qt.AlignmentFlag.AlignCenter)
        self.summary_container.hide()
        self.main_layout.addWidget(self.summary_container)

        self.timer = QTimer()
        self.timer.timeout.connect(self.update_ui_state)
        self.timer.start(200)

        self.render_qr_code()

    def render_qr_code(self):
        if self.qr_qimages:
            qimg = self.qr_qimages[self.qr_page_index]
            self.qr_label.setPixmap(
                QPixmap.fromImage(qimg).scaled(
                    self.WINDOW_W,
                    self.WINDOW_H,
                    Qt.AspectRatioMode.KeepAspectRatio,
                )
            )

    def render_order_summary(self):
        # Mark the children widgets of the layout for deletion, effectively
        # clearing it.
        while self.summary_layout.count():
            child = self.summary_layout.takeAt(0)
            assert child is not None

            widget = child.widget()
            if widget is not None:
                widget.deleteLater()

        header = QLabel("Order Received")
        header.setFont(QFont("default", 24, QFont.Weight.Bold))
        header.setAlignment(Qt.AlignmentFlag.AlignCenter)

        item_list = QWidget()
        item_list_grid = QGridLayout(item_list)
        item_list_grid.setHorizontalSpacing(30)

        bill_amount = 0
        for i, (product, quantity) in enumerate(self.order.ordered_stock.items()):
            price = self.inventory.selling_prices[product]
            line_total = price * quantity
            bill_amount += line_total
            
            item_label = QLabel(product.name)
            item_label.setFont(QFont("default", 12))
            item_list_grid.addWidget(item_label, i, 0)

            item_label = QLabel(f"{quantity}")
            item_label.setFont(QFont("default", 12))
            item_list_grid.addWidget(item_label, i, 1)

            item_label = QLabel("–")
            item_label.setFont(QFont("default", 12))
            item_list_grid.addWidget(item_label, i, 3)

            item_label = QLabel(f"₹{line_total:.2f}")
            item_label.setFont(QFont("default", 12))
            item_list_grid.addWidget(item_label, i, 4)

        total_label = QLabel(f"₹{bill_amount:.2f}")
        total_label.setFont(QFont("default", 28, QFont.Weight.Bold))
        total_label.setAlignment(Qt.AlignmentFlag.AlignCenter)

        self.summary_layout.addWidget(header)
        self.summary_layout.addWidget(total_label)
        self.summary_layout.addWidget(item_list)

    def update_ui_state(self):
        if self.order.pending:
            if self.summary_container.isHidden():
                self.qr_label.hide()
                self.summary_container.show()
                self.render_order_summary()

            display_duration = 1
            if time.time() - self.order_display_start_time > display_duration:
                # NOTE: We execute order here. There should be a payment flow.
                self.order.execute(self.inventory)
                self.generate_retailer_status_qr()
        else:
            self.summary_container.hide()
            self.qr_label.show()
            
            if len(self.qr_qimages) > 1:
                self.qr_page_index = (self.qr_page_index + 1) % len(self.qr_qimages)
                self.render_qr_code()

if __name__ == "__main__":
    parser = optparse.OptionParser()
    parser.add_option(
        "-e",
        "--endpoint",
        dest="api_url",
        default="http://localhost:8000",
        help="the API endpoint URL of the backend",
        metavar="FILE",
    )
    parser.add_option(
        "-p",
        "--pos-id",
        dest="pos_id",
        help="the POS ID to use when communicating with the backend",
    )
    options, _ = parser.parse_args()

    if options.pos_id is None:
        parser.print_help()
        sys.exit(1)

    app = QApplication(sys.argv)
    window = Window(options.api_url, int(options.pos_id))
    window.show()
    sys.exit(app.exec())
