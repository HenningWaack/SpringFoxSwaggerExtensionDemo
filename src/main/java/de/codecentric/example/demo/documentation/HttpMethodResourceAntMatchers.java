package de.codecentric.example.demo.documentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
            try {
                httpSecurity.authorizeRequests().antMatchers(matcher.getMethod(), matcher.getAntPattern()).hasAnyRole(matcher.getRoles());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Could not antMatchers Matcher to httpSecurity. Matcher: " + matcher + ". Exceptzion:" + e);
                throw e;
            }
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
