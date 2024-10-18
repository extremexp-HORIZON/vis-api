package gr.imsi.athenarc.xtremexpvisapi.datasource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.AbstractFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.EqualsFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Filter.RangeFilter;
import gr.imsi.athenarc.xtremexpvisapi.domain.Query.VisualQuery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonQueryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(JsonQueryExecutor.class);

    // This method accepts the parsed JSON array and the VisualQuery
    public List<JsonNode> queryJson(List<JsonNode> jsonData, VisualQuery query) {
        // Apply filters to JSON data
        if (query.getFilters() != null) {
            for (AbstractFilter filter : query.getFilters()) {
                if (filter instanceof RangeFilter) {
                    // Handle Range filters (e.g., Integer, DateTime, Double)
                    jsonData = applyRangeFilter(jsonData, (RangeFilter<?>) filter);
                } else if (filter instanceof EqualsFilter) {
                    // Handle Equals filters (e.g., String, Integer, DateTime)
                    jsonData = applyEqualsFilter(jsonData, (EqualsFilter) filter);
                }
            }
        }

        // Apply projections (select specific columns)
        jsonData = applyProjections(jsonData, query.getColumns());

        // Apply normalization if needed
        applyNormalization(jsonData, query.getScaler(), query.getColumns());

        // Apply aggregation if needed
        if (query.getAggFunction() != null) {
            jsonData = applyAggregation(jsonData, query.getAggFunction());
        }


        // Apply offset and limit
        jsonData = applyLimitAndOffset(jsonData, query.getLimit(), query.getOffset());

        return jsonData;
    }

    private List<JsonNode> applyRangeFilter(List<JsonNode> jsonData, RangeFilter<?> filter) {
        // Example of handling range filters for numeric or date fields
        return jsonData.stream().filter(json -> {
            JsonNode value = json.get(filter.getColumn());
            if (filter instanceof RangeFilter.IntegerRangeFilter) {
                Integer min = (Integer) filter.getMinValue();
                Integer max = (Integer) filter.getMaxValue();
                return value != null && value.asInt() >= min && value.asInt() <= max;
            } else if (filter instanceof RangeFilter.DoubleRangeFilter) {
                Double min = (Double) filter.getMinValue();
                Double max = (Double) filter.getMaxValue();
                return value != null && value.asDouble() >= min && value.asDouble() <= max;
            } else if (filter instanceof RangeFilter.DateTimeRangeFilter) {
                LocalDateTime min = (LocalDateTime) filter.getMinValue();
                LocalDateTime max = (LocalDateTime) filter.getMaxValue();
                LocalDateTime dateTime = LocalDateTime.parse(value.asText());
                return dateTime != null && !dateTime.isBefore(min) && !dateTime.isAfter(max);
            }
            return false;
        }).collect(Collectors.toList());
    }

    private List<JsonNode> applyEqualsFilter(List<JsonNode> jsonData, EqualsFilter filter) {
        // Example of handling equals filters for string, integer, etc.
        return jsonData.stream().filter(json -> {
            JsonNode value = json.get(filter.getColumn());
            if (filter.getValue() instanceof Integer) {
                return value != null && value.asInt() == (Integer) filter.getValue();
            } else if (filter.getValue() instanceof String) {
                return value != null && value.asText().equals((String) filter.getValue());
            }
            return false;
        }).collect(Collectors.toList());
    }

    private List<JsonNode> applyProjections(List<JsonNode> jsonData, List<String> columns) {
        // Keep only the specified columns
        return jsonData.stream().map(json -> {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode projectedNode = mapper.createObjectNode();
            for (String column : columns) {
                ((ObjectNode) projectedNode).set(column, json.get(column));
            }
            return projectedNode;
        }).collect(Collectors.toList());
    }

    private void applyNormalization(List<JsonNode> jsonData, String scaler, List<String> columns) {
        // Apply normalization logic like min-max or Z-score
        if (scaler != null && columns != null) {
            // Implement normalization logic similar to CSV case
            // ...
        }
    }

    private List<JsonNode> applyAggregation(List<JsonNode> jsonData, String aggFunction) {
        // Apply aggregation logic if needed (e.g., sum, mean)
        // ...
        return jsonData;
    }

    private List<JsonNode> applyLimitAndOffset(List<JsonNode> jsonData, Integer limit, Integer offset) {
        // Apply offset and limit to the resulting data
        if (offset != null && offset > 0) {
            jsonData = jsonData.subList(offset, jsonData.size());
        }
        if (limit != null && limit > 0 && jsonData.size() > limit) {
            jsonData = jsonData.subList(0, limit);
        }
        return jsonData;
    }
}
