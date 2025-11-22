package com.example.smartcommunicator.model;

import java.util.Objects; // We need to import this for the new methods

public class Contact {
    // --- UPDATED: These fields are now final and we have a lookupKey ---
    private final String lookupKey; // This is the permanent ID from the phone's database
    private final String name;
    private final String number;

    // --- UPDATED: The constructor now accepts the lookupKey ---
    public Contact(String lookupKey, String name, String number) {
        this.lookupKey = lookupKey;
        this.name = name;
        this.number = number;
    }

    // --- NEW: Getter for the unique lookupKey ---
    public String getLookupKey() {
        return lookupKey;
    }

    // --- The old getters are still here, but they are now for final fields ---
    public String getName() {
        return name;
    }

    public String getNumber() {
        return number;
    }

    // --- DELETED: We have removed the setter methods (setName, setNumber) ---
    // A contact's data should be read-only within the app.

    // --- NEW: We need equals() and hashCode() for List.removeAll() to work correctly ---
    // This ensures Java can identify contacts by their unique lookupKey.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contact contact = (Contact) o;
        return Objects.equals(lookupKey, contact.lookupKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lookupKey);
    }
}
