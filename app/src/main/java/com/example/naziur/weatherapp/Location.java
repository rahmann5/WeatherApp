package com.example.naziur.weatherapp;

public class Location {

    private String countryCode, asciiName;

    public Location(String countryCode, String asciiName) {
        this.countryCode = countryCode;
        this.asciiName = asciiName;
    }

    public String getCountryCode() {
        return countryCode;
    }


    public String getAsciiName() {
        return asciiName;
    }

}
