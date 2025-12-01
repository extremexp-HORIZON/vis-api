package gr.imsi.athenarc.xtremexpvisapi.domain.drone;

import java.time.Instant;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DroneData {
    // --- Auto-generated ID ---
    private Long id;                   // Auto-generated unique identifier for each entry
    
    // --- Identifiers (Primary Keys for De-duplication in DuckDB) ---
    private String sessionId;         // Session identifier for grouping telemetry data
    private String droneId;           // Extracted by Telegraf from the MQTT Topic (e.g., "drone_001")
    private Instant timestamp;        // Maps to the 'timestamp' field from the drone payload

    // --- GPS Data (Flattened from 'gps' object) ---
    private Boolean gpsFix;           // Maps to gps.fix
    private Double lat;               // Maps to gps.lat
    private Double lon;               // Maps to gps.lon
    private Double alt;               // Maps to gps.alt
    private Double speedKmh;          // Maps to gps.speed_kmh
    private Integer satellites;       // Maps to gps.satellites

    // --- LTE Data (Flattened from 'rf.lte' object) ---
    private String ratLte;            // Maps to rf.lte.rat
    private String opModeLte;         // Maps to rf.lte.op_mode
    private String mccmncLte;         // Maps to rf.lte.mccmnc
    private String tacLte;            // Maps to rf.lte.tac
    private String scellIdLte;        // Maps to rf.lte.scell_id
    private String pcellIdLte;        // Maps to rf.lte.pcell_id
    private String bandLte;           // Maps to rf.lte.band
    private Integer earfcnLte;        // Maps to rf.lte.earfcn
    private Integer dlbwMhzLte;       // Maps to rf.lte.dlbw_mhz
    private Integer ulbwMhzLte;       // Maps to rf.lte.ulbw_mhz
    private Double rsrqDbLte;         // Maps to rf.lte.rsrq_db
    private Double rsrpDbmLte;        // Maps to rf.lte.rsrp_dbm
    private Double rssiDbmLte;        // Maps to rf.lte.rssi_dbm
    private Double rssnrDbLte;        // Maps to rf.lte.rssnr_db (can be null)

    // --- 5G Data (Flattened from 'rf.nr5g' object) ---
    private String rat5g;             // Maps to rf.nr5g.rat
    private String pcellId5g;         // Maps to rf.nr5g.pcell_id
    private String band5g;            // Maps to rf.nr5g.band
    private Integer earfcn5g;         // Maps to rf.nr5g.earfcn
    private Double rsrqDb5g;          // Maps to rf.nr5g.rsrq_db
    private Double rsrpDbm5g;         // Maps to rf.nr5g.rsrp_dbm
    private Double rssnrDb5g;         // Maps to rf.nr5g.rssnr_db
}
