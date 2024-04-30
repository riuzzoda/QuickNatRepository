package entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "companies")
public class Company {

    @Id
    private String id;

    @Column(name = "company_name")
    private String companyName;

    @Column
    private String city;

    public Company() {
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

    public void setCity(String city) {
        this.city = city;
    }
}
