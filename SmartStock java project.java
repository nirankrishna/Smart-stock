import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

class Product {

    private final String productId;
    private String name;
    private String category;
    private double unitPrice;
    private int quantityInStock;
    private static final int LOW_STOCK_THRESHOLD = 5;

    public Product(String productId, String name, String category,
                    double unitPrice, int quantityInStock) {
        this.productId = productId;
        this.name = name;
        this.category = category;
        this.unitPrice = unitPrice;
        this.quantityInStock = quantityInStock;
    }

    public String getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getQuantityInStock() {
        return quantityInStock;
    }

    public void setQuantityInStock(int quantityInStock) {
        this.quantityInStock = quantityInStock;
    }

    public boolean isLowStock() {
        return quantityInStock <= LOW_STOCK_THRESHOLD;
    }

    public String toCsvRow() {
        return String.join(",",
                escape(productId), escape(name), escape(category),
                String.valueOf(unitPrice), String.valueOf(quantityInStock));
    }

    public static Product fromCsvRow(String row) {
        String[] parts = row.split(",", -1);
        return new Product(
                unescape(parts[0]), unescape(parts[1]), unescape(parts[2]),
                Double.parseDouble(parts[3]), Integer.parseInt(parts[4]));
    }

    private static String escape(String value) {
        return value.replace(",", ";");
    }

    private static String unescape(String value) {
        return value.replace(";", ",");
    }

    @Override
    public String toString() {
        return String.format("%-8s %-20s %-12s Rs.%-10.2f Qty:%-5d%s",
                productId, name, category, unitPrice, quantityInStock,
                isLowStock() ? "  [LOW STOCK]" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return productId.equals(product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }
}

class InvoiceItem {

    private final String productId;
    private final String productName;
    private final int quantity;
    private final double unitPriceAtSale;

    public InvoiceItem(String productId, String productName,
                        int quantity, double unitPriceAtSale) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unitPriceAtSale = unitPriceAtSale;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPriceAtSale() {
        return unitPriceAtSale;
    }

    public double getLineTotal() {
        return quantity * unitPriceAtSale;
    }

    public String toCsvField() {

        return productId + "|" + quantity + "|" + unitPriceAtSale;
    }

    @Override
    public String toString() {
        return String.format("  %-20s x%-4d @ Rs.%-8.2f = Rs.%.2f",
                productName, quantity, unitPriceAtSale, getLineTotal());
    }
}

class Invoice {

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String invoiceId;
    private final String customerName;
    private final LocalDateTime timestamp;
    private final List<InvoiceItem> items = new ArrayList<>();

    public Invoice(String invoiceId, String customerName, LocalDateTime timestamp) {
        this.invoiceId = invoiceId;
        this.customerName = customerName;
        this.timestamp = timestamp;
    }

    public void addItem(InvoiceItem item) {
        items.add(item);
    }

    public String getInvoiceId() {
        return invoiceId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public List<InvoiceItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public double getGrandTotal() {
        return items.stream().mapToDouble(InvoiceItem::getLineTotal).sum();
    }

    public String toCsvRow() {
        StringBuilder itemsField = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) itemsField.append(";");
            itemsField.append(items.get(i).toCsvField());
        }
        return String.join(",",
                invoiceId, customerName.replace(",", " "),
                timestamp.format(TS_FORMAT), itemsField.toString());
    }

    public String toReceipt() {
        StringBuilder sb = new StringBuilder();
        sb.append("===================================================\n");
        sb.append(" INVOICE: ").append(invoiceId).append("\n");
        sb.append(" Customer : ").append(customerName).append("\n");
        sb.append(" Date     : ").append(timestamp.format(TS_FORMAT)).append("\n");
        sb.append("---------------------------------------------------\n");
        for (InvoiceItem item : items) {
            sb.append(item).append("\n");
        }
        sb.append("---------------------------------------------------\n");
        sb.append(String.format(" GRAND TOTAL: Rs.%.2f\n", getGrandTotal()));
        sb.append("===================================================\n");
        return sb.toString();
    }
}

class InvalidInputException extends Exception {

