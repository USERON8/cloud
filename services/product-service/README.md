# Product Service
Version: 1.1.0

Product domain service providing product, category, and batch operation endpoints.

- Service name: `product-service`
- Port: `8084`
- Database bootstrap: `db/init/product-service/init.sql`
- Test data: `db/test/product-service/test.sql`

## Core Endpoints

- Products: `/api/product/**`
- Categories: `/api/category/**`
- Internal access: `/internal/product/**`

## Local Run

```bash
mvn -pl product-service spring-boot:run
```
