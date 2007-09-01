/*
 * Copyright 2004-2007 H2 Group. Licensed under the H2 License, Version 1.0 (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package org.h2.test.synth;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TestHaltApp extends TestHalt {

    private int rowCount;

    public static void main(String[] args) throws Exception {
        baseDir = TestHalt.DIR;
        new TestHaltApp().start(args);
    }

    private void execute(Statement stat, String sql) throws SQLException {
        traceOperation("execute: " + sql);
        stat.execute(sql);
    }

    protected void testInit() throws SQLException {
        Statement stat = conn.createStatement();
        // stat.execute("CREATE TABLE TEST(ID IDENTITY, NAME VARCHAR(255))");
        for (int i = 0; i < 20; i++) {
            execute(stat, "DROP TABLE IF EXISTS TEST" + i);
            execute(stat, "CREATE TABLE TEST" + i + "(ID INT PRIMARY KEY, NAME VARCHAR(255))");
        }
        for (int i = 0; i < 20; i += 2) {
            execute(stat, "DROP TABLE TEST" + i);
        }
        execute(stat, "DROP TABLE IF EXISTS TEST");
        execute(stat, "CREATE TABLE TEST(ID BIGINT GENERATED BY DEFAULT AS IDENTITY, NAME VARCHAR(255), DATA CLOB)");
    }

    protected void testWaitAfterAppStart() throws Exception {
        int sleep = 10 + random.nextInt(300);
        if ((flags & FLAG_NO_DELAY) == 0) {
            sleep += 1000;
        }
        Thread.sleep(sleep);
    }

    protected void testCheckAfterCrash() throws Exception {
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM TEST");
        rs.next();
        int count = rs.getInt(1);
        System.out.println("count: " + count);
        if (count % 2 == 1) {
            traceOperation("row count: " + count);
            throw new Exception("Unexpected odd row count");
        }
    }

    protected void appStart() throws SQLException {
        Statement stat = conn.createStatement();
        if ((flags & FLAG_NO_DELAY) != 0) {
            execute(stat, "SET WRITE_DELAY 0");
            execute(stat, "SET MAX_LOG_SIZE 1");
        }
        ResultSet rs = stat.executeQuery("SELECT COUNT(*) FROM TEST");
        rs.next();
        rowCount = rs.getInt(1);
        trace("rows: " + rowCount, null);
    }

    protected void appRun() throws Exception {
        conn.setAutoCommit(false);
        traceOperation("setAutoCommit false");
        int rows = 10000 + value;
        PreparedStatement prepInsert = conn.prepareStatement("INSERT INTO TEST(NAME, DATA) VALUES('Hello World', ?)");
        PreparedStatement prepUpdate = conn
                .prepareStatement("UPDATE TEST SET NAME = 'Hallo Welt', DATA = ? WHERE ID = ?");
        for (int i = 0; i < rows; i++) {
            Statement stat = conn.createStatement();
            if ((operations & OP_INSERT) != 0) {
                if ((flags & FLAG_LOBS) != 0) {
                    String s = getRandomString(random.nextInt(200));
                    prepInsert.setString(1, s);
                    traceOperation("insert " + s);
                    prepInsert.execute();
                } else {
                    execute(stat, "INSERT INTO TEST(NAME) VALUES('Hello World')");
                }
                ResultSet rs = stat.getGeneratedKeys();
                rs.next();
                int key = rs.getInt(1);
                traceOperation("inserted key: " + key);
                rowCount++;
            }
            if ((operations & OP_UPDATE) != 0) {
                if ((flags & FLAG_LOBS) != 0) {
                    String s = getRandomString(random.nextInt(200));
                    prepUpdate.setString(1, s);
                    int x = random.nextInt(rowCount + 1);
                    prepUpdate.setInt(2, x);
                    traceOperation("update " + s + " " + x);
                    prepUpdate.execute();
                } else {
                    int x = random.nextInt(rowCount + 1);
                    execute(stat, "UPDATE TEST SET VALUE = 'Hallo Welt' WHERE ID = " + x);
                }
            }
            if ((operations & OP_DELETE) != 0) {
                int x = random.nextInt(rowCount + 1);
                traceOperation("deleting " + x);
                int uc = stat.executeUpdate("DELETE FROM TEST WHERE ID = " + x);
                traceOperation("updated: " + uc);
                rowCount -= uc;
            }
            traceOperation("rowCount " + rowCount);
            trace("rows now: " + rowCount, null);
            if (rowCount % 2 == 0) {
                traceOperation("commit " + rowCount);
                conn.commit();
                trace("committed: " + rowCount, null);
            }
            if ((flags & FLAG_NO_DELAY) != 0) {
                if (random.nextInt(100) == 0) {
                    execute(stat, "CHECKPOINT");
                }
            }
        }
        traceOperation("rollback");
        conn.rollback();
    }

}
