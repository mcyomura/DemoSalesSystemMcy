# Introduction
## Goal
I started my career as a developer and have spent the past 15 years working as an architect (Enterprise, Business, IT, and Solution Architect). As I plan my return to hands-on development—driven by the autonomy of the role and the flexibility to collaborate across different time zones — I refreshed my technical skills through specialized courses and built this demonstration project to put those concepts into practice.

By combining a strong architectural background with hands-on engineering, I bring a holistic view to software development, truly understanding the complete ecosystem.

This is a **portfolio program** designed to showcase proficiency in modern backend development and distributed systems using Java 21, Spring Boot 4, JPA, Kafka, Feign, MapStruct, OpenAPI/Swagger, and Docker.

The backend Sales System consists of 5 microservices and 2 infrastructure containers configured for demonstration. To showcase distributed transactions with **Kafka**, I intentionally designed topics with multiple subscribers to implement a **choreographed SAGA pattern**. Building this provided invaluable hands-on experience in handling event-driven choreography and compensation logic.

# About the development
## C4 - Level 1 (Context)
A high-level diagram illustrating the core Sales System and its external integrations.

![C4 Level 1](Documentation/C4_Sales_System-C4-L1.drawio.png)
## C4 - Level 2 (Containers)
Shows the microservices, databases, and messaging topics. The scope of this demo is filled in blue.

![C4 Level 2](Documentation/C4_Sales_System-C4-L2.drawio.png)

## Design patterns
To demonstrate versatility, **the microservices implement different architectural patterns**, including standard **MVC**, **Clean** Architecture, and **Hexagonal Architecture** (documented in C4 Level 2).

## Other Documentation Artifacts
* **Sequence diagrams**: [Sequence diagram](Documentation/C4_Sales_System-Sequence_diagram.drawio.png) shows component interactions across key flows.
* **Entity relationship diagrams (ERDs)**: [db-orders](Documentation/C4_Sales_System-db-orders.drawio.png), [db-catalog](Documentation/C4_Sales_System-db-catalog.drawio.png), [db-payment](Documentation/C4_Sales_System-db-payment.drawio.png), [db-customer-auth](Documentation/C4_Sales_System-db-customer-auth.drawio.png) (useful when connecting directly to databases via DBeaver or similar tools).
* **State machine**:  [State machine diagram](Documentation/C4_Sales_System-State_machine.drawio.png) shows cart and SAGA statuses.
* The swaggers - While only the BFF is intended for external exposure through an API Gateway in production, specs are provided for all services:
	* [bff](Documentation/swagger_bff.json) or at [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_bff.json)  
 	* [order](Documentation/swagger_order-service.json) or at [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_order-service.json)  
  	* [catalog](Documentation/swagger_catalog-service.json) or at [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_catalog-service.json)
  	* [customer-auth](Documentation/swagger_customer-auth-service.json) or at [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_customer-auth-service.json) 

# Services overview
## Microservices:
* **sales-web-bff (8085):** The only microservice intended to expose its endpoints externally via an API Gateway.
* **order-service (8082):** Manages the cart lifecycle (adding items, calculating totals, checking for stale prices, and processing checkout).
* **catalog-service (8081):** Owns product catalog and inventory/stock management.
* **payment-service(8083):** Mocks integration with a payment provider. It receives a payment token (typically generated client-side via a provider SDK), confirms transactions, and issues refunds when necessary. Note: In this demo, passing a payment token ending in 99 simulates a declined payment.
* **customer-auth-service (8084):** Handles authentication with an Identity Provider (IdP). For this demo, it **integrates with GitHub OAuth 2.0** to eliminate custom infra overhead and leverage existing developer accounts.

## Infrastructure containers:
* **kafka:** Message broker handling distributed event streaming.
* **MariaDB:** I started the local project with MySql, but migrated to MariaDB (lighter) in containers. Although a single database container is used, database schemas remain fully isolated using distinct database users (e.g., catalog-service connects strictly via appCatalogService user credentials).

