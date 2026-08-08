package com.mental.security;

import com.mental.model.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal
        implements UserDetails {


    private User user;

    public UserPrincipal(User user){
        this.user=user;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = user.getRole().toString().trim();

        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        return List.of(new SimpleGrantedAuthority(roleName.toUpperCase()));
    }


    @Override
    public String getPassword(){
        return user.getPasswordHash();
    }



    @Override
    public String getUsername(){
        return user.getEmail();
    }

    public String getEmail(){
    return user.getEmail();
    }

    public Long getId(){
        return user.getId();
    }


}
