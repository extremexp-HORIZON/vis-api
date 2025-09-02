package gr.imsi.athenarc.xtremexpvisapi.service;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.DataSource;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.SourceType;
import gr.imsi.athenarc.xtremexpvisapi.repository.DataSourceRepository;
import gr.imsi.athenarc.xtremexpvisapi.repository.ZoneRepository;
import gr.imsi.athenarc.xtremexpvisapi.service.dataService.v2.DataServiceV2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class DataSourceService {

    private final DataServiceV2 dataServiceV2;

    private final DataSourceRepository dataSourceRepository;

    private final ZoneRepository zoneRepository;

    @Autowired
    public DataSourceService(DataSourceRepository dataSourceRepository, ZoneRepository zoneRepository, DataServiceV2 dataServiceV2) {
        this.dataSourceRepository = dataSourceRepository;
        this.zoneRepository = zoneRepository;
        this.dataServiceV2 = dataServiceV2;
    }

    /**
     * Find a DataSource by fileName
     * 
     * @param fileName the fileName to search for
     * @return Optional containing the DataSource if found
     */
    public Optional<DataSource> findByFileName(String fileName) {
        return dataSourceRepository.findByFileName(fileName);
    }

    /**
     * Get all DataSources
     * 
     * @return List of all DataSources
     */
    public List<DataSource> findAll() {
        return dataSourceRepository.findAll();
    }

    /**
     * Delete a DataSource by fileName
     * 
     * @param fileName the fileName to delete
     * @return true if deletion was successful
     * @throws Exception 
     * @throws SQLException 
     */
    public boolean deleteByFileName(String fileName) throws SQLException, Exception {
        dataServiceV2.deleteFileMetadata(fileName);
        zoneRepository.deleteByFileName(fileName);
        return dataSourceRepository.deleteByFileName(fileName);
    }

    /**
     * Insert a new DataSource
     * 
     * @param dataSource the DataSource to insert
     * @return the inserted DataSource
     */
    public DataSource insert(DataSource dataSource) {
        return dataSourceRepository.insert(dataSource);
    }

    /**
     * Check if a DataSource exists
     * 
     * @param fileName the fileName to check
     * @return true if the DataSource exists
     */
    public boolean existsByFileName(String fileName) {
        return dataSourceRepository.existsByFileName(fileName);
    }

    /**
     * Create a new DataSource with default values
     * 
     * @param fileName   the fileName for the new DataSource
     * @param source     the source path
     * @param format     the file format
     * @param sourceType the source type
     * @return the created DataSource
     */
    public DataSource createDataSource(String fileName, String source, String format, SourceType sourceType) {
        DataSource dataSource = new DataSource();
        dataSource.setFileName(fileName);
        dataSource.setSource(source);
        dataSource.setFormat(format);
        dataSource.setSourceType(sourceType);

        return dataSourceRepository.insert(dataSource);
    }

    /**
     * Get the dataset file path for a DataSource
     * 
     * @param fileName the fileName
     * @return Path to the dataset file
     */
    public Path getDatasetFilePath(String fileName) {
        return dataSourceRepository.getDatasetFilePath(fileName);
    }

    /**
     * Get the metadata file path for a DataSource
     * 
     * @param fileName the fileName
     * @return Path to the metadata file
     */
    public Path getMetadataFilePath(String fileName) {
        return dataSourceRepository.getMetadataFilePath(fileName);
    }

    /**
     * Create a new DataSource from an uploaded file
     * 
     * @param dataSource the DataSource to create
     * @param file       the uploaded file
     * @return the created DataSource
     */
    public DataSource createDataSourceFromUpload(DataSource dataSource, MultipartFile file) {
        try {
            // First, create the DataSource structure
            DataSource createdDataSource = dataSourceRepository.insert(dataSource);

            // Get the target path where the file should be stored
            Path targetPath = dataSourceRepository.getDatasetFilePath(dataSource.getFileName());

            // Save the uploaded file to the target location
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            // Update the DataSource source to point to the actual file location
            createdDataSource.setSource(targetPath.toString());

            // Update the metadata file with the new source path
            dataSourceRepository.insert(createdDataSource);

            return createdDataSource;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create DataSource from upload: " + dataSource.getFileName(), e);
        }
    }

}