    public InvalidInputException(String message) {
        super(message);
    }
}

class ProductNotFoundException extends Exception {

    public ProductNotFoundException(String productId) {
        super("No product found with ID: " + productId);
    }
}

class InsufficientStockException extends Exception {

    public InsufficientStockException(String productId, int requested, int available) {
        super(String.format(
                "Insufficient stock for product '%s': requested %d, only %d available.",
                productId, requested, available));
    }
}

final class FileStorageUtil {

    private FileStorageUtil() {

    }

    public static List<String> readLines(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String line : Files.readAllLines(path)) {
            if (!line.isBlank()) {
                result.add(line);
            }
        }
        return result;
    }

    public static void writeLines(String filePath, List<String> lines) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath, false))) {
            for (String line : lines) {
                out.println(line);
            }
        }
    }

    public static void appendLine(String filePath, String line) throws IOException {
        Path path = Paths.get(filePath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (PrintWriter out = new PrintWriter(new FileWriter(filePath, true))) {
            out.println(line);
        }
    }
}

class AppLogger {

    private static final DateTimeFormatter TS_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String logFilePath;

    public AppLogger(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    public void info(String message) {
        write("INFO", message);
    }

    public void warn(String message) {
        write("WARN", message);
    }

    public void error(String message) {
        write("ERROR", message);
    }

    private synchronized void write(String level, String message) {
        String line = String.format("[%s] %-5s %s",
                LocalDateTime.now().format(TS_FORMAT), level, message);

        System.out.println(line);
        try {
            Path path = Paths.get(logFilePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (PrintWriter out = new PrintWriter(new FileWriter(logFilePath, true))) {
                out.println(line);
            }
        } catch (IOException e) {
            System.err.println("Logging failure (continuing without disk log): " + e.getMessage());
        }
    }
}

class InventoryService {

    private final Map<String, Product> products = new LinkedHashMap<>();
    private final String dataFile;
    private final AppLogger logger;

    public InventoryService(String dataFile, AppLogger logger) {
        this.dataFile = dataFile;
        this.logger = logger;
        loadFromDisk();
    }

    private void loadFromDisk() {
        try {
            List<String> rows = FileStorageUtil.readLines(dataFile);
            for (String row : rows) {
                Product p = Product.fromCsvRow(row);
                products.put(p.getProductId(), p);
            }
            logger.info("Inventory loaded: " + products.size() + " product(s) from " + dataFile);
        } catch (IOException e) {
            logger.error("Could not load inventory file (starting empty): " + e.getMessage());
        }
    }

    private void persist() {
        try {
            List<String> rows = new ArrayList<>();
            for (Product p : products.values()) {
                rows.add(p.toCsvRow());
            }
            FileStorageUtil.writeLines(dataFile, rows);
        } catch (IOException e) {
            logger.error("Failed to persist inventory: " + e.getMessage());
        }
    }

    public Product addProduct(String id, String name, String category,
                               double price, int qty) throws InvalidInputException {
        validate(id, name, price, qty);
        if (products.containsKey(id)) {
            throw new InvalidInputException("Product ID '" + id + "' already exists. Use update instead.");
        }
        Product product = new Product(id, name, category, price, qty);
        products.put(id, product);
        persist();
        logger.info("Added product " + id + " (" + name + ")");
        return product;
    }

    public Product updateProduct(String id, String name, String category,
                                  Double price, Integer qty) throws ProductNotFoundException, InvalidInputException {
        Product existing = getProduct(id);
        if (name != null && !name.isBlank()) existing.setName(name);
        if (category != null && !category.isBlank()) existing.setCategory(category);
        if (price != null) {
            if (price < 0) throw new InvalidInputException("Price cannot be negative.");
            existing.setUnitPrice(price);
        }
        if (qty != null) {
            if (qty < 0) throw new InvalidInputException("Quantity cannot be negative.");
            existing.setQuantityInStock(qty);
        }
        persist();
        logger.info("Updated product " + id);
        return existing;
    }

    public void deleteProduct(String id) throws ProductNotFoundException {
        if (!products.containsKey(id)) {
            throw new ProductNotFoundException(id);
        }
        products.remove(id);
        persist();
        logger.info("Deleted product " + id);
    }

    public Product getProduct(String id) throws ProductNotFoundException {
        Product p = products.get(id);
        if (p == null) {
            throw new ProductNotFoundException(id);
        }
        return p;
    }

    public List<Product> getAllProducts() {
        return new ArrayList<>(products.values());
    }

    public List<Product> getLowStockProducts() {
        List<Product> low = new ArrayList<>();
        for (Product p : products.values()) {
            if (p.isLowStock()) low.add(p);
        }
        return low;
    }

    void reduceStock(String id, int quantity) throws ProductNotFoundException {
        Product p = getProduct(id);
        p.setQuantityInStock(p.getQuantityInStock() - quantity);
        persist();
    }

    Product lookup(String id) throws ProductNotFoundException {
        return getProduct(id);
    }

    private void validate(String id, String name, double price, int qty) throws InvalidInputException {
        if (id == null || id.isBlank()) throw new InvalidInputException("Product ID cannot be blank.");
        if (name == null || name.isBlank()) throw new InvalidInputException("Product name cannot be blank.");
        if (price < 0) throw new InvalidInputException("Price cannot be negative.");
        if (qty < 0) throw new InvalidInputException("Quantity cannot be negative.");
    }
}

class BillingService {

    private final InventoryService inventoryService;
    private final String salesLedgerFile;
    private final AppLogger logger;
    private final AtomicInteger invoiceSequence = new AtomicInteger(1000);

    public BillingService(InventoryService inventoryService, String salesLedgerFile, AppLogger logger) {
        this.inventoryService = inventoryService;
        this.salesLedgerFile = salesLedgerFile;
        this.logger = logger;
    }

    public Invoice generateInvoice(String customerName, Map<String, Integer> cart)
            throws InsufficientStockException, ProductNotFoundException, InvalidInputException {

        if (customerName == null || customerName.isBlank()) {
            throw new InvalidInputException("Customer name cannot be blank.");
        }
        if (cart == null || cart.isEmpty()) {
            throw new InvalidInputException("Cart cannot be empty.");
        }

        Map<String, Product> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            String productId = entry.getKey();
            int qty = entry.getValue();
            if (qty <= 0) {
                throw new InvalidInputException("Quantity for '" + productId + "' must be positive.");
            }
            Product product = inventoryService.lookup(productId);
            if (product.getQuantityInStock() < qty) {
                throw new InsufficientStockException(productId, qty, product.getQuantityInStock());
            }
            resolved.put(productId, product);
        }

        String invoiceId = "INV" + invoiceSequence.getAndIncrement();
        Invoice invoice = new Invoice(invoiceId, customerName, LocalDateTime.now());
        for (Map.Entry<String, Integer> entry : cart.entrySet()) {
            Product product = resolved.get(entry.getKey());
            int qty = entry.getValue();
            invoice.addItem(new InvoiceItem(product.getProductId(), product.getName(),
                    qty, product.getUnitPrice()));
            inventoryService.reduceStock(product.getProductId(), qty);
        }

        persistToLedger(invoice);
        logger.info("Generated " + invoiceId + " for " + customerName
                + " | total Rs." + String.format("%.2f", invoice.getGrandTotal()));
        return invoice;
    }

    private void persistToLedger(Invoice invoice) {
        try {
            FileStorageUtil.appendLine(salesLedgerFile, invoice.toCsvRow());
        } catch (IOException e) {
            logger.error("Failed to write invoice " + invoice.getInvoiceId() + " to sales ledger: " + e.getMessage());
        }
    }
}

class ReportService {

