package examples.repositories;

import examples.models.CompanyDetails;
import net.quicknatrepository.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CompanyDetailsRepository extends Repository<CompanyDetails> {

    public CompanyDetailsRepository() {
        super(CompanyDetails.class);
    }

    // You can override directly the populateEntity method to set the values of the object from the ResultSet.
    // This is useful when you have a custom object with fields that are not directly mapped to the ResultSet.
    @Override
    public void populateEntity(ResultSet resultSet, CompanyDetails obj) throws SQLException {
        obj.setId(resultSet.getString("id"));
        obj.setBusinessType(resultSet.getString("business_type"));
        obj.setDescription(resultSet.getString("description"));
    }
}
