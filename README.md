# Introduction
## Goal
I started my career as developer, but I've worked as architect (enterprise, business, it architect, solution architect) in the past 15 years. I'm planning to return to development and for that I took some courses to get updated and to put all that in practice, I developed this demo. So, I have a great view as architect and now I rejoining this view with the engineer, I'm a developer who really understands the whole environment.

This is a **portfolio program**, the goal is to present skills in programming (microservices, Springboot 3, java 21, JPA, Kafka, Feign, MapStruct, API Doc, Docker).

It is a Sales System (backend) consisting of 5 microservices + 2 infrastructure containers implemented for the demo. For the use of **kafka** I cared to have at least **two subscribers** for a topic, so that I could implement a **SAGA pattern using choreography** and proceed with compensation.

# About the development
## C4 - Level 1
A high level (context) diagram showing main Sales System and external integrations.

![C4 Level 1](Documentation/C4_Sales_System-C4-L1.drawio.png)
## C4 - Level 1
Shows the microservices, databases, messaging topics. In blue the scope of the demo.

![C4 Level 2](Documentation/C4_Sales_System-C4-L2.drawio.png)

## Design patterns
For the demo I cared to use **different code design patterns**, so I have microservices implement using **MVC, Clean architecture and hexagonal** pattern (C4 L2 documents this).

## Other docs: Sequence diagram, database entity diagram and swaggers
* Sequence diagram: [Sequence diagram](Documentation/C4_Sales_System-Sequence_diagram.drawio.png) shows the interaction between the components.
* Entity relationship diagrams: [db-orders](Documentation/C4_Sales_System-db-orders.drawio.png), [db-catalog](Documentation/C4_Sales_System-db-catalog.drawio.png), [db-payment](Documentation/C4_Sales_System-db-payment.drawio.png). In case you wish to connect the database via DBeaver or other. 
* The swaggers (only BFF will be externally exposed, but I provide internal swaggers as well):
	* [bff](Documentation/swagger_bff.json) or at [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_bff.json?token=GHSAT0AAAAAAECAAVB7W7Z27OQNAERJ2CPQ2SNKVJA)  
 	* [order](Documentation/swagger_order-service.json) or at [swagger editor] [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_order-service.json?token=GHSAT0AAAAAAECAAVB6MZDP62N56ZR2ZUOO2SNKZAA)  
  	* [catalog](Documentation/swagger_catalog-service.json) or at [swagger editor](https://editor.swagger.io/?url=https://raw.githubusercontent.com/mcyomura/DemoSalesSystemMcy/refs/heads/master/Documentation/swagger_catalog-service.json?token=GHSAT0AAAAAAECAAVB7NXOZFLPKCF7YFFGK2SNK2BA)  

## Microservices:
* **sales-web-bff (8085):** the only microservice exposing its endpoints through an API Manager.
* **order-service (8082):** deals with the cart (add items, calculate total amount, check if prices are stalled, process cart checkout).
* **catalog-service (8081):** owns product catalog and inventory (stock).
* **payment-service(8083):** a mock microservice to receive a payment token (payment token is supposed to be created by the UI using a SDK from a payment provider, the service is to confirm the payment with the provider, as well as proceed refund when needed). In this demo to mock a declined payment you just pass a payment token ending "99".
* **Customer-auth-service(8084):** authenticates with an idP, for the demo I'm targeting to use GitHub, as it requires zero infrastructure, leverages developers' existing accounts, and uses standard OAuth 2.0it's WIP.

## Infrastructure containers:
* **kafka:** version running using kraft
* **MariaDB:** I started the local project with MySql, but migrated to MariaDB (lighter) in containers. Note that although I have instantiated a single container, the schemas are totally independent. For that I used different application users, so the catalog database has the users root and appCatalogService, and the catalog microservice is configured to connect using the appCatalogService user only.

## Usage of AI
I'm totally pro for the AI usage, nevertheless, as my goal here was to put in practice many concepts, I restrained the usage for solving doubts, exchange ideas, and guide some steps. In the next steps I have an AI course for developers already purchased, the prerequisite for the course is that you master coding yourself.

## Dev vs Hom vs Prod environment
I cared to produce dev and prod application.properties, just for the concept. 
In the demo there's not much diff between them:
* The swagger is generated only in dev profile. 
* In dev time for the price to get stalled is 10 minutes, in production is 6 hours.
* Infrastructures: In a real scenario I consider the kafka topics would not be generated by the code (for governance purposes), urls and servers infrastructures should be different as well.

## Next Steps
* Implement auth-customer-service
* Automation tests (I know, I should've already done this, but I haven't, that would have helped me, let's go for it!)

# Running the demo
## Instructions to run
* Download the zip
* I would build one container at a time:
	* docker compose build bff-service
	* docker compose build order-service
	* docker compose build catalog-service
	* docker compose build payment-service
* Run the containers: docker compose up
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
   "customerId": "827329",
   "paymentToken": "fioej2",
   "bearerToken": "fjioejf"
}
```
Response: 
```json
{
    "id": 1,
    "customerId": 827329,
    "status": "PENDING",
    "totalAmount": 209.88
}
```


### Check cart status (use as parameter the **"id" returned during checkout**)
Method: GET  
URL: http://localhost:8085/api/v1/salesbff/cart/2  
Response:   
```json
{
    "id": 2,
    "customerId": 8273291,
    "status": "APPROVED",
    "inventory_status": 2,
    "payment_status": 2,
    "totalAmount": 120.90
}
```