    private final InventoryService inventoryService;
    private final String salesLedgerFile;
    private final AppLogger logger;

    public ReportService(InventoryService inventoryService, String salesLedgerFile, AppLogger logger) {
        this.inventoryService = inventoryService;
        this.salesLedgerFile = salesLedgerFile;
        this.logger = logger;
    }

    private static class SoldLine {
        String productId;
        int quantity;
        double unitPrice;
    }

    private List<SoldLine> readLedger() {
        List<SoldLine> lines = new ArrayList<>();
        try {
            for (String row : FileStorageUtil.readLines(salesLedgerFile)) {

                String[] cols = row.split(",", 4);
                if (cols.length < 4 || cols[3].isBlank()) continue;
                for (String itemField : cols[3].split(";")) {
                    String[] parts = itemField.split("\\|");
                    if (parts.length != 3) continue;
                    SoldLine sl = new SoldLine();
                    sl.productId = parts[0];
                    sl.quantity = Integer.parseInt(parts[1]);
                    sl.unitPrice = Double.parseDouble(parts[2]);
                    lines.add(sl);
                }
            }
        } catch (IOException e) {
            logger.error("Failed to read sales ledger: " + e.getMessage());
        }
        return lines;
    }

    public double getTotalRevenue() {
        double total = 0;
        for (SoldLine sl : readLedger()) {
            total += sl.quantity * sl.unitPrice;
        }
        return total;
    }

