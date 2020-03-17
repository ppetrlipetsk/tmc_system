package tableslib;

import databaselib.DBEngine;
import databaselib.QueryRepository;
import defines.FieldTypeDefines;
import loglib.ErrorsClass;
import loglib.Logger;
import loglib.MessagesClass;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.*;

public class TTable {
    private final String sourceTable;
    private final String destinationTable;
    private HashMap<String,FieldStateType> difLines;
    private LinkedList<String> deletedLines;
    private HashMap<String, FieldTypeDefines.FieldType> aliases=new HashMap<>();


    public TTable(String sourceTable, String destinationTable) {
        this.sourceTable = sourceTable;
        this.destinationTable=destinationTable;
        difLines=new HashMap<>();
        deletedLines=new LinkedList<>();
    }


    public boolean getAliases() throws SQLException {
        String query=QueryRepository.getAliasesQuery().replace("@tablename@",this.destinationTable);
        try {
            DBEngine.resultExpression(query, new AliasesFill());
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка чтения псевдонимов полей.", true);
            Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка чтения псевдонимов полей.\n"+e.getMessage()+"\n QUERY="+query, true);
            return false;
        }
        return true;
    }

    public boolean getExceptedRecords() {
        String query=QueryRepository.getZMMDifferenceView();

        try {
            DBEngine.resultExpression(query, new DifferenceSelectCallBack());
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка чтения представления, содержащего измененные записи.", true);
            Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка чтения представления, содержащего измененные записи.\n"+e.getMessage()+"\n QUERY="+query, true);
            return false;
        }
        return true;
    }

    public void detectAddedRecords() throws SQLException {
        String query=QueryRepository.getZMMAddedLines();
        try {
            DBEngine.resultExpression(query, new AddedSelectCallBack());
        } catch (SQLException e) {
            ErrorsClass.addedRecordsViewReadError(e,query);
        }
    }

    public void detectDeletedRecords() throws SQLException {
        String query=QueryRepository.getZMMDeletedLines();
        query=query.replace("@dataset@",getDiffValuesStr());
        try {
            DBEngine.resultExpression(query, new DeletedSelectCallBack());
        } catch (SQLException e) {
            ErrorsClass.deletedRecordsViewReadError(e,query);
        }
    }

    /**
     * Возвражает строку ключей, записей, попавших в выборку разницы таблицы импорта и действующей таблицы
     * @return
     */
    private CharSequence getDiffValuesStr() {
        StringBuilder line=new StringBuilder();
        for (Map.Entry<String, FieldStateType> entry : difLines.entrySet()) {
            if (line.length()>0) line.append(",");
            line.append("'").append(entry.getKey()).append("'");

        }
        return line.toString();
    }

    public int delRecords() {
        int count=0;
        for(String idn:deletedLines){
            String[] keys=idn.split("_");
            if (deleteLine(keys)) count++;
        }
        MessagesClass.deletedRecordCountMessage(count);
        return count;
    }

