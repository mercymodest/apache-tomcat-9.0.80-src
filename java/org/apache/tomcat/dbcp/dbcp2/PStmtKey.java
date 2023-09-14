/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tomcat.dbcp.dbcp2;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import org.apache.tomcat.dbcp.dbcp2.PoolingConnection.StatementType;

/**
 * A key uniquely identifying {@link java.sql.PreparedStatement PreparedStatement}s.
 *
 * @since 2.0
 */
public class PStmtKey {

    /**
     * Interface for Prepared or Callable Statement.
     */
    @FunctionalInterface
    private interface StatementBuilder {
        Statement createStatement(Connection connection, PStmtKey key) throws SQLException;
    }

    private static final StatementBuilder CallConcurrency = (c, k) -> c.prepareCall(k.sql, k.resultSetType.intValue(), k.resultSetConcurrency.intValue());
    private static final StatementBuilder CallHoldability = (c, k) -> c.prepareCall(k.sql, k.resultSetType.intValue(), k.resultSetConcurrency.intValue(), k.resultSetHoldability.intValue());
    private static final StatementBuilder CallSQL = (c, k) -> c.prepareCall(k.sql);
    private static final StatementBuilder StatementAutoGeneratedKeys = (c, k) -> c.prepareStatement(k.sql, k.autoGeneratedKeys.intValue());
    private static final StatementBuilder StatementColumnIndexes = (c, k) -> c.prepareStatement(k.sql, k.columnIndexes);
    private static final StatementBuilder StatementColumnNames = (c, k) -> c.prepareStatement(k.sql, k.columnNames);
    private static final StatementBuilder StatementConcurrency = (c, k) -> c.prepareStatement(k.sql, k.resultSetType.intValue(), k.resultSetConcurrency.intValue());
    private static final StatementBuilder StatementHoldability = (c, k) -> c.prepareStatement(k.sql, k.resultSetType.intValue(), k.resultSetConcurrency.intValue(),
        k.resultSetHoldability.intValue());
    private static final StatementBuilder StatementSQL = (c, k) -> c.prepareStatement(k.sql);

    private static StatementBuilder match(final StatementType statementType, final StatementBuilder prep, final StatementBuilder call) {
        switch (Objects.requireNonNull(statementType, "statementType")) {
        case PREPARED_STATEMENT:
            return prep;
        case CALLABLE_STATEMENT:
            return call;
        default:
            throw new IllegalArgumentException(statementType.toString());
        }
    }

    /**
     * SQL defining Prepared or Callable Statement
     */
    private final String sql;

    /**
     * Result set type; one of {@code ResultSet.TYPE_FORWARD_ONLY}, {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     * {@code ResultSet.TYPE_SCROLL_SENSITIVE}.
     */
    private final Integer resultSetType;

    /**
     * Result set concurrency. A concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     * {@code ResultSet.CONCUR_UPDATABLE}.
     */
    private final Integer resultSetConcurrency;

    /**
     * Result set holdability. One of the following {@code ResultSet} constants: {@code ResultSet.HOLD_CURSORS_OVER_COMMIT}
     * or {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}.
     */
    private final Integer resultSetHoldability;

    /**
     * Database catalog.
     */
    private final String catalog;

    /**
     * Database schema.
     */
    private final String schema;

    /**
     * A flag indicating whether auto-generated keys should be returned; one of {@code Statement.RETURN_GENERATED_KEYS} or
     * {@code Statement.NO_GENERATED_KEYS}.
     */
    private final Integer autoGeneratedKeys;

    /**
     * An array of column indexes indicating the columns that should be returned from the inserted row or rows.
     */
    private final int[] columnIndexes;

    /**
     * An array of column names indicating the columns that should be returned from the inserted row or rows.
     */
    private final String[] columnNames;

    /**
     * Statement builder.
     */
    private final transient StatementBuilder statementBuilder;

