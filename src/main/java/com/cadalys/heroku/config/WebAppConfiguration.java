package com.cadalys.heroku.config;

import com.cadalys.heroku.security.AuthInterceptor;
import com.cadalys.heroku.security.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Created by dzmitrykalachou on 22.12.15.
 */
@Configuration
public class WebAppConfiguration extends WebMvcConfigurerAdapter {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor());
    }


    @Bean
    public AuthService authService(){
        return new AuthService();
    }

    @Bean
    public AuthInterceptor authInterceptor(){
        return new AuthInterceptor();
    }



}