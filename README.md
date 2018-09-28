# SpringFoxSwaggerExtensionDemo

Eine populäre Method um REST APIs zu dokumentieren ist Swagger 2. Für Spring (Boot) Projekte bietet sich [Springfox](https://github.com/springfox/springfox) an. Springfox integriert sich recht nahtlos in ein Spring Projekt und stellt für konfigurierte REST Endpoints eine Browser-basierte [Spring-UI](https://swagger.io/tools/swagger-ui/) Representation zur Verfügung. Mittels Annotations im Code können umfangreiche Details und Informationen zur API-Dokumentation hinzugefügt werden, z.B. erweiterte Informationen zu http-Status Codes oder Beschreibungen zu einzelnen Feldern von Ressource-Modellen.

Der große Vorteil von Springfox's Ansatz ist die Verbindung von Code mit Annotations um daraus die REST-API Dokumentation zu generieren. Damit lässt sich ein altbekanntes Problem umgehen, nämlich dass Code und Dokumentation schnell auseinander laufen und damit die Dokumentation häufig nicht dem aktuellen Code entspricht.

Ein Punkt lässt sich allerdings nur schwer mir Springfox dokumentieren, und dass sind Spring-Security Konfigurationen. Spring-Security erlaubt es recht elegant Zugriffe auf die REST-API zu sichern, und stellt eine Brücke zwischen Authentifizierung und Authorisierung her. Der folgende Code-Snippet beispielsweise sichert den REST-Endpoint "/user" für GET- und POST-Zugriffe ab:

```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        super.configure(httpSecurity);

        httpSecurity.csrf().disable();
        httpSecurity.httpBasic();

        httpSecurity.authorizeRequests()
            .antMatchers(HttpMethod.POST, "/user").hasRole("admin")
            .antMatchers(HttpMethod.GET, "/user/*").hasAnyRole("admin", "user");

        httpSecurity.authorizeRequests().anyRequest().fullyAuthenticated();
    }
}
```

Die obige Spring-Security Konfiguration definiert, dass die Ressource "user" sowohl von Usern mit der Rolle "user" als auch mit der Rolle "admin" abgefragt (GET) werden darf, während der schreibende Zugriff (POST) auf dieselbe Ressource nur durch Nutzer mit der Rolle "admin" erlaubt ist.

Eine Spring-Security Java-Konfiguration ist relativ einfach zu lesen und zu verwalten. Ein Problem ist aber, dass diese Config von der eigentlichen Ressource, also dem REST Endpoint, losgelöst ist:

```java
@RestController
public class MyRestController {
    @RequestMapping(method = RequestMethod.GET, value = "/user/{username}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation("Get details of a user with the given username")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Details about the given user"),
            @ApiResponse(code = 401, message = "Cannot authenticate"),
            @ApiResponse(code = 403, message = "Not authorized to get details about the given user")
    })
    public UserDTO getExampleData(@Valid
                                  @ApiParam("Non-empty username") @PathVariable(name = "username", required = true) String username) {
        /*
            Get your user resource somehow and return
         */
    }

    @PostMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation("Create a new user or update an existing user (based on username)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "User was successfully updated"),
            @ApiResponse(code = 200, message = "User was successfully created"),
            @ApiResponse(code = 401, message = "Cannot authenticate"),
            @ApiResponse(code = 403, message = "Not authorized to create or update users")
    })
    public void createUser(@Valid @RequestBody UserDTO userDTO) {
        /*
            Save the user resource somehow
        */
    }
}
```

Der hier dargestellte REST-Controller wird durch Springfox API Annotations dokumentiert (Springfox ist sehr viel mächtiger als hier dargestellt, siehe [Springfox Reference Documentation](https://springfox.github.io/springfox/docs/current/)). Ein Blick auf die generierte Swagger-UI zeigt allerdings, dass die Spring-Security Konfiguration nicht in die Dokumentation einfließt. D.h. es ist nicht ersichtlich, welche Ressource durch welche Rollen aufrufbar ist:

Swagger-UI: ![Alt](docs/img/swagger-ui-standard.png_xxxx "Swagger-UI Standard")

Wie kann also Springfix so erweitert werden, dass die Spring-Security Konfiguration automatisch Teil er Swagger Dokumentation wird und damit eine höhere Kohäsion zwischen Implementierung und Dokumentation entseht?

Im Artikel [Springfox Swagger mit externem Markdown erweitern](https://blog.codecentric.de/2017/09/springfox-swagger-inkludieren-markdown/) hat Markus Höfer von codecentric bereits dargestellt, wie einfach Springfox mit Custom-Annotations erweitert werden kann. Darauf basiserend entstand der hier beschriebene Lösungsweg um Spring-Security Rollen in die Swagger-UI zu übernehmen.

Folgende grundlegende Schritte sind notwendig um eine Custom Annotation für Springfox Swagger-UI zu erstellen:

1. Erzeugen einer Custom Annotation
2. Implementierung eines Custom Springfox OperationBuilderPlugins
3. Custom Annotation an alle REST-Controller Endpoints hinzufügen

In unserem Fall kommt noch ein weiterer Schritt hinzu, nämlich die Spring Security Konfiguration "auslesbar" zu machen.

## Spring Security auslesbar machen

Um eine REST API mit Spring Security zu sichern kann die unten dargestellte API verwendet werden:

```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        ...
        httpSecurity.authorizeRequests()
            .antMatchers(HttpMethod.POST, "/user").hasRole("admin")
            .antMatchers(HttpMethod.GET, "/user/*").hasAnyRole("admin", "user");
        ...
    }
}
```

Ant-Matcher Pattern spezifizieren Http-Methoden und Http-Pfade, auf die nur die definierten Rollen zugreifen dürfen. Diese Konfiguration ist leider "write-only", d.h. die Klasse HttpSecurity (genauer gesagt die Klasse "org.springframework.security.config.annotation.web.configurers.ExpressionUrlAuthorizationConfigurer<HttpSecurity>.ExpressionInterceptUrlRegistry") lässt nicht zu, dass die Konfiguration mittels Ant-Matchers wieder ausgelesen wird. Das ist aber für unseren Use-Case notwendig, denn wir wollen genau diese Information in unserer Swagger Doku haben, müssen sie also auslesen können. Die vorgeschlagene Lösung verwaltet daher die Ant-Matcher Konfiguration in einer eigenständigen Klasse namens "HttpMethodResourceAntMatchers", die sowohl der HttpSecurity Configuration als auch der Swagger Doku über eine weitere Component zur Verfügung steht:

```java
public class HttpMethodResourceAntMatchers {
    Logger logger = LoggerFactory.getLogger(HttpMethodResourceAntMatchers.class);
    // list of all defined matchers
    List<HttpMethodResourceAntMatcher> matcherList = new ArrayList<>();
    /**
     * Applies all existing Matchers to the providd httpSecurity object as .authorizeRequest().antMatchers().hasAnyRole()
     * @param httpSecurity
     * @throws Exception
     */
    public void configure(org.springframework.security.config.annotation.web.builders.HttpSecurity httpSecurity) throws Exception {
        for (HttpMethodResourceAntMatcher matcher : this.matcherList) {
            httpSecurity.authorizeRequests().antMatchers(matcher.getMethod(), matcher.getAntPattern()).hasAnyRole(matcher.getRoles());
        }
    }
    /**
     * Add a new Matcher with HttpMethod and URL-Path
     * @param method
     * @param antPattern
     * @return
     */
    public Role antMatchers(org.springframework.http.HttpMethod method, String antPattern) {
        // create a new matcher
        HttpMethodResourceAntMatcher matcher = new HttpMethodResourceAntMatcher(method, antPattern);
        // add matcher to list of matchers
        this.matcherList.add(matcher);
        // return a Role wrapper object, which forces the user to add the role(s) to the matcher 
        Role role = new Role(matcher, this);
        return role;
    }
    /**
     * Helper class for a builder-like creation pattern
     */
    public class Role {
        HttpMethodResourceAntMatcher matcher;
        HttpMethodResourceAntMatchers matchers;
        
        public Role(HttpMethodResourceAntMatcher matcher, HttpMethodResourceAntMatchers matchers) {
            this.matcher = matcher;
            this.matchers = matchers;
        }
        /**
         * Define which role has access to the given resource identified by the Ant-Matcher
         * @param role
         * @return
         */
        public HttpMethodResourceAntMatchers hasRole(String role) {
            matcher.setRoles(role);
            return matchers;
        }
        /**
         * Add a list of roles which have access to the given resource identified by the Ant-Matcher
         * @param roles
         * @return
         */
        public HttpMethodResourceAntMatchers hasAnyRole(String... roles) {
            matcher.setRoles(roles);
            return matchers;
        }
    }
}
```

Die Klasse HttpMethodResourceAntMatchers imitiert mit den Methoden "antMatchers()" und "hasRole()" die äquivalenten HttpSecurity Methoden. Die hier refernezierte Klasse "HttpMethodResourceAntMatcher" ist ein einfacher Pojo mit den Membern "httpMethod", "antPattern" und "roles".

Die eigentliche Konfiguration der Matcher mit Hilfe der Klasse HttpMethodResourceAntMatchers findet durch die Custom Spring Component "MatchersSecurityConfiguration" statt:

```java
@Component
public class MatchersSecurityConfiguration {
    
    private HttpMethodResourceAntMatchers matchers;

    /**
     * Returns all http matchers
     * @return
     */
    public HttpMethodResourceAntMatchers getMatchers() {
        if (matchers == null) {
            matchers = new HttpMethodResourceAntMatchers();
            matchers.antMatchers(HttpMethod.POST, "/user").hasRole("admin")
                    .antMatchers(HttpMethod.GET, "/user/*").hasAnyRole("admin", "user");
        }
        return matchers;
    }
}
```

MatchersSecurityConfiguration wird sowohl in die Spring Security Config injeziert, als auch in die nachfolgende Swagger Hilfsklasse für unsere Custom Annotation.

Damit kann unsere Spring Security Config folgendermaßen angepasst werden:

```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    
    @Autowired
    private MatchersSecurityConfiguration matchersSecurityConfiguration;
    
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        super.configure(httpSecurity);

        httpSecurity.csrf().disable();
        httpSecurity.httpBasic();
        httpSecurity.authorizeRequests().antMatchers(AUTH_WHITELIST_SPRINGFOX).hasAnyRole("user", "admin");
        // add matchers for REST API to httpSecurity
        this.matchersSecurityConfiguration.getMatchers().configure(httpSecurity);
        
        httpSecurity.authorizeRequests().anyRequest().fullyAuthenticated();
    }
}
```

Jetzt haben wir die Spring Security Konfiguration angepasst. Nun muss die Swagger Annotation und die Springfox Erweiterung erstellt werden.

## Custom Annotation

Für unseren Use-Case erstellen wir die Custom API-Annotation "ApiRoleAccessNotes":

```java
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiRoleAccessNotes {
}
```

Die Annotation hat keinerlei Parameter, sie ist als Marker für unser OperationBuilderPlugin zu sehen.

## Custom Springfox OperationBuilderPlugin 

Damit wir die oben definierte Annotation verwenden können, muss das OperationBuilderPlugin Interface als Spring Component implementiert werden. Die apply-Methode wird vom Springfox DocumentationPluginsManager für jede REST-Resource im Classpath aufgerufen:

```java
@Component
@Order(SwaggerPluginSupport.SWAGGER_PLUGIN_ORDER)
public class OperationNotesResourcesReader implements OperationBuilderPlugin {
    private final DescriptionResolver descriptions;

    @Autowired
    private MatchersSecurityConfiguration matchersSecurityConfiguration;

    final static Logger logger = LoggerFactory.getLogger(OperationNotesResourcesReader.class);

    @Autowired
    public OperationNotesResourcesReader(DescriptionResolver descriptions) {
        this.descriptions = descriptions;
    }

    @Override
    public void apply(OperationContext context) {
        try {
            Optional<ApiRoleAccessNotes> methodAnnotation = context.findAnnotation(ApiRoleAccessNotes.class);
            if ( !methodAnnotation.isPresent() || this.matchersSecurityConfiguration == null) {
                // the REST Resource does not have the @ApiRoleAccessNotes annotion --> ignore
                return;
            }
            String apiRoleAccessNoteText = "Accessible by users having one of the following roles: ";
            HttpMethodResourceAntMatchers matchers = matchersSecurityConfiguration.getMatchers();
            // get all configured ant-matchers and try to match with the current REST resource
            for (HttpMethodResourceAntMatcher matcher : matchers.matcherList) {
                // get the RequestMapping annotion, which contains the http-method
                Optional<RequestMapping> requestMappingOptional = context.findAnnotation(RequestMapping.class);
                if (matcher.getMethod() == getHttpMethod(requestMappingOptional)) {
                    AntPathMatcher antPathMatcher = new AntPathMatcher();
                    String path = context.requestMappingPattern();
                    if (path == null) {
                        continue;
                    }
                    boolean matches = antPathMatcher.match(matcher.getAntPattern(), path);
                    if (matches) {
                        // we found a match for both http-method and URL-path, get the roles
                        apiRoleAccessNoteText += String.join(", ", matcher.getRoles());
                    }
                }

            }
            // add the note text to the Swagger-UI
            context.operationBuilder().notes(descriptions.resolve(apiRoleAccessNoteText));
        } catch (Exception e) {
            logger.error("Error when creating swagger documentation for security roles: " + e);
        }
    }

    private HttpMethod getHttpMethod(Optional<RequestMapping> requestMappingOptional) {
        if (!requestMappingOptional.isPresent()) return null;
        if (requestMappingOptional.get().method() == null || requestMappingOptional.get().method()[0] == null)
            return null;
        RequestMethod requestMethod = requestMappingOptional.get().method()[0];
        switch (requestMethod) {
            case GET:
                return HttpMethod.GET;
            case PUT:
                return HttpMethod.PUT;
            case POST:
                return HttpMethod.POST;
            case DELETE:
                return HttpMethod.DELETE;
        }
        return null;
    }

    @Override
    public boolean supports(DocumentationType delimiter) {
        return SwaggerPluginSupport.pluginDoesApply(delimiter);
    }
}
```
Innerhalb der Klasse prüfen wir, ob die aktuell von Springfox verarbeitete Ressource unsere "ApiRoleAccessNotes" Annotation besitzt. Ist dies der Fall, dann holen wir uns alle Spring-Security Matcher und prüfen, welche davon auf die die aktuelle Ressource passen, sowohl in Bezug auf die Http-Methode also auch auf den konfigurierten URL-Path. 

## Custom Annotion @ REST Controller

Im letzten Schritt müssen wir jetzt die Custom Annotation "@ApiRoleAccessNotes" zu allen REST-Resourcen hinzufügen, für die wir in der Swagger-UI die konfigurierten Rollen sehen wollen:

```java
@RestController
public class MyRestController {
    @RequestMapping(method = RequestMethod.GET, value = "/user/{username}", produces = APPLICATION_JSON_VALUE)
    @ApiOperation("Get details of a user with the given username")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Details about the given user"),
            @ApiResponse(code = 401, message = "Cannot authenticate"),
            @ApiResponse(code = 403, message = "Not authorized to get details about the given user")
    })
    @ApiRoleAccessNotes
    public UserDTO getExampleData(@Valid
                                  @ApiParam("Non-empty username") @PathVariable(name = "username", required = true) String username) {
        /*
            Get your user resource somehow and return
         */
    }

    @PostMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @ApiOperation("Create a new user or update an existing user (based on username)")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "User was successfully updated"),
            @ApiResponse(code = 200, message = "User was successfully created"),
            @ApiResponse(code = 401, message = "Cannot authenticate"),
            @ApiResponse(code = 403, message = "Not authorized to create or update users")
    })
    @ApiRoleAccessNotes
    public void createUser(@Valid @RequestBody UserDTO userDTO) {
        /*
            Save the user resource somehow
        */
    }
}
```

In der Swagger-UI sieht das Ganze dann folgendermaßen aus:

Swagger-UI: ![Alt](docs/img/swagger-ui-withroles.png_xxxx "Swagger-UI with Roles")

## Fazit

Mit ein paar wenigen Kniffen haben wir es geschafft, das unsere Dokumentation wieder etwas näher an den Source-Code gerückt ist. Der Source Code steht unter https://github.com/HenningWaack/SpringFoxSwaggerExtensionDemo zur Verfügung. Viel Spaß damit!