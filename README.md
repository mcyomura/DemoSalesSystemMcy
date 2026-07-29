# Introduction
## Goal
I started my career as developer, but I've worked as architect (enterprise, business, it architect, solution architect) in the past 15 years. I'm planning to return to development (reason is that it is a more standalone role, to work with people in different timezones) and for that I took some courses to get updated and to put all that in practice, I developed this demo. So, I have a great view as architect and now I'm rejoining this view with the engineer, I'm a developer who really understands the whole environment.

This is a **portfolio program**, the goal is to present skills in programming (microservices, Springboot 4, java 21, JPA, Kafka, Feign, MapStruct, API Doc, Docker).

This backend Sales System consists of 5 microservices and 2 infrastructure containers configured for demonstration. To showcase distributed transactions with **Kafka**, I intentionally designed topics with multiple subscribers to implement a **choreographed SAGA pattern**. Building this provided invaluable hands-on experience in handling event-driven choreography and compensation logic.

# About the development
## C4 - Level 1
A high level (context) diagram showing main Sales System and external integrations.

![C4 Level 1](Documentation/C4_Sales_System-C4-L1.drawio.png)
## C4 - Level 2
Shows the microservices, databases, messaging topics. Filled in blue the scope of the demo.

![C4 Level 2](Documentation/C4_Sales_System-C4-L2.drawio.png)

## Design patterns
For the demo I cared to use **different code design patterns**, so I have microservices implemented using **MVC, Clean architecture and hexagonal** pattern (C4 L2 documents this).

## Other docs: Sequence diagram, database entity diagram and swaggers
* Sequence diagram: [Sequence diagram](Documentation/C4_Sales_System-Sequence_diagram.drawio.png) shows the interaction between the components.
* Entity relationship diagrams: [db-orders](Documentation/C4_Sales_System-db-orders.drawio.png), [db-catalog](Documentation/C4_Sales_System-db-catalog.drawio.png), [db-payment](Documentation/C4_Sales_System-db-payment.drawio.png), [db-customer-auth](Documentation/C4_Sales_System-db-customer-auth.drawio.png). In case you wish to connect the database via DBeaver or other.
* State machine:  [State machine diagram](Documentation/C4_Sales_System-State_machine.drawio.png) shows cart and SAGA statuses.
* The swaggers (only BFF will be externally exposed, but I provide internal swaggers as well):
	* [bff](Documentation/swagger_bff.json) or at [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_bff.json)  
 	* [order](Documentation/swagger_order-service.json) or at [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_order-service.json)  
  	* [catalog](Documentation/swagger_catalog-service.json) or at [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_catalog-service.json)
  	* [customer-auth](Documentation/swagger_customer-auth-service.json) or at [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_customer-auth-service.json) 

## Microservices:
* **sales-web-bff (8085):** the only microservice supposed to expose its endpoints through an API Manager.
* **order-service (8082):** deals with the cart (add items, calculate total amount, check if prices are stalled, process cart checkout).
* **catalog-service (8081):** owns product catalog and inventory (stock).
* **payment-service(8083):** a microservice mocking interaction with payment provider. It receives a payment token (payment token is to be created by the UI using a SDK from a payment provider, the service is to confirm the payment with the provider, as well as process refund when needed). In this demo to mock a declined payment you just pass a payment token ending "99".
* **Customer-auth-service(8084):** authenticates with an idP, for the demo it uses GitHub, as it requires zero infrastructure, leverages developers' existing accounts, and uses standard OAuth 2.0.

## Infrastructure containers:
* **kafka:** version running using kafka
* **MariaDB:** I started the local project with MySql, but migrated to MariaDB (lighter) in containers. Note that although I have instantiated a single container, the schemas are totally independent. For that I used different application users, so the catalog database has the root and appCatalogService users, and the catalog microservice is configured to connect using the appCatalogService user only.

## SAGA choreography:
Upon a cart checkout:
* **Order-service sends** an **ORDER-PLACED event** (order_events topic)
* **Catalog-service process ORDER-PLACED event** and deduct stock itens. It then sends a **SUCCESS event** in the inventory_processed topic or a **FAILED event** if any product had not enough stock.
* **Payment-service** at the same time, **process ORDER-PLACED event** and "process" (mocked) the payment. It then sends a **SUCCESS event** in the payment_processed topic or a **FAILED event** if payment was rejected.
* **Order-service receive back both events** from inventory_processed and payment_processed. If **both were SUCCESS**, it then changes the **order status to APPROVED**. If payment/inventory sent a FAILED event, then the order status goes to **CANCELED** and an **STOCK_DECLINED/PAYMENT_DECLINED event** or both is sent. Note: we used an @Version, so that if both events are processed at the same time, and both READ the database before commiting the status change, what will happen is that the first will commit, the second will fail throwing an exception, then kafkaErrorConfig will capture the exception and do not commit the message, that will go to the next attempt and will finally be successfully processed (in the second attempt to process the kafka event).
* If a **STOCK_DECLINED event** were sent, then payment-service will process a **refund** and send back a **REFUNDED** event to update order-service record.
* If a **PAYMENT_DECLINED event** were sent, then catalog-service will process the **stock return** and send back a **RETURNED** event to update order-service record.
* **Notes about the SAGA**: I already mentioned at paragraph 4 (in this section) about the @Version. Another SAGA detail is that to avoid a canceled operation to be processed before an order-placed operation, we use the order_events topic and use the **UUID as the key**, the usage of a key guarantees that the first event (order-placed) will be processed BEFORE any cancelation event for the same order.

## Usage of AI
AI is undeniably here to stay. However, since my primary goal with this project was to solidify my understanding of core concepts, I initially limited AI usage to clarifying doubts, brainstorming, and guiding the initial setup. Toward the end—specifically for automated testing and the final microservice—I experimented with AI-assisted coding. While it significantly boosts productivity, I concluded that developers must deeply understand the underlying code to effectively evaluate, correct, and steer AI-generated outputs.

## Dev vs Hom vs Prod environment
I cared to produce dev and prod application.properties, just for the concept. 
In the demo there's not much diff between them:
* The swagger is generated only in dev profile. 
* In dev time for the price to get stalled is 10 minutes, in production is 6 hours.
* Infrastructures: In a real scenario I consider the kafka topics would not be generated by the code (for governance purposes), urls and servers infrastructures should be different as well.
* In the customer-auth-service for a real environment we need a vault to store the RSA keys pair. I also used a CRON to refresh the keys, which is supposed to be called once (i.e. you need to have only 1 POD). In a real environment you use a scheduler like Control-M or.. you separate just the scheduler in a small service just to be used as a trigger to rotate the keys in a vault. 

# Running the demo
## Instructions to run
* Download the zip (from release v1.0.6 on you must have a Github login to some operations, download v1.0.5 if you want to avoid authentication)
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
