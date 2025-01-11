
package gr.imsi.athenarc.xtremexpvisapi.datasource;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.service.FileService;
import lombok.extern.java.Log;

@Component
@Log
public class DataSourceFactory {

    private final ApplicationContext applicationContext;
    private final FileService fileService;

    public DataSourceFactory(ApplicationContext applicationContext, FileService fileService) {
        this.applicationContext = applicationContext;
        this.fileService = fileService;
    }

    public DataSource createDataSource(SourceType type, String source) {
        CsvDataSource csvDataSource = applicationContext.getBean(CsvDataSource.class);

        String fileName = source.substring(source.lastIndexOf("/") + 1).trim();
        boolean isFileInCache = csvDataSource.getTableCache().containsKey(fileName);
        
        if (type == SourceType.zenoh && !isFileInCache) {
            try {
                fileService.downloadFileFromZenoh(source);
            } catch (Exception e) {
                e.printStackTrace();
            }
            csvDataSource.setSource(fileName);
        }else{
            csvDataSource.setSource(source);
        }
        return csvDataSource;
    }
}