## SAGA choreography:
Upon cart checkout, the workflow executes as follows:
* **Order-service publishes** an **ORDER-PLACED event** to the order_events topic.
* **Catalog-service consumes** the **ORDER-PLACED event** and deducts stock itens. It then sends a **SUCCESS event** on inventory_processed topic or a **FAILED event** if stock is insufficient for any product.
* **Payment-service** at the same time, **consumes ORDER-PLACED event** and "process" (mocked) the payment. It then emits a **SUCCESS event** on payment_processed topic or a **FAILED event** if payment fails.
* **Order-service listens for responses from both inventory_processed and payment_processed**:
	* If **both events were SUCCESS**, it then changes the **order status to APPROVED**.
   	* If payment/inventory published a FAILED event, then the order status changes to **CANCELED** and an **STOCK_DECLINED/PAYMENT_DECLINED event** or both is sent.
* Compensation handling:
	* If a **STOCK_DECLINED event** were raised, then payment-service will process a **refund** and publishes back (inventory-processed topic) a **REFUNDED** event to update order-service record.
	* If a **PAYMENT_DECLINED event** were raised, then catalog-service will process the **stock return** and publishes back (payment-processed topic) a **RETURNED** event to update order-service record.

Technical SAGA Considerations:
* **Optimistic Locking (@Version)**: To handle concurrent updates when both inventory and payment events arrive simultaneously, @Version is used. If two events read the state concurrently, the first commit succeeds while the second fails with an optimistic locking exception. **kafkaErrorConfig** intercepts this failure and retries, allowing the second event to succeed on its next attempt against the newly updated state.
* Event Ordering: To prevent cancellation or compensation events from executing BEFORE an ORDER-PLACED event, **all related messages** publish to the order_events topic **using the Order UUID as the partition key**. This guarantees strict, sequential processing per order.
    
## Usage of AI
AI is undeniably here to stay. However, since my primary goal with this project was to solidify my understanding of core concepts, I initially limited AI usage to clarifying doubts, brainstorming, and guiding the initial setup. Toward the end—specifically for automated testing and the final microservice—I experimented with AI-assisted coding. While it significantly boosts productivity, I concluded that developers must deeply understand the underlying code to effectively evaluate, correct, and steer AI-generated outputs.

## Dev vs Hom vs Prod environment
Dedicated dev and prod profiles (application.properties) were created to demonstrate configuration management best practices:
* Swagger/OpenAPI: Exposed exclusively under the dev profile.
* Price Expiration: Price staleness threshold is set to 10 minutes in dev versus 6 hours in prod
* Infrastructure Governance: In a real-world production setup, Kafka topics would be managed via Infrastructure as Code (IaC) rather than auto-creation. Infrastructure URLs, servers, and networks would also be strictly segmented across environments.
* Secrets Management & Key Rotation: In customer-auth-service, RSA key pairs would be securely managed using a vault. Key rotation (currently triggered via a local CRON) should be decoupled into an isolated scheduler (e.g., Control-M or a dedicated lightweight trigger service) to prevent race conditions when running multiple instances/pods.

# Running the demo
## Instructions to run
* Download the zip (from release v1.0.6 on you must have a Github login and make some configuration in order to run some operations, download v1.0.5 if you want to avoid this)
* I would build one service at a time:
	* docker compose build bff-service
	* docker compose build order-service
	* docker compose build catalog-service
	* docker compose build payment-service
	* docker compose build customer-auth-service
* Run the containers: docker compose up -d
* Wait 2-5 minutes for the services go up
* Use postman to send the requests (see examples below)

