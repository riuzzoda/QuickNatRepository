# Quick Native Repository

## Overview

This Java project introduces a versatile Repository<T> class designed to simplify database CRUD operations for any Java entity annotated with Javax Persistence API. It provides standardized methods to insert, update, delete, and read objects from a MySQL database.

## Features

- **CRUD Operations**: Supports insert, update, delete, and read operations for entities.
- **Advanced Queries**: Methods for performing custom queries, including joins, filtering conditions, and pagination.
- **Sorting and Pagination**: Built-in features for sorting and paginating query results.
- **Multi-Condition Filtering**: Supports reading based on multiple conditions and filters.

## Prerequisites

To use this utility class, ensure you have:
- JDK 11 or higher.
- Maven dependencies for Javax Persistence API and the MySQL JDBC driver.
- MySQL Database

## Setup

Include the following dependency in your `pom.xml` if you are using Maven:

```xml
<dependency>
    <groupId>net.quicknatrepository</groupId>
    <artifactId>quicknat-repository</artifactId>
    <version>1.0.8</version>
</dependency>
```

If you are using Gradle, add the following dependency:

```gradle
implementation group: 'net.quicknatrepository', name: 'quicknat-repository', version: '1.0.8'
```

## Usage

### Basic Setup

Extend the `Repository<T>` class for each of your entities, specifying the entity type:

```java
public class CompanyRepository extends Repository<Company> {
    public CompanyRepository() {
        super(Company.class);
    }
}
```

Make sure your entity class is annotated with JPA annotations:

```java
@Table(name = "companies")
public class Company {

    @Id
    private String id;

    @Column(name = "company_name")
    private String companyName;

    @Column
    private String city;
    
    public Company() {
        // Default constructor
    }
    
    ...
}
```

Create an instance of the repository class:

```java
CompanyRepository repository = new CompanyRepository();
```

You can also use the Repository class directly without creating a custom repository:

```java
Repository<Company> repository = new Repository<>(Company.class);
```

You can specify custom field getters and setters for the entity:

```java
public class CompanyRepository extends Repository<Company> {
    public CompanyRepository() {
        super(Company.class);
        
        bindFieldToGetter("city", Company::getCity);

        // If the setter method does not accept an Object type and instead expects a specific type (e.g., String),
        // use a lambda expression to cast the Object to the required type before passing it to the setter method.
        // Example: bindFieldToSetter("city", (x, y)->{ x.setCity((String)y); });
        bindFieldToSetter("city", Company::setCity);
    }
}
```

Or you can override the `populateEntity` method to manually set the entity fields:

```java
public class CompanyRepository extends Repository<Company> {
    
    public CompanyRepository() {
        super(Company.class);
    }
    
    @Override
    public void populateEntity(ResultSet resultSet, Company obj) throws SQLException {
        obj.setId(resultSet.getString("id"));
        obj.setCompanyName(resultSet.getString("company_name"));
        obj.setCity(resultSet.getString("city"));
        ...
    }
}
```

You can also override the `instantiateEntity` method to manually create a new instance of the entity:

```java
public class CompanyRepository extends Repository<Company> {
    public CompanyRepository() {
        super(Company.class);
    }

    @Override
    public Company instantiateEntity() {
        return new Company();
    }
    
    ...
}
```

This is useful when the entity class has a non-default constructor or requires additional setup before being used.

### CRUD Operations

Here are some examples of how to use the repository class for CRUD operations:

#### Create
```java
CompanyRepository repository = new CompanyRepository();
Company newCompany = new Company("HelloWorldCompany", "Rome");

long affectedRows = repository.insert(connection, newCompany);
```

#### Read

Fetch a single entity by its unique identifier:

```java
Company company = repository.readById(connection, "a03a3812-063b-4df9-a945-d87d4abd6d77");
```

Retrieve all entities from the database:

```java
List<Company> companies = repository.read(connection);
```

Filter entities based on a specific attribute:

```java
List<Company> companiesInRome = repository.readBy(connection, "city", "Rome");
```

Fetch entities based on multiple attribute values:

```java
List<Company> companiesInGenoaOrTurin = repository.readBy(connection, "city", Arrays.asList("Genoa", "Turin"));
```

Use a custom condition to filter entities:

```java
List<Company> companiesRomeOrMilan = repository.readWhere(connection, "city = 'Rome' OR city = 'Milan'");
```

Sort and paginate the results:

```java
List<Company> companies = repository.readBy(connection, "city", "Rome", "company_name DESC", 10L, 0L); // (..., orderByClause, limit, offset)
```

Sort and paginate the results using a `Pageable` object:

```java
Repository.Pageable pageable = new Repository.Pageable(0L, 10L, "companyName", "desc"); // (page, size, sort, order)
List<Company> companies = repository.readBy(connection, "city", "Rome", pageable);
```

#### Update
```java
newCompany.setCity("Genoa");
repository.update(connection, newCompany);
```

#### Delete
```java
repository.delete(connection, newCompany);
```

#### Count Operations

Get the total number of entities in the database:

```java
Long numberOfCompanies = repository.getTotalElements(cn);
```

Get the total number of entities based on a specific condition:

```java
Long numberOfCompaniesInRome = repository.getTotalElementsBy(cn, "city", "Rome");
```

### Advanced Queries

For more complex queries, such as joins or pagination:

```java
List<Company> companies = repository.readByQuery(connection, "SELECT * FROM companies WHERE city = ?;", "Rome");
```

### Other Examples

For more detailed examples, please refer to the Examples.java file located within the `examples`  package of the project.


