package examples;

import examples.models.Company;
import examples.models.ExpandedCompany;
import examples.repositories.CompanyRepository;
import net.quicknatrepository.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * This class, Example, serves as a reference for using classes that extend the Repository class.
 * It contains a series of examples that illustrate how to use the available methods in these classes.
 * Each method is accompanied by a usage example that shows how it can be used in a real context.
 * The examples include CRUD (Create, Read, Update, Delete) operations, as well as more complex operations such as
 * reading data with limits, offsets, and sorting, and executing custom queries.
 * Please refer to these examples to understand how to best use the classes that extend Repository.
 */
public class Examples {

    /**
     *  Consider using a connection manager to get a connection
     * @param cn
     * @throws SQLException
     */
    public static void examples(Connection cn) throws SQLException {


        // Create a new instance of the CompanyRepository class
        CompanyRepository repository = new CompanyRepository();


        // You can also use the Repository class directly without creating a custom repository
        Repository<Company> repository2 = new Repository<>(Company.class);


        // insert example

        Company newCompany = new Company("HelloWorldCompany", "Rome");
        long affectedRows = repository.insert(cn, newCompany);


        // update example

        newCompany.setCity("Genoa");
        repository.update(cn, newCompany);


        // delete example

        repository.delete(cn, newCompany.getId());


        // Read number of elements

        Long numberOfCompanies = repository.getTotalElements(cn);

        Long numberOfCompaniesInRome = repository.getTotalElementsBy(cn, "city", "Rome");


        // readById example

        Company company = repository.readById(cn, "24re23e32err");


        // read examples

        List<Company> companies = repository.read(cn);  // Read all companies

        List<Company> companiesLimit10 = repository.read(cn, 10L);  // Read all companies, limit 10 elements

        List<Company> companiesLimit10Offset5 = repository.read(cn, 10L, 5L);  // Read all companies, limit 10 elements, offset = 5

        List<Company> companiesOrderedByName = repository.read(cn, "company_name DESC");  // Read all companies ordered by name

        List<Company> companiesOrderedByNameLimit10 = repository.read(cn, "company_name DESC", 10L);  // Read all companies ordered by name, limit 10 elements

        List<Company> companiesOrderedByNameLimit10Offset5 = repository.read(cn, "company_name DESC", 10L, 5L);  // Read all companies ordered by name, limit 10 elements, offset = 5

        List<Company> companiesPage = repository.read(cn, new Repository.Pageable(0L,10L,"companyName", "desc"));  // Read fist page (10 elements for page) of companies sorted by name


        // readBy examples

        List<Company> companiesByCity = repository.readBy(cn, "city", "Rome");  // Read all companies by city

        List<Company> companiesByCityLimit10 = repository.readBy(cn, "city", "Rome", 10L);  // Read all companies by city limit 10 elements

        List<Company> companiesByCityLimit10Offset5 = repository.readBy(cn, "city", "Rome", 10L, 5L);  // Read all companies by city limit 10 elements, offset = 5

        List<Company> companiesByCityOrderedByName = repository.readBy(cn, "city", "Rome", "company_name DESC");  // Read all companies by city ordered by name

        List<Company> companiesByCityOrderedByNameLimit10 = repository.readBy(cn, "city", "Rome", "company_name DESC", 10L);  // Read all companies by city ordered by name, limit 10 elements

        List<Company> companiesByCityOrderedByNameLimit10Offset5 = repository.readBy(cn, "city", "Rome", "company_name DESC", 10L, 5L);  // Read all companies by city ordered by name, limit 10 elements, offset = 5

        List<Company> companiesByCityPage = repository.readBy(cn, "city", "Rome", new Repository.Pageable(0L,10L,"companyName", "desc"));  // Read fist page (10 elements for page) of companies by city sorted by name

        // readBy (multi) examples

        List<Company> companiesByCities = repository.readBy(cn, "city", Arrays.asList("Rome", "Genoa"));  // Read all companies by cities

        List<Company> companiesByCitiesLimit10 = repository.readBy(cn, "city", Arrays.asList("Rome", "Genoa"), 10L);  // Read all companies by cities limit 10 elements

        List<Company> companiesByCitiesLimit10Offset5 = repository.readBy(cn, "city", Arrays.asList("Rome", "Genoa"), 10L, 5L);  // Read all companies by cities limit 10 elements, offset = 5

        List<Company> companiesByCitiesOrderedByName = repository.readBy(cn, "city", Arrays.asList("Rome", "Genoa"), "company_name DESC");  // Read all companies by cities ordered by name

        List<Company> companiesByCitiesOrderedByNameLimit10 = repository.readBy(cn, "city", Arrays.asList("Rome", "Genoa"), "company_name DESC", 10L);  // Read all companies by cities ordered by name, limit 10 elements

        List<Company> companiesByCitiesOrderedByNameLimit10Offset5 = repository.readBy(cn, "city", Arrays.asList("Rome", "Genoa"), "company_name DESC", 10L, 5L);  // Read all companies by cities ordered by name, limit 10 elements, offset = 5

        List<Company> companiesByCitiesPage = repository.readBy(cn, "city", Arrays.asList("Rome", "Genoa"), new Repository.Pageable(0L,10L,"companyName", "desc"));  // Read fist page (10 elements for page) of companies by cities sorted by name


        // readWhere examples

        List<Company> companiesWhere = repository.readWhere(cn, "city = 'London' OR city = 'Manchester'"); // Read all companies where city is London or Manchester

        List<Company> companiesWhereLimit10 = repository.readWhere(cn, "city = 'London' OR city = 'Manchester'", 10L); // Read all companies where city is London or Manchester, limit 10 elements

        List<Company> companiesWhereLimit10Offset5 = repository.readWhere(cn, "city = 'London' OR city = 'Manchester'", 10L, 5L); // Read all companies where city is London or Manchester, limit 10 elements, offset = 5

        List<Company> companiesWhereOrderedByName = repository.readWhere(cn, "city = 'London' OR city = 'Manchester'", "company_name DESC"); // Read all companies where city is London or Manchester ordered by name

        List<Company> companiesWhereOrderedByNameLimit10 = repository.readWhere(cn, "city = 'London' OR city = 'Manchester'", "company_name DESC", 10L); // Read all companies where city is London or Manchester ordered by name, limit 10 elements

        List<Company> companiesWhereOrderedByNameLimit10Offset5 = repository.readWhere(cn, "city = 'London' OR city = 'Manchester'", "company_name DESC", 10L, 5L); // Read all companies where city is London or Manchester ordered by name, limit 10 elements, offset = 5

        List<Company> companiesWherePage = repository.readWhere(cn, "city = 'London' OR city = 'Manchester'", new Repository.Pageable(0L,10L,"companyName", "desc")); // Read fist page (10 elements for page) of companies where city is London or Manchester sorted by name


        // readByQuery examples

        List<Company> companyList1 = repository.readByQuery(cn, "SELECT * FROM companies WHERE city = ?;", "Rome");

        List<Company> companyList2 = repository.readByQuery(cn, "SELECT * FROM companies WHERE city = ? OR city = ?;", "Rome", "Venice");

        List<Company> companyList3 = repository.readByQuery(cn, "SELECT c.* FROM companies c INNER JOIN company_details cd ON c.id = cd.company_id WHERE c.city = ? AND cd.business_type = ?;", "Rome", "Industry");

        List<Company> companyList4 = repository.readByQuery(cn, "SELECT c.*, cd.description AS description FROM companies c INNER JOIN company_details cd ON c.id = cd.company_id WHERE c.city = ? AND cd.business_type = ?;", Examples::deserializeExtraFieldsForCompany, "Rome", "Industry");

        List<ExpandedCompany> companyList5 = repository.readByQuery(cn, "SELECT c.*, cd.business_type AS business_type, cd.description AS description FROM companies c INNER JOIN company_details cd ON c.id = cd.company_id WHERE c.city = ? AND cd.business_type = ?;", Examples::createNewExpandedCompanyObject, Examples::deserializeExpandedCompany, "Rome", "Industry");

    }

    public static ExpandedCompany createNewExpandedCompanyObject(){
        return new ExpandedCompany();
    }

    public static void deserializeExtraFieldsForCompany(ResultSet rs, Company company)  {
        try {
            company.setDescriptionExtraField(rs.getString("description"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deserializeExpandedCompany(ResultSet rs, ExpandedCompany expandedCompany)  {
        try {
            expandedCompany.setBusinessType(rs.getString("business_type"));
            expandedCompany.setDescription(rs.getString("description"));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}

