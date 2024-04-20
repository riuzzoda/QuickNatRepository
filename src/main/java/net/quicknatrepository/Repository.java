/*
 * Copyright 2024 Dario Gagliano
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.quicknatrepository;


import javax.persistence.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.*;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Repository class, used to interact with the database.
 * @param <T> The type of the entity
 */
public class Repository<T> {

    // Constants

    public static String INSERT_INTO_RAW_QUERY = "INSERT INTO %s (%s) VALUES (%s);";
    public static String UPDATE_RAW_QUERY = "UPDATE %s SET %s WHERE %s = ?;";

    public static String SELECT_ALL_RAW_QUERY = "SELECT * FROM %s;";
    public static String SELECT_ALL_LIMIT_RAW_QUERY = "SELECT * FROM %s LIMIT %s;";
    public static String SELECT_ALL_LIMIT_OFFSET_RAW_QUERY = "SELECT * FROM %s LIMIT %s OFFSET %s;";
    public static String SELECT_ALL_ORDER_BY_RAW_QUERY = "SELECT * FROM %s ORDER BY %s;";
    public static String SELECT_ALL_ORDER_BY_LIMIT_RAW_QUERY = "SELECT * FROM %s ORDER BY %s LIMIT %s;";
    public static String SELECT_ALL_ORDER_BY_LIMIT_OFFSET_RAW_QUERY = "SELECT * FROM %s ORDER BY %s LIMIT %s OFFSET %s;";

    public static String SELECT_WHERE_RAW_QUERY = "SELECT * FROM %s WHERE %s;";
    public static String SELECT_WHERE_LIMIT_RAW_QUERY = "SELECT * FROM %s WHERE %s LIMIT %s;";
    public static String SELECT_WHERE_LIMIT_OFFSET_RAW_QUERY = "SELECT * FROM %s WHERE %s LIMIT %s OFFSET %s;";
    public static String SELECT_WHERE_ORDER_BY_RAW_QUERY = "SELECT * FROM %s WHERE %s ORDER BY %s;";
    public static String SELECT_WHERE_ORDER_BY_LIMIT_RAW_QUERY = "SELECT * FROM %s WHERE %s ORDER BY %s LIMIT %s;";
    public static String SELECT_WHERE_ORDER_BY_LIMIT_OFFSET_RAW_QUERY = "SELECT * FROM %s WHERE %s ORDER BY %s LIMIT %s OFFSET %s;";

    public static String SELECT_BY_KEY_RAW_QUERY = "SELECT * FROM %s WHERE %s = ?;";
    public static String SELECT_BY_KEY_LIMIT_RAW_QUERY = "SELECT * FROM %s WHERE %s = ? LIMIT %s;";
    public static String SELECT_BY_KEY_LIMIT_OFFSET_RAW_QUERY = "SELECT * FROM %s WHERE %s = ? LIMIT %s OFFSET %s;";
    public static String SELECT_BY_KEY_ORDER_BY_RAW_QUERY = "SELECT * FROM %s WHERE %s = ? ORDER BY %s;";
    public static String SELECT_BY_KEY_ORDER_BY_LIMIT_RAW_QUERY = "SELECT * FROM %s WHERE %s = ? ORDER BY %s LIMIT %s;";
    public static String SELECT_BY_KEY_ORDER_BY_LIMIT_OFFSET_RAW_QUERY = "SELECT * FROM %s WHERE %s = ? ORDER BY %s LIMIT %s OFFSET %s;";

