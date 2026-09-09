#!/usr/bin/env python3

import cv2
import json
import numpy as np
import os
import qrcode
import sys
import threading
import time
import wx
import zxingcpp

os.chdir(os.path.dirname(__file__))

from data.catalog import Catalog
from presentation.pageddata import PagedData
from data.inventory import Inventory
from data.order import Order
from data.product import Product

class Window(wx.Frame):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.catalog = Catalog(json.load(open("spec/products.json")))
        self.inventory = Inventory(self.catalog)
        self.order = Order(self.catalog)
        self.paged_data_retailer_status = PagedData()
        self.paged_data_order_request = PagedData()

        # Reference: https://www.qrcode.com/en/about/version.html
        QR_VERSION = 25
        QR_MAX_BYTES = 1273

        self.qr_page_index = 0
        self.qr_pages = []
        for paged_data in self.paged_data_retailer_status.get_data_pages_from_data(
            self.inventory.to_retailer_status(), QR_MAX_BYTES
        ):
            qr = qrcode.QRCode(QR_VERSION, qrcode.ERROR_CORRECT_L)
            qr.add_data(paged_data)
            qr_image = qr.make_image().get_image().convert("RGB")
            qr_image = qr_image.resize((qr_image.width // 2, qr_image.height // 2))
            self.qr_pages.append((qr_image.width, qr_image.height, qr_image.tobytes()))

        self.video_feed = cv2.VideoCapture(0)
        threading.Thread(target=self.scan_qr_code_thread, daemon=True).start()

        self.init_ui()

    def scan_qr_code_thread(self):
        # Inversion is done so as to compensate for QR codes which are drawn in
        # dark mode.
        inverted_qr_code = False

        while True:
            time.sleep(0.1)
            if self.order.ordered_stock:  # Order pending in system
                continue

            image = self.video_feed.read()[1]
            if image is None:
                raise RuntimeError("no camera feed available")

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
                self.order.from_order_request(data)

    def init_ui(self):
        self.root = wx.Panel(self)
        self.inner = wx.Panel(self.root, -1, style=wx.ALIGN_CENTER)

        self.root_vbox = wx.BoxSizer(wx.VERTICAL)
        self.root.SetSizer(self.root_vbox)

        self.root_vbox_hbox = wx.BoxSizer(wx.HORIZONTAL)
        self.root_vbox.Add(self.root_vbox_hbox, 1, wx.ALL | wx.ALIGN_CENTER, 5)

        self.root_vbox_hbox.Add(self.inner, 0, wx.ALL | wx.ALIGN_CENTER)

        self.inner_box = wx.BoxSizer(wx.VERTICAL)
        self.inner.SetSizer(self.inner_box)

        self.render_qr_code()

        self.Bind(wx.EVT_TIMER, self.timer_show_next_qr_code)
        self.timer = wx.Timer(self)
        self.timer.Start(200)

    def render_qr_code(self):
        qr_image = wx.Image(*self.qr_pages[self.qr_page_index])
        qr_bitmap = qr_image.ConvertToBitmap()

        self.qr_bitmap_widget = wx.StaticBitmap(self.inner, bitmap=qr_bitmap)
        self.qr_bitmap_widget.SetScaleMode(wx.StaticBitmap.Scale_AspectFit)
        self.inner_box.Add(self.qr_bitmap_widget, 0, wx.CENTER)

    def timer_show_next_qr_code(self, timer_event: wx.TimerEvent):
        self.qr_bitmap_widget.Destroy()
        self.qr_page_index = (self.qr_page_index + 1) % len(self.qr_pages)

        self.render_qr_code()

if __name__ == "__main__":
    app = wx.App()
    Window(None, title="PQ Merchant POS").Show()
    app.MainLoop()
