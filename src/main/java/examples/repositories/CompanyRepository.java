package examples.repositories;

import examples.models.Company;
import net.quicknatrepository.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;

public class CompanyRepository extends Repository<Company> {

    public CompanyRepository() {

        // You need to call the super constructor with the class of the object you are working with (Company in this case)
        super(Company.class);

        // You can add custom getters for the fields of the object using method references
        bindFieldToGetter("city", Company::getCity);

        // For the setter, you can't use the method reference directly because the setCity method in the Company class expects a String, not an Object
        // Therefore, you need to use a lambda expression that casts the Object to a String before passing it to the setCity method
        // bindFieldToSetter("city", (x, y)->{ x.setCity((String)y); });
        // However, if you define a setter method in the Company class that accepts an Object, you can use the method reference directly
        bindFieldToSetter("city", Company::setCity);

        // If you want to ignore a field, you can bind it to a lambda expression that does nothing
        // This is useful when you have fields that are set by the constructor and should not be updated
        bindFieldToSetter("company_name", (x, y)->{ /* Ignored */ });
    }

    // You can override the instantiateEntity method to return an instance of the object
    // This is useful when you have a custom constructor for the object
    @Override
    public Company instantiateEntity(ResultSet resultSet) throws SQLException {
        return new Company(resultSet.getString("company_name"));
    }

    // If you don't need to take any data from the ResultSet to create the object, you can override the instantiateEntity method without arguments
    // @Override
    // public Company instantiateEntity() { return new Company(/* arguments */); }
    // You can override only one of the two instantiateEntity methods. If you override both, the one with arguments will be used

}
