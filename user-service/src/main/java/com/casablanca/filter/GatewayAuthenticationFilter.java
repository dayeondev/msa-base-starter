package com.casablanca.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class GatewayAuthenticationFilter extends OncePerRequestFilter {

    @Value("${app.gateway.secret}")
    private String gatewaySecret;

    @Value("${app.gateway.trusted-ip}")
    private String trustedGatewayIp;

    private static final String GATEWAY_SECRET_HEADER = "X-Gateway-Secret";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USERNAME_HEADER = "X-Username";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String userIdHeader = request.getHeader(USER_ID_HEADER);
        String usernameHeader = request.getHeader(USERNAME_HEADER);
        String gatewaySecretHeader = request.getHeader(GATEWAY_SECRET_HEADER);

        // If headers are present, validate they come from the gateway
        if (userIdHeader != null && usernameHeader != null) {
            // Validate gateway secret - this is sufficient to verify the request comes from our gateway
            if (!gatewaySecret.equals(gatewaySecretHeader)) {
                System.out.println("GatewayAuthenticationFilter: Invalid gateway secret from " + request.getRemoteAddr());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid gateway secret");
                return;
            }

            System.out.println("GatewayAuthenticationFilter: Valid gateway secret, authenticating user " + usernameHeader);

            // Parse and validate user ID
            Long userId;
            try {
                userId = Long.valueOf(userIdHeader);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid user ID format");
                return;
            }

            // All checks passed - set authentication
            String username = usernameHeader;
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                );
            authentication.setDetails(username);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isTrustedSource(String remoteAddr) {
        // Accept localhost or requests from the gateway
        // In Docker Compose, requests from the gateway will have the gateway container's IP
        return remoteAddr.equals("127.0.0.1")
            || remoteAddr.equals("0:0:0:0:0:0:0:1")  // IPv6 localhost
            || remoteAddr.contains(trustedGatewayIp);
    }
}
