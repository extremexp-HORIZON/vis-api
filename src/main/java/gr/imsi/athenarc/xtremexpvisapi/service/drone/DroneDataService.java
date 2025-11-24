package gr.imsi.athenarc.xtremexpvisapi.service.drone;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gr.imsi.athenarc.xtremexpvisapi.domain.drone.DroneData;
import gr.imsi.athenarc.xtremexpvisapi.repository.drone.DroneDataRepository;
import gr.imsi.athenarc.xtremexpvisapi.domain.drone.DroneTelemetryRequest;

@Service
public class DroneDataService {

    private static final Logger LOG = LoggerFactory.getLogger(DroneDataService.class);

    private final DroneDataRepository droneDataRepository;

    public DroneDataService(DroneDataRepository droneDataRepository) {
        this.droneDataRepository = droneDataRepository;
    }
    
    public List<DroneData> getDroneData(DroneTelemetryRequest request) {
        try {
            return droneDataRepository.findByRequest(request);
        } catch (SQLException e) {
            LOG.error("SQL Error getting drone data: {} - SQL State: {}", e.getMessage(), e.getSQLState(), e);
            throw new RuntimeException("Failed to get drone data", e);
        }
    }

    public int countDroneData(DroneTelemetryRequest request) {
        try {
            return droneDataRepository.countByRequest(request);
        } catch (SQLException e) {
            LOG.error("SQL Error counting drone data: {} - SQL State: {}", e.getMessage(), e.getSQLState(), e);
            throw new RuntimeException("Failed to count drone data", e);
        }
    }

    public List<String> getAllDroneIds() {
        try {
            return droneDataRepository.findAllDroneIds();
        } catch (SQLException e) {
            LOG.error("SQL Error getting all drone ids: {} - SQL State: {}", e.getMessage(), e.getSQLState(), e);
            throw new RuntimeException("Failed to get all drone ids", e);
        }
    }

    public Optional<DroneData> getLatestDroneData(String droneId) {
        try {
            return droneDataRepository.findLatestByDroneId(droneId);
        } catch (SQLException e) {
            LOG.error("SQL Error getting latest drone data: {} - SQL State: {}", e.getMessage(), e.getSQLState(), e);
            throw new RuntimeException("Failed to get latest drone data", e);
        }
    }
}
