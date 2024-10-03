package gr.imsi.athenarc.xtremexpvisapi.domain.DataExplorationAdditionalParams;

import java.util.List;

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
    
   
    // Getters and Setters for GeographicalParams
    public String getLat() {
        return lat;
    }
    public void setLat(String lat) {
        this.lat = lat;
    }
    public String getLon() {
        return lon;
    }
    public void setLon(String lon) {
        this.lon = lon;
    }
    public List<String> getAttributes() {
        return attributes;
    }
    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }
    public BoundingBox getBbox() {
        return bbox;
    }
    public void setBbox(BoundingBox bbox) {
        this.bbox = bbox;
    }
    public int getZoomLevel() {
        return zoomLevel;
    }
    public void setZoomLevel(int zoomLevel) {
        this.zoomLevel = zoomLevel;
    }
   
}
