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
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Repository class, used to interact with the database.
 * @param <T> The type of the entity
 */
public class Repository<T> {

    // Constants

    private final static String INSERT_INTO_RAW_QUERY = "INSERT INTO %s (%s) VALUES (%s);";
    private final static String UPDATE_RAW_QUERY = "UPDATE %s SET %s WHERE %s = ?;";

    private final static String SELECT_ALL_RAW_QUERY = "SELECT %s FROM %s;";
    private final static String SELECT_ALL_LIMIT_RAW_QUERY = "SELECT %s FROM %s LIMIT %s;";
    private final static String SELECT_ALL_LIMIT_OFFSET_RAW_QUERY = "SELECT %s FROM %s LIMIT %s OFFSET %s;";
    private final static String SELECT_ALL_ORDER_BY_RAW_QUERY = "SELECT %s FROM %s ORDER BY %s;";
    private final static String SELECT_ALL_ORDER_BY_LIMIT_RAW_QUERY = "SELECT %s FROM %s ORDER BY %s LIMIT %s;";
    private final static String SELECT_ALL_ORDER_BY_LIMIT_OFFSET_RAW_QUERY = "SELECT %s FROM %s ORDER BY %s LIMIT %s OFFSET %s;";

    private final static String SELECT_WHERE_RAW_QUERY = "SELECT %s FROM %s WHERE %s;";
    private final static String SELECT_WHERE_LIMIT_RAW_QUERY = "SELECT %s FROM %s WHERE %s LIMIT %s;";
    private final static String SELECT_WHERE_LIMIT_OFFSET_RAW_QUERY = "SELECT %s FROM %s WHERE %s LIMIT %s OFFSET %s;";
    private final static String SELECT_WHERE_ORDER_BY_RAW_QUERY = "SELECT %s FROM %s WHERE %s ORDER BY %s;";
    private final static String SELECT_WHERE_ORDER_BY_LIMIT_RAW_QUERY = "SELECT %s FROM %s WHERE %s ORDER BY %s LIMIT %s;";
    private final static String SELECT_WHERE_ORDER_BY_LIMIT_OFFSET_RAW_QUERY = "SELECT %s FROM %s WHERE %s ORDER BY %s LIMIT %s OFFSET %s;";

    private final static String SELECT_BY_KEY_RAW_QUERY = "SELECT %s FROM %s WHERE %s = ?;";
    private final static String SELECT_BY_KEY_LIMIT_RAW_QUERY = "SELECT %s FROM %s WHERE %s = ? LIMIT %s;";
    private final static String SELECT_BY_KEY_LIMIT_OFFSET_RAW_QUERY = "SELECT %s FROM %s WHERE %s = ? LIMIT %s OFFSET %s;";
    private final static String SELECT_BY_KEY_ORDER_BY_RAW_QUERY = "SELECT %s FROM %s WHERE %s = ? ORDER BY %s;";
    private final static String SELECT_BY_KEY_ORDER_BY_LIMIT_RAW_QUERY = "SELECT %s FROM %s WHERE %s = ? ORDER BY %s LIMIT %s;";
    private final static String SELECT_BY_KEY_ORDER_BY_LIMIT_OFFSET_RAW_QUERY = "SELECT %s FROM %s WHERE %s = ? ORDER BY %s LIMIT %s OFFSET %s;";

    private final static String SELECT_BY_KEYS_RAW_QUERY = "SELECT %s FROM %s WHERE %s IN ( %s );";
    private final static String SELECT_BY_KEYS_LIMIT_RAW_QUERY = "SELECT %s FROM %s WHERE %s IN ( %s ) LIMIT %s;";
    private final static String SELECT_BY_KEYS_LIMIT_OFFSET_RAW_QUERY = "SELECT %s FROM %s WHERE %s IN ( %s ) LIMIT %s OFFSET %s;";
    private final static String SELECT_BY_KEYS_ORDER_BY_RAW_QUERY = "SELECT %s FROM %s WHERE %s IN ( %s ) ORDER BY %s;";
    private final static String SELECT_BY_KEYS_ORDER_BY_LIMIT_RAW_QUERY = "SELECT %s FROM %s WHERE %s IN ( %s ) ORDER BY %s LIMIT %s;";
    private final static String SELECT_BY_KEYS_ORDER_BY_LIMIT_OFFSET_RAW_QUERY = "SELECT %s FROM %s WHERE %s IN ( %s ) ORDER BY %s LIMIT %s OFFSET %s;";

