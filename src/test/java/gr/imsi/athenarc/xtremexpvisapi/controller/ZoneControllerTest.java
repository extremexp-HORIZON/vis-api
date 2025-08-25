package gr.imsi.athenarc.xtremexpvisapi.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params.Zone;
import gr.imsi.athenarc.xtremexpvisapi.service.ZoneService;

@ExtendWith(MockitoExtension.class)
class ZoneControllerTest {

    @Mock
    private ZoneService zoneService;

    private ZoneController zoneController;

    @BeforeEach
    void setUp() {
        zoneController = new ZoneController(zoneService);
    }

    @Test
    void testCreateZoneSuccessfully() {
        // Arrange
        Zone zone = new Zone();
        zone.setFileName("test.csv");
        zone.setName("Test Zone");
        zone.setType("test");
        zone.setDescription("Test description");
        zone.setStatus("active");
        
        Zone savedZone = new Zone();
        savedZone.setId("zone_a1b2c3d4e5f6");
        savedZone.setFileName("test.csv");
        savedZone.setName("Test Zone");
        savedZone.setType("test");
        savedZone.setDescription("Test description");
        savedZone.setStatus("active");
        savedZone.setCreatedAt(LocalDateTime.now().toString());
        
        when(zoneService.save(any(Zone.class))).thenReturn(savedZone);
        
        // Act
        ResponseEntity<Zone> response = zoneController.createZone(zone);
        
        // Assert
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("zone_a1b2c3d4e5f6", response.getBody().getId());
        assertEquals("test.csv", response.getBody().getFileName());
        assertEquals("Test Zone", response.getBody().getName());
        
        // Verify service was called
        verify(zoneService).save(zone);
    }

    @Test
    void testCreateZoneWithIdRejected() {
        // Arrange
        Zone zone = new Zone();
        zone.setId("existing-zone-123");
        zone.setFileName("test.csv");
        zone.setName("Test Zone");
        
        // Act
        ResponseEntity<Zone> response = zoneController.createZone(zone);
        
        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNull(response.getBody());
        
        // Verify service was NOT called
        verify(zoneService, never()).save(any());
    }

    @Test
    void testCreateZoneWithEmptyIdRejected() {
        // Arrange
        Zone zone = new Zone();
        zone.setId(""); // Empty ID
        zone.setFileName("test.csv");
        zone.setName("Test Zone");
        
        // Act
        ResponseEntity<Zone> response = zoneController.createZone(zone);
        
        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNull(response.getBody());
        
        // Verify service was NOT called
        verify(zoneService, never()).save(any());
    }

    @Test
    void testCreateZoneWithWhitespaceIdRejected() {
        // Arrange
        Zone zone = new Zone();
        zone.setId("   "); // Whitespace ID
        zone.setFileName("test.csv");
        zone.setName("Test Zone");
        
        // Act
        ResponseEntity<Zone> response = zoneController.createZone(zone);
        
        // Assert
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNull(response.getBody());
        
        // Verify service was NOT called
        verify(zoneService, never()).save(any());
    }