    public Map<String, Integer> getTopSellingProducts(int limit) {
        Map<String, Integer> unitsSold = new LinkedHashMap<>();
        for (SoldLine sl : readLedger()) {
            unitsSold.merge(sl.productId, sl.quantity, Integer::sum);
        }
        Map<String, Integer> sorted = new LinkedHashMap<>();
        unitsSold.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(limit)
                .forEach(e -> sorted.put(e.getKey(), e.getValue()));
        return sorted;
    }

    public List<Product> getLowStockAlerts() {
        return inventoryService.getLowStockProducts();
    }

    public String buildSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("========== SALES & INVENTORY REPORT ==========\n");
        sb.append(String.format("Total Revenue: Rs.%.2f%n", getTotalRevenue()));
        sb.append(String.format("Products in catalogue: %d%n", inventoryService.getAllProducts().size()));

        sb.append("\n-- Top Selling Products --\n");
        Map<String, Integer> top = getTopSellingProducts(5);
        if (top.isEmpty()) {
            sb.append("  (no sales recorded yet)\n");
        } else {
            for (Map.Entry<String, Integer> e : top.entrySet()) {
                sb.append(String.format("  %-10s %d unit(s) sold%n", e.getKey(), e.getValue()));
            }
        }

        sb.append("\n-- Low Stock Alerts (<= 5 units) --\n");
        List<Product> low = getLowStockAlerts();
        if (low.isEmpty()) {
            sb.append("  (none - all products adequately stocked)\n");
        } else {
            for (Product p : low) {
                sb.append("  ").append(p).append("\n");
            }
        }
        sb.append("================================================\n");
        return sb.toString();
    }
}

public class SmartStock {

    private static final String DATA_DIR = "data";
    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        AppLogger logger = new AppLogger(DATA_DIR + "/app.log");
        InventoryService inventoryService = new InventoryService(DATA_DIR + "/products.csv", logger);
        BillingService billingService = new BillingService(inventoryService, DATA_DIR + "/sales_ledger.csv", logger);
        ReportService reportService = new ReportService(inventoryService, DATA_DIR + "/sales_ledger.csv", logger);

        seedSampleDataIfEmpty(inventoryService, logger);

