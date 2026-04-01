package com.luv2code.ecommerce.config;

import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import javax.annotation.PostConstruct;

@Configuration
@ConditionalOnProperty(prefix = "stripe.api", name = "secret-key")
public class StripeConfiguration {

    @Autowired
    private StripeProperties stripeProperties;

    @PostConstruct
    public void init() {
        String secretKey = stripeProperties.getSecretKey();
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("Stripe configuration error: secret-key is required. Set STRIPE_SECRET_KEY environment variable.");
        }
        Stripe.apiKey = secretKey;
    }
}
