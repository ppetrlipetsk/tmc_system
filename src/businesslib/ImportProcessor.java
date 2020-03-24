package businesslib;

import databaselib.DBEngine;
import databaselib.QueryRepository;
import defines.FieldTypeDefines;
import loglib.ErrorsClass;
import loglib.Logger;
import loglib.MessagesClass;
import tableslib.ITableRouter;
import tableslib.TTable;
import tableslib.TableTools;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class ImportProcessor {

    public  HashMap<String, FieldTypeDefines.FieldType> getAliases(String destinationTable)  {

        String query=QueryRepository.getAliasesQuery().replace("@tablename@",destinationTable);
        AliasesLoader aliasesLoader=new AliasesLoader();

        try {
            DBEngine.resultExpression(query, aliasesLoader);
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка чтения псевдонимов полей.", true);
            Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка чтения псевдонимов полей.\n"+e.getMessage()+"\n QUERY="+query, true);
        }
        return aliasesLoader.getAliases();
    }

    public HashMap<String, FieldStateType> getChangedRecords(TTable table) {
        String query=table.getDifferenceViewQuery();
        HashMap<String, FieldStateType> changedRecords=null;
        DifferenceSelectCallBack differenceSelectCallBack=new DifferenceSelectCallBack();
        try {

            DBEngine.resultExpression(query, differenceSelectCallBack);
            changedRecords=differenceSelectCallBack.getChangedRecords();

        } catch (SQLException e) {
            e.printStackTrace();
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка чтения представления, содержащего измененные записи.", true);
            Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка чтения представления, содержащего измененные записи.\n"+e.getMessage()+"\n QUERY="+query, true);
        }
        return changedRecords;
    }

    public void detectAddedRecords(HashMap<String,FieldStateType> changedRecords, TTable table) throws SQLException {
        //String query=QueryRepository.getZMMAddedLines();
        String query=table.getAddedLinesQuery();
        try {
            AddedSelectCallBack addedSelectCallBack=new AddedSelectCallBack(changedRecords);
            DBEngine.resultExpression(query, addedSelectCallBack);
        } catch (SQLException e) {
            ErrorsClass.addedRecordsViewReadError(e,query);
        }
    }

    public LinkedList<String> detectDeletedRecords(HashMap<String,FieldStateType> changedRecords, TTable table) throws SQLException {

        String query=table.getDeletedRecordsQuery(getDiffValuesStr(changedRecords));

        LinkedList<String> deletedLines=null;
        try {
            DeletedSelectCallBack deletedSelectCallBack= new DeletedSelectCallBack();
            DBEngine.resultExpression(query, deletedSelectCallBack);
            deletedLines=deletedSelectCallBack.getDeletedRecords();
        } catch (SQLException e) {
            ErrorsClass.deletedRecordsViewReadError(e,query);
        }
        return deletedLines;
    }

    public boolean changeRecords(HashMap<String,FieldStateType> changedRecords, TTable table) {
        //Выбираем только те записи, которые новые или измененные в таблице импорта
        String query=table.getImportDifferenceRecordsQuery(getDiffValuesStr(changedRecords));

        try {
            DBEngine.resultExpression(query, new ImportRecords(changedRecords, table));
        } catch (SQLException e) {
            ErrorsClass.changeRecordsError(e,query);
        }
        return false;
    }

    public int delRecords(LinkedList<String> deletedRecords, TTable table) {
        int count=0;
        for(String idn:deletedRecords){
            String[] keys=table.getKeys(idn);
            if (table.deleteLine(keys)) count++;
        }
        MessagesClass.deletedRecordCountMessage(count);
        return count;
    }


    /**
     * Возвражает строку ключей, записей, попавших в выборку разницы таблицы импорта и действующей таблицы
     * @return
     */
    public String getDiffValuesStr(HashMap<String, FieldStateType> changedRecords) {
        StringBuilder line=new StringBuilder();
        for (Map.Entry<String, FieldStateType> entry : changedRecords.entrySet()) {
            if (line.length()>0) line.append(",");
            line.append("'").append(entry.getKey()).append("'");

        }
        return line.toString();
    }



    /** Класс, реализующий интерфейс ResultSetCallBackMethod, реализует механизм получения списка псевдонимов из
     * набора данных результата выполнения запроса.
     * Возвращает набор псевдонимов типа HashMap<String, FieldTypeDefines.FieldType>
      */
    private class AliasesLoader implements DBEngine.ResultSetCallBackMethod {
        private HashMap<String, FieldTypeDefines.FieldType> aliases;

        public HashMap<String, FieldTypeDefines.FieldType> getAliases() {
            return aliases;
        }

        public AliasesLoader() {
            this.aliases = new HashMap<>();
        }

        @Override
        public void call(ResultSet resultSet) throws SQLException {
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        String fieldalias=resultSet.getString("fieldalias");
                        FieldTypeDefines.FieldType fieldType=TableTools.detectFieldType(resultSet.getString("fieldtype"));
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


    class DifferenceSelectCallBack implements DBEngine.ResultSetCallBackMethod {
        private HashMap<String, FieldStateType> changedRecords;

        public DifferenceSelectCallBack() {
            this.changedRecords = new HashMap<>();
        }

        public HashMap<String, FieldStateType> getChangedRecords() {
            return changedRecords;
        }

        @Override
        public void call(ResultSet resultSet) throws SQLException {
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        changedRecords.put(resultSet.getString("idn"),FieldStateType.UPDATE);
                    }
                }
                catch (SQLException e){
                    e.printStackTrace();
                    Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Ошибка чтения поля IDN записи БД." , true);
                    Logger.putLineToLogs(new String[] {Logger.ERRORLOG}, "Ошибка чтения поля IDN записи БД \n"+e.getMessage(), true);
                    throw new SQLException(e);
                }
            }
            Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Определение измененных записей-OK \nНайдено:"+ changedRecords.size(), true);
        }
    }

    class AddedSelectCallBack implements DBEngine.ResultSetCallBackMethod {
        private HashMap<String, FieldStateType> changedRecords;

        public AddedSelectCallBack(HashMap<String, FieldStateType> changedRecords) {
            this.changedRecords = changedRecords;
        }

        public HashMap<String, FieldStateType> getChangedRecords() {
            return changedRecords;
        }

        @Override
        public void call(ResultSet resultSet) throws SQLException {
            int count=0;
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        changedRecords.put(resultSet.getString("idn"),FieldStateType.INSERT);
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
        private LinkedList<String> deletedRecords;

        public DeletedSelectCallBack() {
            this.deletedRecords=new LinkedList<>();
        }

        public LinkedList<String> getDeletedRecords() {
            return deletedRecords;
        }

        @Override
        public void call(ResultSet resultSet) throws SQLException {
            if ((resultSet!=null)){
                int count=0;
                try {
                    while (resultSet.next()) {
                        deletedRecords.add(resultSet.getString("idn"));
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

    private class ImportRecords implements DBEngine.ResultSetCallBackMethod {
        private HashMap<String, FieldStateType> changedRecords;
        ITableRouter tableRouter;

        public ImportRecords(HashMap<String, FieldStateType> changedRecords, ITableRouter tableRouter) {
            this.changedRecords = changedRecords;
            this.tableRouter = tableRouter;
        }

        @Override
        public void call(ResultSet resultSet) throws SQLException {
            String idn="";
            int countUpdate=0;
            int countAdd=0;
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        idn= resultSet.getString("idn");
                        FieldStateType fieldType=changedRecords.get(idn);
                        if (FieldStateType.INSERT==fieldType){
                            tableRouter.insertRecord(resultSet);
                            countAdd++;
                        }
                        else{
                            if (FieldStateType.UPDATE==fieldType){
                                tableRouter.updateRecord(resultSet);
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

    public enum FieldStateType{
        INSERT,UPDATE
    }

}
