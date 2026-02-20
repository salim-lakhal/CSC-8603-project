package com.insurance.identity.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;

@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    // Map the Spring-WS dispatcher to /ws/*
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {

        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        // Rewrite soap:address to match the actual server host/port at runtime
        servlet.setTransformWsdlLocations(true);

        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    // Bean name "identity" determines the WSDL URL: /ws/identity.wsdl
    @Bean(name = "identity")
    public DefaultWsdl11Definition defaultWsdl11Definition() {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setWsdl(new ClassPathResource("wsdl/identity.wsdl"));
        return definition;
    }
}
