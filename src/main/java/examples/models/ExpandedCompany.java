package examples.models;


public class ExpandedCompany extends Company {

    private String businessType;
    private String description;

    public ExpandedCompany() {
    }


    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
