// User.java
package com.example.ecoinitiatives.model;

public class User {
    private String id;
    private String name;
    private String email;
    private String login;
    private String role; // "user" или "admin"

    public User() {}

    public User(String id, String name, String email, String login, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.login = login;
        this.role = role;
    }

    // Getters и Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}