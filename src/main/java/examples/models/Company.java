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

    @Column(name = "details_id")
    private String detailsId;

    private CompanyDetails details;


    // Default constructor
    public Company() {
    }

    // Constructor with companyName argument
    // This constructor is used to show how to create an instance of the object
    // when you can't use the default constructor because it requires a non-null argument
    public Company(String companyName) {
        this.companyName = companyName;
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

    public String getDetailsId() {
        return detailsId;
    }

    public void setDetailsId(String detailsId) {
        this.detailsId = detailsId;
    }

    public CompanyDetails getDetails() {
        return details;
    }

    public void setDetails(CompanyDetails details) {
        this.details = details;
    }



}
