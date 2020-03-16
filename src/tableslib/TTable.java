package tableslib;

import databaselib.DBEngine;
import databaselib.QueryRepository;
import defines.FieldTypeDefines;
import typeslib.DetectTypeClass;

import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.*;

public class TTable {
    private final String sourceTable;
    private final String destinationTable;
    private HashMap<String,FieldStateType> difLines;
    private LinkedList<String> deletedLines;
    private int recordsCount;

    //private RecordFieldsMap aliases=new RecordFieldsMap(16, 0.75f,false);
    private HashMap<String, FieldTypeDefines.FieldType> aliases=new HashMap<>();


    public TTable(String sourceTable, String destinationTable) {
        this.sourceTable = sourceTable;
        this.destinationTable=destinationTable;
        difLines=new HashMap<>();
       // addedLines=new ArrayList<>();
        deletedLines=new LinkedList<>();
    }

    public String getSourceTable() {
        return sourceTable;
    }

//    public RecordFieldsMap getFields() {
//        return fields;
//    }

    private long getTableId(String tableName) throws SQLException {
       // long  tableId=tableslib.TableClass.getTableId(tableName);
        //return tableId;
        return 0;
    }

    private  void setCorrectionFieldType(LinkedHashMap<String,FieldRecord> fields, String fieldName, FieldTypeDefines.FieldType fieldType) {
        FieldRecord field=fields.get(fieldName);
        field.setFieldType(fieldType);
        fields.put(fieldName,field);
    }


    private FieldTypeDefines.FieldType getFieldTypeByStr(String s){
        return DetectTypeClass.getFieldType(s);
    }

    public int getRecordsCount() {
        return recordsCount;
    }

    public void getChangedRecords() {
    }

    public void getAliases() {
        String query=QueryRepository.getAliasesQuery().replace("@tablename@",this.destinationTable);

        try {
            DBEngine.resultExpression(query, new AliasesFill());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getExceptRecords() throws SQLException {
        String query=QueryRepository.getZMMDifferenceView();
        DBEngine.resultExpression(query, new DifferenceSelectCallBack());
    }

    public void getAddedRecords() {
        String query=QueryRepository.getZMMAddedLines();
        try {
            DBEngine.resultExpression(query, new AddedSelectCallBack());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void getDeletedRecords() {
        String query=QueryRepository.getZMMDeletedLines();
        query=query.replace("@dataset@",getDiffValuesStr());
        try {
            DBEngine.resultExpression(query, new DeletedSelectCallBack());
        } catch (SQLException e) {
            e.printStackTrace();
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

    public void delRecords() {
    }

    public void changeRecords() {
        //Выбираем только те записи, которые новые или измененные в таблице импорта
        String query=QueryRepository.getZMMImportDifRecords().replace("@range@",getDiffValuesStr());
        try {
            DBEngine.resultExpression(query, new fromImportToZMM());
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public void addRecords() {
        String query=QueryRepository.getZMMImportDifRecords().replace("@range@",getDiffValuesStr());
        try {
            DBEngine.resultExpression(query, new fromImportToZMM());
        } catch (SQLException e) {
            e.printStackTrace();
        }


    }

    public void AddRecords() {
    }

    class DifferenceSelectCallBack implements DBEngine.ResultSetCallBackMethod {
        @Override
        public void call(ResultSet resultSet) {
            //ArrayList<String> idn=new ArrayList<>();
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        difLines.put(resultSet.getString("idn"),FieldStateType.UPDATE);
                    }
                }
                catch (SQLException e){
                }
            }
        }
    }

    class AddedSelectCallBack implements DBEngine.ResultSetCallBackMethod {
        @Override
        public void call(ResultSet resultSet) {
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        difLines.put(resultSet.getString("idn"),FieldStateType.INSERT);
                    }
                }
                catch (SQLException e){
                }
            }
        }
    }

    class DeletedSelectCallBack implements DBEngine.ResultSetCallBackMethod {
        @Override
        public void call(ResultSet resultSet) {
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        deletedLines.add(resultSet.getString("idn"));
                    }
                }
                catch (SQLException e){
                }
            }
        }
    }

