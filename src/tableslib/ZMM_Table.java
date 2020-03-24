package tableslib;

import databaselib.QueryRepository;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ZMM_Table extends TTable {
    public ZMM_Table( String destinationTable) {
        super( destinationTable);
    }

    @Override
    public String getInsertQueryStr(StringBuilder fieldsStr, StringBuilder valuesStr) {
        return QueryRepository.getZMMInsertQuery().replace("@values@",valuesStr).replace("@fields@",fieldsStr);
    }

    public String getDeleteLineQuery(String[] keys){
        return QueryRepository.getZMMDeleteQuery().replace("@potrebnost_pen@",keys[0]).replace("@pozitsiya_potrebnosti_pen@",keys[1]);
    }

    @Override
    public String getAddedLinesQuery() {
        return QueryRepository.getZMMAddedLines();
    }

    @Override
    public String getDifferenceViewQuery() {
        return QueryRepository.getZMMDifferenceView();
    }

    @Override
    public String getDeletedRecordsQuery(String changedRecords) {
        String query=QueryRepository.getZMMDeletedLines();
        return query.replace("@dataset@",changedRecords);
    }

    @Override
    public String getImportDifferenceRecordsQuery(String range) {
        return QueryRepository.getZMMImportDifRecords().replace("@range@",range);
    }

    @Override
    protected String getUpdateQueryFromTable(ResultSet resultSet) throws SQLException {
        long potrebnost_pen=resultSet.getLong("potrebnost_pen");
        int pozitsiya_potrebnosti_pen=resultSet.getInt("pozitsiya_potrebnosti_pen");
        return QueryRepository.getZMMUpdateQuery().replace("@potrebnost_pen@",new Long(potrebnost_pen).toString()).replace("@pozitsiya_potrebnosti_pen@",new Integer(pozitsiya_potrebnosti_pen).toString());
    }

    @Override
    public String[] getKeys(String idn) {
        return idn.split("_");
    }
}
