
package gr.imsi.athenarc.xtremexpvisapi.datasource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import gr.imsi.athenarc.xtremexpvisapi.domain.SOURCE_TYPE;


@Component
public class DataSourceFactory {

    private final ApplicationContext applicationContext;

    @Autowired
    public DataSourceFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public DataSource createDataSource(SOURCE_TYPE type, String source) {
        switch (type) {
            case ZENOH:
            case CSV:
                CsvDataSource csvDataSource = applicationContext.getBean(CsvDataSource.class);
                csvDataSource.setSource(source);
                return csvDataSource;
            default:
                throw new IllegalArgumentException("Unknown data source type: " + type);
        }
    }
}
