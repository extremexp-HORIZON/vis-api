
package gr.imsi.athenarc.xtremexpvisapi.datasource;

public class DataSourceFactory {

    public DataSourceFactory() {}

    public DataSource createDataSource(String type, String source) {
        switch (type.toLowerCase()) {
            case "csv":
            case "json":
            case "zenoh":
                CsvDataSource csvDataSource = new CsvDataSource(source);
                return csvDataSource;
            default:
                throw new IllegalArgumentException("Unknown data source type: " + type);
        }
    }
}
