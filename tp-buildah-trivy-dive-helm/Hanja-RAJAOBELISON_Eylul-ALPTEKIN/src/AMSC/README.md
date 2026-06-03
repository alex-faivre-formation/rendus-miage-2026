# TP Architectures Micro-Services sur Cloud

Ce TP met en place une architecture MS pour une banque (simplifiée) avec Spring Boot.

Liste des MS :
* amc_clients : MS qui gère les profils des clients hébergés dans une BD MySQL ;
* amc_comptes : MS qui gère les informations des comptes hébergées dans une BD MongoDB ; 
* amc_composite : MS composite travaillant avec les deux autres MS ;

En plus on a les MS edges suivants :
* amc_annuaire : un service de découverte Eureka ;
* amc_proxy : un API Gateway ;
* amc_configserver : un service de gestion des configurations hébergées dans les dépôts git amc_config 
 (version hors docker) et amc_config_docker (version avec docker) ;
* un service de traçage avec Spring Boot et Zipkin ;
* un service de monitoring avec Spring Boot et Prometheus.

Une fois le git clone du dépôt principal réalisé, il faut cloner les dépôts suivants :
* https://github.com/hialmar/amc_clients
* https://github.com/hialmar/amc_comptes
* https://github.com/hialmar/amc_composite
* https://github.com/hialmar/amc_annuaire
* https://github.com/hialmar/amc_proxy
* https://github.com/hialmar/amc_configserver


Note : il y a des versions des services clients, comptes, composite et proxy (gateway) avec et sans sécurité (voir plus bas)


Donc, il faut faire :

```
git clone https://github.com/hialmar/AMSC.git
cd AMSC
git clone https://github.com/hialmar/amc_clients.git
git clone https://github.com/hialmar/amc_comptes.git
git clone https://github.com/hialmar/amc_composite.git
git clone https://github.com/hialmar/amc_annuaire.git
git clone https://github.com/hialmar/amc_proxy.git
git clone https://github.com/hialmar/amc_configserver.git
```

Vous pouvez aussi cloner les dépôts de fichiers de configuration si vous voulez héberger les vôtres :
* https://github.com/hialmar/amc_config
* https://github.com/hialmar/amc_config_docker


### BD et produits à lancer

Pour commencer le TP, il faut les dépendances suivantes (qui seront intégrées au docker-compose.yml)

Commande pour la BD MySQL :

docker run --name monsql_bq -p 3306:3306  -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=banquebd -d mysql:oracle

Commande pour la BD Mongo :

docker run --name mongo_bq -p 27017:27017 -e MONGO_INITDB_ROOT_USERNAME=root -e MONGO_INITDB_ROOT_PASSWORD=root -e MONGO_INITDB_DATABASE=banque_spring -d mongo:latest

Commande pour zipkin :

docker run -d -p 9411:9411 --name zipkin openzipkin/zipkin

Commande pour Prometheus :

docker run --name my-prometheus --mount type=bind,source=prometheus.yml,destination=/etc/prometheus/prometheus.yml -p 9090:9090 prom/prometheus

### Autre solution : docker-compose.yml

Tous les services sont prêts pour lancement avec "docker-compose up".





### Sécurisation avec Okta

On va utiliser le produit Auth0 de la société Okta.
C'est un outil qui permet de faire de l'IAM (Identity and Access Management) dans le Cloud.
En gros leur plateforme gère l'essentiel : authentification, identification, autorisation, droits d'accès, utilisateurs, rôles...

Première étape : créer un compte développeur gratuit sur https://developer.okta.com/signup/.

Attention : bien choisir "Customer Identity Cloud" qui va vous amener sur l'outil Auth0.

Une fois le compte créé (vous pouvez utiliser votre compte GitHub, Google ou Microsoft) il faut créer une application.

On va choisir une application SPA (Single Page Application).

Une fois l'application créée on vous propose de récupérer un template d'application front écrite avec Angular (ou autre techno).

Choisissons de l'Angular.

Vu que nous n'avons pas encore d'application front on va choisir "I want to explore a sample app".

Attention : il s'agit du deuxième bouton bleu (celui qui vous donne une application pré-configurée).

Comme l'indique la page de téléchargement, il va falloir ajouter l'URL de développement classique de ng serve http://localhost:4200 pour qu'elle puisse :
* Servir d'URL Callback (sur laquelle on va pouvoir re-diriger un utilisateur une fois identifié) ;
* Servir d'URL Logout (sur laquelle on va pouvoir re-diriger un utilisateur une fois déconnecté) ;
* Être référencée comme Origine Web (pour gérer l'authentification multi sites sans qu'on puisse récupérer les données d'authentification sur n'importe quel site).

On fait tout ça dans la page Settings de l'application.

Dans la page Connections, on peut activer ou désactiver les connections OpenID avec GitHub, Google...

