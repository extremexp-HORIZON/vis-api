
package gr.imsi.athenarc.xtremexpvisapi.datasource;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import gr.imsi.athenarc.xtremexpvisapi.domain.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.service.FileService;

@Component
public class DataSourceFactory {

    private final ApplicationContext applicationContext;
    private final FileService fileService;

    public DataSourceFactory(ApplicationContext applicationContext, FileService fileService) {
        this.applicationContext = applicationContext;
        this.fileService = fileService;
    }

    public DataSource createDataSource(SourceType type, String source) {
        CsvDataSource csvDataSource = applicationContext.getBean(CsvDataSource.class);
        csvDataSource.setSource(source);
        // Handle the case that the file does not exist in the cache / filesystem
        if (type == SourceType.ZENOH) {
            try {
                fileService.downloadFileFromZenoh(source);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return csvDataSource;
    }
}
