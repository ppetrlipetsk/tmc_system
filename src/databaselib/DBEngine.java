package databaselib;
import throwlib.FieldTypeError;

import java.lang.reflect.InvocationTargetException;

import java.sql.*;

public class DBEngine {
    protected static Connection connection;


    public Connection getConnection() {
        return connection;
    }

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public static final String DRV_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";

    public static Connection connectDB(String url, String username, String password, String instanceName, String databaseName) throws SQLException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Object drv=initDriver();
        if (drv!=null)
            connection = initConnection(url, username, password, instanceName, databaseName);
        else
            throw new ClassNotFoundException("Драйвер JDBC не найден!");
        return connection;
    }

    public static Object initDriver() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Object res=Class.forName(DRV_NAME).getDeclaredConstructor().newInstance();
        return res;
    }

    public static Connection initConnection(String url, String username, String password, String instanceName, String databaseName) throws SQLException {
        Connection connection = null;
        String connectionString = String.format(url, instanceName, databaseName, username, password);
        System.out.println("begin connection");
        System.out.println(connectionString);
        connection = DriverManager.getConnection(connectionString);
        System.out.println("Connection to DB succesfull!");
        return connection;
    }


    /**
     * Выполняет запрос в БД и не возвращает результирующий набор записей
     * @param connection - соединение с БД Connection
     * @param expr- выражение
     * @return - Statement, усли успешно, иначе null.
     * @throws SQLException
     */
    public static Statement execute(Connection connection, String expr) throws SQLException {
        Statement statement = connection.createStatement();
        if (statement.execute(expr)) return statement;
        else
            return null;
    }

    /**
     * Выполняет запрос в БД и не возвращает результирующий набор записей
     * @param expr- выражение
     * @return - Statement, усли успешно, иначе null.
     * @throws SQLException
     */
    public static Statement execute(String expr) throws SQLException {
        return execute(connection,expr);
    }

    /**
     * Выполняет запрос в БД и возвращает результирующий набор записей
     * @param callback - метод обработки набора данных
     * @param expr - выражение, например: select * from tablename
     * @return набор записей
     * @throws SQLException
     */
    public static void resultExpression(String expr,ResultSetCallBackMethod callback) throws SQLException {
        resultExpression(connection,expr,callback);
    }

    /**
     * Выполняет запрос в БД и возвращает результирующий набор записей
     * @param connection - экземпляр класса Connection
     * @param expr - выражение, например: select * from tablename
     * @param callback- метод обратного вызова для обработки результатов.
     * @return набор записей
     * @throws SQLException
     */
     public static void resultExpression(Connection connection, String expr, ResultSetCallBackMethod callback) throws SQLException {
        Statement statement;
        ResultSet resultSet = null;
        statement = connection.createStatement();
        resultSet = statement.executeQuery(expr);
        if ((callback != null) && (resultSet!=null)){
                 callback.call(resultSet);
        }
        resultSet.close();
        statement.close();
    }

/*
    public static ResultSet resultPreraredExpression(Connection connection, String expr) throws SQLException {
        Statement statement;
        ResultSet resultSet = null;
        return resultSet;
    }

    public static ResultSet resultCallableExpression(Connection connection, String expr) throws SQLException {
        CallableStatement statement;
        ResultSet resultSet = null;
        statement = connection.prepareCall(expr);
        resultSet = statement.executeQuery();
        statement.close();
        return resultSet;
    }
    public static ResultSet resultCallableExpression(String expr) throws SQLException {
        return resultCallableExpression(connection,expr);
    }

    /**
     * Выполняет запрос в БД и возвращает результирующий набор записей
     * @param expr - выражение, например: select * from tablename
     * @return набор записей
     * @throws SQLException
     */
  /*
    public static ResultSet resultExpression(String expr) throws SQLException {
        ResultSet resultSet = resultExpression(connection,expr);
        return resultSet;
    }
*/
    /**
     * Возвращает количество записей в наборе данных
     * @param resultSet - набор данных
     * @return количество записей
     * @throws SQLException
     */
    public static int getRowCount(ResultSet resultSet) throws SQLException {
        int rowCount = 0;
        resultSet.last();
        rowCount = resultSet.getRow();
        return rowCount;
    }


    /**
     * Выполняет вставку строки в таблицу БД и возвращает id вставленной строки
     * @param query - запрос
     * @return уникальный ключ вставленной записи
     * @throws SQLException
     */
    public static long insertQuery(String query) throws SQLException {
        String[] returnId={"id"};
        long id=-1;
        PreparedStatement statement = connection.prepareStatement(query, returnId);
        int affectedRows = statement.executeUpdate();
        if (affectedRows ==0){
            throw new SQLException("Creating user failed, no rows affected.");
        }

        try (ResultSet rs = statement.getGeneratedKeys()) {
            if (rs.next()) {
                id=rs.getInt(1);
            }
            rs.close();
        }
        statement.close();
        return id;
    }

    /**
     * Выполняет вставку строки в таблицу БД и возвращает id вставленной строки
     * Отличие от InsertQuery в том, что insertPreparedQuery предварительно подготавливает запрос с помощью вызова метода IStatementFieldsSetter.setValues(statement)
     * @param query - запрос
     * @param filler - метод, заполняющий данными запрос
     * @return уникальный ключ вставленной записи
     *
     * @throws SQLException
     */
    public static long insertPreparedQuery(String query, IStatementFieldsSetter filler) throws SQLException, FieldTypeError {
        String[] returnId={"id"};
        long id=-1;
        PreparedStatement statement = connection.prepareStatement(query, returnId);
        filler.setValues(statement);
        int affectedRows = statement.executeUpdate();
        if (affectedRows ==0){
            throw new SQLException("Creating user failed, no rows affected.");
        }
        try (ResultSet rs = statement.getGeneratedKeys()) {
            if (rs.next()) {
                id=rs.getInt(1);
            }
            rs.close();
        }
        statement.close();
        return id;
    }

    public interface ResultSetCallBackMethod {
        void call(ResultSet resultSet);
    }

}