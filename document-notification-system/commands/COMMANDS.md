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