## Postman examples (usage)
### Get paged list of products (default page = 0, size = 10):
Method: GET  
URL: http://localhost:8085/api/v1/salesbff/products (default) or
http://localhost:8085/api/v1/salesbff/products?page=1&size=5 (paged)  
Response:  
```json
{
    "content": [
        {
            "description": "Touch control lamp with 3 intensity levels",
            "id": 6,
            "name": "LED Desk Lamp",
            "price": 39.90,
            "quantityInStock": 85,
            "sku": "HOME-LAMP-LED-02",
            "supplierName": "Home & Comfort Data"
        },
        {
            "description": "L-shaped desk made of MDF wood",
            "id": 7,
            "name": "L-Shaped Office Desk",
            "price": 349.00,
            "quantityInStock": 15,
            "sku": "HOME-DESK-L-03",
            "supplierName": "Home & Comfort Data"
        },
        {
            "description": "Pack of 10 silicone cable organizers",
            "id": 8,
            "name": "Cable Organizer Kit",
            "price": 15.00,
            "quantityInStock": 200,
            "sku": "HOME-CABLE-ORG-04",
            "supplierName": "Home & Comfort Data"
        },
        {
            "description": "Orthopedic seat cushion for office chairs",
            "id": 9,
            "name": "Memory Foam Cushion",
            "price": 79.90,
            "quantityInStock": 45,
            "sku": "HOME-CUSH-VIS-05",
            "supplierName": "Home & Comfort Data"
        },
        {
            "description": "Articulated monitor arm with desk clamp",
            "id": 10,
            "name": "Dual Monitor Mount",
            "price": 189.90,
            "quantityInStock": 40,
            "sku": "HOME-MONI-SUP-06",
            "supplierName": "Home & Comfort Data"
        }
    ],
    "empty": false,
    "first": false,
    "last": false,
    "number": 1,
    "numberOfElements": 5,
    "pageable": {
        "offset": 5,
        "pageNumber": 1,
        "pageSize": 5,
        "paged": true,
        "sort": {
            "empty": true,
            "sorted": false,
            "unsorted": true
        },
        "unpaged": false
    },
    "size": 5,
    "sort": {
        "empty": true,
        "sorted": false,
        "unsorted": true
    },
    "totalElements": 52,
    "totalPages": 11
}
```

### Get product details (valid products 1 to 52):
Method: GET  
URL: http://localhost:8085/api/v1/salesbff/products/1  
Response:  
```json
{
    "description": "Wireless mechanical keyboard with brown switches",
    "id": 1,
    "name": "Mechanical Keyboard RGB",
    "price": 80.00,
    "quantityInStock": 50,
    "sku": "TECH-KEYB-RGB-BR",
    "supplierName": "Tech Components Ltd"
}
```

### Add item to empty cart
Method: POST  
URL: http://localhost:8085/api/v1/salesbff/cart/items  
Request: Select: Body - raw - JSON format and post:
```json
{
   "productId": 1,
   "quantity": 2
}
```
Response:
```json
{
    "customerId": null,
    "items": [
        {
            "productId": 1,
            "quantity": 2,
            "unitaryPriceAtCart": 89.99
        }
    ],
    "pricesUpdated": false,
    "status": "DRAFT",
    "totalAmount": 179.98,
    "uuid": "e02107ef-61d0-41db-a402-406a9375c345"
}
```

### Add item to existing cart
Method: POST  
URL: http://localhost:8085/api/v1/salesbff/cart/items  
Request: Select: Body - raw - JSON format and post using the **"uuid" returned on previews call**  
```json
{
   "productId": 3,
   "quantity": 1,
    "uuid": "e02107ef-61d0-41db-a402-406a9375c345"
}
```
Response:  
```json
{
    "customerId": null,
    "items": [
        {
            "productId": 1,
            "quantity": 2,
            "unitaryPriceAtCart": 89.99
        },
        {
            "productId": 3,
            "quantity": 1,
            "unitaryPriceAtCart": 29.90
        }
    ],
    "pricesUpdated": false,
    "status": "DRAFT",
    "totalAmount": 209.88,
    "uuid": "e02107ef-61d0-41db-a402-406a9375c345"
}
```

### Proceed to cart checkout
Method: POST  
URL: http://localhost:8085/api/v1/salesbff/cart/checkout  
Request: Select: Body - raw - JSON format and post using the **"uuid" returned on previews call**. If you want a declined payment, make the paymentToken ending with "99"  
Post:  
```json
{
   "uuid":  "e02107ef-61d0-41db-a402-406a9375c345",
   "customerId": "82732917",
   "paymentToken": "fioej2",
   "bearerToken": "fjioejf"
}
```
Response: 
```json
{
    "id": 1,
    "customerId": 82732917,
    "status": "PENDING",
    "inventory_status": "PENDING",
    "payment_status": "PENDING",
    "totalAmount": 59.80
}
```


### Check cart status (use as parameter the **"id" returned during checkout**)
Method: GET  
URL: http://localhost:8085/api/v1/salesbff/cart/2  
Response:   
```json
{
    "id": 1,
    "customerId": 82732917,
    "status": "APPROVED",
    "inventory_status": "SUCCESS",
    "payment_status": "SUCCESS",
    "totalAmount": 59.80
}
```
