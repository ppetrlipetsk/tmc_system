package defines;

import java.util.HashMap;

public class FieldsDefines {
    public static HashMap<String, FieldTypeDefines.FieldType> fields =new HashMap<>();
    static {
        fields.put("iuspt_table&id", FieldTypeDefines.FieldType.INTTYPE);
        fields.put("iuspt_table&potrebnost_pen", FieldTypeDefines.FieldType.STRINGTYPE);
        fields.put("ppz_table&zz_pos_num", FieldTypeDefines.FieldType.STRINGTYPE);
        fields.put("svodpen_table&naimenovanie_materiala__(polnoe)", FieldTypeDefines.FieldType.LONGSTRINGTYPE);
        fields.put("svodpen_table&N_zakupki_umts_i_k", FieldTypeDefines.FieldType.STRINGTYPE);
        fields.put("svodpen_table&kol_vo_zayavleno", FieldTypeDefines.FieldType.FLOATTYPE);


    }


}
