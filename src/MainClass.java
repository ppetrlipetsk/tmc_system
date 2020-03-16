import databaselib.DBSettings;
import defines.ApplicationConfig;
import tableslib.TTable;
import typeslib.tparameter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainClass {
    private static Map<String, tparameter> parameters;
    static{
        parameters=new HashMap<>();
        parameters.put("sourcetable",new tparameter("",true));
        parameters.put("destinationtable",new tparameter("",true));
    }


    public static void main(String[] args) {
        if (!initLogClass()) return;
        doIt(args);
        loglib.Logger.closeAll();
    }

    private static boolean initLogClass() {
        new loglib.Logger("ErrorLog","errorlog.log",2);
        new loglib.Logger("AppLog","applog.log",2);
        try {
            loglib.Logger.getLogger("ErrorLog").init();
            loglib.Logger.getLogger("AppLog").init();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Ошибка инициализации системы логгирования. Аварийное завершение работы.");
            return false;
        }
        return true;
    }

    private static boolean parseProgramParameters(String[] args) {

        for( String arg:args){
            String[] par=arg.split("=");
            if ((par==null)||(par.length!=2)||!parameters.containsKey(par[0])) {
                loglib.Logger.putLineToLogs(new String[] {"ErrorLog","AppLog"},"\"Ошибка параметра \"+par",true);
                return false;
            }
            parameters.get(par[0]).setValue(par[1]);
        }

        Iterator<Map.Entry<String, tparameter>> entries = parameters.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, tparameter> entry = entries.next();
            String s=entry.getValue().getValue();
            if ((s.length()==0)&&(entry.getValue().isRequire())) {
                loglib.Logger.putLineToLogs(new String[] {"ErrorLog","AppLog"},"Ошибка параметра!",true);
                return false;
            }
        }
        return true;
    }

    private static boolean doIt(String[] args) {
        if (!ApplicationConfig.initApplicationValues()) return false;
        if (!parseProgramParameters(args)) return false;

        boolean result=true;
        if (!dataBaseConnection()) result=false;
        else {
            //TODO
            TTable sourceTable=new TTable(parameters.get("sourcetable").getValue(),parameters.get("sourcetable").getValue());
            sourceTable.getAliases();
            try {
                sourceTable.getExceptRecords();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            sourceTable.getChangedRecords();
            sourceTable.getAddedRecords();
            sourceTable.changeRecords();
            sourceTable.getDeletedRecords();
          //  sourceTable.delRecords();
            //sourceTable.getAliases();

            sourceTable.AddRecords();
        }
        return result;
    }

    private static boolean dataBaseConnection() {
        try {
            databaselib.DBEngine.connectDB(DBSettings.connectionUrl,DBSettings.userName,DBSettings.pass,DBSettings.instanceName,DBSettings.databaseName);
            loglib.Logger.getLogger("AppLog").put("Соединение с БД:"+DBSettings.instanceName+":SUCCESS\n",true);
            return true;
        }
        catch (Exception e){
            loglib.Logger.getLogger("AppLog").putLine("Ошибка подключения к БД...",true);
            loglib.Logger.getLogger("ErrorLog").putLine("Ошибка подключения к БД..."+DBSettings.instanceName);
        }
        return false;
    }


}