    public static String SELECT_BY_KEYS_RAW_QUERY = "SELECT * FROM %s WHERE %s IN ( %s );";
    public static String SELECT_BY_KEYS_LIMIT_RAW_QUERY = "SELECT * FROM %s WHERE %s IN ( %s ) LIMIT %s;";
    public static String SELECT_BY_KEYS_LIMIT_OFFSET_RAW_QUERY = "SELECT * FROM %s WHERE %s IN ( %s ) LIMIT %s OFFSET %s;";
    public static String SELECT_BY_KEYS_ORDER_BY_RAW_QUERY = "SELECT * FROM %s WHERE %s IN ( %s ) ORDER BY %s;";
    public static String SELECT_BY_KEYS_ORDER_BY_LIMIT_RAW_QUERY = "SELECT * FROM %s WHERE %s IN ( %s ) ORDER BY %s LIMIT %s;";
    public static String SELECT_BY_KEYS_ORDER_BY_LIMIT_OFFSET_RAW_QUERY = "SELECT * FROM %s WHERE %s IN ( %s ) ORDER BY %s LIMIT %s OFFSET %s;";

    public static String DELETE_BY_KEY_RAW_QUERY = "DELETE FROM %s WHERE %s = ?;";
    public static String DELETE_ALL_QUERY = "DELETE FROM %s;";

    public static String SELECT_TOTAL_ROWS_RAW_QUERY = "SELECT COUNT(%s) as total FROM %s";
    public static String SELECT_TOTAL_ROWS_BY_KEY_RAW_QUERY = "SELECT COUNT(%s) as total FROM %s WHERE %s = ?;";

    /**
     * Generate a string with n mnemonic raw values
     * @param n The number of values
     * @return The string with the values
     */
    public static String generateMnemonicRawValues (int n){
        String temp = "?,".repeat(n);
        return new StringBuilder(temp).deleteCharAt(temp.length() -1).toString();
    }

    /**
     * Pageable class, used to paginate the results.
     */
    public static class Pageable {
        protected Long page;
        protected Long size;
        protected Optional<String> sortField;
        protected Optional<String> sortOrder;

        public Pageable(Long page, Long size) {
            this.page = page;
            this.size = size;
            this.sortField = Optional.empty();
            this.sortOrder = Optional.empty();
        }

        public Pageable(Long page, Long size, String sortField, String sortOrder) {
            this.page = page;
            this.size = size;
            this.sortField = sortField.isEmpty() ? Optional.empty() : Optional.of(sortField);
            this.sortOrder = sortOrder.isEmpty() ? Optional.empty() : Optional.of(sortOrder);
        }

        public void setPage(Long page) {
            this.page = page;
        }

        public void setSize(Long size) {
            this.size = size;
        }

        public void setSortField(String sortField) {
            this.sortField = sortField.isEmpty() ? Optional.empty() : Optional.of(sortField);
        }

        public void setSortOrder(String sortOrder) {
            this.sortOrder = sortOrder.isEmpty() ? Optional.empty() : Optional.of(sortOrder);
        }

    }

    private Class<T>    typeClass;                              // The type class of the entity
    private String      tableName;                              // The table name of the entity
    private boolean     hasPublicKey            = false;        // The entity has a public key
    private String      publicKeyColumnName     = null;         // The public key column name
    private int         publicKeyColumnIndex    = -1;           // The public key column index
    private Boolean     autoIncrement           = false;        // The public key is autoincrement

    private List<String> columnNames = new ArrayList<>();                                   // The column names
    private Map<String, String> fieldColumnNamesMap = new LinkedHashMap<>();                // The field column names map (field name -> column name)
    private Map<String, BiConsumer<T,Object>> fieldValueSettersMap = new LinkedHashMap<>(); // The field value setters map (column name -> setter)
    private Map<String, Function<T,Object>> fieldValueGetterMap = new LinkedHashMap<>();    // The field value getter map (column name -> getter)

    // Constructor

    /**
     * Constructor
     * @param typeClass The type class
     */
    public Repository(Class<T> typeClass) {
        this.init(typeClass);
    }

    // Utility methods

    /**
     * Get the column name from the field name
     * @param fieldName The field name
     * @return The column name
     */
    public String getFieldColumnName (String fieldName){
        return this.fieldColumnNamesMap.get(fieldName);
    }

    /**
     * Get the field name from the column name
     * @param columnName The column name
     * @return The field name
     */
    public String getColumnFieldName (String columnName){
        for (Map.Entry<String,String> entry : this.fieldColumnNamesMap.entrySet()){
            if (entry.getValue().equals(columnName)){
                return entry.getKey();
            }
        } return null;
    }

