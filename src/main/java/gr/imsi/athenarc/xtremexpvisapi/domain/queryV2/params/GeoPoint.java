package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params;

public record GeoPoint(double lat, double lon) {
    public double getLat() {
        return lat;
    }
    public double getLon() {
        return lon;
    }
}