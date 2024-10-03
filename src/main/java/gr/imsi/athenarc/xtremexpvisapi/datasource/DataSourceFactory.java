
package gr.imsi.athenarc.xtremexpvisapi.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;


@Component
public class DataSourceFactory {

    private final ApplicationContext applicationContext;

    @Autowired
    public DataSourceFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public DataSource createDataSource(String type, String source) {
        switch (type.toLowerCase()) {
            case "csv":
            case "json":
            case "zenoh":
                CsvDataSource csvDataSource = applicationContext.getBean(CsvDataSource.class);
                return csvDataSource;
            default:
                throw new IllegalArgumentException("Unknown data source type: " + type);
        }
    }
}