        boolean running = true;
        while (running) {
            printMenu();
            String choice = SCANNER.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> viewProducts(inventoryService);
                    case "2" -> addProduct(inventoryService);
                    case "3" -> updateProduct(inventoryService);
                    case "4" -> deleteProduct(inventoryService);
                    case "5" -> createInvoice(billingService);
                    case "6" -> System.out.println(reportService.buildSummaryReport());
                    case "0" -> {
                        running = false;
                        System.out.println("Goodbye!");
                    }
                    default -> System.out.println("Invalid option. Please choose a number from the menu.");
                }
            } catch (InvalidInputException | ProductNotFoundException | InsufficientStockException e) {

                logger.warn(e.getMessage());
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {

                logger.error("Unexpected error: " + e.getMessage());
                System.out.println("Something went wrong: " + e.getMessage());
            }
        }
        SCANNER.close();
    }

    private static void printMenu() {
        System.out.println("\n============ SmartStock Menu ============");
        System.out.println("1. View all products");
        System.out.println("2. Add product");
        System.out.println("3. Update product");
        System.out.println("4. Delete product");
        System.out.println("5. Create invoice (sell products)");
        System.out.println("6. View sales & inventory report");
        System.out.println("0. Exit");
        System.out.print("Choose an option: ");
    }

    private static void viewProducts(InventoryService inventoryService) {
        List<Product> products = inventoryService.getAllProducts();
        if (products.isEmpty()) {
            System.out.println("No products in inventory yet.");
            return;
        }
        System.out.println("\n-- Current Inventory --");
        for (Product p : products) {
            System.out.println(p);
        }
    }

    private static void addProduct(InventoryService inventoryService) throws InvalidInputException {
        System.out.print("Product ID: ");
        String id = SCANNER.nextLine().trim();
        System.out.print("Name: ");
        String name = SCANNER.nextLine().trim();
        System.out.print("Category: ");
        String category = SCANNER.nextLine().trim();
        double price = readDouble("Unit price: ");
        int qty = readInt("Quantity in stock: ");
        Product p = inventoryService.addProduct(id, name, category, price, qty);
        System.out.println("Added: " + p);
    }

    private static void updateProduct(InventoryService inventoryService)
            throws ProductNotFoundException, InvalidInputException {
        System.out.print("Product ID to update: ");
        String id = SCANNER.nextLine().trim();
        System.out.print("New name (blank = keep current): ");
        String name = SCANNER.nextLine().trim();
        System.out.print("New category (blank = keep current): ");
        String category = SCANNER.nextLine().trim();
        System.out.print("New price (blank = keep current): ");
        String priceStr = SCANNER.nextLine().trim();
        System.out.print("New quantity (blank = keep current): ");
        String qtyStr = SCANNER.nextLine().trim();

        Double price = priceStr.isBlank() ? null : Double.parseDouble(priceStr);
        Integer qty = qtyStr.isBlank() ? null : Integer.parseInt(qtyStr);
        Product p = inventoryService.updateProduct(id, name, category, price, qty);
        System.out.println("Updated: " + p);
    }

    private static void deleteProduct(InventoryService inventoryService) throws ProductNotFoundException {
        System.out.print("Product ID to delete: ");
        String id = SCANNER.nextLine().trim();
        inventoryService.deleteProduct(id);
        System.out.println("Deleted product " + id);
    }

    private static void createInvoice(BillingService billingService)
            throws InvalidInputException, ProductNotFoundException, InsufficientStockException {
        System.out.print("Customer name: ");
        String customer = SCANNER.nextLine().trim();
        Map<String, Integer> cart = new LinkedHashMap<>();
        System.out.println("Enter product ID and quantity (blank product ID to finish):");
        while (true) {
            System.out.print("  Product ID: ");
            String id = SCANNER.nextLine().trim();
            if (id.isBlank()) break;
            int qty = readInt("  Quantity: ");
            cart.put(id, qty);
        }
        Invoice invoice = billingService.generateInvoice(customer, cart);
        System.out.println(invoice.toReceipt());
    }

    private static void seedSampleDataIfEmpty(InventoryService inventoryService, AppLogger logger) {
        if (!inventoryService.getAllProducts().isEmpty()) {
            return;
        }
        try {
            inventoryService.addProduct("P001", "Wireless Mouse", "Electronics", 499.00, 40);
            inventoryService.addProduct("P002", "Mechanical Keyboard", "Electronics", 2499.00, 15);
            inventoryService.addProduct("P003", "Notebook (A5)", "Stationery", 60.00, 200);
            inventoryService.addProduct("P004", "USB-C Cable 1m", "Electronics", 249.00, 4);
            inventoryService.addProduct("P005", "Desk Lamp", "Home", 899.00, 8);
            logger.info("Seeded sample product catalogue for first run.");
        } catch (InvalidInputException e) {
            logger.error("Failed to seed sample data: " + e.getMessage());
        }
    }

    private static double readDouble(String prompt) throws InvalidInputException {
        System.out.print(prompt);
        String raw = SCANNER.nextLine().trim();
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("'" + raw + "' is not a valid number.");
        }
    }

    private static int readInt(String prompt) throws InvalidInputException {
        System.out.print(prompt);
        String raw = SCANNER.nextLine().trim();
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            throw new InvalidInputException("'" + raw + "' is not a valid whole number.");
        }
    }
}

