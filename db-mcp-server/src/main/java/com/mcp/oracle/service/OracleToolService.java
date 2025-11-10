package com.mcp.oracle.service;

import java.io.Reader;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(OracleToolService.class);

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
     * Returns pretty-printed JSON containing the table count and table names.
     *
     * @return JSON string describing available tables
     */
    @Tool(name = "list_tables", description = "Get a list of all tables in CPP database")
    public String listTables() {
        log.info("list_tables tool invoked");
        try (OracleConnection conn = getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT table_name FROM user_tables ORDER BY table_name")) {

            List<String> tables = new ArrayList<>();
            while (rs.next()) {
                tables.add(rs.getString(1));
            }

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"tableCount\": ").append(tables.size()).append(",\n");
            json.append("  \"tables\": [\n");

            for (int i = 0; i < tables.size(); i++) {
                String tableName = tables.get(i);
                json.append("    ");
                if (tableName == null) {
                    json.append("null");
                } else {
                    String safeName = tableName.trim();
                    if (safeName.isEmpty()) {
                        safeName = "(unnamed table)";
                    }
                    json.append("\"").append(jsonEscape(safeName)).append("\"");
                }

                if (i < tables.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}");

            return json.toString();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Get structure information of specified table in Oracle database.
     * Returns pretty-printed JSON describing each column and primary key metadata.
     *
     * @param tableName name of the table to describe
     * @return JSON string containing column metadata
     */
    @Tool(name = "describe_table", description = "Get structure information of specified table in Oracle database")
    public String describeTable(@ToolParam(description = "Table name to describe") String tableName) {
        log.info("describe_table tool invoked for tableName='{}'", abbreviateForLog(tableName));
        if (tableName == null || tableName.trim().isEmpty()) {
            return "Error: table name is required.";
        }

        String trimmedTable = tableName.trim();
        String upperTable = trimmedTable.toUpperCase();

        try (OracleConnection conn = getConnection()) {
            List<String> pkColumnOrder = new ArrayList<>();
            Set<String> pkColumns = new HashSet<>();

            try (PreparedStatement pkStmt = conn.prepareStatement(
                    "SELECT cols.column_name FROM all_constraints cons, all_cons_columns cols "
                            + "WHERE cons.constraint_type = 'P' AND cons.constraint_name = cols.constraint_name "
                            + "AND cons.owner = cols.owner AND cols.table_name = ?")) {
                pkStmt.setString(1, upperTable);

                try (ResultSet rs = pkStmt.executeQuery()) {
                    while (rs.next()) {
                        String pk = rs.getString(1);
                        if (pk != null) {
                            pkColumnOrder.add(pk);
                            pkColumns.add(pk.toUpperCase());
                        }
                    }
                }
            }

            List<ColumnDetail> columns = new ArrayList<>();
            try (PreparedStatement columnStmt = conn.prepareStatement(
                    "SELECT column_name, data_type, nullable, data_length "
                            + "FROM user_tab_columns WHERE table_name = ? ORDER BY column_id")) {
                columnStmt.setString(1, upperTable);

                try (ResultSet rs = columnStmt.executeQuery()) {
                    while (rs.next()) {
                        ColumnDetail detail = new ColumnDetail();
                        detail.name = rs.getString("COLUMN_NAME");
                        detail.dataType = rs.getString("DATA_TYPE");

                        String nullableFlag = rs.getString("NULLABLE");
                        detail.nullable = nullableFlag != null && nullableFlag.equalsIgnoreCase("Y");

                        int length = rs.getInt("DATA_LENGTH");
                        detail.dataLength = rs.wasNull() ? null : Integer.valueOf(length);

                        String normalizedName = detail.name == null ? null : detail.name.toUpperCase();
                        detail.primaryKey = normalizedName != null && pkColumns.contains(normalizedName);

                        columns.add(detail);
                    }
                }
            }

            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"table\": \"").append(jsonEscape(trimmedTable)).append("\",\n");
            json.append("  \"tableUpper\": \"").append(jsonEscape(upperTable)).append("\",\n");
            json.append("  \"found\": ").append(columns.isEmpty() ? "false" : "true").append(",\n");
            json.append("  \"primaryKeyColumns\": [\n");

            for (int i = 0; i < pkColumnOrder.size(); i++) {
                String pk = pkColumnOrder.get(i);
                json.append("    \"").append(jsonEscape(pk)).append("\"");
                if (i < pkColumnOrder.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ],\n");
            json.append("  \"columnCount\": ").append(columns.size()).append(",\n");
            json.append("  \"columns\": [\n");

            for (int i = 0; i < columns.size(); i++) {
                ColumnDetail detail = columns.get(i);
                json.append("    {\n");
                json.append("      \"name\": ").append(detail.name == null ? "null" : "\"" + jsonEscape(detail.name) + "\"").append(",\n");
                json.append("      \"dataType\": ").append(detail.dataType == null ? "null" : "\"" + jsonEscape(detail.dataType) + "\"").append(",\n");
                json.append("      \"nullable\": ").append(detail.nullable ? "true" : "false").append(",\n");
                json.append("      \"dataLength\": ").append(detail.dataLength == null ? "null" : detail.dataLength.toString()).append(",\n");
                json.append("      \"primaryKey\": ").append(detail.primaryKey ? "true" : "false").append("\n");
                json.append("    }");
                if (i < columns.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
            }

            json.append("  ]\n");
            json.append("}");

            return json.toString();
        } catch (Exception e) {
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
        log.info("execute_sql tool invoked with sql='{}'", abbreviateForLog(sql));
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
     * Retrieve the most recent LOG entries for a component identifier that were created before the provided timestamp.
     * Results are ordered from newest to oldest.
     *
     * @param compId     component identifier to search
     * @param beforeIso  exclusive upper bound timestamp in ISO8601 format
     * @param maxRecords maximum number of records to return; negative values return all matches
     * @return pretty-printed JSON containing the matching log entries
     */
    @Tool(name = "retrieve_log", description = "Retrieve LOG entries before a timestamp for a component identifier")
    public String retrieveLog(
            @ToolParam(description = "Component identifier to search") String compId,
            @ToolParam(description = "Exclusive upper bound timestamp (ISO8601)") String beforeIso,
            @ToolParam(description = "Maximum number of records to return; negative for all") int maxRecords) {
        log.info("retrieve_log tool invoked with compId='{}', beforeIso='{}', maxRecords={}",
                abbreviateForLog(compId), abbreviateForLog(beforeIso), maxRecords);

        if (compId == null || compId.trim().isEmpty()) {
            return "Error: comp_id is required.";
        }

        try {
            Timestamp before = parseIsoTimestamp(beforeIso);

            String baseSql = "SELECT ID, COMP_TYPE_ID, COMP_ID, COMP_UID, CREATED, LOG_LEVEL_ID, LOG_CATEGORY_ID, "
                    + "LOG_SUB_CATEGORY, ENTRY, USER_DEF_ID, EXECUTION_CONTEXT, LOG_ERROR_CATEGORY_ID, LOG_CODE, "
                    + "API_CONTEXT FROM LOG WHERE COMP_ID = ? AND CREATED < ? ORDER BY CREATED DESC, ID DESC";

            String sql = maxRecords < 0
                    ? baseSql
                    : "SELECT * FROM (" + baseSql + ") WHERE ROWNUM <= ?";

            try (OracleConnection conn = getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {

                ps.setString(1, compId.trim());
                ps.setTimestamp(2, before);
                if (maxRecords >= 0) {
                    ps.setInt(3, maxRecords);
                }

                List<LogRecord> records = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        records.add(mapLogRecord(rs));
                    }
                }

                populateLogCategoryNames(conn, records);

                return formatLogRecordsJson(compId.trim(), before, maxRecords, records);
            }
        } catch (IllegalArgumentException | DateTimeParseException e) {
            return "Error: Invalid timestamp format - " + e.getMessage();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Summarize LOG table entries for a specific component identifier by returning the first and last rows found.
     *
     * @param compId component identifier to summarize
     * @return JSON summary containing entry counts and first/last timestamps
     */
    @Tool(name = "summarize_log", description = "Summarize LOG entries for a component identifier")
    public String summarizeLog(
            @ToolParam(description = "Component identifier to summarize") String compId) {
        log.info("summarize_log tool invoked with compId='{}'", abbreviateForLog(compId));

        if (compId == null || compId.trim().isEmpty()) {
            return "Error: comp_id is required.";
        }

        String trimmedCompId = compId.trim();

        try (OracleConnection conn = getConnection()) {
            long totalEntries = fetchLogCount(conn, trimmedCompId);
            LogEntrySummary firstEntry = totalEntries > 0 ? fetchLogEntry(conn, trimmedCompId, true) : null;
            LogEntrySummary lastEntry = totalEntries > 0 ? fetchLogEntry(conn, trimmedCompId, false) : null;

            StringBuilder json = new StringBuilder();
            json.append("{");
            json.append("\"compId\":\"").append(jsonEscape(trimmedCompId)).append("\",");
            json.append("\"totalEntries\":").append(totalEntries).append(",");
            json.append("\"firstEntry\":").append(entryToJson(firstEntry)).append(",");
            json.append("\"lastEntry\":").append(entryToJson(lastEntry));
            json.append("}");

            return json.toString();
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

    private long fetchLogCount(OracleConnection conn, String compId) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM LOG WHERE COMP_ID = ?")) {
            ps.setString(1, compId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    private LogEntrySummary fetchLogEntry(OracleConnection conn, String compId, boolean first) throws Exception {
        String orderClause = first ? "ASC" : "DESC";
        String sql = "SELECT ID, COMP_ID, CREATED, ENTRY FROM ("
                + "SELECT ID, COMP_ID, CREATED, ENTRY FROM LOG WHERE COMP_ID = ? ORDER BY CREATED " + orderClause
                + ", ID " + orderClause
                + ") WHERE ROWNUM = 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, compId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    LogEntrySummary summary = new LogEntrySummary();
                    summary.id = rs.getLong("ID");
                    if (rs.wasNull()) {
                        summary.id = null;
                    }
                    summary.created = rs.getTimestamp("CREATED");
                    summary.entry = rs.getString("ENTRY");
                    return summary;
                }
            }
        }

        return null;
    }

    private String entryToJson(LogEntrySummary entry) {
        if (entry == null) {
            return "null";
        }

        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"id\":").append(entry.id == null ? "null" : entry.id.toString()).append(",");

        String createdIso = toIsoString(entry.created);
        Long createdUnix = toUnixMillis(entry.created);
        if (createdIso == null) {
            json.append("\"createdIso\":null,");
            json.append("\"createdUnixMs\":null,");
        } else {
            json.append("\"createdIso\":\"").append(jsonEscape(createdIso)).append("\",");
            json.append("\"createdUnixMs\":").append(createdUnix);
            json.append(",");
        }

        String entryText = entry.entry == null ? null : jsonEscape(entry.entry);
        json.append("\"entry\":").append(entryText == null ? "null" : "\"" + entryText + "\"");
        json.append("}");

        return json.toString();
    }

    private LogRecord mapLogRecord(ResultSet rs) throws Exception {
        LogRecord record = new LogRecord();
        record.id = getNullableLong(rs, "ID");
        record.compTypeId = getNullableLong(rs, "COMP_TYPE_ID");
        record.compId = rs.getString("COMP_ID");
        record.compUid = rs.getString("COMP_UID");
        record.created = rs.getTimestamp("CREATED");
        record.logLevelId = getNullableLong(rs, "LOG_LEVEL_ID");
        record.logCategoryId = getNullableLong(rs, "LOG_CATEGORY_ID");
        record.logSubCategory = rs.getString("LOG_SUB_CATEGORY");
        record.entry = rs.getString("ENTRY");
        record.userDefId = getNullableLong(rs, "USER_DEF_ID");
        record.executionContext = rs.getString("EXECUTION_CONTEXT");
        record.logErrorCategoryId = getNullableLong(rs, "LOG_ERROR_CATEGORY_ID");
        record.logCode = getNullableLong(rs, "LOG_CODE");
        record.apiContext = rs.getString("API_CONTEXT");
        return record;
    }

    private void populateLogCategoryNames(OracleConnection conn, List<LogRecord> records) throws Exception {
        if (records.isEmpty()) {
            return;
        }

        Map<Long, String> cache = new HashMap<>();

        try (PreparedStatement ps = conn.prepareStatement("SELECT NAME FROM LOG_CATEGORY WHERE ID = ?")) {
            for (LogRecord record : records) {
                Long categoryId = record.logCategoryId;
                if (categoryId == null) {
                    continue;
                }

                if (cache.containsKey(categoryId)) {
                    record.logCategoryName = cache.get(categoryId);
                    continue;
                }

                ps.setLong(1, categoryId);
                String name = null;
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        name = rs.getString(1);
                    }
                }

                cache.put(categoryId, name);
                record.logCategoryName = name;
            }
        }
    }

    private Long getNullableLong(ResultSet rs, String column) throws Exception {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private String formatLogRecordsJson(String compId, Timestamp before, int maxRecords, List<LogRecord> records) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"compId\": \"").append(jsonEscape(compId)).append("\",\n");
        json.append("  \"beforeIso\": \"").append(jsonEscape(toIsoString(before))).append("\",\n");
        json.append("  \"beforeUnixMs\": ").append(before == null ? "null" : toUnixMillis(before)).append(",\n");
        json.append("  \"maxRecords\": ").append(maxRecords).append(",\n");
        json.append("  \"returnedCount\": ").append(records.size()).append(",\n");
        json.append("  \"records\": [\n");

        for (int i = 0; i < records.size(); i++) {
            LogRecord record = records.get(i);
            json.append("    {\n");
            json.append("      \"id\": ").append(record.id == null ? "null" : record.id.toString()).append(",\n");
            json.append("      \"compTypeId\": ").append(record.compTypeId == null ? "null" : record.compTypeId.toString()).append(",\n");
            json.append("      \"compId\": ").append(record.compId == null ? "null" : "\"" + jsonEscape(record.compId) + "\"").append(",\n");
            json.append("      \"compUid\": ").append(record.compUid == null ? "null" : "\"" + jsonEscape(record.compUid) + "\"").append(",\n");
            json.append("      \"createdIso\": ").append(record.created == null ? "null" : "\"" + jsonEscape(toIsoString(record.created)) + "\"").append(",\n");
            json.append("      \"createdUnixMs\": ").append(record.created == null ? "null" : toUnixMillis(record.created)).append(",\n");
            json.append("      \"logLevelId\": ").append(record.logLevelId == null ? "null" : record.logLevelId.toString()).append(",\n");
            json.append("      \"logCategoryId\": ").append(record.logCategoryId == null ? "null" : record.logCategoryId.toString()).append(",\n");
            json.append("      \"logCategoryName\": ").append(record.logCategoryName == null ? "null" : "\"" + jsonEscape(record.logCategoryName) + "\"").append(",\n");
            json.append("      \"logSubCategory\": ").append(record.logSubCategory == null ? "null" : "\"" + jsonEscape(record.logSubCategory) + "\"").append(",\n");
            json.append("      \"entry\": ").append(record.entry == null ? "null" : "\"" + jsonEscape(record.entry) + "\"").append(",\n");
            json.append("      \"userDefId\": ").append(record.userDefId == null ? "null" : record.userDefId.toString()).append(",\n");
            json.append("      \"executionContext\": ").append(record.executionContext == null ? "null" : "\"" + jsonEscape(record.executionContext) + "\"").append(",\n");
            json.append("      \"logErrorCategoryId\": ").append(record.logErrorCategoryId == null ? "null" : record.logErrorCategoryId.toString()).append(",\n");
            json.append("      \"logCode\": ").append(record.logCode == null ? "null" : record.logCode.toString()).append(",\n");
            json.append("      \"apiContext\": ").append(record.apiContext == null ? "null" : "\"" + jsonEscape(record.apiContext) + "\"").append("\n");
            json.append("    }");
            if (i < records.size() - 1) {
                json.append(",");
            }
            json.append("\n");
        }

        json.append("  ]\n");
        json.append("}");

        return json.toString();
    }

    private String jsonEscape(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) c));
                    } else {
                        escaped.append(c);
                    }
            }
        }
        return escaped.toString();
    }

    private String toIsoString(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().atOffset(ZoneOffset.UTC).toString();
    }

    private Long toUnixMillis(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toInstant().toEpochMilli();
    }

    private String abbreviateForLog(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        int maxLength = 200;
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength) + "...";
    }

    private static class ColumnDetail {
        String name;
        String dataType;
        boolean nullable;
        Integer dataLength;
        boolean primaryKey;
    }

    private static class LogEntrySummary {
        Long id;
        Timestamp created;
        String entry;
    }

    private static class LogRecord {
        Long id;
        Long compTypeId;
        String compId;
        String compUid;
        Timestamp created;
        Long logLevelId;
        Long logCategoryId;
        String logCategoryName;
        String logSubCategory;
        String entry;
        Long userDefId;
        String executionContext;
        Long logErrorCategoryId;
        Long logCode;
        String apiContext;
    }
}
