# Port project

```
mvn jetty:run-war


curl -i "localhost:8080/config?key=AIzaSyCeRbzdWf4qDOjkq2B-c84jl206IwSUV6o"
curl -i "localhost:8080/bot/message?text=message&key=AIzaSyCeRbzdWf4qDOjkq2B-c84jl206IwSUV6o"
```

How to create port contract (develop)
```
curl -i -X POST -H "Content-Type: application/json" -d '{"name":"Vera","bot":"illusionist","key":"CLIENT_KEY","_prop":{"bot":{"type":"STRING","index":true},"key":{"type":"STRING","index":true}}}' "https://datastore.develop.promethist.ai/port/contract?key=AIzaSyDpYmTgXGmZY-vWO6ryOcSQ5YZhBsu6NWc"
```
