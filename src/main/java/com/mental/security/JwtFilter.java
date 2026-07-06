package com.mental.security;

import com.mental.model.entity.User;
import com.mental.repository.UserRepository;
import com.mental.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter
        extends OncePerRequestFilter {


    private final JwtService jwt;


    private final UserRepository repo;



    @Override
    protected void doFilterInternal(
            HttpServletRequest req,
            HttpServletResponse res,
            FilterChain chain)

            throws IOException,ServletException{


        String header =
                req.getHeader("Authorization");


        if(header!=null &&
                header.startsWith("Bearer ")) {


            String token =
                    header.substring(7);


            String email =
                    jwt.extractEmail(token);


            User user =
                    repo.findByEmail(email)
                            .orElse(null);


            if(user!=null){


                UserPrincipal principal =
                        new UserPrincipal(user);



                Authentication auth =
                        new UsernamePasswordAuthenticationToken(
                                principal,
                                null,
                                principal.getAuthorities()
                        );



                SecurityContextHolder
                        .getContext()
                        .setAuthentication(auth);

            }

        }


        chain.doFilter(req,res);

    }

}