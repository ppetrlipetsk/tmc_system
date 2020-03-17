import databaselib.DBSettings;
import defines.ApplicationConfig;
import loglib.Logger;
import loglib.MessagesClass;
import tableslib.TTable;
import typeslib.tparameter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class MainClass {
    public static final int LINESLIMIT = 2;
    private static final String ERRORLOGFILENAME = "errorlog.log";
    private static final String APPLOGFILENAME = "applog.log";
    private static final String ERRORLOG = "errorlog";
    private static final String APPLOG = "applog";

    private static Map<String, tparameter> parameters;
    static{
        parameters=new HashMap<>();
        parameters.put("sourcetable",new tparameter("",false));
        parameters.put("destinationtable",new tparameter("",false));
        parameters.put(APPLOG,new tparameter(APPLOGFILENAME,false));
        parameters.put(ERRORLOG,new tparameter(ERRORLOGFILENAME,false));
    }


    public static void main(String[] args) {
        if (args.length==0){
            MessagesClass.showAppParams();
        }
        else {
            if (!parseProgramParameters(args)) return;
            if (!initLogClass()) return;
            MessagesClass.putDateToLog();
            if (!ApplicationConfig.initApplicationValues()) return;

            if (dataBaseConnection()) {
                TTable sourceTable = new TTable(parameters.get("sourcetable").getValue(), parameters.get("sourcetable").getValue());
                try {
                    sourceTable.getAliases();

                    sourceTable.getExceptedRecords();
                    sourceTable.detectAddedRecords();
                    sourceTable.detectDeletedRecords();

                    MessagesClass.importProcessMessageBegin();
                    sourceTable.changeRecords();
                    sourceTable.delRecords();
                    MessagesClass.importProcessMessageEnd();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            }
            loglib.Logger.closeAll();
        }
    }

    private static boolean initLogClass() {
        new loglib.Logger(Logger.ERRORLOG,parameters.get(ERRORLOG).getValue(),LINESLIMIT);
        new loglib.Logger(Logger.APPLOG,parameters.get(APPLOG).getValue(), LINESLIMIT);
        try {
            loglib.Logger.getLogger(Logger.ERRORLOG).init();
            loglib.Logger.getLogger(Logger.APPLOG).init();
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
                //loglib.Logger.putLineToLogs(new String[] {"ErrorLog","AppLog"},"Ошибка параметра "+par,true);
                System.out.println("Ошибка параметра "+par);
                return false;
            }
            parameters.get(par[0]).setValue(par[1]);
        }

        Iterator<Map.Entry<String, tparameter>> entries = parameters.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<String, tparameter> entry = entries.next();
            String s=entry.getValue().getValue();
            if ((s.length()==0)&&(entry.getValue().isRequire())) {
                System.out.println("Ошибка параметра "+entry.getKey());
                return false;
            }
        }
        return true;
    }

    private static boolean dataBaseConnection() {
        try {
            databaselib.DBEngine.connectDB(DBSettings.connectionUrl,DBSettings.userName,DBSettings.pass,DBSettings.instanceName,DBSettings.databaseName);
            loglib.Logger.getLogger(Logger.APPLOG).put("Соединение с БД:"+DBSettings.instanceName+":SUCCESS\n",true);
            return true;
        }
        catch (Exception e){
            loglib.Logger.getLogger(Logger.APPLOG).putLine("Ошибка подключения к БД...",true);
            loglib.Logger.getLogger(Logger.ERRORLOG).putLine("Ошибка подключения к БД..."+DBSettings.instanceName);
        }
        return false;
    }


}