    /**
     * Statement type, prepared or callable.
     */
    private final StatementType statementType;

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @deprecated Use {@link #PStmtKey(String, String, String)}.
     */
    @Deprecated
    public PStmtKey(final String sql) {
        this(sql, null, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param resultSetType A result set type; one of {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or {@code ResultSet.TYPE_SCROLL_SENSITIVE}.
     * @param resultSetConcurrency A concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     *        {@code ResultSet.CONCUR_UPDATABLE}.
     * @deprecated Use {@link #PStmtKey(String, String, String, int, int)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final int resultSetType, final int resultSetConcurrency) {
        this(sql, null, resultSetType, resultSetConcurrency, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @deprecated Use {@link #PStmtKey(String, String, String)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog) {
        this(sql, catalog, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param autoGeneratedKeys A flag indicating whether auto-generated keys should be returned; one of
     *        {@code Statement.RETURN_GENERATED_KEYS} or {@code Statement.NO_GENERATED_KEYS}.
     * @deprecated Use {@link #PStmtKey(String, String, String, int)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int autoGeneratedKeys) {
        this(sql, catalog, StatementType.PREPARED_STATEMENT, Integer.valueOf(autoGeneratedKeys));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param resultSetType A result set type; one of {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or {@code ResultSet.TYPE_SCROLL_SENSITIVE}.
     * @param resultSetConcurrency A concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     *        {@code ResultSet.CONCUR_UPDATABLE}.
     * @deprecated Use {@link #PStmtKey(String, String, String, int, int)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int resultSetType, final int resultSetConcurrency) {
        this(sql, catalog, resultSetType, resultSetConcurrency, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param resultSetType a result set type; one of {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or {@code ResultSet.TYPE_SCROLL_SENSITIVE}.
     * @param resultSetConcurrency A concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     *        {@code ResultSet.CONCUR_UPDATABLE}
     * @param resultSetHoldability One of the following {@code ResultSet} constants:
     *        {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}.
     * @deprecated Use {@link #PStmtKey(String, String, String, int, int, int)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability) {
        this(sql, catalog, resultSetType, resultSetConcurrency, resultSetHoldability, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param resultSetType a result set type; one of {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @param resultSetConcurrency A concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     *        {@code ResultSet.CONCUR_UPDATABLE}.
     * @param resultSetHoldability One of the following {@code ResultSet} constants:
     *        {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}.
     * @param statementType The SQL statement type, prepared or callable.
     * @deprecated Use {@link #PStmtKey(String, String, String, int, int, int, PoolingConnection.StatementType)}
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int resultSetType, final int resultSetConcurrency, final int resultSetHoldability,
            final StatementType statementType) {
        this(sql, catalog, null, Integer.valueOf(resultSetType), Integer.valueOf(resultSetConcurrency), Integer.valueOf(resultSetHoldability), null, null, null, statementType,
            k -> match(statementType, StatementHoldability, CallHoldability));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param resultSetType A result set type; one of {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or {@code ResultSet.TYPE_SCROLL_SENSITIVE}.
     * @param resultSetConcurrency A concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     *        {@code ResultSet.CONCUR_UPDATABLE}.
     * @param statementType The SQL statement type, prepared or callable.
     * @deprecated Use {@link #PStmtKey(String, String, String, int, int, PoolingConnection.StatementType)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int resultSetType, final int resultSetConcurrency, final StatementType statementType) {
        this(sql, catalog, null, Integer.valueOf(resultSetType), Integer.valueOf(resultSetConcurrency), null, null, null, null, statementType,
            k -> match(statementType, StatementConcurrency, CallConcurrency));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param columnIndexes An array of column indexes indicating the columns that should be returned from the inserted row
     *        or rows.
     * @deprecated Use {@link #PStmtKey(String, String, String, int[])}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final int[] columnIndexes) {
        this(sql, catalog, null, null, null, null, null, columnIndexes, null, StatementType.PREPARED_STATEMENT, StatementColumnIndexes);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param statementType The SQL statement type, prepared or callable.
     * @deprecated Use {@link #PStmtKey(String, String, String, PoolingConnection.StatementType)}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final StatementType statementType) {
        this(sql, catalog, null, null, null, null, null, null, null, statementType, k -> match(statementType, StatementSQL, CallSQL));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param statementType The SQL statement type, prepared or callable.
     * @param autoGeneratedKeys A flag indicating whether auto-generated keys should be returned; one of
     *        {@code Statement.RETURN_GENERATED_KEYS} or {@code Statement.NO_GENERATED_KEYS}.
     * @deprecated Use {@link #PStmtKey(String, String, String, PoolingConnection.StatementType, Integer)}
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final StatementType statementType, final Integer autoGeneratedKeys) {
        this(sql, catalog, null, null, null, null, autoGeneratedKeys, null, null, statementType,
            k -> match(statementType, StatementAutoGeneratedKeys, CallSQL));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param schema The schema
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema) {
        this(sql, catalog, schema, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param schema The schema
     * @param autoGeneratedKeys A flag indicating whether auto-generated keys should be returned; one of
     *        {@code Statement.RETURN_GENERATED_KEYS} or {@code Statement.NO_GENERATED_KEYS}.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int autoGeneratedKeys) {
        this(sql, catalog, schema, StatementType.PREPARED_STATEMENT, Integer.valueOf(autoGeneratedKeys));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param schema The schema
     * @param resultSetType A result set type; one of {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or {@code ResultSet.TYPE_SCROLL_SENSITIVE}.
     * @param resultSetConcurrency A concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     *        {@code ResultSet.CONCUR_UPDATABLE}.
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int resultSetType, final int resultSetConcurrency) {
        this(sql, catalog, schema, resultSetType, resultSetConcurrency, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param schema The schema
     * @param resultSetType a result set type; one of {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or {@code ResultSet.TYPE_SCROLL_SENSITIVE}.
     * @param resultSetConcurrency A concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     *        {@code ResultSet.CONCUR_UPDATABLE}
     * @param resultSetHoldability One of the following {@code ResultSet} constants:
     *        {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability) {
        this(sql, catalog, schema, resultSetType, resultSetConcurrency, resultSetHoldability, StatementType.PREPARED_STATEMENT);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param schema The schema.
     * @param resultSetType a result set type; one of {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or {@code ResultSet.TYPE_SCROLL_SENSITIVE}
     * @param resultSetConcurrency A concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     *        {@code ResultSet.CONCUR_UPDATABLE}.
     * @param resultSetHoldability One of the following {@code ResultSet} constants:
     *        {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}.
     * @param statementType The SQL statement type, prepared or callable.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int resultSetType, final int resultSetConcurrency,
            final int resultSetHoldability, final StatementType statementType) {
        this(sql, catalog, schema, Integer.valueOf(resultSetType), Integer.valueOf(resultSetConcurrency), Integer.valueOf(resultSetHoldability), null, null, null, statementType,
            k -> match(statementType, StatementHoldability, CallHoldability));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param schema The schema.
     * @param resultSetType A result set type; one of {@code ResultSet.TYPE_FORWARD_ONLY},
     *        {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or {@code ResultSet.TYPE_SCROLL_SENSITIVE}.
     * @param resultSetConcurrency A concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     *        {@code ResultSet.CONCUR_UPDATABLE}.
     * @param statementType The SQL statement type, prepared or callable.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int resultSetType, final int resultSetConcurrency,
            final StatementType statementType) {
        this(sql, catalog, schema, Integer.valueOf(resultSetType), Integer.valueOf(resultSetConcurrency), null, null, null, null, statementType,
            k -> match(statementType, StatementConcurrency, CallConcurrency));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param schema The schema.
     * @param columnIndexes An array of column indexes indicating the columns that should be returned from the inserted row
     *        or rows.
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final int[] columnIndexes) {
        this(sql, catalog, schema, null, null, null, null, columnIndexes, null, StatementType.PREPARED_STATEMENT, StatementColumnIndexes);
    }

    private PStmtKey(final String sql, final String catalog, final String schema, final Integer resultSetType, final Integer resultSetConcurrency,
        final Integer resultSetHoldability, final Integer autoGeneratedKeys, final int[] columnIndexes, final String[] columnNames,
        final StatementType statementType, final Function<PStmtKey, StatementBuilder> statementBuilder) {
        this.sql = Objects.requireNonNull(sql, "sql").trim();
        this.catalog = catalog;
        this.schema = schema;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;
        this.autoGeneratedKeys = autoGeneratedKeys;
        this.columnIndexes = clone(columnIndexes);
        this.columnNames = clone(columnNames);
        this.statementBuilder = Objects.requireNonNull(Objects.requireNonNull(statementBuilder, "statementBuilder").apply(this), "statementBuilder");
        this.statementType = statementType;
    }

    // Root constructor.
    private PStmtKey(final String sql, final String catalog, final String schema, final Integer resultSetType, final Integer resultSetConcurrency,
        final Integer resultSetHoldability, final Integer autoGeneratedKeys, final int[] columnIndexes, final String[] columnNames,
        final StatementType statementType, final StatementBuilder statementBuilder) {
        this.sql = sql;
        this.catalog = catalog;
        this.schema = schema;
        this.resultSetType = resultSetType;
        this.resultSetConcurrency = resultSetConcurrency;
        this.resultSetHoldability = resultSetHoldability;
        this.autoGeneratedKeys = autoGeneratedKeys;
        this.columnIndexes = clone(columnIndexes);
        this.columnNames = clone(columnNames);
        this.statementBuilder = Objects.requireNonNull(statementBuilder, "statementBuilder");
        this.statementType = statementType;
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param schema The schema.
     * @param statementType The SQL statement type, prepared or callable.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final StatementType statementType) {
        this(sql, catalog, schema, null, null, null, null, null, null, statementType, k -> match(statementType, StatementSQL, CallSQL));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param schema The schema.
     * @param statementType The SQL statement type, prepared or callable.
     * @param autoGeneratedKeys A flag indicating whether auto-generated keys should be returned; one of
     *        {@code Statement.RETURN_GENERATED_KEYS} or {@code Statement.NO_GENERATED_KEYS}.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final StatementType statementType, final Integer autoGeneratedKeys) {
        this(sql, catalog, schema, null, null, null, autoGeneratedKeys, null, null, statementType,
            k -> match(statementType, StatementAutoGeneratedKeys, CallSQL));
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param schema The schema.
     * @param columnNames An array of column names indicating the columns that should be returned from the inserted row or
     *        rows.
     * @since 2.5.0
     */
    public PStmtKey(final String sql, final String catalog, final String schema, final String[] columnNames) {
        this(sql, catalog, schema, null, null, null, null, null, columnNames, StatementType.PREPARED_STATEMENT, StatementColumnNames);
    }

    /**
     * Constructs a key to uniquely identify a prepared statement.
     *
     * @param sql The SQL statement.
     * @param catalog The catalog.
     * @param columnNames An array of column names indicating the columns that should be returned from the inserted row or
     *        rows.
     * @deprecated Use {@link #PStmtKey(String, String, String, String[])}.
     */
    @Deprecated
    public PStmtKey(final String sql, final String catalog, final String[] columnNames) {
        this(sql, catalog, null, null, null, null, null, null, columnNames, StatementType.PREPARED_STATEMENT, StatementColumnNames);
    }

    private int[] clone(final int[] array) {
        return array == null ? null : array.clone();
    }

    private String[] clone(final String[] array) {
        return array == null ? null : array.clone();
    }

    /**
     * Creates a new Statement from the given Connection.
     *
     * @param connection The Connection to use to create the statement.
     * @return The statement.
     * @throws SQLException Thrown when there is a problem creating the statement.
     */
    public Statement createStatement(final Connection connection) throws SQLException {
        return statementBuilder.createStatement(connection, this);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PStmtKey other = (PStmtKey) obj;
        if (!Objects.equals(autoGeneratedKeys, other.autoGeneratedKeys)) {
            return false;
        }
        if (!Objects.equals(catalog, other.catalog)) {
            return false;
        }
        if (!Arrays.equals(columnIndexes, other.columnIndexes)) {
            return false;
        }
        if (!Arrays.equals(columnNames, other.columnNames)) {
            return false;
        }
        if (!Objects.equals(resultSetConcurrency, other.resultSetConcurrency)) {
            return false;
        }
        if (!Objects.equals(resultSetHoldability, other.resultSetHoldability)) {
            return false;
        }
        if (!Objects.equals(resultSetType, other.resultSetType)) {
            return false;
        }
        if (!Objects.equals(schema, other.schema)) {
            return false;
        }
        if (!Objects.equals(sql, other.sql)) {
            return false;
        }
        return statementType == other.statementType;
    }

    /**
     * Gets a flag indicating whether auto-generated keys should be returned; one of {@code Statement.RETURN_GENERATED_KEYS}
     * or {@code Statement.NO_GENERATED_KEYS}.
     *
     * @return a flag indicating whether auto-generated keys should be returned.
     */
    public Integer getAutoGeneratedKeys() {
        return autoGeneratedKeys;
    }

    /**
     * Gets the catalog.
     *
     * @return The catalog.
     */
    public String getCatalog() {
        return catalog;
    }

    /**
     * Gets an array of column indexes indicating the columns that should be returned from the inserted row or rows.
     *
     * @return An array of column indexes.
     */
    public int[] getColumnIndexes() {
        return clone(columnIndexes);
    }

    /**
     * Gets an array of column names indicating the columns that should be returned from the inserted row or rows.
     *
     * @return An array of column names.
     */
    public String[] getColumnNames() {
        return clone(columnNames);
    }

    /**
     * Gets the result set concurrency type; one of {@code ResultSet.CONCUR_READ_ONLY} or
     * {@code ResultSet.CONCUR_UPDATABLE}.
     *
     * @return The result set concurrency type.
     */
    public Integer getResultSetConcurrency() {
        return resultSetConcurrency;
    }

    /**
     * Gets the result set holdability, one of the following {@code ResultSet} constants:
     * {@code ResultSet.HOLD_CURSORS_OVER_COMMIT} or {@code ResultSet.CLOSE_CURSORS_AT_COMMIT}.
     *
     * @return The result set holdability.
     */
    public Integer getResultSetHoldability() {
        return resultSetHoldability;
    }

    /**
     * Gets the result set type, one of {@code ResultSet.TYPE_FORWARD_ONLY}, {@code ResultSet.TYPE_SCROLL_INSENSITIVE}, or
     * {@code ResultSet.TYPE_SCROLL_SENSITIVE}.
     *
     * @return the result set type.
     */
    public Integer getResultSetType() {
        return resultSetType;
    }

    /**
     * Gets the schema.
     *
     * @return The catalog.
     */
    public String getSchema() {
        return schema;
    }

    /**
     * Gets the SQL statement.
     *
     * @return the SQL statement.
     */
    public String getSql() {
        return sql;
    }

    /**
     * Gets the SQL statement type.
     *
     * @return The SQL statement type.
     */
    public StatementType getStmtType() {
        return statementType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(autoGeneratedKeys, catalog, Integer.valueOf(Arrays.hashCode(columnIndexes)), Integer.valueOf(Arrays.hashCode(columnNames)),
            resultSetConcurrency, resultSetHoldability, resultSetType, schema, sql, statementType);
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("PStmtKey: sql=");
        buf.append(sql);
        buf.append(", catalog=");
        buf.append(catalog);
        buf.append(", schema=");
        buf.append(schema);
        buf.append(", resultSetType=");
        buf.append(resultSetType);
        buf.append(", resultSetConcurrency=");
        buf.append(resultSetConcurrency);
        buf.append(", resultSetHoldability=");
        buf.append(resultSetHoldability);
        buf.append(", autoGeneratedKeys=");
        buf.append(autoGeneratedKeys);
        buf.append(", columnIndexes=");
        buf.append(Arrays.toString(columnIndexes));
        buf.append(", columnNames=");
        buf.append(Arrays.toString(columnNames));
        buf.append(", statementType=");
        buf.append(statementType);
        return buf.toString();
    }
}
