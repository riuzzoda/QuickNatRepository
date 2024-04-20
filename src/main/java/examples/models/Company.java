package examples.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.util.UUID;

@Entity(name = "companies")
public class Company {

    @Id
    @Column
    private String id;

    @Column(name = "company_name")
    private String companyName;

    @Column
    private String city;

    // This field is not mapped to a column in the database
    // It is used to demonstrate how to handle extra fields
    private String descriptionExtraField;

    public Company() {
    }

    public Company(String companyName, String city) {
        this.id = UUID.randomUUID().toString();
        this.companyName = companyName;
        this.city = city;
    }

    public Company(String id, String companyName, String city) {
        this.id = id;
        this.companyName = companyName;
        this.city = city;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCity() {
        return city;
    }

    // Setter for the 'city' field that accepts a String as an argument
    // This is a typical setter method where the type of the argument matches the type of the field
    public void setCity(String city) {
        this.city = city;
    }

    // Overloaded setter for the 'city' field that accepts an Object as an argument
    // This setter is designed to be used with the 'setFieldSetter' method in the 'Repository' class
    // It allows you to pass a method reference directly to the 'setFieldSetter' method
    // The Object argument is cast to a String before being assigned to the 'city' field
    public void setCity(Object city) {
        this.city = (String)city;
    }


    // Getter for the 'descriptionExtraField' field
    public String getDescriptionExtraField() {
        return descriptionExtraField;
    }

    // Setter for the 'descriptionExtraField' field
    public void setDescriptionExtraField(String descriptionExtraField) {
        this.descriptionExtraField = descriptionExtraField;
    }

}
