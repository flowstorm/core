# Port project

```
mvn jetty:run-war


curl -i "localhost:8080/config?key=AIzaSyCeRbzdWf4qDOjkq2B-c84jl206IwSUV6o"
curl -i -X POST -H "Content-Type: application/json" -d '{"text":"hello"}' "localhost:8080/message?key=AIzaSyCeRbzdWf4qDOjkq2B-c84jl206IwSUV6o"
```

How to create port contract (develop)
```
curl -i -X POST -H "Content-Type: application/json" -d '{"name":"Client name","bot":"illusionist","key":"CLIENT_KEY","botKey":"BOT_API_KEY","model":"MODEL_ID","_prop":{"bot":{"type":"STRING","index":true},"key":{"type":"STRING","index":true}}}' "https://datastore.develop.promethist.ai/port/contract?key=AIzaSyDpYmTgXGmZY-vWO6ryOcSQ5YZhBsu6NWc"
```
