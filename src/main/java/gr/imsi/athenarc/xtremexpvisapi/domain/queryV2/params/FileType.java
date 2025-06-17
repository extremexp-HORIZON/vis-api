package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params;


public enum FileType {
    CSV(".csv"),
    PARQUET(".parquet"),
    JSON(".json");

    private String extension;

    FileType(String extension) {
        this.extension = extension;
    }

    public String getExtension() {
        return extension;
    }
}
