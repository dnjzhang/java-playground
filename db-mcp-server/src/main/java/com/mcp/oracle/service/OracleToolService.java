package com.mcp.oracle.service;

import java.io.Reader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import com.mcp.oracle.config.OracleToolConfig;

import oracle.jdbc.OracleConnection;
import oracle.jdbc.pool.OracleDataSource;

/**
 * Oracle database service implementation
 * Provides methods for interacting with Oracle database
 * 
 */
@Service
public class OracleToolService {

    private final OracleToolConfig oracleToolConfig;

    /**
     * Constructor for OracleService
     * 
     * @param oracleToolConfig Oracle database configuration
     */
    public OracleToolService(OracleToolConfig oracleToolConfig) {
        this.oracleToolConfig = oracleToolConfig;
    }

    /**
     * Get Oracle database connection
     * 
     * @return OracleConnection instance
     * @throws Exception if connection fails
     */
    private OracleConnection getConnection() throws Exception {
        OracleDataSource ds = new OracleDataSource();
        ds.setURL(oracleToolConfig.getConnectionString());
        ds.setUser(oracleToolConfig.getUsername());
        ds.setPassword(oracleToolConfig.getPassword());
        return (OracleConnection) ds.getConnection();
    }

    /**
     * Get a list of all tables in Oracle database.
     * Returns a Markdown formatted summary that is easy for humans to scan and
     * simple for LLMs to parse.
     * 
     * @return String containing a Markdown table with table names
     */
    @Tool(name = "list_tables", description = "Get a list of all tables in Oracle database")
    public String listTables() {
        try (OracleConnection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT table_name FROM user_tables ORDER BY table_name")) {

            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }

            if (tables.isEmpty()) {
                return "Tables found: 0\n\nNo tables available for the current schema.";
            }

            StringBuilder result = new StringBuilder();
            result.append("Tables found: ").append(tables.size()).append("\n\n");
            result.append("| # | Table Name |\n");
            result.append("|---|------------|\n");

            for (int i = 0; i < tables.size(); i++) {
                String tableName = tables.get(i).replace("|", "\\|").trim();
                result.append("| ")
                        .append(i + 1)
                        .append(" | ")
                        .append(tableName.isEmpty() ? "(unnamed table)" : tableName)
                        .append(" |\n");
            }

            return result.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get structure information of specified table in Oracle database
     * Returns table structure in CSV format including column names, data types, and
     * constraints
     * 
     * @param tableName name of the table to describe
     * @return String containing table structure in CSV format
     */
    @Tool(name = "describe_table", description = "Get structure information of specified table in Oracle database")
    public String describeTable(@ToolParam(description = "Table name to describe") String tableName) {
        try (OracleConnection conn = getConnection();
                Statement stmt = conn.createStatement()) {

            StringBuilder result = new StringBuilder();
            result.append("COLUMN_NAME,DATA_TYPE,NULLABLE,DATA_LENGTH,PRIMARY_KEY\n");

            // Get primary keys
            List<String> pkColumns = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT cols.column_name FROM all_constraints cons, all_cons_columns cols " +
                            "WHERE cons.constraint_type = 'P' AND cons.constraint_name = cols.constraint_name " +
                            "AND cons.owner = cols.owner AND cols.table_name = '" + tableName.toUpperCase() + "'")) {

                while (rs.next()) {
                    pkColumns.add(rs.getString(1));
                }
            }

            // Get column info
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT column_name, data_type, nullable, data_length " +
                            "FROM user_tab_columns WHERE table_name = '" + tableName.toUpperCase() + "' " +
                            "ORDER BY column_id")) {

                while (rs.next()) {
                    String colName = rs.getString(1);
                    result.append(String.format("%s,%s,%s,%d,%s\n",
                            colName,
                            rs.getString(2),
                            rs.getString(3),
                            rs.getInt(4),
                            pkColumns.contains(colName) ? "YES" : "NO"));
                }
            }

            return result.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Execute Oracle SQL statement
     * Supports both query (SELECT) and non-query (INSERT, UPDATE, DELETE)
     * statements
     * 
     * @param sql SQL statement to execute
     * @return String containing query results or affected rows count
     */
    @Tool(name = "execute_sql", description = "Execute Oracle SQL statement")
    public String executeSql(@ToolParam(description = "SQL statement to execute") String sql) {
        try {
            if (sql.trim().toUpperCase().startsWith("SELECT")) {
                try (OracleConnection conn = getConnection();
                        Statement stmt = conn.createStatement();
                        ResultSet rs = stmt.executeQuery(sql)) {

                    StringBuilder result = new StringBuilder();

                    // Add column headers
                    int columnCount = rs.getMetaData().getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1)
                            result.append(",");
                        result.append(rs.getMetaData().getColumnName(i));
                    }
                    result.append("\n");

                    // Add data rows
                    while (rs.next()) {
                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1)
                                result.append(",");
                            Object value = rs.getObject(i);
                            result.append(formatResultValue(value));
                        }
                        result.append("\n");
                    }

                    return result.toString();
                }
            } else {
                // Handle non-query statements (INSERT, UPDATE, DELETE, etc.)
                try (OracleConnection conn = getConnection();
                        Statement stmt = conn.createStatement()) {

                    int affectedRows = stmt.executeUpdate(sql);
                    return "Success: " + affectedRows + " rows affected";
                }
            }
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Search the LOG table for entries created within the provided ISO8601 time
     * range.
     * 
     * @param startIso inclusive start timestamp in ISO8601 format
     * @param endIso   inclusive end timestamp in ISO8601 format
     * @return CSV formatted rows describing the matching log entries
     */
    @Tool(name = "search_log", description = "Search LOG table entries within a time range")
    public String searchLog(
            @ToolParam(description = "Inclusive start timestamp in ISO8601 format") String startIso,
            @ToolParam(description = "Inclusive end timestamp in ISO8601 format") String endIso) {

        try {
            Timestamp start = parseIsoTimestamp(startIso);
            Timestamp end = parseIsoTimestamp(endIso);

            if (start.after(end)) {
                return "Error: start time must be before or equal to end time.";
            }

            String sql = "SELECT ID, COMP_TYPE_ID, COMP_ID, COMP_UID, CREATED, LOG_LEVEL_ID, LOG_CATEGORY_ID, "
                    + "LOG_SUB_CATEGORY, ENTRY, USER_DEF_ID, EXECUTION_CONTEXT, LOG_ERROR_CATEGORY_ID, LOG_CODE, "
                    + "API_CONTEXT FROM LOG WHERE CREATED BETWEEN ? AND ? ORDER BY CREATED";

            try (OracleConnection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setTimestamp(1, start);
                ps.setTimestamp(2, end);

                try (ResultSet rs = ps.executeQuery()) {
                    int columnCount = rs.getMetaData().getColumnCount();
                    StringBuilder headerBuilder = new StringBuilder();
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) {
                            headerBuilder.append(",");
                        }
                        headerBuilder.append(rs.getMetaData().getColumnName(i));
                    }

                    List<String> rows = new ArrayList<>();
                    while (rs.next()) {
                        StringBuilder row = new StringBuilder();
                        for (int i = 1; i <= columnCount; i++) {
                            if (i > 1) {
                                row.append(",");
                            }
                            Object value = rs.getObject(i);
                            row.append(formatResultValue(value));
                        }
                        rows.add(row.toString());
                    }

                    if (rows.isEmpty()) {
                        return String.format(
                                "Log entries: 0\n\nNo log records found between %s and %s.", startIso, endIso);
                    }

                    StringBuilder result = new StringBuilder();
                    result.append("Log entries: ").append(rows.size())
                            .append(" between ").append(startIso).append(" and ").append(endIso).append("\n\n");
                    result.append(headerBuilder).append("\n");
                    for (String row : rows) {
                        result.append(row).append("\n");
                    }
                    return result.toString();
                }
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return "Error: Invalid timestamp format - " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    private String formatResultValue(Object value) {
        if (value == null) {
            return "NULL";
        }

        if (value instanceof Clob) {
            return readClob((Clob) value);
        }

        return value.toString();
    }

    private String readClob(Clob clob) {
        if (clob == null) {
            return "NULL";
        }

        try (Reader reader = clob.getCharacterStream()) {
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            return builder.toString();
        } catch (Exception e) {
            return "<CLOB read error: " + e.getMessage() + ">";
        }
    }

    private Timestamp parseIsoTimestamp(String isoTimestamp) {
        if (isoTimestamp == null || isoTimestamp.trim().isEmpty()) {
            throw new IllegalArgumentException("Timestamp value is required");
        }

        String trimmed = isoTimestamp.trim();

        try {
            OffsetDateTime odt = OffsetDateTime.parse(trimmed);
            return Timestamp.from(odt.toInstant());
        } catch (DateTimeParseException ex) {
            LocalDateTime ldt = LocalDateTime.parse(trimmed);
            return Timestamp.valueOf(ldt);
        }
    }

}
