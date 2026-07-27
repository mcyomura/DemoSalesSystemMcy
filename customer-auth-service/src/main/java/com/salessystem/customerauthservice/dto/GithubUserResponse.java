package com.salessystem.customerauthservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing the user profile details fetched from GitHub API.
 */
public class GithubUserResponse {

    private Long id;
    private String login;
    private String name;
    private String email;
    @JsonProperty("avatar_url")
    private String avatarUrl;

    public GithubUserResponse() {
    }

    public GithubUserResponse(Long id, String login, String name, String email, String avatarUrl) {
        this.id = id;
        this.login = login;
        this.name = name;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}