    private boolean deleteLine(String[] keys) {
        String query=QueryRepository.getZMMDeleteQuery().replace("@potrebnost_pen@",keys[0]).replace("@pozitsiya_potrebnosti_pen@",keys[1]);
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

    public boolean changeRecords() {
        //Выбираем только те записи, которые новые или измененные в таблице импорта
        String query=QueryRepository.getZMMImportDifRecords().replace("@range@",getDiffValuesStr());
        try {
            DBEngine.resultExpression(query, new fromImportToZMM());
        } catch (SQLException e) {
            ErrorsClass.changeRecordsError(e,query);
        }
        return false;
    }

    /*
    public void addRecords() {
        String query=QueryRepository.getZMMImportDifRecords().replace("@range@",getDiffValuesStr());
        try {
            DBEngine.resultExpression(query, new fromImportToZMM());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
*/

    class DifferenceSelectCallBack implements DBEngine.ResultSetCallBackMethod {
        @Override
        public void call(ResultSet resultSet) throws SQLException {
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        difLines.put(resultSet.getString("idn"),FieldStateType.UPDATE);
                    }
                }
                catch (SQLException e){
                    e.printStackTrace();
                    Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка чтения поля IDN записи БД." , true);
                    Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка чтения поля IDN записи БД \n"+e.getMessage(), true);
                    throw new SQLException(e);
                }
            }
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Определение измененных записей-OK \nНайдено:"+difLines.size(), true);
        }
    }


    class AddedSelectCallBack implements DBEngine.ResultSetCallBackMethod {
        @Override
        public void call(ResultSet resultSet) throws SQLException {
            int count=0;
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        difLines.put(resultSet.getString("idn"),FieldStateType.INSERT);
                        count++;
                    }
                }
                catch (SQLException e){
                    loglib.ErrorsClass.fieldReadErrorLog(e);
                }
                loglib.MessagesClass.addedFieldsMessage(count);
            }
            else
                loglib.MessagesClass.noAddedFieldsMessage();

        }
    }

    class DeletedSelectCallBack implements DBEngine.ResultSetCallBackMethod {
        @Override
        public void call(ResultSet resultSet) throws SQLException {
            if ((resultSet!=null)){
                int count=0;
                try {
                    while (resultSet.next()) {
                        deletedLines.add(resultSet.getString("idn"));
                        count++;

                    }
                }
                catch (SQLException e){
                    ErrorsClass.deletedRecordsReadError(e);
                }
                MessagesClass.deletedRecordsMessage(count);
            }
        }
    }

    private class AliasesFill implements DBEngine.ResultSetCallBackMethod {

        @Override
        public void call(ResultSet resultSet) throws SQLException {
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        String fieldalias=resultSet.getString("fieldalias");
                        FieldTypeDefines.FieldType fieldType=detectFieldType(resultSet.getString("fieldtype"));
                        aliases.put(fieldalias, fieldType);
                    }
                }
                catch (SQLException e){
                    Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка чтения поля FIELDALIAS записи БД, при чтении таблицы псевдонимов полей." , true);
                    Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка чтения поля FIELDALIAS записи БД, при чтении таблицы псевдонимов полей. \n"+e.getMessage(), true);
                    throw new SQLException(e);
                }
            }
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Чтение таблицы псевдонимов полей... Найдено:"+aliases.size() , true);
        }
    }

    private FieldTypeDefines.FieldType detectFieldType(String fieldtype) {
        return FieldTypeDefines.FieldType.valueOf(fieldtype);
    }

    private class fromImportToZMM implements DBEngine.ResultSetCallBackMethod {
        @Override
        public void call(ResultSet resultSet) throws SQLException {
            String idn="";
            int countUpdate=0;
            int countAdd=0;
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        idn= resultSet.getString("idn");
                        FieldStateType fieldType=difLines.get(idn);
                        if (FieldStateType.INSERT==fieldType){
                            insertRecord(resultSet);
                            countAdd++;
                        }
                        else{
                            if (FieldStateType.UPDATE==fieldType){
                                updateRecord(resultSet);
                                countUpdate++;
                            }
                        }
                    }
                }
                catch (SQLException e){
                    ErrorsClass.recordUpdateError(e, idn);
                }
                MessagesClass.updateRecordsCountMessage(countUpdate);
                MessagesClass.insertRecordsCountMessage(countAdd);
            }
        }
    }

    private void insertRecord(ResultSet resultSet) {
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

    private boolean generateInsertQueryArguments(ResultSet resultSet, StringBuilder fieldsStr, StringBuilder valuesStr) {
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

    private boolean updateRecord(ResultSet resultSet) {
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

    private String getUpdateQueryStr(ResultSet resultSet) throws SQLException {

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

    private String getFieldValueStr(FieldTypeDefines.FieldType fieldType,String valueStr ){
        String mask=FieldTypeDefines.getTypesFieldDBMask().get(fieldType);
        if (valueStr==null)
            return "null";
        else
        return mask.replace("@value@", valueStr);
    }

    enum FieldStateType{
        INSERT,UPDATE
    }

}
