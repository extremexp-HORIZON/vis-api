package gr.imsi.athenarc.xtremexpvisapi.service.mlevaluation;

import gr.imsi.athenarc.xtremexpvisapi.datasource.CsvDataSource;
import gr.imsi.athenarc.xtremexpvisapi.datasource.DataSourceFactory;
import gr.imsi.athenarc.xtremexpvisapi.domain.queryv1.params.SourceType;
import java.nio.file.Paths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import tech.tablesaw.api.Table;

/**
 * Caches parsed evaluation Tables per CSV path. The cache must live on a separate bean (not in
 * {@link ModelEvaluationService}) because Spring's proxy-based @Cacheable only intercepts
 * cross-bean calls — self-invocation within the same class bypasses the cache.
 */
@Service
public class EvaluationTableLoader {

  private static final Logger LOG = LoggerFactory.getLogger(EvaluationTableLoader.class);

  private final DataSourceFactory dataSourceFactory;

  public EvaluationTableLoader(DataSourceFactory dataSourceFactory) {
    this.dataSourceFactory = dataSourceFactory;
  }

  @Cacheable(value = "mlEvalTables", key = "#path")
  public Table loadTable(String path) {
    CsvDataSource ds = (CsvDataSource) dataSourceFactory.createDataSource(SourceType.csv, path);
    if (ds == null) {
      throw new IllegalStateException("Failed to create data source for path: " + path);
    }
    LOG.info("Loading ML evaluation table from path: {}", path);
    return ds.readCsvFromFile(Paths.get(path));
  }
}
