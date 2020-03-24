package tableslib;

import databaselib.DBEngine;
import defines.FieldTypeDefines;
import loglib.ErrorsClass;
import loglib.Logger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class TTable  implements ITableRouter {
    private final String destinationTable;
    private HashMap<String, FieldTypeDefines.FieldType> aliases=new HashMap<>();

    public void setAliases(HashMap<String, FieldTypeDefines.FieldType> aliases) {
        this.aliases = aliases;
    }

    public String getDestinationTable() {
        return destinationTable;
    }

    public TTable(String destinationTable) {
      //  this.sourceTable = sourceTable;
        this.destinationTable=destinationTable;
    }

    protected boolean generateInsertQueryArguments(ResultSet resultSet, StringBuilder fieldsStr, StringBuilder valuesStr) {
        for (Map.Entry<String, FieldTypeDefines.FieldType> alias: aliases.entrySet()){
            String fieldName=alias.getKey();
            FieldTypeDefines.FieldType fieldType=alias.getValue();
            if (fieldsStr.length()>0)fieldsStr.append(",");
            fieldsStr.append(fieldName);
            if (valuesStr.length()>0)valuesStr.append(",");
            try {
                valuesStr.append(getFieldValueStr(fieldType,resultSet.getString(fieldName)));
            } catch (SQLException e) {
                e.printStackTrace();
                Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка генерирования выражения вставки строки. FieldName="+fieldName, true);
                Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка генерирования выражения вставки строки. FieldName="+fieldName+"\n"+e.getMessage(), true);
                return false;
            }
        }
        return true;
    }

    protected String getUpdateQueryValues(ResultSet resultSet) throws SQLException {

        StringBuilder valuesStr=new StringBuilder();

        for (Map.Entry<String, FieldTypeDefines.FieldType> alias: aliases.entrySet()){
            String fieldName=alias.getKey();
            FieldTypeDefines.FieldType fieldType=alias.getValue();

            if (valuesStr.length()>0){
                valuesStr.append(",");
            }
            valuesStr.append(fieldName).append("=");
                valuesStr.append(getFieldValueStr(fieldType,resultSet.getString(fieldName)));
        }
        return valuesStr.toString();
    }

    protected String getFieldValueStr(FieldTypeDefines.FieldType fieldType,String valueStr ){
        String mask=FieldTypeDefines.getTypesFieldDBMask().get(fieldType);
        if (valueStr==null)
            return "null";
        else
        return mask.replace("@value@", valueStr);
    }

    @Override
    public boolean updateRecord(ResultSet resultSet) {
        String values= null;

        try {
            values = getUpdateQueryValues(resultSet);
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка гененирования строки запроса обновления записи.", true);
            Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка гененирования строки запроса обновления записи. \n"+e.getMessage(), true);
            return false;

        }

        try {

            String query=getUpdateQueryFromTable(resultSet).replace("@values@",values);
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
            String query = getInsertQueryStr(fieldsStr, valuesStr);
            DBEngine.execute(query);
        } catch (SQLException e) {
            ErrorsClass.recordInsertError(e);
        }
    }

    @Override
    public boolean deleteLine(String[] keys) {
        String query=getDeleteLineQuery(keys);
        try {
            DBEngine.execute(query);
            //Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Запись IDN="+keys.toString()+" успешно удалена.", true);
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка удаления записи. IDN="+keys.toString(), true);
            Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка удаления записи.IDN="+keys.toString()+e.getMessage()+"\n QUERY="+query, true);
            return false;
        }
        return true;
    }

    abstract String getInsertQueryStr(StringBuilder fieldsStr, StringBuilder valuesStr);

    abstract String getDeleteLineQuery(String[] keys);

    public abstract String getDifferenceViewQuery();

    public abstract String getAddedLinesQuery();

    public abstract String getDeletedRecordsQuery(String changedRecords);

    public abstract String getImportDifferenceRecordsQuery(String range);

    protected abstract String getUpdateQueryFromTable(ResultSet resultSet) throws SQLException;

    public abstract String[] getKeys(String idn);
}
