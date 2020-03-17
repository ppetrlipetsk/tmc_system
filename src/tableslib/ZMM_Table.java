package tableslib;

import databaselib.DBEngine;
import databaselib.QueryRepository;
import loglib.ErrorsClass;
import loglib.Logger;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ZMM_Table extends TTable implements ITableRouter {
    public ZMM_Table(String sourceTable, String destinationTable) {
        super(sourceTable, destinationTable);
    }
    @Override
    public boolean updateRecord(ResultSet resultSet) {
        long potrebnost_pen=0;
        int pozitsiya_potrebnosti_pen=0;
        String query;
        String values= null;

        try {
            values = getUpdateQueryStr(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка гененирования строки запроса обновления записи.", true);
            Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка гененирования строки запроса обновления записи. \n"+e.getMessage(), true);
            return false;

        }

        try {
            potrebnost_pen=resultSet.getLong("potrebnost_pen");
            pozitsiya_potrebnosti_pen=resultSet.getInt("pozitsiya_potrebnosti_pen");
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка чтения IDN", true);
            Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка чтения IDN\n"+e.getMessage(), true);
            return false;
        }

            try {
            query=QueryRepository.getZMMUpdateQuery().replace("@values@",values).replace("@potrebnost_pen@",new Long(potrebnost_pen).toString()).replace("@pozitsiya_potrebnosti_pen@",new Integer(pozitsiya_potrebnosti_pen).toString());
            DBEngine.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка обновления строки.", true);
            Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка обновления строки.\n"+e.getMessage(), true);
            return false;
        }
        return true;
    }

    @Override
    public void insertRecord(ResultSet resultSet) {
            StringBuilder fieldsStr=new StringBuilder();
            StringBuilder valuesStr=new StringBuilder();
            generateInsertQueryArguments(resultSet, fieldsStr, valuesStr);
            try {
                String query=QueryRepository.getZMMInsertQuery().replace("@values@",valuesStr).replace("@fields@",fieldsStr);
                DBEngine.execute(query);
            } catch (SQLException e) {
                ErrorsClass.recordInsertError(e);
            }
    }

    private String getDeleteLineQuery(String[] keys){
        return QueryRepository.getZMMDeleteQuery().replace("@potrebnost_pen@",keys[0]).replace("@pozitsiya_potrebnosti_pen@",keys[1]);
    }

    public boolean deleteLine(String[] keys) {
        String query=getDeleteLineQuery(keys);
            try {
                DBEngine.execute(query);
                Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Запись IDN="+keys[0]+"_"+keys[1]+" успешно удалена.", true);
            } catch (SQLException e) {
                e.printStackTrace();
                Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка удаления записи. IDN="+keys[0]+"_"+keys[1], true);
                Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка удаления записи.IDN="+keys[0]+"_"+keys[1]+e.getMessage()+"\n QUERY="+query, true);
                return false;
            }
            return true;
        }

}