    @Test
    void testCreateZoneWithNullBody() {
        // Act
        ResponseEntity<Zone> response = zoneController.createZone(null);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).save(any());
    }

    @Test
    void testCreateZoneWithMissingRequiredFields() {
        // Arrange
        Zone zone = new Zone();
        zone.setFileName("test.csv");
        // Missing name field
        
        when(zoneService.save(any(Zone.class))).thenThrow(new IllegalArgumentException("Zone name is required"));
        
        // Act
        ResponseEntity<Zone> response = zoneController.createZone(zone);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was called but threw exception
        verify(zoneService).save(zone);
    }

    @Test
    void testGetAllZonesSuccessfully() {
        // Arrange
        Zone zone1 = new Zone();
        zone1.setId("zone_1");
        zone1.setFileName("test1.csv");
        zone1.setName("Zone 1");
        
        Zone zone2 = new Zone();
        zone2.setId("zone_2");
        zone2.setFileName("test2.csv");
        zone2.setName("Zone 2");
        
        List<Zone> zones = Arrays.asList(zone1, zone2);
        when(zoneService.findAll()).thenReturn(zones);
        
        // Act
        ResponseEntity<List<Zone>> response = zoneController.getAllZones();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        
        // Verify service was called
        verify(zoneService).findAll();
    }

    @Test
    void testGetZonesByFileNameSuccessfully() {
        // Arrange
        String fileName = "test.csv";
        Zone zone = new Zone();
        zone.setId("zone_1");
        zone.setFileName(fileName);
        zone.setName("Test Zone");
        
        List<Zone> zones = Arrays.asList(zone);
        when(zoneService.findByFileName(fileName)).thenReturn(zones);
        
        // Act
        ResponseEntity<List<Zone>> response = zoneController.getZonesByFileName(fileName);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(fileName, response.getBody().get(0).getFileName());
        
        // Verify service was called
        verify(zoneService).findByFileName(fileName);
    }

    @Test
    void testGetZonesByFileNameWithEmptyFileName() {
        // Arrange
        String fileName = "";
        
        // Act
        ResponseEntity<List<Zone>> response = zoneController.getZonesByFileName(fileName);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).findByFileName(any());
    }

    @Test
    void testUpdateZoneSuccessfully() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "zone_123";
        
        Zone updateData = new Zone();
        updateData.setName("Updated Zone Name");
        updateData.setType("updated");
        updateData.setStatus("inactive");
        updateData.setDescription("Updated description");
        
        Zone updatedZone = new Zone();
        updatedZone.setId(zoneId);
        updatedZone.setFileName(fileName);
        updatedZone.setName("Updated Zone Name");
        updatedZone.setType("updated");
        updatedZone.setStatus("inactive");
        updatedZone.setDescription("Updated description");
        updatedZone.setCreatedAt("2024-01-15T10:30:00");
        
        when(zoneService.save(any(Zone.class))).thenReturn(updatedZone);
        
        // Act
        ResponseEntity<Zone> response = zoneController.updateZone(fileName, zoneId, updateData);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(zoneId, response.getBody().getId());
        assertEquals(fileName, response.getBody().getFileName());
        assertEquals("Updated Zone Name", response.getBody().getName());
        assertEquals("updated", response.getBody().getType());
        assertEquals("inactive", response.getBody().getStatus());
        assertEquals("Updated description", response.getBody().getDescription());
        
        // Verify service was called with the correct data
        verify(zoneService).save(argThat(zone -> 
            zoneId.equals(zone.getId()) && 
            fileName.equals(zone.getFileName()) &&
            "Updated Zone Name".equals(zone.getName())
        ));
    }

    @Test
    void testUpdateZoneWithEmptyFileName() {
        // Arrange
        String fileName = "";
        String zoneId = "zone_123";
        Zone updateData = new Zone();
        updateData.setName("Updated Name");
        
        // Act
        ResponseEntity<Zone> response = zoneController.updateZone(fileName, zoneId, updateData);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).save(any());
    }

    @Test
    void testUpdateZoneWithEmptyId() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "";
        Zone updateData = new Zone();
        updateData.setName("Updated Name");
        
        // Act
        ResponseEntity<Zone> response = zoneController.updateZone(fileName, zoneId, updateData);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).save(any());
    }

    @Test
    void testUpdateZoneWithNullBody() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "zone_123";
        Zone updateData = null;
        
        // Act
        ResponseEntity<Zone> response = zoneController.updateZone(fileName, zoneId, updateData);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).save(any());
    }

    @Test
    void testUpdateZoneWithWhitespaceFileName() {
        // Arrange
        String fileName = "   ";
        String zoneId = "zone_123";
        Zone updateData = new Zone();
        updateData.setName("Updated Name");
        
        // Act
        ResponseEntity<Zone> response = zoneController.updateZone(fileName, zoneId, updateData);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).save(any());
    }

    @Test
    void testUpdateZoneWithWhitespaceId() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "   ";
        Zone updateData = new Zone();
        updateData.setName("Updated Name");
        
        // Act
        ResponseEntity<Zone> response = zoneController.updateZone(fileName, zoneId, updateData);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).save(any());
    }

    @Test
    void testUpdateZoneWithServiceException() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "zone_123";
        Zone updateData = new Zone();
        updateData.setName("Updated Name");
        
        when(zoneService.save(any(Zone.class))).thenThrow(new RuntimeException("Service error"));
        
        // Act
        ResponseEntity<Zone> response = zoneController.updateZone(fileName, zoneId, updateData);
        
        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        
        // Verify service was called
        verify(zoneService).save(any(Zone.class));
    }

    @Test
    void testUpdateZoneWithValidationException() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "zone_123";
        Zone updateData = new Zone();
        updateData.setName("Updated Name");
        
        when(zoneService.save(any(Zone.class))).thenThrow(new IllegalArgumentException("Validation error"));
        
        // Act
        ResponseEntity<Zone> response = zoneController.updateZone(fileName, zoneId, updateData);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was called
        verify(zoneService).save(any(Zone.class));
    }

    @Test
    void testUpdateZoneOverridesRequestBodyValues() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "zone_123";
        
        Zone updateData = new Zone();
        updateData.setId("different-id"); // This should be ignored
        updateData.setFileName("different-file.csv"); // This should be ignored
        updateData.setName("Updated Name");
        updateData.setType("updated");
        
        Zone updatedZone = new Zone();
        updatedZone.setId(zoneId);
        updatedZone.setFileName(fileName);
        updatedZone.setName("Updated Name");
        updatedZone.setType("updated");
        
        when(zoneService.save(any(Zone.class))).thenReturn(updatedZone);
        
        // Act
        ResponseEntity<Zone> response = zoneController.updateZone(fileName, zoneId, updateData);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // Verify that the path parameters override the request body values
        verify(zoneService).save(argThat(zone -> 
            zoneId.equals(zone.getId()) && 
            fileName.equals(zone.getFileName()) &&
            "Updated Name".equals(zone.getName()) &&
            "updated".equals(zone.getType())
        ));
    }

    @Test
    void testGetZonesByTypeSuccessfully() {
        // Arrange
        String type = "commercial";
        Zone zone1 = new Zone();
        zone1.setId("zone_1");
        zone1.setFileName("test1.csv");
        zone1.setName("Commercial Zone 1");
        zone1.setType(type);
        
        Zone zone2 = new Zone();
        zone2.setId("zone_2");
        zone2.setFileName("test2.csv");
        zone2.setName("Commercial Zone 2");
        zone2.setType(type);
        
        List<Zone> zones = Arrays.asList(zone1, zone2);
        when(zoneService.findByType(type)).thenReturn(zones);
        
        // Act
        ResponseEntity<List<Zone>> response = zoneController.getZonesByType(type);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(type, response.getBody().get(0).getType());
        assertEquals(type, response.getBody().get(1).getType());
        
        // Verify service was called
        verify(zoneService).findByType(type);
    }

    @Test
    void testGetZonesByTypeWithEmptyType() {
        // Arrange
        String type = "";
        
        // Act
        ResponseEntity<List<Zone>> response = zoneController.getZonesByType(type);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).findByType(any());
    }

    @Test
    void testGetZonesByStatusSuccessfully() {
        // Arrange
        String status = "active";
        Zone zone1 = new Zone();
        zone1.setId("zone_1");
        zone1.setFileName("test1.csv");
        zone1.setName("Active Zone 1");
        zone1.setStatus(status);
        
        Zone zone2 = new Zone();
        zone2.setId("zone_2");
        zone2.setFileName("test2.csv");
        zone2.setName("Active Zone 2");
        zone2.setStatus(status);
        
        List<Zone> zones = Arrays.asList(zone1, zone2);
        when(zoneService.findByStatus(status)).thenReturn(zones);
        
        // Act
        ResponseEntity<List<Zone>> response = zoneController.getZonesByStatus(status);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals(status, response.getBody().get(0).getStatus());
        assertEquals(status, response.getBody().get(1).getStatus());
        
        // Verify service was called
        verify(zoneService).findByStatus(status);
    }

    @Test
    void testGetZonesByStatusWithEmptyStatus() {
        // Arrange
        String status = "";
        
        // Act
        ResponseEntity<List<Zone>> response = zoneController.getZonesByStatus(status);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).findByStatus(any());
    }

    @Test
    void testGetZoneByFileNameAndIdSuccessfully() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "zone_123";
        
        Zone zone = new Zone();
        zone.setId(zoneId);
        zone.setFileName(fileName);
        zone.setName("Test Zone");
        
        when(zoneService.findByFileNameAndId(fileName, zoneId)).thenReturn(java.util.Optional.of(zone));
        
        // Act
        ResponseEntity<Zone> response = zoneController.getZoneByFileNameAndId(fileName, zoneId);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(zoneId, response.getBody().getId());
        assertEquals(fileName, response.getBody().getFileName());
        assertEquals("Test Zone", response.getBody().getName());
        
        // Verify service was called
        verify(zoneService).findByFileNameAndId(fileName, zoneId);
    }

    @Test
    void testGetZoneByFileNameAndIdNotFound() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "zone_123";
        
        when(zoneService.findByFileNameAndId(fileName, zoneId)).thenReturn(java.util.Optional.empty());
        
        // Act
        ResponseEntity<Zone> response = zoneController.getZoneByFileNameAndId(fileName, zoneId);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        // Verify service was called
        verify(zoneService).findByFileNameAndId(fileName, zoneId);
    }

    @Test
    void testGetZoneByFileNameAndIdWithEmptyFileName() {
        // Arrange
        String fileName = "";
        String zoneId = "zone_123";
        
        // Act
        ResponseEntity<Zone> response = zoneController.getZoneByFileNameAndId(fileName, zoneId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).findByFileNameAndId(any(), any());
    }

    @Test
    void testGetZoneByFileNameAndIdWithEmptyId() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "";
        
        // Act
        ResponseEntity<Zone> response = zoneController.getZoneByFileNameAndId(fileName, zoneId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).findByFileNameAndId(any(), any());
    }

    @Test
    void testDeleteZoneSuccessfully() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "zone_123";
        
        when(zoneService.deleteByFileNameAndId(fileName, zoneId)).thenReturn(true);
        
        // Act
        ResponseEntity<Void> response = zoneController.deleteZone(fileName, zoneId);
        
        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        
        // Verify service was called
        verify(zoneService).deleteByFileNameAndId(fileName, zoneId);
    }

    @Test
    void testDeleteZoneNotFound() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "zone_123";
        
        when(zoneService.deleteByFileNameAndId(fileName, zoneId)).thenReturn(false);
        
        // Act
        ResponseEntity<Void> response = zoneController.deleteZone(fileName, zoneId);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        // Verify service was called
        verify(zoneService).deleteByFileNameAndId(fileName, zoneId);
    }

    @Test
    void testDeleteZoneWithEmptyFileName() {
        // Arrange
        String fileName = "";
        String zoneId = "zone_123";
        
        // Act
        ResponseEntity<Void> response = zoneController.deleteZone(fileName, zoneId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).deleteByFileNameAndId(any(), any());
    }

    @Test
    void testDeleteZoneWithEmptyId() {
        // Arrange
        String fileName = "test.csv";
        String zoneId = "";
        
        // Act
        ResponseEntity<Void> response = zoneController.deleteZone(fileName, zoneId);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).deleteByFileNameAndId(any(), any());
    }

    @Test
    void testDeleteAllZonesByFileNameSuccessfully() {
        // Arrange
        String fileName = "test.csv";
        
        when(zoneService.deleteByFileName(fileName)).thenReturn(true);
        
        // Act
        ResponseEntity<Void> response = zoneController.deleteAllZonesByFileName(fileName);
        
        // Assert
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        
        // Verify service was called
        verify(zoneService).deleteByFileName(fileName);
    }

    @Test
    void testDeleteAllZonesByFileNameNotFound() {
        // Arrange
        String fileName = "test.csv";
        
        when(zoneService.deleteByFileName(fileName)).thenReturn(false);
        
        // Act
        ResponseEntity<Void> response = zoneController.deleteAllZonesByFileName(fileName);
        
        // Assert
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        
        // Verify service was called
        verify(zoneService).deleteByFileName(fileName);
    }

    @Test
    void testDeleteAllZonesByFileNameWithEmptyFileName() {
        // Arrange
        String fileName = "";
        
        // Act
        ResponseEntity<Void> response = zoneController.deleteAllZonesByFileName(fileName);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).deleteByFileName(any());
    }

    @Test
    void testCheckZonesExistSuccessfully() {
        // Arrange
        String fileName = "test.csv";
        
        when(zoneService.existsByFileName(fileName)).thenReturn(true);
        
        // Act
        ResponseEntity<Boolean> response = zoneController.checkZonesExist(fileName);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody());
        
        // Verify service was called
        verify(zoneService).existsByFileName(fileName);
    }

    @Test
    void testCheckZonesExistWithEmptyFileName() {
        // Arrange
        String fileName = "";
        
        // Act
        ResponseEntity<Boolean> response = zoneController.checkZonesExist(fileName);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).existsByFileName(any());
    }

    @Test
    void testGetAllFileNamesSuccessfully() {
        // Arrange
        List<String> fileNames = Arrays.asList("test1.csv", "test2.csv", "test3.csv");
        when(zoneService.getAllFileNames()).thenReturn(fileNames);
        
        // Act
        ResponseEntity<List<String>> response = zoneController.getAllFileNames();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().size());
        assertTrue(response.getBody().contains("test1.csv"));
        assertTrue(response.getBody().contains("test2.csv"));
        assertTrue(response.getBody().contains("test3.csv"));
        
        // Verify service was called
        verify(zoneService).getAllFileNames();
    }

    @Test
    void testGetZoneFilePathSuccessfully() {
        // Arrange
        String fileName = "test.csv";
        String filePath = "/path/to/zones/test.csv.json";
        
        when(zoneService.getZoneFilePath(fileName)).thenReturn(filePath);
        
        // Act
        ResponseEntity<String> response = zoneController.getZoneFilePath(fileName);
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(filePath, response.getBody());
        
        // Verify service was called
        verify(zoneService).getZoneFilePath(fileName);
    }

    @Test
    void testGetZoneFilePathWithEmptyFileName() {
        // Arrange
        String fileName = "";
        
        // Act
        ResponseEntity<String> response = zoneController.getZoneFilePath(fileName);
        
        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // Verify service was NOT called
        verify(zoneService, never()).getZoneFilePath(any());
    }

    @Test
    void testHealthCheck() {
        // Act
        ResponseEntity<String> response = zoneController.healthCheck();
        
        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Zone service is running", response.getBody());
        
        // No service calls for health check
        verifyNoInteractions(zoneService);
    }
}
