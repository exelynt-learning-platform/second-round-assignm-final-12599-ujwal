package com.luv2code.ecommerce.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.InitializingBean;

@Configuration
@ConfigurationProperties(prefix = "stripe.api")
public class StripeProperties implements InitializingBean {
    private String secretKey;
    private String publishableKey;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("Stripe secret key is required. Set STRIPE_SECRET_KEY environment variable.");
        }
        if (publishableKey == null || publishableKey.trim().isEmpty()) {
            throw new IllegalStateException("Stripe publishable key is required. Set STRIPE_PUBLISHABLE_KEY environment variable.");
        }
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getPublishableKey() {
        return publishableKey;
    }

    public void setPublishableKey(String publishableKey) {
        this.publishableKey = publishableKey;
    }
}
