package org.nana.api.security;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class CurrentUser {

    private String username;

    public String username() {
        return username;
    }

    void set(String username) {
        this.username = username;
    }
}