enum FieldStateType{
        INSERT,UPDATE, DELETE
}

    private class AliasesFill implements DBEngine.ResultSetCallBackMethod {

        @Override
        public void call(ResultSet resultSet) {
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        String fieldalias=resultSet.getString("fieldalias");
                        FieldTypeDefines.FieldType fieldType=detectFieldType(resultSet.getString("fieldtype"));
                        aliases.put(fieldalias, fieldType);
                    }
                }
                catch (SQLException e){
                }
            }
        }
    }

    private FieldTypeDefines.FieldType detectFieldType(String fieldtype) {
        return FieldTypeDefines.FieldType.valueOf(fieldtype);
    }

    private class fromImportToZMM implements DBEngine.ResultSetCallBackMethod {
        @Override
        public void call(ResultSet resultSet) {
            if ((resultSet!=null)){
                try {
                    while (resultSet.next()) {
                        String idn= resultSet.getString("idn");
                        FieldStateType fieldType=difLines.get(idn);
                        if (FieldStateType.INSERT==fieldType){
                            insertRecord(resultSet,idn);
                        }
                        else{
                            if (FieldStateType.UPDATE==fieldType){
                                updateRecord(resultSet,idn);
                            }

                        }
                    }
                }
                catch (SQLException e){
                }
            }
        }
    }

    private void insertRecord(ResultSet resultSet, String idn) {
        StringBuilder fieldsStr=new StringBuilder();
        StringBuilder valuesStr=new StringBuilder();

        for (Map.Entry<String,FieldTypeDefines.FieldType> alias: aliases.entrySet()){
            String fieldName=alias.getKey();
            FieldTypeDefines.FieldType fieldType=alias.getValue();
            if (fieldsStr.length()>0)fieldsStr.append(",");
            fieldsStr.append(fieldName);
            if (valuesStr.length()>0)valuesStr.append(",");
            try {
                valuesStr.append(getFieldValueStr(fieldName,fieldType,resultSet));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }



        System.out.println("fields="+fieldsStr);
        System.out.println("values="+valuesStr);
    }

    private void updateRecord(ResultSet resultSet, String idn) {
        //private HashMap<String, FieldTypeDefines.FieldType> aliases=new HashMap<>();
        //StringBuilder fieldsStr=new StringBuilder();
        StringBuilder valuesStr=new StringBuilder();
        try {
        for (Map.Entry<String,FieldTypeDefines.FieldType> alias: aliases.entrySet()){
            String fieldName=alias.getKey();
            FieldTypeDefines.FieldType fieldType=alias.getValue();
            if (valuesStr.length()>0)valuesStr.append(",");
            valuesStr.append(fieldName).append("=");
            if (fieldName.equals("summa_pozitsii_potrebnosti")){
                System.out.println("stop");
            }

                valuesStr.append(getFieldValueStr(fieldName,fieldType,resultSet));
        }
            long potrebnost_pen=resultSet.getLong("potrebnost_pen");
            int pozitsiya_potrebnosti_pen=resultSet.getInt("pozitsiya_potrebnosti_pen");


            String query=QueryRepository.getZMMUpdateQuery().replace("@values@",valuesStr).replace("@potrebnost_pen@",new Long(potrebnost_pen).toString()).replace("@pozitsiya_potrebnosti_pen@",new Integer(pozitsiya_potrebnosti_pen).toString());
            DBEngine.execute(query);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("values="+valuesStr);
    }

    private String getFieldValueStr(String fieldName, FieldTypeDefines.FieldType fieldType, ResultSet resultSet) throws SQLException {
        String mask=FieldTypeDefines.getTypesFieldDBMask().get(fieldType);
        String valueStr="";
        //if ((fieldType==FieldTypeDefines.FieldType.STRINGTYPE)||(fieldType==FieldTypeDefines.FieldType.LONGSTRINGTYPE)) {
            valueStr=resultSet.getString(fieldName);
        /*}
        else
        if ((fieldType==FieldTypeDefines.FieldType.FLOATTYPE)||(fieldType==FieldTypeDefines.FieldType.DECIMALTYPE) ){
            valueStr=new Float(resultSet.getFloat(fieldName)).toString();
        }
        else
        if ((fieldType==FieldTypeDefines.FieldType.INTTYPE)){
            valueStr=new Integer(resultSet.getInt(fieldName)).toString();
        }
        else
        if ((fieldType==FieldTypeDefines.FieldType.BIGINTTYPE)){
            valueStr=new Long(resultSet.getLong(fieldName)).toString();
        }
        else
        if ((fieldType==FieldTypeDefines.FieldType.DATETYPE)){
            Date dat=resultSet.getDate(fieldName);
            valueStr=dat.toString();
        }
        */
        return mask.replace("@value@", valueStr);
    }
}
