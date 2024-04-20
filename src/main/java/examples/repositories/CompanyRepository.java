package examples.repositories;

import examples.models.Company;
import net.quicknatrepository.Repository;

public class CompanyRepository extends Repository<Company> {

    public CompanyRepository() {
        super(Company.class);

        // You can add custom getters for the fields of the object using method references
        bindFieldToGetter("city", Company::getCity);

        // For the setter, you can't use the method reference directly because the setCity method in the Company class expects a String, not an Object
        // Therefore, you need to use a lambda expression that casts the Object to a String before passing it to the setCity method
        // bindFieldToSetter("city", (x, y)->{ x.setCity((String)y); });
        // However, if you define a setter method in the Company class that accepts an Object, you can use the method reference directly
        bindFieldToSetter("city", Company::setCity);
    }

    // You can override the instantiateEntity method to return an instance of the object
    @Override
    public Company instantiateEntity() {
        return new Company();
    }
}
