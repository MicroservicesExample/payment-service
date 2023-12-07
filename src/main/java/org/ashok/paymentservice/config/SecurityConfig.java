package org.ashok.paymentservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoders;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache;

@EnableWebFluxSecurity
@Configuration
public class SecurityConfig {
	
	@Value( "${spring.security.oauth2.resourceserver.jwt.issuer-uri}") 
	private String issuerUri;	
	
	@Bean
	SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
		
		return 
				http
					.authorizeExchange(exchange ->
					      exchange.anyExchange().authenticated() //all requests require authentication
						)
					.oauth2ResourceServer(oauth2ResourceServer ->
						  oauth2ResourceServer.jwt(Customizer.withDefaults())		//jwt authentication			
						)
					.requestCache(requestCacheSpec ->
						  requestCacheSpec.requestCache(NoOpServerRequestCache.getInstance()) //No request session cache, stateless 
					    )
					.csrf(ServerHttpSecurity.CsrfSpec::disable) // no browser based direct client so csrf disabled
					.build();
				
				
	}
	
	@Bean
	public ReactiveJwtDecoder jwtDecoder() {
	    return ReactiveJwtDecoders.fromIssuerLocation(issuerUri);
	}
	

}
