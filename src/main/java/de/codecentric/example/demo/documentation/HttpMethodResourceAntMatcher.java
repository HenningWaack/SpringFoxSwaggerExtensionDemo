package de.codecentric.example.demo.documentation;

import org.springframework.http.HttpMethod;

import java.util.Arrays;

public class HttpMethodResourceAntMatcher {
    HttpMethod method;
    String antPattern;
    String[] roles;

    public HttpMethodResourceAntMatcher(HttpMethod method, String antPattern) {
        this.method = method;
        this.antPattern = antPattern;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getAntPattern() {
        return antPattern;
    }

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String... roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "Matcher{" +
                "method=" + method +
                ", antPattern='" + antPattern + '\'' +
                ", roles=" + Arrays.toString(roles) +
                '}';
    }
}
