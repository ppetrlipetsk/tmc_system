package databaselib;

import java.util.Map;

public class QueryRepository {
    public static String getZMMDifferenceView(){
        return "SELECT [idn]  FROM [dogc].[dbo].[zmm_difference_view]";
        //return "SELECT * FROM [dogc].[dbo].[tables]";
    }
    public static String getZMMAddedLines(){
        return "  SELECT [idn] FROM [dogc].[dbo].[zmm_difference_view] " +
                "    except " +
                " SELECT [idn] FROM [dogc].[dbo].[zmm_idn_view]";
    }

    public static String getZMMDeletedLines() {
        return "select idn, deleted from ( SELECT * FROM zmm_idn_view"+
                " EXCEPT"+
                " SELECT  * FROM zmm_import_idn_view ) i1 "+
                " where idn not in (@dataset@) and (deleted is null or deleted=0)";
    }

    public static String getAliasesQuery() {
        return "select * from aliases where table_id=(select id from tables where tablename='@tablename@')";
    }

    public static String getZMMImportDifRecords(){
        return "select * from [dogc].[dbo].zmm_import_idn_view where idn in (@range@)";
    }

    public static String getZMMDeleteQuery(){
        return "update [dogc].[dbo].zmm set deleted=1 where potrebnost_pen=@potrebnost_pen@ and pozitsiya_potrebnosti_pen=@pozitsiya_potrebnosti_pen@";
    }

    public static String getZMMUpdateQuery(){
        return "UPDATE [dbo].[ZMM]" +
                "   SET @values@" +
                " WHERE potrebnost_pen=@potrebnost_pen@ and pozitsiya_potrebnosti_pen=@pozitsiya_potrebnosti_pen@";
    }

    public static String getZMMInsertQuery() {
        return "INSERT INTO [dbo].[ZMM] (@fields@) values (@values@)";
    }

    public static String getZMMDeleteDiffQueryStr(){
        return "    SELECT  [idn]" +
                "    FROM [dogc].[dbo].[zmm_del_dif_view] where idn  in (@values@)";
    }
}
