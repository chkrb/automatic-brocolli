# Idea Title

Stock & Queue Management Platform for Indian Retailers

# Idea Description

The idea can be broadly categorized into:
1. Organizing customers in a queue, and effectively managing and satisfying
   their needs.
2. Automatically collecting sales records and enabling data analytics for the
   retailer.

The idea solution involves a simple server-client architecture of nodes. Every
retailer will have a server installed in their shop. This server is the single
source of truth, it stores the inventory state in the store, which includes
items in stock, amount/quantity of stock, selling price, etc.

The clients to this server are the point-of-sale terminals. These are custom
hardware devices developed under the scope of this project. These terminals are
customer-facing, they accept orders from the customer and convey it to the
merchant server. It also handles order payment and acknowledgement.

The customers use a mobile application to communicate with the POS terminal.
This is a stateless, half-duplex method of communication, with the use of QR
codes encoding a minimal in-house binary specification. The workflow is as
follows:
- The information about the stocked items is conveyed by the POS terminal using
QR code(s), which is/are scanned by the app.
- The app lets you select items, and generate a QR containing the order items
and quantities.
- The POS terminal scans the order QR and proceeds with payment, via UPI.
- On payment, the POS terminal conveys the order to the server, and displays
acknowledgement to the user.

The communication between the merchant local network and the customer app is
completely offline and stateless, bypassing network requirements entirely.

# Idea Template

_PDF format. Source code available [here](./template.typ)_

# Abstract/Summary

This project involves streamlining the procedure of Indian retailers,
especially in network-constrained regions and remote areas. The _"Kirana"_
retailer culture has an indigenous setup, which can easily get overwhelmed
during rush hours or on pre-festive days. This may lead to unanswered orders or
incorrect accounting, inviting dissatisfaction of customers.

Converting the completely offline and pen-and-paper based execution to a
computerised workflow is the aim of this project. Special hardware are to be
introduced in each store as point-of-sale terminals to streamline the queue.
Each store would also be equipped with a local network server to store sale
data and perform analytics and accounting. Development prioritizes an
offline-first and stateless model solution.

# Technology Bucket

IoT and Electronics
