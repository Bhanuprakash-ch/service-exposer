[![Dependency Status](https://www.versioneye.com/user/projects/573a15e3a0ca35004cf783ba/badge.svg?style=flat)](https://www.versioneye.com/user/projects/573a15e3a0ca35004cf783ba)

Service exposer
==========

Service-exposer was developed to overcome difficulties related to exposing service instances with HTTP interface to end-user. In order to access newly created service instances, service-exposer registers instances routes in CF. This routes are constructed using service credentials. However, no direct way exists to get service credentials, therefore service-exposer is also responsible for retriving and storing credentials in database for all service instances of given type.
    
Service discovery
-----------------
To check for newly created/deleted service instances, service-exposer periodically sends REST calls to CF to retrive current list of all services instances of given type. Type of services which instances will be taken into account are defined in application.yml file in form of given list:

```
$ serviceTypes: "rstudio,ipython"
```
 
In next step, for each newly created/deleted service instance of given type, credentials are retireved and stored/deleted from redis database.

Route register
-----------------
GoRouter was used for registering new routes in CF in order to make services instances accesible for users via URL adress. However GoRouter requires to periodically sends register message in order for service instance URLs to stay active. Therefore this application contains second loop that periodically sends register message containing URLs for service instances. This URLs adresses are composed from credentials previously stored in redis database.

Security
-----------------
User can only access list of service instances from specific space. To prevent from registrating services with the same name from different space, GUID of each service instance is injected into generated URL:

```
$ <serviceInstanceName>-<instanceGUID>.<domainName>
```

All special characters from service name are replace into "_" character. Also application forbids to create serivces with names such as api,uaa or login to prevent undifned behavour in CF. For communication with cloud foundry via REST, Oauth2 client credentials needed to be defined in application.yml:

```
spring.oauth2:
  client:
    accessTokenUri: ${vcap.services.sso.credentials.tokenUri}
    userAuthorizationUri: ${vcap.services.sso.credentials.authorizationUri}
    clientId: ${vcap.services.sso.credentials.clientId}
    clientSecret: ${vcap.services.sso.credentials.clientSecret}
```

Application endpoint
-----------------

Credentials and paths for each running service instance are stored inside redis database. To access this information’s, one endpoint is available for retrieving credentials for particular org and space.

*url:* /rest/tools/service_instances?org=orgGUID&space=spaceGUID&service=serviceType

Required services
-----------------
Service-exposer requires following service to function properly:

* **Redis DB** - for storing service instance credentials.

Also following connections need to be avaiable:
* **Connection to NATS** - a messaging system used in CF for communicating with GoRouter to register service instances paths.

Required libraries
-----------------
Following libraries are necessary to successfully build user-management:

* **cf-client** - separate library to communicate with cloud foundry layer.

How to build
------------
It's a Spring Boot application build by maven. All that's needed is a single command to compile, run tests and build a jar:

```
$ mvn verify
```

How to run locally
------------------

```
$ mvn spring-boot:run
```