#### Modification du front partie 1 :

Une fois récupéré le projet Web, vous allez y trouver un fichier auth_config.json 
qui est ignoré par GitHub (vu qu'il va contenir des infos confidentielles).
Voilà ce qu'il contient par défaut :

```
{
"domain": "VOTRE DOMAINE.us.auth0.com",
"clientId": "VOTRE ID CLIENT",
"authorizationParams": {
"audience": "{yourApiIdentifier}"
},
"apiUri": "http://localhost:3001",
"appUri": "http://localhost:4200",
"errorPath": "/error"
}
```

Les infos domain et clientId ont été initialisées automatiquement et correspondent à ce qui est affiché sur la page Settings sur le site Auth0.

Il va falloir maintenant modifier audience et apiUri pour y indiquer les infos de votre application Spring.

Attention : ne pas oublier de modifier apiUri !!!

On va indiquer, pour les deux, les infos d'amc_proxy (le projet correspondant à la Gateway) : http://localhost:10000

Note : pour tester, vous pouvez aussi sécuriser amc_clients (service clients), amc_comptes (service comptes)... mais normalement tout doit passer par la Gateway.

Attention : il ne faut surtout pas inclure le / à la fin de l'URL.

Donc votre fichier doit se finir comme suit :

```
...
"authorizationParams": {
"audience": "http://localhost:10000"
},
"apiUri": "http://localhost:10000",
"appUri": "http://localhost:4200",
"errorPath": "/error"
}
```

Une fois ces modifications faites, on va déclarer l'API sur le serveur Auth0 :
* Sur la gauche vous allez trouver un menu Applications avec un sous-menu API ;
* Cliquez sur API puis "Create API" ;
* Indiquez le nom que vous voulez puis http://localhost:10000
* Laissez le reste tel quel

On peut ensuite lancer l'application une première fois en faisant le classique :

```
npm install && npm start

```

#### Modifications Des serveurs de ressources (client, comptes et composite) :


Note : il y a des branches avec et sans sécurité pour ces trois dépôts git.


Ajouts de dépendances pour transformer le serveur client (par exemple) en serveur de ressources au sens Oauth et ajouter les classes de config pour Okta.

Attention : une fois ces modifications réalisées nous ne pourrons plus utiliser l'application sans authentification !

##### Ajoute des dépendances au pom.xml :

```
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
        </dependency>

        <dependency>
            <groupId>com.okta.spring</groupId>
            <artifactId>okta-spring-boot-starter</artifactId>
            <version>3.0.6</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
```

##### Ajout de la sécurité sur les services :

* Ajout de @EnableMethodSecurity pour activer la sécurité
* Ajout d'un Bean de type SecurityWebFilterChain pour configurer la sécurité

```
@SpringBootApplication
@EnableDiscoveryClient
@EnableMethodSecurity(prePostEnabled = true) // ICI
public class AmcClientsApplication {

...
    @Bean // ET LA
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults()));

        // process CORS annotations
        http.cors(Customizer.withDefaults());

        // force a non-empty response body for 401's to make the response more browser friendly
        Okta.configureResourceServer401ResponseBody(http);

        return http.build();
    }
```

##### Modification dans application.yml :

* Ajout des URLs qui fournissent les jetons JWT (Note : dev-nj3gclnzfe2tmzvt est le nom de mon application à changer) :
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://dev-nj3gclnzfe2tmzvt.us.auth0.com/
          jwk-set-uri: https://dev-nj3gclnzfe2tmzvt.us.auth0.com/.well-known/jwks.json
* Ajout des URLs pour Okta (fournisseur authentification et URL de l'appli elle-même pour comparaison dans les jetons)
  (Note : dev-nj3gclnzfe2tmzvt est le nom de mon application à changer) :
 okta:
    oauth2:
      issuer: https://dev-nj3gclnzfe2tmzvt.us.auth0.com/
      audience: http://localhost:10000


```
# Proprietes de l'application
spring:
  application:
    name: amcclients                                   # nom de l'application
  cloud:
    # Activation remontée management dans Eureka
    config:
      service-registry:
        auto-registration:
          register-management: on
  # configuration lien vers serveur Zipkin

  datasource:
    url: jdbc:mysql://banque-mysql:3306/banquebd?serverTimezone=UTC  # URL mysql
    username: root
    password: root
  jpa:                                                      # Configuration JPA
    database-platform: org.hibernate.dialect.MySQLDialect  # On va parler en MySQL
    hibernate:
      # NE PAS LAISSER EN PROD
      ddl-auto: update                                      # strategie create-drop.
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://dev-nj3gclnzfe2tmzvt.us.auth0.com/
          jwk-set-uri: https://dev-nj3gclnzfe2tmzvt.us.auth0.com/.well-known/jwks.json

okta:
  oauth2:
   issuer: https://dev-nj3gclnzfe2tmzvt.us.auth0.com/
   audience: http://localhost:10000
```

#### Modifications pour OpenFeign :

En plus de ce qui est indiqué au-dessus, pour le composite il nous faut transmettre le jeton d'authentification quand
on appelle les autres services.

D'abord, il faut le récupérer dans le contrôleur REST :

```
    @GetMapping("{id}")
    public ClientWithCompte getClient(@PathVariable("id") Long id, @RequestHeader("Authorization") String bearerToken) {
        logger.info("ClientComptes : demande récup comptes d'un client avec id:{}", id);
        ClientWithCompte c = clientsCompteRepository.getClientWithComptes(id, bearerToken);
        logger.info("ClientComptes : demande récup comptes client:{}", c);
        return c;
    }
```

Puis, on le transmet lors des appels OpenFeign :
```
    public ClientWithCompte getClientWithComptes(Long idclient, String bearerToken) {
        logger.info("On a 1 demande");
        logger.info("On envoie la demande au service client");
        logger.info("Token "+bearerToken);

        try {
            // On récupère 1 objet client
            Client c = this.clientclients.getClient(idclient, bearerToken);
            logger.info("On a recue la réponse client : {}", c);

            // On récupère la liste des comptes pour 1 client donné
            logger.info("On envoie la demande au service compte");
            List<Compte> cpts = this.clientcomptes.getComptes(c.getId(), bearerToken);
            logger.info("On a recue la réponse compte : {}", c);
    ... 
```

Et voilà comment on fait dans un client OpenFeign :

```
@FeignClient("amcclients")
@Headers("Authorization: {token}")
public interface ClientClients {

    // Déclaration d'usage d'une méthode
    /*
        UPDATE API GATEWAY
        Ancien : @RequestMapping(method = RequestMethod.GET, value = "/api/clients/{id}", produces = "application/json")
        Modif URL pour enlever le /api/clients
     */
    @RequestMapping(method = RequestMethod.GET, value = "{id}", produces = "application/json")
    Client getClient(@PathVariable Long id, @RequestHeader("Authorization") String token);
}
```

#### Modifications de la gateway :

Note : il y a, ici aussi, des branches avec et sans sécurité pour ce dépôt.

##### Modification dans application.yml :

* Ajout de CORS (voir aussi plus bas si vous passez par le serveur de configuration) ;

```
# Proprietes de l'application
spring:
  application:
    name: apigateway                                   # nom de l'application
  cloud:
    # Configuration de l'API Gateway
    gateway:
      # ICI : CORS
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:4200"
            allowedMethods:
              - GET
              - POST
              - DELETE
              - PUT
              - PATCH
              - OPTIONS
        add-to-simple-url-handler-mapping: true
```

##### Fonctionnement :

* S'authentifier avec l'appli Angular
* Avec les outils de dev du navigateur récupérer le jeton : Authorisation: Bearer XXXXXX
* Copier le jeton d'authentification dans le plugin du navigateur et l'utiliser pour vos requêtes


##### Problème de CORS en passant par le serveur de configurations ?

Note : je ne suis pas certain de cette partie, si ça marche sans, prévenez-moi.
Il est possible que mon problème venait de caches.

Pour que le CORS fonctionne, il faut, apparemment, mettre les propriétés dans
le fichier application.yml initial (avant la récupération via le config server).

Voilà le contenu du fichier application.yml :
```
# Proprietes de l'application
spring:
  application:
    name: apigateway
  profiles:
    active: dev
  # Adresse du service de configuration
  config:
    # SANS DOCKER COMPOSE
    # import: optional:configserver:http://localhost:10003
    # AVEC DOCKER COMPOSE
    import: optional:configserver:http://bnkconfigsrv:10003
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://dev-nj3gclnzfe2tmzvt.us.auth0.com/
          jwk-set-uri: https://dev-nj3gclnzfe2tmzvt.us.auth0.com/.well-known/jwks.json
  cloud:
    # Configuration de l'API Gateway
    gateway:
      # ICI : ne pas oublier le CORS
      globalcors:
        cors-configurations:
          '[/**]':
            allowedOrigins: "http://localhost:4200"
            allowedMethods:
              - GET
              - POST
              - DELETE
              - PUT
              - PATCH
              - OPTIONS
        add-to-simple-url-handler-mapping: true
# ICI : il faut aussi préciser les infos pour Okta
okta:
  oauth2:
    issuer: https://dev-nj3gclnzfe2tmzvt.us.auth0.com/
    audience: http://localhost:10000

```

##### Modification du front partie 2

Pour vérifier que ça marche, dans le fichier src/app/api.service.ts 
de l'application Angular, on peut appeler, par exemple, notre endpoint /api/clients qui liste les profils clients :

```
  ping$() {
    return this.http.get(`${config.apiUri}/api/clients`);
  }
```

On peut aussi changer les numéros de ports indiqués dans le fichier 
src/app/pages/external-api/external-api-component.html (vu que nous utilisons le port 10000 pour la gateway) :

```
  <div *ngIf="hasApiError" class="alert alert-danger" role="alert">
    An error occured when trying to call the local API on port 10000. Ensure the local API is started using either `npm run dev` or `npm run
    server:api`.
  </div>

  <p class="lead">Ping an external API by clicking the button below.</p>

  <p>
    This will call a local API on port 10000 that would have been started if you
    run <code>npm run dev</code> (or <code>npm run server:api</code>). An access token is sent as part of the
    request's `Authorization` header and the API will validate it using the
    API's audience value.
  </p>
```

##### Ajout du front dans le docker-compose

L'application que vous avez récupéré contient déjà un Dockerfile.

Il suffit donc d'ajouter le service dans le fichier compose :

```
  # Front Angular
  banque-front:
    # Lancement service de gateway
    build:
      context: amc_front # Répertoire (dans répertoire courant) contenant le dockerfile
      dockerfile: Dockerfile
    image: banque-front:8.0 # Pour placer le TAG de version sur le nom de l'image
    ports:
      - "4200:4200" # Exposition port 4200 (à virer pour passer par Gateway)
    restart: "no"
    container_name: bnkfront
    depends_on:
      - banque-apigateway
    networks:
      - backend
    environment:
      WAIT_HOSTS: bnkapigateway:10000  # Attente demarrage services (attente 30s MAX)
```

Une fois le conteneur déployé, on peut, pour l'instant, accéder au front via le port 
dédié : 4200 : http://localhost:4200

Si vous avez des problèmes avec des images de profil 
(notamment celles venant de github) il faut modifier le fichier server.js
du front. Dans la partie directives de contentSecurityPolicy :

```
        'img-src': ["'self'", 'data:', '*.gravatar.com', 'avatars.githubusercontent.com'],
```



##### Ajout du front dans la gateway

Pour faire en sorte que l'application Angular hébergée par docker se trouve derrière 
la Gateway (il n'y aura donc plus de problème de CORS).

Il nous faut indiquer la redirection vers l'application :

```
        #Service Frontend Angular
        - id: amc_frontend
          uri: http://bnkfront:4200/
          predicates:
            # On matche tout ce qui commence par /ui
            - Path=/ui/**
          filters:
            - RewritePath=/ui/(?<segment>/?.*), /$\{segment}
          metadata:
            cors:
              allowedOrigins: '*'
              allowedMethods:
                - GET
              allowedHeaders: '*'
              maxAge: 30
```

Il nous faut, aussi, rediriger toutes les autres requêtes vers le front (pour gérer les fichiers css et autres
qui ne sont pas gérés par Angular)

```
        - id: catchall
          uri: http://bnkfront:4200/
          predicates:
            # On matche tout et on renvoie vers l'ui
            - Path=/**
          filters:
            - RewritePath=/ui/(?<segment>/?.*), /$\{segment}
          metadata:
            cors:
              allowedOrigins: '*'
              allowedMethods:
                - GET
              allowedHeaders: '*'
              maxAge: 30
```

Enfin, dans l'application Angular, il faut modifier le HREF pour indiquer à Angular que son URL commence par /ui.
Ca se fait dans le fichier app.module.ts qui se trouve dans src/app :

```
    {provide: APP_BASE_HREF, useValue: '/ui/'}
```

### Reference Documentation

For further reference, please consider the following sections:

* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.0.4/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.0.4/maven-plugin/reference/html/#build-image)
* [Eureka Discovery Client](https://docs.spring.io/spring-cloud-netflix/docs/current/reference/html/#service-discovery-eureka-clients)
* [Validation](https://docs.spring.io/spring-boot/docs/3.0.4/reference/htmlsingle/#io.validation)
* [OpenFeign](https://docs.spring.io/spring-cloud-openfeign/docs/current/reference/html/)
* [Spring Web](https://docs.spring.io/spring-boot/docs/3.0.4/reference/htmlsingle/#web)
* [Cloud Bootstrap](https://docs.spring.io/spring-cloud-commons/docs/current/reference/html/)

### Guides

The following guides illustrate how to use some features concretely:

* [Service Registration and Discovery with Eureka and Spring Cloud](https://spring.io/guides/gs/service-registration-and-discovery/)
* [Validation](https://spring.io/guides/gs/validating-form-input/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/rest/)

### Additional Links

These additional references should also help you:

* [Declarative REST calls with Spring Cloud OpenFeign sample](https://github.com/spring-cloud-samples/feign-eureka)


https://springbootlearning.medium.com/using-micrometer-to-trace-your-spring-boot-app-1fe6ff9982ae