package de.codecentric.example.demo;

import de.codecentric.example.demo.documentation.MatchersSecurityConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private MatchersSecurityConfiguration matchersSecurityConfiguration;

    private static final String[] AUTH_WHITELIST_SPRINGFOX = {
            // -- swagger ui
            "/swagger-resources/**",
            "/swagger-ui.html",
            "/v2/api-docs",
            "/webjars/**"
    };

    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
        super.configure(httpSecurity);

        httpSecurity.csrf().disable();
        httpSecurity.httpBasic();
        // auth for REST api
        httpSecurity.authorizeRequests()
                //.antMatchers(HttpMethod.POST, "/user").hasRole("admin")
                //.antMatchers(HttpMethod.GET, "/user/*").hasAnyRole("admin", "user")
                .antMatchers(AUTH_WHITELIST_SPRINGFOX).hasAnyRole("user", "admin");

        matchersSecurityConfiguration.getMatchers().configure(httpSecurity);

        httpSecurity.authorizeRequests().anyRequest().fullyAuthenticated();
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication()
                .withUser("user").password("userpw").roles("user")
                .and()
                .withUser("admin").password("adminpw").roles("user", "admin");
    }

    @SuppressWarnings("deprecation")
    @Bean
    public static NoOpPasswordEncoder passwordEncoder() {
        return (NoOpPasswordEncoder) NoOpPasswordEncoder.getInstance();
    }
}