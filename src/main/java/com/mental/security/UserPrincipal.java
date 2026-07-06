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

    // UserPrincipal.java ထဲတွင် ဤအတိုင်း အစားထိုးပြင်ဆင်ပါ
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // 1. Enum ဖြစ်စေ၊ String ဖြစ်စေ .name() သို့မဟုတ် .toString() သို့ သေချာပြောင်းလဲခြင်း
        String roleName = user.getRole().toString().trim();

        // 2. တကယ်လို့ database ထဲက role မှာ ROLE_ ဆိုတာ ကြိုမပါခဲ့ရင် အရှေ့ကနေ ပေါင်းပေးခြင်း
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        // 3. စာလုံးအကြီး/အသေး ပြဿနာမရှိစေရန် ToUpperCase ပြုလုပ်ခြင်း
        return List.of(new SimpleGrantedAuthority(roleName.toUpperCase()));
    }


    @Override
    public String getPassword(){
        return user.getPasswordHash();
    }


    @Override
    public String getUsername(){
        return user.getUsername();
    }

    public String getEmail(){
    return user.getEmail();
    }


}
