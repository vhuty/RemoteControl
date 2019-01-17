package com.example.username.remotecontrol.connections;

import android.util.Log;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class ConnectorMySQL {
    private final String TAG = "connectionMySQL";

    private final String DB_LOGIN = "diagoby";
    private final String DB_PASSWORD = "Cmdsdb1";
    private final String DB_URL = "jdbc:mysql://127.0.0.1:3306/";
    protected final String DB_NAME = "diagoby";

    private Connection DB_CONNECTION;
    protected Statement DB_STATEMENT;

    public ConnectorMySQL() {
        try {
            DB_CONNECTION = DriverManager.getConnection(DB_URL + DB_NAME, DB_LOGIN, DB_PASSWORD);
            DB_STATEMENT = DB_CONNECTION.createStatement();
            DB_STATEMENT.execute("set character set utf8");
            DB_STATEMENT.execute("set names utf8");
        } catch (SQLException e) {
            Log.d(TAG, "Connection to MySQL-database exception", e);
        }
    }
}

