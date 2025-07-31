package com.sacco.banking.security;

import com.sacco.banking.entity.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

//@Getter
//@AllArgsConstructor
public class UserPrincipal extends org.springframework.security.core.userdetails.User {

    private final Member member;

    public UserPrincipal(Member member) {
        super(member.getMemberNumber(), member.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        this.member = member;
    }

    public String getMemberNumber() {
        return member.getMemberNumber();
    }

    public Member getMember() {
        return member;
    }
}
