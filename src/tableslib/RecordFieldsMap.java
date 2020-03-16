package tableslib;

import java.util.LinkedHashMap;

public class RecordFieldsMap {

        LinkedHashMap<String, FieldRecord> fields;

    public RecordFieldsMap(LinkedHashMap<String, FieldRecord> fields) {
        this.fields = fields;
    }

    public RecordFieldsMap(int i, float v, boolean b) {
        fields=new LinkedHashMap<>(i,v,b);
    }

    public LinkedHashMap<String, FieldRecord> getFields() {
            return fields;
        }

        public void setFields(LinkedHashMap<String, FieldRecord> fields) {
            this.fields = fields;
        }
}
