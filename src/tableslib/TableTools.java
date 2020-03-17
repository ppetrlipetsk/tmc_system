package tableslib;

import defines.FieldTypeDefines;

public class TableTools {
    public static FieldTypeDefines.FieldType detectFieldType(String fieldtype) {
        return FieldTypeDefines.FieldType.valueOf(fieldtype);
    }

    private String getFieldValueStr(FieldTypeDefines.FieldType fieldType,String valueStr ){
        String mask=FieldTypeDefines.getTypesFieldDBMask().get(fieldType);
        if (valueStr==null)
            return "null";
        else
            return mask.replace("@value@", valueStr);
    }


}