    private final static String DELETE_BY_KEY_RAW_QUERY = "DELETE FROM %s WHERE %s = ?;";
    private final static String DELETE_BY_KEYS_RAW_QUERY = "DELETE FROM %s WHERE %s  IN ( %s );";
    private final static String DELETE_WHERE_RAW_QUERY = "DELETE FROM %s WHERE %s = ?;";
    private final static String DELETE_ALL_RAW_QUERY = "DELETE FROM %s;";

    private final static String SELECT_TOTAL_ROWS_RAW_QUERY = "SELECT COUNT(%s) as total FROM %s";
    private final static String SELECT_TOTAL_ROWS_BY_KEY_RAW_QUERY = "SELECT COUNT(%s) as total FROM %s WHERE %s = ?;";
    private final static String SELECT_TOTAL_ROWS_BY_KEYS_RAW_QUERY = "SELECT COUNT(%s) as total FROM %s WHERE %s IN ( %s );";
    private final static String SELECT_TOTAL_ROWS_WHERE_RAW_QUERY = "SELECT COUNT(%s) as total FROM %s WHERE %s;";

    /**
     * Generate a string with n mnemonic raw values
     * @param n The number of values
     * @return The string with the values
     */
    public static String generateSQLPlaceholders(int n){
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

        /**
         * Constructor
         * @param page The page
         * @param size The size
         */
        public Pageable(Long page, Long size) {
            this.page = page;
            this.size = size;
            this.sortField = Optional.empty();
            this.sortOrder = Optional.empty();
        }

        /**
         * Constructor
         * @param page The page
         * @param size The size
         * @param sortField The sort field
         * @param sortOrder The sort order
         */
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

        public Long getPage() {
            return page;
        }

        public Long getSize() {
            return size;
        }

        public Long offset(){
            return page * size;
        }


    }

    private Class<T> typeClass;                     // The type class of the entity
    private String tableName;                       // The table name of the entity
    private String publicKeyColumnName = null;      // The public key column name
    private int publicKeyColumnIndex = -1;          // The public key column index
    private Boolean autoIncrement = false;          // The public key is autoincrement
    private String columnNamesString;               // The column names string

    private final List<String> columnNames = new ArrayList<>();                                   // The column names
    private final Map<String, String> fieldColumnNamesMap = new LinkedHashMap<>();                // The field column names map (field name -> column name)
    private final Map<String, BiConsumer<T,Object>> fieldValueSettersMap = new LinkedHashMap<>(); // The field value setters map (column name -> setter)
    private final Map<String, Function<T,Object>> fieldValueGetterMap = new LinkedHashMap<>();    // The field value getter map (column name -> getter)

    // Constructor

    /**
     * Constructor
     * @param typeClass The type class
     */
    public Repository(Class<T> typeClass) {
        this.init(typeClass);
    }

    // Getters

    /**
     * Get the column name from the field name
     * @param fieldName The field name
     * @return The column name
     */
    public final String getFieldColumnName (String fieldName){
        return this.fieldColumnNamesMap.get(fieldName);
    }

