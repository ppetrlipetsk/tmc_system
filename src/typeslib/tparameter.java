package typeslib;

public class tparameter {
    private String value;
    private final boolean require;

    public String getValue() {
        return value;
    }

    public boolean isRequire() {
        return require;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public tparameter(String value, boolean require) {
        this.value = value;
        this.require = require;
    }
}
