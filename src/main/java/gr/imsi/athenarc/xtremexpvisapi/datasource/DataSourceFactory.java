package gr.imsi.athenarc.xtremexpvisapi.datasource;

public class DataSourceFactory {
    public static DataSource createDataSource(String type, String source) {
        switch (type.toLowerCase()) {
            case "csv":
                return new CsvDataSource(source);
            default:
                throw new IllegalArgumentException("Unknown data source type: " + type);
        }
    }
}