package com.andrewlalis.perfin.model;

/**
 * A profile is essentially a complete set of data that the application can
 * operate on, sort of like a save file or user account. The profile contains
 * a set of accounts, transaction records, attached documents, historical data,
 * and more. A profile can be imported or exported easily from the application,
 * and can be encrypted for additional security. Each profile also has its own
 * settings.
 * Practically, each profile is stored as its own isolated database file, with
 * a name corresponding to the profile's name.
 */
public class Profile {
    private String name;

}