    /**
     * Get the field name from the column name
     * @param columnName The column name
     * @return The field name
     */
    public final String getColumnFieldName (String columnName){
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
    public final String getPublicKeyColumnName() {
        return publicKeyColumnName;
    }

    /**
     * Get the table name
     * @return The table name
     */
    public final String getTableName() {
        return tableName;
    }

    /**
     * Get the column names
     * @return The column names
     */
    public final List<String> getColumnNames() {
        return columnNames;
    }

    /**
     * Get the column names by index
     * @param index The index
     * @return The column name
     */
    public final String getColumnNameByIndex (int index){
        return this.columnNames.get(index);
    }

    /**
     * Get the column index by name
     * @param columnName The column name
     * @return The column index
     */
    public final int getColumnIndexByName (String columnName){
        return this.columnNames.indexOf(columnName);
    }

    /**
     * Get the public key column index
     * @return The public key column index
     */
    public final int getPublicKeyColumnIndex() {
        return publicKeyColumnIndex;
    }

    public Optional<String> getOrderByClauseFromPageable(Pageable pageable){
        if (pageable.sortField.isEmpty() || pageable.sortOrder.isEmpty()){
            return Optional.empty();
        } else {
            String sortColumnName = getFieldColumnName(pageable.sortField.get());
            String desc = pageable.sortOrder.get().equals("desc") ? " DESC" : "";
            String orderByClause = sortColumnName.concat(desc);
            return Optional.of(orderByClause);
        }
    }

    // Bind methods

    /**
     * Bind a field to a setter
     * @param columnName The column name
     * @param setter The setter
     */
    public final void bindFieldToSetter(String columnName, BiConsumer<T,Object> setter){
        this.fieldValueSettersMap.put(columnName,setter);
    }

    /**
     * Bind a field to a getter
     * @param columnName The column name
     * @param getter The getter
     */
    public final void bindFieldToGetter(String columnName, Function<T,Object> getter){
        this.fieldValueGetterMap.put(columnName,getter);
    }

    // Count methods

    /**
     * Get the number of elements
     * @param connection The connection
     * @return The number of elements
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final long getTotalElements(Connection connection) throws SQLException {
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final long getTotalElementsBy(Connection connection, String column, Object value) throws SQLException {
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
     * Get the number of elements by keys
     * @param connection The connection
     * @param column The column
     * @param values The values
     * @return The number of elements
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final long getTotalElementsBy(Connection connection, String column, List<Object> values) throws SQLException {
        if (values.isEmpty()) return 0L;
        String rawValues = generateSQLPlaceholders(values.size());
        String query = String.format(SELECT_TOTAL_ROWS_BY_KEYS_RAW_QUERY, this.publicKeyColumnName, this.tableName, column, rawValues);
        PreparedStatement statement = connection.prepareStatement(query);
        this.populateStatement(statement,values);
        ResultSet resultSet = statement.executeQuery();
        if (resultSet.next()){
            return resultSet.getLong("total");
        }
        return 0;
    }

    /**
     * Get the number of elements where
     * @param connection The connection
     * @param whereClause The where clause
     * @return The number of elements
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final long getTotalElementsWhere(Connection connection, String whereClause) throws SQLException {
        String query = String.format(SELECT_TOTAL_ROWS_WHERE_RAW_QUERY, this.publicKeyColumnName, this.tableName, whereClause);
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        ResultSet resultSet = preparedStatement.executeQuery();
        if (resultSet.next()){
            return resultSet.getLong("total");
        }
        return 0;
    }

    // Insert methods

    /**
     * Insert an entity
     * @param connection The connection
     * @param entity The entity
     * @return The number of affected rows
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final int insert(Connection connection, T entity) throws SQLException {
        List<T> entities = new ArrayList<>();
        entities.add(entity);
        return insert(connection, entities)[0];
    }

    /**
     * Insert entities
     * @param connection The connection
     * @param entities The entities
     * @return An array with the number of affected rows (one for each entity)
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final int[] insert(Connection connection, List<T> entities) throws SQLException {
        if (entities.isEmpty()) return new int[0];

        connection.setAutoCommit(false);  // Start transaction
        try {
            // Assumes all entities have the same columns to be inserted
            List<String> columns = this.autoIncrement ?
                    columnNames.stream().filter(c -> !c.equals(publicKeyColumnName)).collect(Collectors.toList()) :
                    new ArrayList<>(columnNames);

            String columnsList = String.join(",", columns);
            String columnRawValues = generateSQLPlaceholders(columns.size());
            String query = String.format(INSERT_INTO_RAW_QUERY, this.tableName, columnsList, columnRawValues);

            PreparedStatement statement = this.autoIncrement ?
                    connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS) :
                    connection.prepareStatement(query);

            for (T entity : entities) {
                populateStatement(statement, entity, columns);
                statement.addBatch();
            }

            int[] ints = statement.executeBatch();

            if (this.autoIncrement) {
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {

                    BiConsumer<T, Object> idSetter = this.fieldValueSettersMap.get(publicKeyColumnName);

                    for (T entity : entities) {
                        if (generatedKeys.next()) {
                            long key = generatedKeys.getLong(1);
                            idSetter.accept(entity, key);
                        } else {
                            throw new SQLException("Creation failed, no ID obtained for one of the entities.");
                        }
                    }
                }
            }

            connection.commit();  // Commit transaction
            return ints;

        } catch (SQLException e) {
            connection.rollback();  // Roll back transaction on error
            throw e;
        } finally {
            connection.setAutoCommit(true);  // Reset auto-commit to true
        }
    }

    // Update methods

    /**
     * Update an entity
     * @param connection The connection
     * @param entity The entity
     * @return The number of affected rows
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final int update(Connection connection, T entity) throws SQLException {
        List<T> entities = new ArrayList<>();
        entities.add(entity);
        return update(connection, entities)[0];
    }

    /**
     * Update entities
     * @param connection The connection
     * @param entities The entities
     * @return An array with the number of affected rows (one for each entity)
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final int[] update(Connection connection, List<T> entities) throws SQLException {

        try {
            connection.setAutoCommit(false);
            List<String> columnsToUpdate = new ArrayList<>(this.columnNames);
            columnsToUpdate.remove(this.publicKeyColumnIndex);
            StringBuilder builder = new StringBuilder();
            columnsToUpdate.forEach((String column)->{
                builder.append(column.concat(" = ?,"));
            });
            String updateBodyRawQuery = builder.deleteCharAt( builder.length() -1).toString();
            String query = String.format(UPDATE_RAW_QUERY, this.tableName,updateBodyRawQuery,this.publicKeyColumnName);
            PreparedStatement statement = connection.prepareStatement(query);
            Function<T, Object> idGetter = this.fieldValueGetterMap.get(this.publicKeyColumnName);

            for (T entity : entities) {
                int parameterIndex = populateStatement(statement,entity,columnsToUpdate);
                Object entityId = idGetter.apply(entity);
                statement.setObject(parameterIndex + 1, entityId);
                statement.addBatch();
            }

            return statement.executeBatch();

        } catch (SQLException e) {
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    // Delete methods

    /**
     * Delete an entity
     * @param connection The connection
     * @param entity The entity
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final void delete(Connection connection, T entity) throws SQLException {
        Object id = fieldValueGetterMap.get(publicKeyColumnName).apply(entity);
        deleteById(connection, id);
    }

    /**
     * Delete entities
     * @param connection The connection
     * @param entities The entities
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final void delete(Connection connection, List<T> entities) throws SQLException {

        connection.setAutoCommit(false);

        try{

            String query = String.format(DELETE_BY_KEY_RAW_QUERY, this.tableName, publicKeyColumnName);
            PreparedStatement statement = connection.prepareStatement(query);
            Function<T, Object> idGetter = fieldValueGetterMap.get(publicKeyColumnName);

            for (T entity : entities) {
                Object id = idGetter.apply(entity);
                statement.setObject(1, id);
                statement.addBatch();
            }

            statement.executeBatch();

            connection.commit();

        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    /**
     * Delete entities with a where clause
     * @param connection The connection
     * @param whereClause The where clause
     * @return True if the operation was successful
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final boolean deleteWhere(Connection connection, String whereClause) throws SQLException {
        String query = String.format(DELETE_WHERE_RAW_QUERY, this.tableName, whereClause);
        PreparedStatement statement = connection.prepareStatement(query);
        return statement.execute();
    }

    /**
     * Delete an entity by key
     * @param connection The connection
     * @param columnName The column name
     * @param value The value
     * @return True if the operation was successful
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final boolean deleteBy(Connection connection, String columnName, Object value) throws SQLException {
        String query = String.format(DELETE_BY_KEY_RAW_QUERY, this.tableName, columnName);
        PreparedStatement statement = connection.prepareStatement(query);
        statement.setObject(1, value);
        return statement.execute();
    }

    /**
     * Delete entities by key
     * @param connection The connection
     * @param columnName The column name
     * @param values The values
     * @return True if the operation was successful
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final void deleteBy(Connection connection, String columnName, List<Object> values) throws SQLException {
        if (values.isEmpty()) return;
        String rawKeys = generateSQLPlaceholders(values.size());
        String query = String.format(DELETE_BY_KEYS_RAW_QUERY, this.tableName, columnName, rawKeys);
        PreparedStatement statement = connection.prepareStatement(query);
        populateStatement(statement, values);
        statement.execute();
    }

    /**
     * Delete an entity by id
     * @param connection The connection
     * @param id The id
     * @return True if the operation was successful
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final boolean deleteById(Connection connection, Object id) throws SQLException {
        return deleteBy(connection, this.publicKeyColumnName, id);
    }

    /**
     * Delete entities by ids
     * @param connection The connection
     * @param ids The ids
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final void deleteByIds(Connection connection, List<Object> ids) throws SQLException {
        deleteBy(connection, this.publicKeyColumnName, ids);
    }

    /**
     * Delete all entities
     * @param connection The connection
     * @return True if the operation was successful
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final boolean deleteAll(Connection connection) throws SQLException {
        String query = String.format(DELETE_ALL_RAW_QUERY, this.tableName);
        PreparedStatement statement = connection.prepareStatement(query);
        return statement.execute();
    }

    // Read methods

    /**
     * Read entities
     * @param connection The connection
     * @param pageable The pageable
     * @return The list of entities
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> read(Connection connection, Pageable pageable) throws SQLException {
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> read(Connection connection) throws SQLException {
        String query = String.format(SELECT_ALL_RAW_QUERY, this.columnNamesString, this.tableName);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities
     * @param connection The connection
     * @param limit The limit (number of entities to read)
     * @return The list of entities
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> read(Connection connection, Long limit) throws SQLException {
        String query = String.format(SELECT_ALL_LIMIT_RAW_QUERY, this.columnNamesString, this.tableName, limit);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> read(Connection connection, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_ALL_LIMIT_OFFSET_RAW_QUERY, this.columnNamesString, this.tableName, limit, offset);
        PreparedStatement statement = connection.prepareStatement(query);
        ResultSet rs = statement.executeQuery();
        return readResultSet(rs);
    }

    /**
     * Read entities
     * @param connection The connection
     * @param orderByClause The order by clause
     * @return The list of entities
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> read(Connection connection, String orderByClause) throws SQLException {
        String query = String.format(SELECT_ALL_ORDER_BY_RAW_QUERY, this.columnNamesString, this.tableName, orderByClause);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> read(Connection connection, String orderByClause, Long limit) throws SQLException {
        String query = String.format(SELECT_ALL_ORDER_BY_LIMIT_RAW_QUERY, this.columnNamesString, this.tableName, orderByClause, limit);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> read(Connection connection, String orderByClause, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_ALL_ORDER_BY_LIMIT_OFFSET_RAW_QUERY, this.columnNamesString, this.tableName, orderByClause, limit, offset);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readWhere(Connection connection, String whereClause, Pageable pageable) throws SQLException {
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readWhere(Connection connection, String whereClause) throws SQLException {
        String query = String.format(SELECT_WHERE_RAW_QUERY, this.columnNamesString, this.tableName, whereClause);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readWhere(Connection connection, String whereClause, Long limit) throws SQLException {
        String query = String.format(SELECT_WHERE_LIMIT_RAW_QUERY, this.columnNamesString, this.tableName, whereClause, limit);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readWhere(Connection connection, String whereClause, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_WHERE_LIMIT_OFFSET_RAW_QUERY, this.columnNamesString, this.tableName, whereClause, limit, offset);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readWhere(Connection connection, String whereClause, String orderByClause) throws SQLException {
        String query = String.format(SELECT_WHERE_ORDER_BY_RAW_QUERY, this.columnNamesString, this.tableName, whereClause, orderByClause);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readWhere(Connection connection, String whereClause, String orderByClause, Long limit) throws SQLException {
        String query = String.format(SELECT_WHERE_ORDER_BY_LIMIT_RAW_QUERY, this.columnNamesString, this.tableName, whereClause, orderByClause, limit);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readWhere(Connection connection, String whereClause, String orderByClause, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_WHERE_ORDER_BY_LIMIT_OFFSET_RAW_QUERY, this.columnNamesString, this.tableName, whereClause, orderByClause, limit, offset);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, Object value, Pageable pageable) throws SQLException {
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, List<Object> values, Pageable pageable) throws SQLException {
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, Object value) throws SQLException {
        String query = String.format(SELECT_BY_KEY_RAW_QUERY, this.columnNamesString, this.tableName, columnName);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, Object value, Long limit) throws SQLException {
        String query = String.format(SELECT_BY_KEY_LIMIT_RAW_QUERY, this.columnNamesString, this.tableName, columnName, limit);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, Object value, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_BY_KEY_LIMIT_OFFSET_RAW_QUERY, this.columnNamesString, this.tableName, columnName, limit, offset);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, Object value, String orderByClause) throws SQLException {
        String query = String.format(SELECT_BY_KEY_ORDER_BY_RAW_QUERY, this.columnNamesString, this.tableName, columnName, orderByClause);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, Object value, String orderByClause, Long limit) throws SQLException {
        String query = String.format(SELECT_BY_KEY_ORDER_BY_LIMIT_RAW_QUERY, this.columnNamesString, this.tableName, columnName, orderByClause, limit);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, Object value, String orderByClause, Long limit, Long offset) throws SQLException {
        String query = String.format(SELECT_BY_KEY_ORDER_BY_LIMIT_OFFSET_RAW_QUERY, this.columnNamesString, this.tableName, columnName, orderByClause, limit, offset);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, List<Object> values) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateSQLPlaceholders(values.size());
        String query = String.format(SELECT_BY_KEYS_RAW_QUERY, this.columnNamesString, this.tableName, columnName, rawValues);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, List<Object> values, Long limit) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateSQLPlaceholders(values.size());
        String query = String.format(SELECT_BY_KEYS_LIMIT_RAW_QUERY, this.columnNamesString, this.tableName, columnName, rawValues, limit);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, List<Object> values, Long limit, Long offset) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateSQLPlaceholders(values.size());
        String query = String.format(SELECT_BY_KEYS_LIMIT_OFFSET_RAW_QUERY, this.columnNamesString, this.tableName, columnName, rawValues, limit, offset);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, List<Object> values, String orderByClause) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateSQLPlaceholders(values.size());
        String query = String.format(SELECT_BY_KEYS_ORDER_BY_RAW_QUERY, this.columnNamesString, this.tableName, columnName, rawValues, orderByClause);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, List<Object> values, String orderByClause, Long limit) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateSQLPlaceholders(values.size());
        String query = String.format(SELECT_BY_KEYS_ORDER_BY_LIMIT_RAW_QUERY, this.columnNamesString, this.tableName, columnName, rawValues, orderByClause, limit);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readBy(Connection connection, String columnName, List<Object> values, String orderByClause, Long limit, Long offset) throws SQLException {
        if (values.isEmpty()) return new ArrayList<>();
        String rawValues = generateSQLPlaceholders(values.size());
        String query = String.format(SELECT_BY_KEYS_ORDER_BY_LIMIT_OFFSET_RAW_QUERY, this.columnNamesString, this.tableName, columnName, rawValues, orderByClause, limit, offset);
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readByQuery(Connection connection,String query, Object...values) throws SQLException {
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readByQuery(Connection connection,String query, List<Object> values) throws SQLException{
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readByQuery(Connection connection, String query, BiConsumer<ResultSet,T> consumer, Object...values) throws SQLException {
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final List<T> readByQuery(Connection connection, String query, BiConsumer<ResultSet,T> consumer, List<Object> values) throws SQLException {
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final <C extends T> List<C> readByQuery(Connection connection, String query, Supplier<C> supplier,  BiConsumer<ResultSet,C> consumer, Object...values) throws SQLException {
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final <C extends T> List<C> readByQuery(Connection connection, String query, Supplier<C> supplier, BiConsumer<ResultSet,C> consumer, List<Object> values) throws SQLException {
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
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public final T readById(Connection connection, Object value) throws SQLException {
        List<T> results = readBy(connection, publicKeyColumnName, value);
        if (results.isEmpty()) return null;
        return results.get(0);
    }

    // Overrideable methods

    /**
     * Instantiate a new entity of type T. This method can be overridden if custom instantiation logic is required.
     * @return A new instance of type T
     */
    public T instantiateEntity()  {
        T temp = null;
        try {
            Constructor<T> constructor = typeClass.getDeclaredConstructor();
            temp = constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        } return temp;
    }

    /**
     * Instantiate a new entity of type T. This method can be overridden if custom instantiation logic is required.
     * @param resultSet The ResultSet from which to retrieve data
     * @return A new instance of type T
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public T instantiateEntity(ResultSet resultSet) throws SQLException {
        return instantiateEntity();
    }

    /**
     * Populates the given entity with data from the ResultSet. This method can be overridden if custom population logic is required.
     * @param resultSet The ResultSet from which to retrieve data
     * @param obj The entity to be populated
     * @throws SQLException The SQL exception if the operation fails for any reason
     */
    public void populateEntity(ResultSet resultSet, T obj) throws SQLException {
        for (String columnName : columnNames) {
            Object rawValue = resultSet.getObject(columnName);
            this.fieldValueSettersMap.get(columnName).accept(obj, rawValue);
        }
    }

    // Private methods

    private List<T> readResultSet(ResultSet resultSet) throws SQLException {
        ArrayList<T> results = new ArrayList<>();
        T temp = null;
        while (resultSet.next()) {
            temp = this.instantiateEntity(resultSet);
            this.populateEntity(resultSet,temp);
            results.add(temp);
        } return results;
    }

    private List<T> readResultSet(ResultSet resultSet,BiConsumer<ResultSet,T> consumer) throws SQLException {
        ArrayList<T> results = new ArrayList<>();
        T temp = null;
        while (resultSet.next()) {
            temp = this.instantiateEntity(resultSet);
            this.populateEntity(resultSet,temp);
            consumer.accept(resultSet,temp);
            results.add(temp);
        } return results;
    }

    private <C extends T> List<C> readResultSet(ResultSet resultSet, Supplier<C> supplier, BiConsumer<ResultSet,C> consumer) throws SQLException {
        ArrayList<C> results = new ArrayList<>();
        C temp = null;
        while (resultSet.next()) {
            temp = supplier.get();
            this.populateEntity(resultSet,temp);
            consumer.accept(resultSet,temp);
            results.add(temp);
        } return results;
    }

    private void populateStatement(PreparedStatement statement, List<Object> values) throws SQLException {
        for (int i = 0; i < values.size(); i++) {
            statement.setObject(i+1,values.get(i));
        }
    }

    private int populateStatement(PreparedStatement statement, T entity, List<String> columns) throws SQLException {
        int i;
        for (i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            Object value = fieldValueGetterMap.get(column).apply(entity);
            statement.setObject(i + 1,value);
        } return i;
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

        this.columnNamesString = generateColumnNames();

    }

    private String generateColumnNames(){
        StringBuilder builder = new StringBuilder();
        columnNames.forEach((String column)->{
            builder.append(column.concat(","));
        });
        return builder.deleteCharAt( builder.length() -1).toString();
    }

    private void initField(Field field, int fieldIndex){

        String columnName = "";

        if (field.isAnnotationPresent(Column.class)){
            columnName = field.getAnnotation(Column.class).name().isEmpty() ?
                    field.getName() : field.getAnnotation(Column.class).name();
        }

        if (field.isAnnotationPresent(Id.class)){

            if (columnName.isEmpty()){
                columnName = field.getName();
            }

            this.publicKeyColumnName = columnName;
            this.publicKeyColumnIndex = fieldIndex;

            if (field.isAnnotationPresent(GeneratedValue.class) &&
                    field.getAnnotation(GeneratedValue.class).strategy().equals(GenerationType.AUTO)){
                this.autoIncrement = true;
            }
        }

        if (!columnName.isEmpty()){
            this.columnNames.add(columnName);
            this.fieldColumnNamesMap.put(field.getName(),columnName);
            detectSetterAndGetter(field,columnName);
        }
    }

    private void detectSetterAndGetter(Field field, String columnName){

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
        } else if (field.getType().equals(Timestamp.class)){
            this.fieldValueSettersMap.put(columnName, (x, y) -> {
                try {
                    field.set(x, Timestamp.valueOf((LocalDateTime) y));
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
