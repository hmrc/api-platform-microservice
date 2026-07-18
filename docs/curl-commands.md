# API Platform Microservice Curl Commmands (wip)
---
### Endpoints Summary

| Path                                                                                                                            |  Method  | Description                                |
|---------------------------------------------------------------------------------------------------------------------------------|----------|--------------------------------------------|
| [`/api-definitions/nonopen`](#user-content-get-retrieve-all-not-open-api-definintions)                                          | `GET`    | Retrieve All Not Open API Definintions     |
| [`/api-definitions/open`](#user-content-get-retrieve-all-open-api-definintions)                                                 | `GET`    | Retrieve All Open API Definintions         | 


--- 
 
### GET Retrieve All Not Open API Definintions 
#### `GET /api-definitions/nonopen`
Retrieve all API definitions that are not considered open. 
ENVIRONMENT options: "SANDBOX", "PRODUCTION" 
  
 
##### curl command
```
-X GET https://api-platform-microservice.public.mdtp/api-definitions/nonopen?environment={ENVIRONMENT} \
    -H "Content-Type: application/json" \
    -H "Accept: application/vnd.hmrc.1.0+json" 
```
 
---

--- 
 
### GET Retrieve All Open API Definintions 
#### `GET /api-definitions/open`
Retrieve all API definitions that are considered open. 
ENVIRONMENT options: "SANDBOX", "PRODUCTION" 
  
 
##### curl command
```
-X GET https://api-platform-microservice.public.mdtp/api-definitions/open?environment={ENVIRONMENT} \
    -H "Content-Type: application/json" \
    -H "Accept: application/vnd.hmrc.1.0+json" 
```
 
---