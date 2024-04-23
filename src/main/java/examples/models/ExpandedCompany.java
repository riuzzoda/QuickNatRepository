package examples.models;


public class ExpandedCompany extends Company {

    // This field is not mapped to a column in the database
    // It is used to demonstrate how to handle extra fields
    private String extraFieldFromAnotherTable;

    public ExpandedCompany() {
    }

    // Getter for the 'descriptionExtraField' field
    public String getExtraFieldFromAnotherTable() {
        return extraFieldFromAnotherTable;
    }

    // Setter for the 'descriptionExtraField' field
    public void setExtraFieldFromAnotherTable(String extraFieldFromAnotherTable) {
        this.extraFieldFromAnotherTable = extraFieldFromAnotherTable;
    }
}
