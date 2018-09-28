package de.codecentric.example.demo;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

@Configuration
//@EnableGlobalMethodSecurity(prePostEnabled = true)
//@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String[] AUTH_WHITELIST_SPRINGFOX = {
            // -- swagger ui
            "/swagger-resources/**",
            "/swagger-ui.html",
            "/v2/api-docs",
            "/webjars/**"
    };

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        super.configure(http);

        http.csrf().disable();
        http.httpBasic();
        // auth for REST api
        http.authorizeRequests()
                .antMatchers(HttpMethod.POST, "/user").hasRole("admin")
                .antMatchers(HttpMethod.GET, "/user/*").hasAnyRole("admin", "user")
                .antMatchers(AUTH_WHITELIST_SPRINGFOX).hasAnyRole("user", "admin");
        // auth for swagger ui
        http.authorizeRequests()
                .antMatchers(AUTH_WHITELIST_SPRINGFOX).hasAnyRole("user", "admin");
        http.authorizeRequests().anyRequest().fullyAuthenticated();
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