package com.mental.security;

import com.mental.model.entity.User;
import com.mental.repository.UserRepository;
import com.mental.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwt;
    private final UserRepository repo;
    private final HandlerExceptionResolver resolver;

    // HandlerExceptionResolver ကို Inject လုပ်ပေးပါ
    public JwtFilter(JwtService jwt, UserRepository repo,
                     @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
        this.jwt = jwt;
        this.repo = repo;
        this.resolver = resolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain)
            throws IOException, ServletException {

        String header = req.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            try {
                String email = jwt.extractEmail(token);

                if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    User user = repo.findByEmail(email).orElse(null);

                    if (user != null) {
                        UserPrincipal principal = new UserPrincipal(user);

                        Authentication auth = new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );

                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception e) {
                // Filter ထဲတွင် ဖြစ်သော Exception ကို GlobalExceptionHandler သို့ လွှဲပေးပါသည်
                resolver.resolveException(req, res, null, e);
                return;
            }
        }

        chain.doFilter(req, res);
    }
}