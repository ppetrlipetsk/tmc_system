package loglib;

import java.util.Date;

public class MessagesClass {
    public static void addedFieldsMessage(int count) {
        Logger.putLineToLogs(new String[] {Logger.APPLOG}, "в т.ч. добавлено:"+count, true);
    }

    public static void noAddedFieldsMessage() {
        Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Добавленных записей не найдено.", true);
    }

    public static void deletedRecordsMessage(int count) {
        Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Удаленных записей:"+count, true);
    }

    public static void updateRecordsCountMessage(int count) {
        Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Обновлено записей:"+count, true);
    }

    public static void deletedRecordCountMessage(int count) {
        Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Удалено записей:"+count, true);
    }

    public static void importProcessMessageBegin() {
        Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Процесс импорта начат...", true);
    }
    public static void importProcessMessageEnd() {
        Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Процесс импорта закончен...", true);
    }

    public static void insertRecordsCountMessage(int count) {
        Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Добавлено записей:"+count, true);
    }

    public static void showAppParams() {
        System.out.println("\n\nПараметры программы:\n");
        System.out.println("applog- имя файла журнала приложения" +
                "(applog=имя файла)\n" +
                "errorlog- имя файла журнала ошибок приложения" +
                "(errorlog=имя файла)\n");
    }

    public static void putDateToLog() {
        String fdate=new Date().toString();
        Logger.putLineToLogs(new String[] {Logger.APPLOG}, "Начало работы программы импорта: "+fdate, true);
    }
}
