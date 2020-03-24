package tableslib;

import java.sql.ResultSet;

public interface ITableRouter {
    boolean updateRecord(ResultSet resultSet);

    void insertRecord(ResultSet resultSet);

    boolean deleteLine(String[] keys);

}
