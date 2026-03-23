## correr docker postgres

```bash
docker run -d --name my-postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=admin -e POSTGRES_DB=postgres -p 5432:5432 postgres:15
```

## Kafka con docker

cremos el modulo de infraestructure

creamos la carpeta docker-compose/.env

primero ejecutarenos zookeper

```bash
docker-compose -f common.yml -f zookeeper.yml up
```

luego correremos el kafka cluster

```bash
docker-compose -f common.yml -f kafka_cluster.yml up
```

iniciaremos el cluster

```bash
docker-compose -f common.yml -f init_kafka.yml up
```

para ver kafka a un nivel visual, se puede abrir

```
http://localhost:9000/
```

le damos en cluster/add cluster

```
document-notification-system-cluster
```

```
zookeeper:2181
```

save/view cluster

## Create Document

```
curl --request POST \
  --url http://localhost:8181/documents \
  --header 'Content-Type: application/json' \
  --data '{
  "customerId": "550e8400-e29b-41d4-a716-446655440001",
  "labels": [
    {
      "itemId": "123e4567-e89b-12d3-a456-426614174001",
      "amount": 1500.00,
      "lateInterest": 45.00,
      "regularInterest": 30.00,
      "subTotal": 1575.00
    },
    {
      "itemId": "123e4567-e89b-12d3-a456-426614174002",
      "amount": 850.50,
      "lateInterest": 25.50,
      "regularInterest": 17.00,
      "subTotal": 893.00
    }
  ],
  "documentInformation": {
    "address": {
      "postalCode": "10001",
      "street": "123 Main Street",
      "city": "New York",
      "state": "NY",
      "zipCode": "10001",
      "country": "USA"
    },
    "periodStartDate": "2026-01-01",
    "periodEndDate": "2026-01-31",
    "totalLateInterest": 70.50,
    "totalRegularInterest": 47.00,
    "totalAmount": 2468.00,
    "documentType": "PDF"
  }
}'
```

avro

```
mvn -f "C:\proyectos\DOCUMENT-NOTIFICATION-SYSTEM\document-notification-system\pom.xml" -pl infraestructure/kafka/kafka-model -am generate-sources
```

## Generating graphics for each module 


```bash
mvn com.github.ferstl:depgraph-maven-plugin:graph
```

or 
```bash
mvn com.github.ferstl:depgraph-maven-plugin:aggregate -DcreateImage=true -DreduceEdges=false -Dscope=compile "-Dincludes=com.document.notification.system*:*"
```

## .ENV


## Newman - Mass Load Testing

Install Newman globally:
```bash
npm install -g newman
```

Run 500 requests with 100ms delay between each:
```bash
newman run commands/postman/create-document-collection.json -n 500 --delay-request 100
```

Run 1000 requests with detailed report:
```bash
newman run commands/postman/create-document-collection.json -n 1000 --delay-request 100 --reporters cli,json --reporter-json-export commands/postman/results.json
```

Run with custom base URL (e.g., different environment):
```bash
newman run commands/postman/create-document-collection.json -n 500 --delay-request 100 --env-var "base_url=http://localhost:8181"
```

## .ENV


```
MAIL_FROM=some value
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=some value
MAIL_PASSWORD=some value
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS_ENABLE=true
MAIL_SMTP_STARTTLS_REQUIRED=true
MAIL_SMTP_CONNECTION_TIMEOUT=5000
MAIL_SMTP_TIMEOUT=5000
MAIL_SMTP_WRITE_TIMEOUT=5000
```