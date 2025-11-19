package com.example.smartcommunicator.model;

public class Contact {
    private String name;
    private String number; // Field for the phone number

    // Constructor to create a new Contact object
    public Contact(String name, String number) {
        this.name = name;
        this.number = number;
    }

    // Getter method for the contact's name
    public String getName() {
        return name;
    }

    // Setter method for the contact's name
    public void setName(String name) {
        this.name = name;
    }

    // --- THIS IS THE METHOD THAT FIXES THE ERROR ---
    // Getter method for the contact's number
    public String getNumber() {
        return number;
    }

    // Setter method for the contact's number
    public void setNumber(String number) {
        this.number = number;
    }
}
