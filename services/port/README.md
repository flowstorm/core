# Port project

```
mvn jetty:run-war

# get config
curl -i "localhost:8080/contract?key=VeraTest.4"

# send message
curl -i -X PUT -H "Content-Type: application/json" -d '{"text":"hello"}' "localhost:8080/message?key=VeraTest.4"

# read file
curl -i "localhost:8080/file/5d5cd56ac87aa5439369e863"
```