    /**
     * Get the public key column name
     * @return The public key column name
     */
    public String getPublicKeyColumnName() {
        return publicKeyColumnName;
    }

    /**
     * Get the table name
     * @return The table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Get the column names
     * @return The column names
     */
    public List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Get the column names by index
     * @param index The index
     * @return The column name
     */
    public String getColumnNameByIndex (int index){
        return this.columnNames.get(index);
    }

    /**
     * Get the column index by name
     * @param columnName The column name
     * @return The column index
     */
    public int getColumnIndexByName (String columnName){
        return this.columnNames.indexOf(columnName);
    }

    /**
     * Get the public key column index
     * @return The public key column index
     */
    public int getPublicKeyColumnIndex() {
        return publicKeyColumnIndex;
    }

    /**
     * Get the number of elements
     * @param connection The connection
     * @return The number of elements
     * @throws SQLException
     */
    public long getTotalElements(Connection connection) throws SQLException {
        String query = String.format(SELECT_TOTAL_ROWS_RAW_QUERY, this.publicKeyColumnName, this.tableName);
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()){
            return resultSet.getLong("total");
        }
        return 0;
    }

    /**
     * Get the number of elements by key
     * @param connection The connection
     * @param column The column
     * @param value The value
     * @return The number of elements
     * @throws SQLException
     */
    public long getTotalElementsBy(Connection connection, String column, Object value) throws SQLException {
        String query = String.format(SELECT_TOTAL_ROWS_BY_KEY_RAW_QUERY, this.publicKeyColumnName, this.tableName, column);
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setObject(1, value);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()){
            return resultSet.getLong("total");
        }
        return 0;
    }

    /**
     * Bind a field to a setter
     * @param columnName The column name
     * @param setter The setter
     */
    public void bindFieldToSetter(String columnName, BiConsumer<T,Object> setter){
        this.fieldValueSettersMap.put(columnName,setter);
    }

    /**
     * Bind a field to a getter
     * @param columnName The column name
     * @param getter The getter
     */
    public void bindFieldToGetter(String columnName, Function<T,Object> getter){
        this.fieldValueGetterMap.put(columnName,getter);
    }

    // Insert methods

    /**
     * Insert an entity
     * @param connection The connection
     * @param entity The entity
     * @return The number of affected rows
     * @throws SQLException
     */
    public long insert(Connection connection, T entity) throws SQLException {
        if (this.autoIncrement){
            List<String> temp = new ArrayList<String>(columnNames);
            temp.remove(this.publicKeyColumnIndex);
            return this.insert(connection,entity,temp);
        } else {
            return this.insert(connection,entity, columnNames);
        }
    }

    private long insert(Connection connection, T entity, List<String> columns) throws SQLException {
        String columnsList = String.join(",",columns);
        String columnRawValues = generateMnemonicRawValues(columns.size());
        String query = String.format(INSERT_INTO_RAW_QUERY, this.tableName,columnsList,columnRawValues);
        PreparedStatement statement = this.autoIncrement ?
                connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS) :
                connection.prepareStatement(query);
        this.populateStatement(statement,entity,columns);
        int affectedRows = statement.executeUpdate();
        if (affectedRows == 0) {
            throw new SQLException("Creation failed, no rows affected.");
        }
        if (this.autoIncrement){
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getLong(1);
                }
                else {
                    throw new SQLException("Creation failed, no ID obtained.");
                }
            }
        } return affectedRows;
    }

    // Update methods

    /**
     * Update an entity
     * @param connection The connection
     * @param entity The entity
     * @return The number of affected rows
     * @throws SQLException
     */
    public int update(Connection connection, T entity) throws SQLException {
        List<String> temp = new ArrayList<String>(this.columnNames);
        temp.remove(this.publicKeyColumnIndex);
        return update(connection,entity,temp);
    }

    private int update(Connection connection, T entity,List<String> columns) throws SQLException {
        StringBuilder builder = new StringBuilder();
        columns.forEach((String column)->{
            builder.append(column.concat(" = ?,"));
        });
        String updateBodyRawQuery = builder.deleteCharAt( builder.length() -1).toString();
        String query = String.format(UPDATE_RAW_QUERY, this.tableName,updateBodyRawQuery,this.publicKeyColumnName);
        PreparedStatement statement = connection.prepareStatement(query);
        int parameterIndex = populateStatement(statement,entity,columns);
        Object entityId = this.fieldValueGetterMap.get(this.publicKeyColumnName).apply(entity);
        statement.setObject(parameterIndex + 1, entityId);
        return statement.executeUpdate();
    }

    // Delete methods

    /**
     * Delete an entity by public key
     * @param connection The connection
     * @param publicKey The public key
     * @return True if the operation was successful
     * @throws SQLException
     */
    public boolean delete(Connection connection, Object publicKey) throws SQLException {
        String query = String.format(DELETE_BY_KEY_RAW_QUERY, this.tableName,this.publicKeyColumnName);
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, publicKey);
        return statement.execute();
    }

    /**
     * Delete all entities
     * @param connection The connection
     * @return True if the operation was successful
     * @throws SQLException
     */
    public boolean deleteAll(Connection connection) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(DELETE_ALL_QUERY);
        return statement.execute();
    }

    // Read methods

    /**
     * Read entities
     * @param connection The connection
     * @param pageable The pageable
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> read(Connection connection, Pageable pageable) throws SQLException {
        Long offset = pageable.page * pageable.size;
        if (pageable.sortField.isEmpty() || pageable.sortOrder.isEmpty()){
            return read(connection,pageable.size, offset);
        } else {
            String sortColumnName = getFieldColumnName(pageable.sortField.get());
            String desc = pageable.sortOrder.get().equals("desc") ? " DESC" : "";
            String orderByClause = sortColumnName.concat(desc);
            return read(connection, orderByClause, pageable.size, offset);
        }
    }

    /**
     * Read entities
     * @param connection The connection
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> read(Connection connection) throws SQLException {
        String query = String.format(SELECT_ALL_RAW_QUERY, this.tableName);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities
     * @param connection The connection
     * @param limit The limit (number of entities to read)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> read(Connection connection, Long limit) throws SQLException {
        String query = String.format(SELECT_ALL_LIMIT_RAW_QUERY, this.tableName, limit);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities
     * @param connection The connection
     * @param limit The limit (number of entities to read)
     * @param offset The offset (number of entities to skip)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> read(Connection connection, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_ALL_LIMIT_OFFSET_RAW_QUERY, this.tableName, limit, offset);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities
     * @param connection The connection
     * @param orderByClause The order by clause
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> read(Connection connection, String orderByClause) throws SQLException {
        String query = String.format(SELECT_ALL_ORDER_BY_RAW_QUERY, this.tableName, orderByClause);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities
     * @param connection The connection
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @param limit The limit (number of entities to read)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> read(Connection connection, String orderByClause, Long limit) throws SQLException {
        String query = String.format(SELECT_ALL_ORDER_BY_LIMIT_RAW_QUERY, orderByClause, this.tableName, limit);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities
     * @param connection The connection
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @param limit The limit (number of entities to read)
     * @param offset The offset (number of entities to skip)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> read(Connection connection, String orderByClause, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_ALL_ORDER_BY_LIMIT_OFFSET_RAW_QUERY, orderByClause, this.tableName, limit, offset);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    // Read where methods

    /**
     * Read entities where
     * @param connection The connection
     * @param whereClause The where clause, e.g. "name = 'John'"
     * @param pageable The pageable
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readWhere(Connection connection, String whereClause, Pageable pageable) throws SQLException {
        Long offset = pageable.page * pageable.size;
        if (pageable.sortField.isEmpty() || pageable.sortOrder.isEmpty()){
            return readWhere(connection, whereClause, pageable.size, offset);
        } else {
            String sortColumnName = getFieldColumnName(pageable.sortField.get());
            String desc = pageable.sortOrder.get().equals("desc") ? " DESC" : "";
            String orderByClause = sortColumnName.concat(desc);
            return readWhere(connection, whereClause, orderByClause, pageable.size, offset);
        }
    }

    /**
     * Read entities where
     * @param connection The connection
     * @param whereClause The where clause, e.g. "name = 'John'"
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readWhere(Connection connection, String whereClause) throws SQLException {
        String query = String.format(SELECT_WHERE_RAW_QUERY, this.tableName, whereClause);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities where
     * @param connection The connection
     * @param whereClause The where clause, e.g. "name = 'John'"
     * @param limit The limit (number of entities to read)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readWhere(Connection connection, String whereClause, Long limit) throws SQLException {
        String query = String.format(SELECT_WHERE_LIMIT_RAW_QUERY, this.tableName, whereClause, limit);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities where
     * @param connection The connection
     * @param whereClause The where clause, e.g. "name = 'John'"
     * @param limit The limit (number of entities to read)
     * @param offset The offset (number of entities to skip)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readWhere(Connection connection, String whereClause, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_WHERE_LIMIT_OFFSET_RAW_QUERY, this.tableName, whereClause, limit, offset);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities where
     * @param connection The connection
     * @param whereClause The where clause, e.g. "name = 'John'"
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readWhere(Connection connection, String whereClause, String orderByClause) throws SQLException {
        String query = String.format(SELECT_WHERE_ORDER_BY_RAW_QUERY, this.tableName, whereClause, orderByClause);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities where
     * @param connection The connection
     * @param whereClause The where clause, e.g. "name = 'John'"
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @param limit The limit (number of entities to read)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readWhere(Connection connection, String whereClause, String orderByClause, Long limit) throws SQLException {
        String query = String.format(SELECT_WHERE_ORDER_BY_LIMIT_RAW_QUERY, whereClause, orderByClause, this.tableName, limit);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities where
     * @param connection The connection
     * @param whereClause The where clause, e.g. "name = 'John'"
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @param limit The limit (number of entities to read)
     * @param offset The offset (number of entities to skip)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readWhere(Connection connection, String whereClause, String orderByClause, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_WHERE_ORDER_BY_LIMIT_OFFSET_RAW_QUERY, whereClause, orderByClause, this.tableName, limit, offset);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    // Read by methods

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param value The value
     * @param pageable The pageable
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, Object value, Pageable pageable) throws SQLException {
        Long offset = pageable.page * pageable.size;
        if (pageable.sortField.isEmpty() || pageable.sortOrder.isEmpty()){
            return readBy(connection, columnName, value, pageable.size, offset);
        } else {
            String sortColumnName = getFieldColumnName(pageable.sortField.get());
            String desc = pageable.sortOrder.get().equals("desc") ? " DESC" : "";
            String orderByClause = sortColumnName.concat(desc);
            return readBy(connection, columnName, value, orderByClause, pageable.size, offset);
        }
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param values The values
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, List<Object> values, Pageable pageable) throws SQLException {
        Long offset = pageable.page * pageable.size;
        if (pageable.sortField.isEmpty() || pageable.sortOrder.isEmpty()){
            return readBy(connection, columnName, values, pageable.size, offset);
        } else {
            String sortColumnName = getFieldColumnName(pageable.sortField.get());
            String desc = pageable.sortOrder.get().equals("desc") ? " DESC" : "";
            String orderByClause = sortColumnName.concat(desc);
            return readBy(connection, columnName, values, orderByClause, pageable.size, offset);
        }
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param value The value
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, Object value) throws SQLException {
        String query = String.format(SELECT_BY_KEY_RAW_QUERY, this.tableName, columnName);
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, value);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param value The value
     * @param limit The limit (number of entities to read)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, Object value, Long limit) throws SQLException {
        String query = String.format(SELECT_BY_KEY_LIMIT_RAW_QUERY, this.tableName, columnName, limit);
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, value);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param value The value
     * @param limit The limit (number of entities to read)
     * @param offset The offset (number of entities to skip)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, Object value, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_BY_KEY_LIMIT_OFFSET_RAW_QUERY, this.tableName, columnName, limit, offset);
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, value);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param value The value
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, Object value, String orderByClause) throws SQLException {
        String query = String.format(SELECT_BY_KEY_ORDER_BY_RAW_QUERY, this.tableName, columnName, orderByClause);
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, value);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param value The value
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @param limit The limit (number of entities to read)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, Object value, String orderByClause, Long limit) throws SQLException {
        String query = String.format(SELECT_BY_KEY_ORDER_BY_LIMIT_RAW_QUERY, this.tableName, columnName, orderByClause, limit);
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, value);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param value The value
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @param limit The limit (number of entities to read)
     * @param offset The offset (number of entities to skip)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, Object value, String orderByClause, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_BY_KEY_ORDER_BY_LIMIT_OFFSET_RAW_QUERY, this.tableName, columnName, orderByClause, limit, offset);
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, value);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param values The values
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, List<Object> values) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateMnemonicRawValues(values.size());
        String query = String.format(SELECT_BY_KEYS_RAW_QUERY, this.tableName, columnName, rawValues);
        PreparedStatement statement = connection.prepareStatement(query);
        this.populateStatement(statement,values);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param values The values
     * @param limit The limit (number of entities to read)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, List<Object> values, Long limit) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateMnemonicRawValues(values.size());
        String query = String.format(SELECT_BY_KEYS_LIMIT_RAW_QUERY, this.tableName, columnName, rawValues, limit);
        PreparedStatement statement = connection.prepareStatement(query);
        this.populateStatement(statement,values);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param values The values
     * @param limit The limit (number of entities to read)
     * @param offset The offset (number of entities to skip)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, List<Object> values, Long limit, Long offset) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateMnemonicRawValues(values.size());
        String query = String.format(SELECT_BY_KEYS_LIMIT_OFFSET_RAW_QUERY, this.tableName, columnName, rawValues, limit, offset);
        PreparedStatement statement = connection.prepareStatement(query);
        this.populateStatement(statement,values);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param values The values
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, List<Object> values, String orderByClause) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateMnemonicRawValues(values.size());
        String query = String.format(SELECT_BY_KEYS_ORDER_BY_RAW_QUERY, this.tableName, columnName, rawValues, orderByClause);
        PreparedStatement statement = connection.prepareStatement(query);
        this.populateStatement(statement,values);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param values The values
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @param limit The limit (number of entities to read)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, List<Object> values, String orderByClause, Long limit) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateMnemonicRawValues(values.size());
        String query = String.format(SELECT_BY_KEYS_ORDER_BY_LIMIT_RAW_QUERY, this.tableName, columnName, rawValues, orderByClause, limit);
        PreparedStatement statement = connection.prepareStatement(query);
        this.populateStatement(statement,values);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by
     * @param connection The connection
     * @param columnName The column name
     * @param values The values
     * @param orderByClause The order by clause, e.g. "name DESC"
     * @param limit The limit (number of entities to read)
     * @param offset The offset (number of entities to skip)
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readBy(Connection connection, String columnName, List<Object> values, String orderByClause, Long limit, Long offset) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateMnemonicRawValues(values.size());
        String query = String.format(SELECT_BY_KEYS_ORDER_BY_LIMIT_OFFSET_RAW_QUERY, this.tableName, columnName, rawValues, orderByClause, limit, offset);
        PreparedStatement statement = connection.prepareStatement(query);
        this.populateStatement(statement,values);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    // Read by query methods

    /**
     * Read entities by query
     * @param connection The connection
     * @param query The query, e.g. "SELECT * FROM table WHERE name = ?"
     * @param values The values for the query
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readByQuery(Connection connection,String query, Object...values) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        List<Object> valueList = Arrays.asList(values);
        this.populateStatement(statement,valueList);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by query
     * @param connection The connection
     * @param query The query, e.g. "SELECT * FROM table WHERE name = ?"
     * @param values The values for the query
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readByQuery(Connection connection,String query, List<Object> values) throws SQLException{
        PreparedStatement statement = connection.prepareStatement(query);
        this.populateStatement(statement,values);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities by query
     * @param connection The connection
     * @param query The query, e.g. "SELECT * FROM table WHERE name = ?"
     * @param consumer The consumer for the entity and the result set (for custom reading)
     * @param values The values for the query
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readByQuery(Connection connection, String query, BiConsumer<ResultSet,T> consumer, Object...values) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        List<Object> valueList = Arrays.asList(values);
        this.populateStatement(statement,valueList);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs,consumer);
    }

    /**
     * Read entities by query
     * @param connection The connection
     * @param query The query, e.g. "SELECT * FROM table WHERE name = ?"
     * @param consumer The consumer for the entity and the result set (for custom reading)
     * @param values The values for the query
     * @return The list of entities
     * @throws SQLException
     */
    public List<T> readByQuery(Connection connection, String query, BiConsumer<ResultSet,T> consumer, List<Object> values) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        this.populateStatement(statement,values);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs,consumer);
    }

    /**
     * Read entities by query
     * @param connection The connection
     * @param query The query, e.g. "SELECT * FROM table WHERE name = ?"
     * @param supplier The supplier for the entity that will be supply a new entity instance
     * @param consumer The consumer for the entity and the result set (for custom reading)
     * @param values The values for the query
     * @return The list of entities
     * @throws SQLException
     */
    public <C extends T> List<C> readByQuery(Connection connection, String query, Supplier<C> supplier,  BiConsumer<ResultSet,C> consumer, Object...values) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        List<Object> valueList = Arrays.asList(values);
        this.populateStatement(statement,valueList);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs,supplier,consumer);
    }

    /**
     * Read entities by query
     * @param connection The connection
     * @param query The query, e.g. "SELECT * FROM table WHERE name = ?"
     * @param supplier The supplier for the entity that will be supply a new entity instance
     * @param consumer The consumer for the entity and the result set (for custom reading)
     * @param values The values for the query
     * @return The list of entities
     * @throws SQLException
     */
    public <C extends T> List<C> readByQuery(Connection connection, String query, Supplier<C> supplier, BiConsumer<ResultSet,C> consumer, List<Object> values) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(query);
        this.populateStatement(statement,values);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs,supplier,consumer);
    }

    // Read by id method

    /**
     * Read an entity by id
     * @param connection The connection
     * @param value The id of the entity
     * @return The entity
     * @throws SQLException
     */
    public T readById(Connection connection, Object value) throws SQLException {
        List<T> results = readBy(connection, publicKeyColumnName, value);
        if (results.isEmpty()) return null;
        return results.get(0);
    }

    // Overrideable methods

    public T instantiateEntity()  {
        T temp = null;
        try {
            Constructor<T> constructor = typeClass.getDeclaredConstructor();
            temp = constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        } return temp;
    }

    public void populateEntity(ResultSet resultSet, T obj) throws SQLException {
        for (String columnName : columnNames) {
            Object rawValue = resultSet.getObject(columnName);
            this.fieldValueSettersMap.get(columnName).accept(obj, rawValue);
        }
    }

    // Private methods

    private int populateStatement (PreparedStatement statement, List<Object> values) throws SQLException {
        int i;
        for (i = 0; i < values.size(); i++) {
            statement.setObject(i+1,values.get(i));
        } return i;
    }

    private int populateStatement (PreparedStatement statement, T entity, List<String> columns) throws SQLException {
        int i;
        for (i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            Object value = fieldValueGetterMap.get(column).apply(entity);
            statement.setObject(i + 1,value);
        } return i;
    }

    private List<T> readResultSet(ResultSet rs) throws SQLException {
        ArrayList<T> results = new ArrayList<>();
        T temp = null;
        while (rs.next()) {
            temp = this.instantiateEntity();
            this.populateEntity(rs,temp);
            results.add(temp);
        } return results;
    }

    private List<T> readResultSet  (ResultSet resultSet,BiConsumer<ResultSet,T> consumer) throws SQLException {
        ArrayList<T> results = new ArrayList<>();
        T temp = null;
        while (resultSet.next()) {
            temp = this.instantiateEntity();
            this.populateEntity(resultSet,temp);
            consumer.accept(resultSet,temp);
            results.add(temp);
        } return results;
    }

    private <C extends T> List<C> readResultSet  (ResultSet resultSet, Supplier<C> supplier, BiConsumer<ResultSet,C> consumer) throws SQLException {
        ArrayList<C> results = new ArrayList<>();
        C temp = null;
        while (resultSet.next()) {
            temp = supplier.get();
            this.populateEntity(resultSet,temp);
            consumer.accept(resultSet,temp);
            results.add(temp);
        } return results;
    }

    private void init(Class <T> typeClass){
        this.typeClass = typeClass;
        if (typeClass.isAnnotationPresent(Table.class)){
            String annotationTableName = typeClass.getAnnotation(Table.class).name();
            this.tableName = !annotationTableName.isEmpty() ? annotationTableName : typeClass.getSimpleName();
        } else if (typeClass.isAnnotationPresent(Entity.class)){
            String annotationTableName = typeClass.getAnnotation(Entity.class).name();
            this.tableName = !annotationTableName.isEmpty() ? annotationTableName : typeClass.getSimpleName();
        }  else {
            this.tableName = typeClass.getSimpleName();
        }
        Field[] declaredFields = typeClass.getDeclaredFields();
        for (int i = 0; i < declaredFields.length; i++){
            this.initField(declaredFields[i],i);
        }
    }

    private void initField (Field field, int fieldIndex){
        if (field.isAnnotationPresent(Column.class)){
            String columnName = field.getAnnotation(Column.class).name().isEmpty() ?
                    field.getName() : field.getAnnotation(Column.class).name();
            columnNames.add(columnName);
            fieldColumnNamesMap.put(field.getName(),columnName);
            detectSetterAndGetter(field,columnName);
            if (field.isAnnotationPresent(Id.class)){ // isFirstClass &&
                this.hasPublicKey = true;
                this.publicKeyColumnName = columnName;
                this.publicKeyColumnIndex = fieldIndex;
                if (field.isAnnotationPresent(GeneratedValue.class) &&
                        field.getAnnotation(GeneratedValue.class).strategy().equals(GenerationType.AUTO)){
                    this.autoIncrement = true;
                }
            }
        }
    }

    private void detectSetterAndGetter (Field field, String columnName){

        field.setAccessible(true);

        if (field.getType().isEnum()){
            this.fieldValueSettersMap.put(columnName, (x, y) -> {
                try {
                    final Class<? extends Enum> enumType = (Class<? extends Enum>) field.getType();
                    final Enum<?> enumValue = Enum.valueOf(enumType, (String) y);
                    field.set(x, enumValue);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
            this.fieldValueGetterMap.put(columnName, (x) -> {
                try {
                    Object value = field.get(x);
                    return value.toString();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            });
        } else if (field.getType().equals(Date.class)){
            this.fieldValueSettersMap.put(columnName, (x, y) -> {
                try {
                    final Timestamp timestampValue = (Timestamp) y;
                    Date date = new Date(timestampValue.getTime());
                    field.set(x, date);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
            this.fieldValueGetterMap.put(columnName, (x) -> {
                try {
                    Object value = field.get(x);
                    return value;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            });
        } else {
            this.fieldValueSettersMap.put(columnName, (x, y) -> {
                try {
                    field.set(x, y);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
            this.fieldValueGetterMap.put(columnName, (x) -> {
                try {
                    Object value = field.get(x);
                    return value;
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            });
        }
    }
}