class SmartStockTest {

    private static int passed = 0;
    private static int failed = 0;
    private static final String TEST_DIR = "data/test-run";

    public static void main(String[] args) throws Exception {
        cleanTestDir();
        AppLogger logger = new AppLogger(TEST_DIR + "/test.log");

        testAddAndRetrieveProduct(logger);
        testDuplicateProductRejected(logger);
        testNegativePriceRejected(logger);
        testProductNotFoundOnMissingId(logger);
        testInvoiceReducesStock(logger);
        testInsufficientStockRejected(logger);
        testReportRevenueAggregation(logger);
        testLowStockDetection(logger);

        System.out.println("\n========================================");
        System.out.println("Tests passed: " + passed + " | failed: " + failed);
        System.out.println("========================================");
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testAddAndRetrieveProduct(AppLogger logger) {
        try {
            InventoryService inv = new InventoryService(TEST_DIR + "/t1_products.csv", logger);
            inv.addProduct("T1", "Test Widget", "Test", 10.0, 5);
            Product p = inv.getProduct("T1");
            check("addAndRetrieveProduct", p.getName().equals("Test Widget") && p.getQuantityInStock() == 5);
        } catch (Exception e) {
            fail("addAndRetrieveProduct", e);
        }
    }

    private static void testDuplicateProductRejected(AppLogger logger) {
        try {
            InventoryService inv = new InventoryService(TEST_DIR + "/t2_products.csv", logger);
            inv.addProduct("T2", "Widget A", "Test", 10.0, 5);
            try {
                inv.addProduct("T2", "Widget B", "Test", 12.0, 3);
                fail("duplicateProductRejected", null);
            } catch (InvalidInputException expected) {
                pass("duplicateProductRejected");
            }
        } catch (Exception e) {
            fail("duplicateProductRejected", e);
        }
    }

    private static void testNegativePriceRejected(AppLogger logger) {
        InventoryService inv = new InventoryService(TEST_DIR + "/t3_products.csv", logger);
        try {
            inv.addProduct("T3", "Bad Widget", "Test", -5.0, 5);
            fail("negativePriceRejected", null);
        } catch (InvalidInputException expected) {
            pass("negativePriceRejected");
        } catch (Exception e) {
            fail("negativePriceRejected", e);
        }
    }

    private static void testProductNotFoundOnMissingId(AppLogger logger) {
        InventoryService inv = new InventoryService(TEST_DIR + "/t4_products.csv", logger);
        try {
            inv.getProduct("DOES_NOT_EXIST");
            fail("productNotFoundOnMissingId", null);
        } catch (ProductNotFoundException expected) {
            pass("productNotFoundOnMissingId");
        }
    }

    private static void testInvoiceReducesStock(AppLogger logger) {
        try {
            InventoryService inv = new InventoryService(TEST_DIR + "/t5_products.csv", logger);
            inv.addProduct("T5", "Stock Widget", "Test", 20.0, 10);
            BillingService billing = new BillingService(inv, TEST_DIR + "/t5_sales.csv", logger);

            Map<String, Integer> cart = new LinkedHashMap<>();
            cart.put("T5", 3);
            Invoice invoice = billing.generateInvoice("Alice", cart);

            Product after = inv.getProduct("T5");
            check("invoiceReducesStock",
                    after.getQuantityInStock() == 7 && invoice.getGrandTotal() == 60.0);
        } catch (Exception e) {
            fail("invoiceReducesStock", e);
        }
    }

    private static void testInsufficientStockRejected(AppLogger logger) {
        try {
            InventoryService inv = new InventoryService(TEST_DIR + "/t6_products.csv", logger);
            inv.addProduct("T6", "Scarce Widget", "Test", 5.0, 2);
            BillingService billing = new BillingService(inv, TEST_DIR + "/t6_sales.csv", logger);

            Map<String, Integer> cart = new LinkedHashMap<>();
            cart.put("T6", 5);
            try {
                billing.generateInvoice("Bob", cart);
                fail("insufficientStockRejected", null);
            } catch (InsufficientStockException expected) {

                Product unchanged = inv.getProduct("T6");
                check("insufficientStockRejected", unchanged.getQuantityInStock() == 2);
            }
        } catch (Exception e) {
            fail("insufficientStockRejected", e);
        }
    }

    private static void testReportRevenueAggregation(AppLogger logger) {
        try {
            InventoryService inv = new InventoryService(TEST_DIR + "/t7_products.csv", logger);
            inv.addProduct("T7", "Report Widget", "Test", 100.0, 10);
            BillingService billing = new BillingService(inv, TEST_DIR + "/t7_sales.csv", logger);
            ReportService report = new ReportService(inv, TEST_DIR + "/t7_sales.csv", logger);

            Map<String, Integer> cart1 = new LinkedHashMap<>();
            cart1.put("T7", 2);
            billing.generateInvoice("Carol", cart1);

            Map<String, Integer> cart2 = new LinkedHashMap<>();
            cart2.put("T7", 3);
            billing.generateInvoice("Dave", cart2);

            check("reportRevenueAggregation", report.getTotalRevenue() == 500.0);
        } catch (Exception e) {
            fail("reportRevenueAggregation", e);
        }
    }

    private static void testLowStockDetection(AppLogger logger) {
        try {
            InventoryService inv = new InventoryService(TEST_DIR + "/t8_products.csv", logger);
            inv.addProduct("T8A", "Plentiful Widget", "Test", 10.0, 50);
            inv.addProduct("T8B", "Scarce Widget", "Test", 10.0, 3);
            ReportService report = new ReportService(inv, TEST_DIR + "/t8_sales.csv", logger);

            boolean containsScarce = report.getLowStockAlerts().stream()
                    .anyMatch(p -> p.getProductId().equals("T8B"));
            boolean excludesPlentiful = report.getLowStockAlerts().stream()
                    .noneMatch(p -> p.getProductId().equals("T8A"));
            check("lowStockDetection", containsScarce && excludesPlentiful);
        } catch (Exception e) {
            fail("lowStockDetection", e);
        }
    }

    private static void cleanTestDir() {
        File dir = new File(TEST_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        }
    }

    private static void check(String name, boolean condition) {
        if (condition) pass(name); else fail(name, null);
    }

    private static void pass(String name) {
        passed++;
        System.out.println("[PASS] " + name);
    }

    private static void fail(String name, Exception e) {
        failed++;
        System.out.println("[FAIL] " + name + (e != null ? " - " + e.getMessage() : ""));
    }
}

