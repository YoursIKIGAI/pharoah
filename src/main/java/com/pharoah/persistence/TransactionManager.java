package com.pharoah.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Manages database transactions across multiple operations using pure JDBC.
 * 
 * Demonstrates:
 * 1. Disabling autocommit via conn.setAutoCommit(false).
 * 2. Atomic multi-table updates: all operations succeed or all fail together.
 * 3. Explicit conn.commit() when operations complete successfully.
 * 4. Explicit conn.rollback() in catch block if any exception occurs.
 * 5. Restoring connection state in finally block.
 */
public class TransactionManager {

    @FunctionalInterface
    public interface TransactionAction<T> {
        T execute(Connection conn) throws SQLException;
    }

    @FunctionalInterface
    public interface VoidTransactionAction {
        void execute(Connection conn) throws SQLException;
    }

    private final DatabaseConnectionFactory connectionFactory;

    public TransactionManager(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * Executes a transactional block returning a result.
     * Automatically commits on success, rolls back on any SQLException or RuntimeException.
     */
    public <T> T executeInTransaction(TransactionAction<T> action) throws SQLException {
        Connection conn = null;
        try {
            conn = connectionFactory.getConnection();
            conn.setAutoCommit(false); // 1. Begin Transaction

            T result = action.execute(conn);

            conn.commit(); // 2. Commit Transaction
            System.out.println("[TRANSACTION] Committed successfully.");
            return result;

        } catch (SQLException | RuntimeException e) {
            System.err.println("[TRANSACTION FAILED] Rolling back changes due to: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback(); // 3. Rollback Transaction
                    System.err.println("[TRANSACTION] Rollback completed.");
                } catch (SQLException rollbackEx) {
                    System.err.println("[TRANSACTION ERROR] Failed during rollback: " + rollbackEx.getMessage());
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Reset state
                    conn.close();
                } catch (SQLException closeEx) {
                    System.err.println("[TRANSACTION] Failed to close connection: " + closeEx.getMessage());
                }
            }
        }
    }

    /**
     * Executes a void transactional block.
     */
    public void executeInTransaction(VoidTransactionAction action) throws SQLException {
        executeInTransaction(conn -> {
            action.execute(conn);
            return null;
        });
    }
}
