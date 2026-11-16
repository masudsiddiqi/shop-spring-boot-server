# shop-api

A small e-commerce backend (Spring Boot 3.5, Java 17) that implements `shop-openapi.yaml` (Shop API Spec v1.0.0).
It is a **test target** for an API test framework: four resources with different behaviour
(users, products, cart, orders), roles with different permissions, ownership, an order life cycle and an idempotent POST.
Data is kept in memory. A restart resets the data and invalidates every token.

## Run

```
mvn spring-boot:run          # http://localhost:8081
```

Swagger: open https://editor.swagger.io and paste `shop-openapi.yaml`. CORS allows that page.
For a test framework run: `mvn test -DbaseUri=http://localhost:8081`.

## Accounts (password for all: `password123`)

| username | role | id |
|---|---|---|
| admin | admin | 1 |
| seller1 | seller | 2 |
| customer1 | customer | 3 |
| customer2 | customer | 4 |

`POST /users` (public) creates a new account. It is always a customer.

## Who may call what

| operation | anonymous | admin | seller | customer |
|---|---|---|---|---|
| `GET /ping`, `POST /auth/login`, `POST /users` | yes | yes | yes | yes |
| `GET /users/me` | 401 | yes | yes | yes |
| `GET /users` | 401 | yes | 403 | 403 |
| `GET /products`, `GET /products/{id}` | 401 | yes | yes | yes |
| `POST /products`, `PUT/PATCH /products/{id}` | 401 | yes | yes | 403 |
| `DELETE /products/{id}` | 401 | yes | 403 | 403 |
| `/cart`, `/cart/items/{productId}` (all methods) | 401 | yes | 403 | yes |
| `POST /orders`, `GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/cancel` | 401 | yes | 403 | yes |
| `POST /orders/{id}/ship` | 401 | yes | 403 | 403 |

A permission is named `resource:action` (for example `product:create`). The roles and their permissions are in `domain/Role.java`.

## Rules that need data (so they are not in the security rules)

- **Ownership.** A customer sees and cancels only the own orders. The order of another customer is a **403**. An admin sees every order.
- **Order life cycle.** `placed` -> `shipped` (admin) or `placed` -> `cancelled` (owner or admin). Shipped and cancelled are final (409).
- **Placing an order** takes the items of the cart, lowers the stock and empties the cart. An empty cart is a **409**. Not enough stock is a **409** (the cart and the stock stay as they were).
- **Idempotency-Key** (header, required, 8 to 64 characters). Same key + same body: **200** with the first order. Same key + different body: **409**. A missing header is **400**. The key is scoped to the account.
- **Stock.** A cart quantity above the stock is a **409**. `HOME-001` has no stock on purpose. Cancelling an order gives the stock back.
- **Product delete.** A product that is in an order that is `placed` or `shipped` cannot be deleted (**409**). The sku is unique (**409**).
- **Paging.** `page` is 1 or more and `limit` is 1 to 50 (else **422**). A page beyond the last one is an empty page, not an error.

## Where an error comes from (the layers)

| status | made by |
|---|---|
| 413 | `PayloadSizeFilter` (before security and routing) |
| 401, 403 | security rules (`SecurityConfig`) |
| 404 (unknown path), 405, 406, 415 | routing (Spring MVC). 405 carries the `Allow` header |
| 400 | the request cannot be read: broken JSON, wrong type, unknown field, unknown enum value, wrong path or query type, missing required header or query parameter, path id below 1 |
| 422 | the request is readable, but a value breaks a rule (required, length, range, format) |
| 404 (record), 403 (not yours), 409 | the stores (`domain/*Store.java`) |

## Try it

```
# log in, keep the token
TOKEN=$(curl -s -X POST localhost:8081/auth/login -H 'Content-Type: application/json' \
        -d '{"username":"customer1","password":"password123"}' | sed 's/.*"token":"\([^"]*\)".*/\1/')

curl -s localhost:8081/products?category=books -H "Authorization: Bearer $TOKEN"
curl -s -X PUT localhost:8081/cart/items/1 -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' -d '{"quantity":2}'
curl -si -X POST localhost:8081/orders -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -H 'Idempotency-Key: order-0001-abc' \
     -d '{"shippingAddress":{"street":"12 Market Street","city":"Dhaka","postalCode":"1212","country":"BD"}}'
# the same command again -> 200 with the same order (not a second order)
curl -s -X POST localhost:8081/orders/1/cancel -H "Authorization: Bearer $TOKEN"
```

## For the test framework

Suggested role accounts (the key format of `config.properties`: `auth.role.<resource>No<Action>Role.username/.password`):

| key | account | why |
|---|---|---|
| `productNoCreateRole`, `productNoUpdateRole` | customer1 | a customer cannot create or update a product |
| `productNoDeleteRole` | seller1 | a seller cannot delete a product (an admin can) |
| `cartNoUpdateRole` | seller1 | a seller has no cart permission |
| `orderNoCreateRole` | seller1 | a seller cannot place an order |
| `userNoReadRole` | customer1 | `GET /users` is admin only |
| `auth.other.customer` | customer2 | another account with the SAME role: used to test ownership (reading the order of customer1) |

Things you will meet in the framework (expected, not a backend bug):

- **204 has no body and no `Content-Type`** (`DELETE /products/{id}`, `DELETE /cart`). A header check that requires `Content-Type` on every response fails for them.
- **201 and `Location`** (`POST /products`, `POST /orders`): the success status is not 200, and a `Location` header exists.
- **A list is an object** `{items, page, limit, total}`, not an array.
- **`Idempotency-Key`** is an extra header on top of the normal auth headers.
- **Every success body has its own schema**; the error body is the same `ProblemDetails` for all.

To make a test fail on purpose, change `domain/Role.java`: for example add `"product:create"` to `customer`, restart, and the scenarios that expect a 403 for a customer must fail.

## Not real on purpose

Plain-text passwords, opaque random tokens (the spec says "JWT" only as a format hint), no database, no password rules beyond the length.
