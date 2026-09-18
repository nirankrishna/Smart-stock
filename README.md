# Smart-stock
SmartStock – A console-based Inventory &amp; Billing Management System with inventory CRUD operations, atomic billing, invoicing, sales analytics, CSV persistence, custom exceptions, and automated testing.

# SmartStock — Inventory & Billing Management System

A command-line Java application built for the **Programming in Java** evaluated
project. SmartStock lets a small retail store manage its product catalogue,
generate invoices for sales, and view revenue / low-stock analytics — all
from a terminal, with no database server or GUI required.

## Overview

Retail shop owners often juggle stock counts, billing, and sales insight
across spreadsheets or notebooks, which quickly get out of sync. SmartStock
centralizes all three into one console application with simple, durable
file-based storage.

## Features

- **Inventory Management** — add, update, delete, and list products
  (ID, name, category, unit price, quantity), with automatic low-stock
  flagging (≤ 5 units).
- **Billing & Invoice Generation** — build a cart of product IDs and
  quantities, validate stock availability atomically, generate an invoice
  with a unique ID, and print a formatted receipt. Stock is deducted only
  after every line in the cart is confirmed valid.
- **Sales Reporting & Analytics** — total revenue, top-selling products,
  and live low-stock alerts, computed from the persisted sales ledger and
  current inventory.
- Custom checked exceptions (`InvalidInputException`,
  `ProductNotFoundException`, `InsufficientStockException`) for clear,
  recoverable error handling — the CLI never crashes on bad input.
- File-based persistence (CSV) under `data/` — no external database needed.
- Simple audit logging to `data/app.log`.
- A dependency-free, assertion-based test suite covering all three modules.

## Technologies / Tools Used

- Java 17+ (developed and tested on JDK 21)
- Core Java only: Collections Framework, `java.nio.file`, `java.time`,
  streams — **no external libraries or build tool required**
- Plain-text CSV files for persistence

## Project Structure

```
SmartStock/
├── README.md
├── statement.md
├── src/
│   └── com/smartstock/
│       ├── Main.java                        # CLI entry point / menu
│       ├── model/
│       │   ├── Product.java
│       │   ├── Invoice.java
│       │   └── InvoiceItem.java
│       ├── service/
│       │   ├── InventoryService.java        # Module 1
│       │   ├── BillingService.java          # Module 2
│       │   └── ReportService.java           # Module 3
│       ├── exception/
│       │   ├── InvalidInputException.java
│       │   ├── ProductNotFoundException.java
│       │   └── InsufficientStockException.java
│       ├── util/
│       │   ├── FileStorageUtil.java
│       │   └── AppLogger.java
│       └── test/
│           └── SmartStockTest.java          # assertion-based test suite
└── data/                                      # created automatically at runtime
    ├── products.csv
    ├── sales_ledger.csv
    └── app.log
```

## Steps to Install & Run

**Prerequisite:** a JDK (11 or newer) on your PATH. Verify with:

```bash
javac -version
java -version
```

**1. Clone the repository**

```bash
git clone https://github.com/{github-username}/{repo-name}.git
cd {repo-name}
```

**2. Compile**

```bash
find src -name "*.java" > sources.txt
javac -d out @sources.txt
```

**3. Run the application**

```bash
java -cp out com.smartstock.Main
```

On first run, SmartStock automatically seeds a sample catalogue of five
products so the menu isn't empty. All data is written under `data/` in the
current working directory and reloaded automatically on the next run.

**4. Using the menu**

```
============ SmartStock Menu ============
1. View all products
2. Add product
3. Update product
4. Delete product
5. Create invoice (sell products)
6. View sales & inventory report
0. Exit
```

Follow the on-screen prompts. For "Create invoice", enter a product ID and
quantity for each cart line, then press Enter on a blank product ID to
finish and generate the invoice.

## Instructions for Testing

The project ships with a dependency-free, assertion-based test suite (no
JUnit installation required) covering all three modules: product CRUD and
validation, invoice generation and stock deduction, insufficient-stock
rejection, revenue aggregation, and low-stock detection.

```bash
javac -d out @sources.txt
java -cp out com.smartstock.test.SmartStockTest
```

Expected output ends with a summary such as:

```
========================================
Tests passed: 8 | failed: 0
========================================
```

The test suite writes its own isolated data files under
`data/test-run/` so it never touches your real inventory or sales data.

## Screenshots

_See the attached project report (PDF) for menu walkthrough screenshots
and sample console output._
