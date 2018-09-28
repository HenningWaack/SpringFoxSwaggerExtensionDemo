package de.codecentric.example.demo.documentation;

import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.OperationBuilderPlugin;
import springfox.documentation.spi.service.contexts.OperationContext;
import springfox.documentation.spring.web.DescriptionResolver;
import springfox.documentation.swagger.common.SwaggerPluginSupport;

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
            if (methodAnnotation.isPresent() ) {
                String noteText = "Accessible by users having one of the following roles: ";
                if (matchersSecurityConfiguration != null) {
                    HttpMethodResourceAntMatchers allMatchers = matchersSecurityConfiguration.getMatchers();
                    for (HttpMethodResourceAntMatcher m : allMatchers.matcherList) {
                        Optional<RequestMapping> requestMappingOptional = context.findAnnotation(RequestMapping.class);
                        if (m.getMethod() == getHttpMethod(requestMappingOptional)) {
                            AntPathMatcher matcher = new AntPathMatcher();
                            String path = context.requestMappingPattern();
                            if (path == null) {
                                continue;
                            }
                            boolean matches = matcher.match(m.getAntPattern(), path);
                            if (matches) {
                                noteText += String.join(", ", m.getRoles());
                            }
                        }

                    }
                    context.operationBuilder().notes(descriptions.resolve(noteText));
                }
            }
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