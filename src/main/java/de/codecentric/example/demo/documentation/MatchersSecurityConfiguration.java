package de.codecentric.example.demo.documentation;


import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

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
