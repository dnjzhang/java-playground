package com.mcp.oracle;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.mcp.oracle.service.OracleToolService;

import lombok.extern.slf4j.Slf4j;

/**
 * Test class for OracleService
 * Contains unit tests for all OracleService methods
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class OracleToolServiceTest {

    @Autowired
    private OracleToolService oracleToolService;

    /**
     * Test listTables method
     * Verifies that the method returns a non-empty list of tables
     * 
     */
    @Test
    public void testListTables() {
        String result = oracleToolService.listTables();
        log.info(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertFalse(result.startsWith("Error:"));
    }

    /**
     * Test describeTable method
     * Verifies that the method returns valid table structure information
     * 
     */
    @Test
    public void testDescribeTable() {
        // First get a table name
        String tableName = oracleToolService.listTables().split("\n")[0];
        log.info("firstTable---"+tableName);
        String result = oracleToolService.describeTable(tableName);
        log.info(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertFalse(result.startsWith("Error:"));
        assertTrue(result.contains("COLUMN_NAME"));
    }

    /**
     * Test executeSql method with valid SELECT query
     * Verifies that the method returns valid query results
     * 
     */
    @Test
    public void testExecuteQuery() {
        // First get a table name
        String tableName = oracleToolService.listTables().split("\n")[0];
        
        String query = "SELECT * FROM " + tableName + " WHERE ROWNUM <= 5";
        String result = oracleToolService.executeSql(query);
        log.info(result);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertFalse(result.startsWith("Error:"));
    }

    /**
     * Test executeSql method with invalid query
     * Verifies that the method handles non-SELECT statements appropriately
     * 
     */
    @Test
    public void testExecuteQueryWithInvalidQuery() {
        String result = oracleToolService.executeSql("INSERT INTO test VALUES (1)");
        log.info(result);
        assertTrue(result.startsWith("Error"));
    }

} 