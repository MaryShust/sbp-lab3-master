package com.example.sbp.security.jaas;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringApplicationContextHolder implements ApplicationContextAware {
    private static SpringApplicationContextHolder instance;
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.applicationContext = context;
        register(this);
    }

    private static void register(SpringApplicationContextHolder holder) {
        instance = holder;
    }

    public static <T> T getBean(Class<T> beanClass) {
        SpringApplicationContextHolder holder = instance;
        ApplicationContext ctx = holder != null ? holder.applicationContext : null;
        if (ctx == null) {
            throw new IllegalStateException("ApplicationContext is not initialized");
        }
        return ctx.getBean(beanClass);
    }
}