package de.codecentric.example.demo;

public class UserDTO {


    private String mail;
    private String password;
    private String lastName;
    private String name;
    private String address;
    private String username;

    public UserDTO() {
    }

    public UserDTO(String username, String mail, String password, String lastName, String name, String address) {
        this.username = username;
        this.mail = mail;
        this.password = password;
        this.lastName = lastName;
        this.name = name;
        this.address = address;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
