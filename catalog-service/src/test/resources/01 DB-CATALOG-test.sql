-- Drop tables if they exist to apply the clean new schema (Warning: Clears data)
DROP TABLE IF EXISTS inventory;
DROP TABLE IF EXISTS products;
DROP TABLE IF EXISTS suppliers;


-- Create Suppliers Table
CREATE TABLE suppliers (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    tax_id VARCHAR(20) NOT NULL UNIQUE, -- Government Tax ID
    contact_email VARCHAR(100),
    phone_number VARCHAR(15),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create Products Table (Now linking to Supplier)
CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    price DECIMAL(10, 2) NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE,  -- Stock Keeping Unit
    supplier_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);

-- Create Inventory Table (Separated for advanced tracking)
CREATE TABLE inventory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL UNIQUE, -- UNIQUE guarantees a 1:1 relationship here
    quantity INT NOT NULL DEFAULT 0,
    warehouse_location VARCHAR(100), -- E.g., 'Aisle 3, Shelf B'
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- 1. Populating Suppliers (ID 1 and 2 will be generated automatically)
INSERT INTO suppliers (name, tax_id, contact_email) VALUES
('Tech Components Ltd', '12.345.678/0001-99', 'sales@techcomponents.com'),
('Global Apparel Corp', '98.765.432/0001-11', 'orders@globalapparel.com'),
('Home & Comfort Data', '55.444.333/0001-22', 'b2b@homecomfort.com'),
('Sports & Velocity', '44.333.222/0001-11', 'supply@velocity.com'),
('Books & Media Dist', '33.222.111/0001-00', 'contact@booksdist.com');

-- 2. Populating Products (Linking to the suppliers created above)
-- Assumes Supplier 1 supplies tech, Supplier 2 supplies clothing
INSERT INTO products (name, description, price, sku, supplier_id) VALUES
('Mechanical Keyboard RGB', 'Wireless mechanical keyboard with brown switches', 89.99, 'TECH-KEYB-RGB-BR', 1),
('Gaming Mouse 16000 DPI', 'Ergonomic gaming mouse with programmable buttons', 45.50, 'TECH-MOUSE-16K-ERG', 1),
('Premium Cotton T-Shirt Black M', '100% organic cotton minimalist t-shirt', 29.90, 'APPA-TSHIRT-BLK-M', 2);


INSERT INTO inventory (product_id, quantity, warehouse_location) VALUES
(1, 50, 'Aisle A, Shelf 3'),
(2, 120, 'Aisle A, Shelf 4'),
(3, 200, 'Aisle C, Shelf 1');