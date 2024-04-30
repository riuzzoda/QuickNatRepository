package net.quicknatrepository;

import entities.Company;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import org.h2.jdbcx.JdbcConnectionPool;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class RepositoryTest {

    private static String createCompaniesTableQuery = "CREATE TABLE companies (" +
            "id VARCHAR(5) NOT NULL UNIQUE, " +
            "company_name VARCHAR(36) NOT NULL, " +
            "city VARCHAR(45) NOT NULL, " +
            "PRIMARY KEY (id));";

    private static String populateCompaniesTableQuery = "INSERT INTO companies (id, company_name, city) VALUES " +
            "('co001', 'Veloxia Technologies', 'Genoa')," +
            "('co002', 'Zephyr Dynamics', 'Genoa')," +
            "('co003', 'EchoSafe Security', 'Genoa')," +
            "('co004', 'Solstice Renewables', 'Genoa')," +
            "('co005', 'Vortex Gaming', 'Milan')," +
            "('co006', 'Aether Innovations', 'Milan')," +
            "('co007', 'BluePeak Logistics', 'Milan')," +
            "('co008', 'Prisma Design Co', 'Rome')," +
            "('co009', 'Lumina Textiles', 'Rome')," +
            "('co010', 'Celestia Entertainment', 'Turin')," +
            "('co011', 'Mirage Media Studios', 'Turin')," +
            "('co012', 'PyroTech Electronics', 'Venice');";

    private static DataSource dataSource;

    @BeforeAll
    public static void setUp() throws SQLException {
        dataSource = JdbcConnectionPool.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "user", "pass");
        try (Connection conn = dataSource.getConnection()) {
            conn.createStatement().execute(createCompaniesTableQuery);
            conn.createStatement().execute(populateCompaniesTableQuery);
        }
    }

    @Test
    public void testCRUD() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {

                Repository<Company> repo = new Repository<>(Company.class);

                Company company = new Company("co013","New Codes", "Florence");
                long affectedRows = repo.insert(conn, company);
                assertEquals(1, affectedRows);

                Company savedCompany = repo.readById(conn, "co013");
                assertEquals(company.getId(), savedCompany.getId());
                assertEquals(company.getCompanyName(), savedCompany.getCompanyName());
                assertEquals(company.getCity(), savedCompany.getCity());

                savedCompany.setCity("Naples");
                repo.update(conn, savedCompany);
                Company updatedCompany = repo.readById(conn, "co013");
                assertEquals("Naples", updatedCompany.getCity());

                repo.delete(conn, "co013");

                List<Company> companies = repo.readBy(conn, "id", "co013");
                assertEquals(0, companies.size());

            } finally {
                conn.rollback();
            }
        }
    }

    @Test
    public void testRead() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {

                Repository<Company> repo = new Repository<>(Company.class);

                List<Company> results = repo.read(conn);
                assertEquals(12, results.size());

                results = repo.read(conn, 5L);
                assertEquals(5, results.size());

                results = repo.read(conn, 5L, 10L);
                assertEquals(2, results.size());

                results = repo.read(conn, "company_name");
                assertEquals(12, results.size());
                assertEquals("Aether Innovations", results.get(0).getCompanyName());

                results = repo.read(conn, "company_name", 5L);
                assertEquals(5, results.size());
                assertEquals("Aether Innovations", results.get(0).getCompanyName());

                results = repo.read(conn, "company_name", 5L, 1L);
                assertEquals(5, results.size());
                assertEquals("BluePeak Logistics", results.get(0).getCompanyName());

                results = repo.read(conn, new Repository.Pageable(0L, 3L, "companyName", "desc"));
                assertEquals(3, results.size());
                assertEquals("Zephyr Dynamics", results.get(0).getCompanyName());

            } finally {
                conn.rollback();
            }
        }
    }

    @Test
    public void testReadBy() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // inizia la transazione
            try {

                Repository<Company> repo = new Repository<>(Company.class);

                List<Company> results = repo.readBy(conn, "city", "Milan");
                assertEquals(3, results.size());

                results = repo.readBy(conn, "city", "Milan", 2L);
                assertEquals(2, results.size());

                results = repo.readBy(conn, "city", "Milan", 3L, 1L);
                assertEquals(2, results.size());

                results = repo.readBy(conn, "city", "Milan", "company_name");
                assertEquals(3, results.size());
                assertEquals("Aether Innovations", results.get(0).getCompanyName());

                results = repo.readBy(conn, "city", "Milan", "company_name",2L);
                assertEquals(2, results.size());
                assertEquals("Aether Innovations", results.get(0).getCompanyName());

                results = repo.readBy(conn, "city", "Milan", "company_name",3L, 1L);
                assertEquals(2, results.size());
                assertEquals("BluePeak Logistics", results.get(0).getCompanyName());

                results = repo.readBy(conn, "city", "Milan", new Repository.Pageable(0L, 2L, "companyName", "desc"));
                assertEquals(2, results.size());
                assertEquals("Vortex Gaming", results.get(0).getCompanyName());

            } finally {
                conn.rollback();
            }
        }
    }

    @Test
    public void testReadByMulti() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // inizia la transazione
            try {

                Repository<Company> repo = new Repository<>(Company.class);

                List<Company> results = repo.readBy(conn, "city", Arrays.asList("Turin", "Venice"));
                assertEquals(3, results.size());

                results = repo.readBy(conn, "city", Arrays.asList("Turin", "Venice"), 2L);
                assertEquals(2, results.size());

                results = repo.readBy(conn, "city", Arrays.asList("Turin", "Venice"), 3L, 1L);
                assertEquals(2, results.size());

                results = repo.readBy(conn, "city", Arrays.asList("Turin", "Venice"), "company_name");
                assertEquals(3, results.size());
                assertEquals("Celestia Entertainment", results.get(0).getCompanyName());

                results = repo.readBy(conn, "city", Arrays.asList("Turin", "Venice"), "company_name",2L);
                assertEquals(2, results.size());
                assertEquals("Celestia Entertainment", results.get(0).getCompanyName());

                results = repo.readBy(conn, "city", Arrays.asList("Turin", "Venice"), "company_name",3L, 1L);
                assertEquals(2, results.size());
                assertEquals("Mirage Media Studios", results.get(0).getCompanyName());

                results = repo.readBy(conn, "city", Arrays.asList("Turin", "Venice"), new Repository.Pageable(0L, 2L, "companyName", "desc"));
                assertEquals(2, results.size());
                assertEquals("PyroTech Electronics", results.get(0).getCompanyName());

            } finally {
                conn.rollback();
            }
        }
    }

    @Test
    public void testReadWhere() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false); // inizia la transazione
            try {

                Repository<Company> repo = new Repository<>(Company.class);

                List<Company> results = repo.readWhere(conn, "city = 'Milan'");
                assertEquals(3, results.size());

                results = repo.readWhere(conn, "city = 'Milan'", 2L);
                assertEquals(2, results.size());

                results = repo.readWhere(conn, "city = 'Milan'", 3L, 1L);
                assertEquals(2, results.size());

                results = repo.readWhere(conn, "city = 'Milan'", "company_name");
                assertEquals(3, results.size());
                assertEquals("Aether Innovations", results.get(0).getCompanyName());

                results = repo.readWhere(conn, "city = 'Milan'", "company_name",2L);
                assertEquals(2, results.size());
                assertEquals("Aether Innovations", results.get(0).getCompanyName());

                results = repo.readWhere(conn, "city = 'Milan'", "company_name",3L, 1L);
                assertEquals(2, results.size());
                assertEquals("BluePeak Logistics", results.get(0).getCompanyName());

                results = repo.readWhere(conn, "city = 'Milan'", new Repository.Pageable(0L, 2L, "companyName", "desc"));
                assertEquals(2, results.size());
                assertEquals("Vortex Gaming", results.get(0).getCompanyName());

            } finally {
                conn.rollback();
            }
        }
    }
}
