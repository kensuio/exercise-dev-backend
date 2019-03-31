# Users API


__Assumptions made__

1. Password. Passwords passed via secured network and store it directly

2. User filed `userName` is unique.

3. It is possible for different `userName`s to have the same `emailAddress`.

4. User role is defined (ex.: by auth layer) for each request.

5. We treat `version` of user from metadat as meter of `Backward compatibility`

6. All data valid (ex: email)


__Specefication__

Filed `password` is to be hidden for all requests except `Sign up` (to be done)

Common response statuses for all requests (if no other specified):

    500 - Internal server error: in case of unknown server issue
    503 - Service unavailable: in case of timeout

Error Object:

```json
    {
      "err": "<error message as string>"
    }
```

1. ##### Get user

    request should be of the following format:
     
    `GET /api/v1/users/{id}`
    
    Request headers: `User-Version` -  version of the user, recent by default (to be done)
    
    Response statuses:
    
       200 Ok       : returns user json in the body
       404 Not Found: no user found for `id`, returns error object
       410 Gone     : user with `id` deleted
       
2. ##### Sign up

    `POST /api/v1/users`
    
    Request body: 
    
    ```json
     {
       "name": "john",
       "email": "john2@test.com",
       "password": "777"
     }
    ```
    
    Response statuses: 
        
        200 Ok      : returns UserData
        409 Conflict: user with same name already exists, returns error object
        410 Gone    : user with `id` deleted
        
    Response body `UserData`:
    
    ```json
    {
        "email": "john2@test.com",
        "id": "c29d2e44-89ae-46c4-a3b4-9d013cc0a284",
        "name": "john",
        "password": "Password(777)",
        "status": "Active",
        "version": 1
    }
    ```     
        
3. ##### Update email

    `PUT /api/v1/users/{id}/email`

    Request body: 
    
    ```json
    {
        "email": "john3@test.com"
    }
    ```
    
    Response statuses: 
        
        204 No content  : updated sucessfully 
        404 Not Found   : no user found for `id`, returns error object
        410 Gone        : user with `id` deleted
        
    Response headers: `User-Version` - new version of the user in case of status `204`
    
 4. ##### Update/reset password
 
    `PUT /api/v1/users/{id}/password`   
    
    Request body: 
    
    ```json
    {
        "password": "<optional>"
    }
    ```
    
    For optional `password` filed possible 2 cases:
    
    - value set: **update** password
    - value not set: **reset** password
    
    Response statuses: 
        
        204 No content  : done sucessfully 
        404 Not Found   : no user found for `id`, returns error object
        410 Gone        : user with `id` deleted
        
    Response headers: `User-Version` - new version of the user in case of status `204`

 5. ##### Block/unblock user 
 
    To be used by Admin
    
    `PUT /api/admin/v1/users/{id}/status`
    
    Request body: 
    
    ```json
    {
        "status": "<blocked | active>"
    }
    ```
    
    Response statuses: 
        
        204 No content  : done sucessfully 
        400 Bad request : if `status` other the allowed, returns error object 
        404 Not Found   : no user found for `id`, returns error object
        409 Conflict    : if try to active active user or block blocked user, returns error object
        410 Gone        : user with `id` deleted, returns error object
            
    Response headers: `User-Version` - new version of the user in case of status `204`
   
 5. ##### List all users 
 
    To be used by Admin
    
    `GET /api/admin/v1/users`
    
    Response statuses:
    
        200 Ok       : returns list if users data in the body
        

__How to run__

Default port: 8080

Run applicatiom

```bash
sbt run
```

Request example:

```bash
curl GET  http://localhost:8080/api/admin/v1/users
```
 


