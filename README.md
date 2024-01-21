# Digital Wallet Service
## Description
This is a simple API service that holds the balances of customers.

The balance of a given customer can be debited or credited.

All the existing transactions of a given customer can be returned.

Refer to the [api-specification.yml](api-docs/api-specification.yml) for the API specification.

## Compilation and execution
Execute:
```
mvn clean install
java -jar target/digital-wallet-service-0.0.1-SNAPSHOT.jar
```
To run the application in a port other than the default `8080`, e.g. `8000`:
```
java -jar -Dserver.port=8000 target/digital-wallet-service-0.0.1-SNAPSHOT.jar
```

## API requests
### Get Customer Transactions
GET request to `/transactions/{customerId}?pageNumber={page}&pageSize={size}`. Example:
```
curl --location 'localhost:8000/transactions/11111111-1111-1111-1111-111111111111?pageNumber=0&pageSize=4'
```

The query parameters `pageNumber` and `pageSize` are optional (default values will be taken if not provided).

### Create Customer Transaction
POST request to `/transactions` with the below body:
```
{
    "correlationId": <UUID unique string>,
    "customerId": "<UUID of existing customer>",
    "amount": <decimal number>,
    "operation": <"ADD" or "WITHDRAW">
}
```

Following the accepted standards of RESTful APIs, the POST endpoint does not include verb, and consequently this endpoint is common for both credit (`"ADD"`) and debit (`"WITHDRAW"`) transactions, being the field `operation` what defines the kind of transaction, and consequently the provided `amount` must always be a positive number.

Example of request:
```
curl --location 'localhost:8000/transactions' \
--header 'Content-Type: application/json' \
--data '{
    "correlationId": "4f298259-6a39-4dca-a004-e61332d112c3",
    "customerId": "11111111-1111-1111-1111-111111111111",
    "amount": 100.00,
    "operation": "WITHDRAW"
}'
```

## Additional notes
This is a demo API that uses an in-memory database. Initial data with balances and transactions for two costumers is loaded at startup ([data.sql](src/main/resources/data.sql)).

When the application is executed, the database can be accessed at `http://localhost:8080/h2-console`, with the below details:
- JDBC URL: `jdbc:h2:mem:db`
- User Name: `sa`
- Password:

There are three entities, `Customer`, `Balance` and `Transaction`, but the `Customer` entity (which is thought to keep the customer details) is not being used at the moment. Instead, the `Balance` entity is used to verify the existence of a customer, as this table will hold a unique record per customer.

To minimize the database usage when checking whether the balance requested for a given `customerId` exists, the application uses cache for this field.

For simplicity, Security is not implemented, neither Customer creation/signup.