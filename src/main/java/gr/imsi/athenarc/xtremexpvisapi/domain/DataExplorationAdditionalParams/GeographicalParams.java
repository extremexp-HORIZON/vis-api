package gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams;

import java.util.List;

import lombok.Data;

@Data
public class GeographicalParams {
    private String lat;
    private String lon;
    private List<String> attributes;
    private BoundingBox bbox;
    private int zoomLevel;

    public static class BoundingBox {
        private double north;
        private double south;
        private double east;
        private double west;
        // Getters and Setters
        public double getNorth() {
            return north;
        }
        public void setNorth(double north) {
            this.north = north;
        }
        public double getSouth() {
            return south;
        }
        public void setSouth(double south) {
            this.south = south;
        }
        public double getEast() {
            return east;
        }
        public void setEast(double east) {
            this.east = east;
        }
        public double getWest() {
            return west;
        }
        public void setWest(double west) {
            this.west = west;
        }
    }

}
