package de.codecentric.example.demo.documentation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import java.util.ArrayList;
import java.util.List;

public class HttpMethodResourceAntMatchers {

    Logger logger = LoggerFactory.getLogger(HttpMethodResourceAntMatchers.class);

    List<HttpMethodResourceAntMatcher> matcherList = new ArrayList<>();

    public HttpMethodResourceAntMatchers() {

    }

    public HttpMethodResourceAntMatchers(HttpMethodResourceAntMatchers... matchersList) {
        for( HttpMethodResourceAntMatchers matchers : matchersList ) {
            this.matcherList.addAll(matchers.getMatcherList());

        }
    }



    public List<HttpMethodResourceAntMatcher> getMatcherList() {
        return matcherList;
    }

    /**
     * Applies all existing Matchers to the providd httpSecurity object as .authorizeRequest().antMatchers().hasAnyRole()
     * @param httpSecurity
     * @throws Exception
     */
    public void applySecurity(HttpSecurity httpSecurity) throws Exception {
        for (HttpMethodResourceAntMatcher m : this.matcherList) {
            try {
                httpSecurity.authorizeRequests().antMatchers(m.getMethod(), m.getAntPattern()).hasAnyRole(m.getRoles());

            } catch (Exception e) {
                e.printStackTrace();
                logger.error("Could not antMatchers Matcher to httpSecurity. Matcher: " + m + ". Exceptzion:" + e);
                throw e;
            }
        }
    }

    private HttpMethodResourceAntMatchers antMatchers(HttpMethodResourceAntMatcher matcher) {
        matcherList.add(matcher);
        return this;
    }

    public Role antMatchers(HttpMethod method, String antPattern) {
        HttpMethodResourceAntMatcher m = new HttpMethodResourceAntMatcher(method, antPattern);
        this.antMatchers(m);
        Role role = new Role(m, this);
        return role;
    }

    public class Role {

        HttpMethodResourceAntMatcher matcher;
        HttpMethodResourceAntMatchers matchers;

        public Role(HttpMethodResourceAntMatcher matcher, HttpMethodResourceAntMatchers matchers) {
            this.matcher = matcher;
            this.matchers = matchers;
        }

        public HttpMethodResourceAntMatchers hasRole(String role) {
            matcher.setRoles(role);
            return matchers;
        }

        public HttpMethodResourceAntMatchers hasAnyRole(String... roles) {
            matcher.setRoles(roles);
            return matchers;
        }
    }
}
