package com.mcp.oracle.service;

import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
 * @author yue9527
 */
@Service
public class OracleToolService {

    private final OracleToolConfig oracleToolConfig;

    /**
     * Constructor for OracleService
     * 
     * @param oracleToolConfig Oracle database configuration
     * @author yue9527
     */
    public OracleToolService(OracleToolConfig oracleToolConfig) {
        this.oracleToolConfig = oracleToolConfig;
    }

    /**
     * Get Oracle database connection
     * 
     * @return OracleConnection instance
     * @throws Exception if connection fails
     * @author yue9527
     */
    private OracleConnection getConnection() throws Exception {
        OracleDataSource ds = new OracleDataSource();
        ds.setURL(oracleToolConfig.getConnectionString());
        ds.setUser(oracleToolConfig.getUsername());
        ds.setPassword(oracleToolConfig.getPassword());
        return (OracleConnection) ds.getConnection();
    }

    /**
     * Get a list of all tables in Oracle database
     * Returns a newline-separated list of table names
     * 
     * @return String containing list of table names
     * @author yue9527
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

            return tables.stream()
                    .collect(Collectors.joining("\n"));
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
     * @author yue9527
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
     * @author yue9527
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
                            result.append(value != null ? value.toString() : "NULL");
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

}