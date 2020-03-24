package tableslib;

import databaselib.QueryRepository;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PPS_Table extends TTable {

    public PPS_Table( String destinationTable) {
        super( destinationTable);
    }

    @Override
    public String getInsertQueryStr(StringBuilder fieldsStr, StringBuilder valuesStr) {
        return QueryRepository.getPPSInsertQuery().replace("@values@",valuesStr).replace("@fields@",fieldsStr);
    }

    public String getDeleteLineQuery(String[] keys){
        return QueryRepository.getPPSDeleteQuery().replace("@idn@",keys[0]);
    }

    @Override
    public String getAddedLinesQuery() {
        return QueryRepository.getPPSAddedLines();
    }

    @Override
    public String getDifferenceViewQuery() {
        return QueryRepository.getPPSDifferenceView();
    }

    @Override
    public String getDeletedRecordsQuery(String changedRecords) {
        String query=QueryRepository.getPPSDeletedLines();
        if (changedRecords.length()>0) query= query.replace("@dataset@"," idn not in (@dataset@) and");
        query=query.replace("@dataset@",changedRecords);
        return query;
    }

    @Override
    public String getImportDifferenceRecordsQuery(String range) {
        return QueryRepository.getPPSImportDifRecords().replace("@range@",range);
    }

    @Override
    protected String getUpdateQueryFromTable(ResultSet resultSet) throws SQLException {
        String idn=resultSet.getString("idn");
        return QueryRepository.getPPSUpdateQuery().replace("@idn@",idn);
    }

    @Override
    public String[] getKeys(String idn) {
        return new String[] {idn};
    }
}
