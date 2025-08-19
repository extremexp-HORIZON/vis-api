package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.Range;

import gr.imsi.athenarc.xtremexpvisapi.service.shared.FloatRangeDeserializer;
import gr.imsi.athenarc.xtremexpvisapi.service.shared.FloatRangeSerializer;
import lombok.Data;

@Data
public class Rectangle {
    @JsonDeserialize(using = FloatRangeDeserializer.class)
    @JsonSerialize(using = FloatRangeSerializer.class)
    private Range<Float> lon;
    
    @JsonDeserialize(using = FloatRangeDeserializer.class)
    @JsonSerialize(using = FloatRangeSerializer.class)
    private Range<Float> lat;

    @JsonCreator
    public Rectangle(
            @JsonProperty("lon") Range<Float> lon,
            @JsonProperty("lat") Range<Float> lat) {
        this.lon = lon;
        this.lat = lat;
    }

    // Default constructor for Jackson
    public Rectangle() {
    }

    @JsonIgnore
    public boolean contains(float x, float y) {
        return lon.contains(x) && lat.contains(y);
    }

    @JsonIgnore
    public boolean contains(Point point) {
        return contains(point.getX(), point.getY());
    }

    @JsonIgnore
    public boolean intersects(Rectangle other) {
        return this.lon.isConnected(other.getLon()) && !this.lon.intersection(other.getLon()).isEmpty()
                && this.lat.isConnected(other.getLat()) && !this.lat.intersection(other.getLat()).isEmpty();
    }

    @JsonIgnore
    public boolean encloses(Rectangle other) {
        return this.lon.encloses(other.getLon()) && this.lat.encloses(other.getLat());
    }

    @JsonIgnore
    public double getCenterX() {
        return (lon.lowerEndpoint() + lon.upperEndpoint()) / 2d;
    }

    @JsonIgnore
    public double getCenterY() {
        return (lat.lowerEndpoint() + lat.upperEndpoint()) / 2d;
    }

    @JsonIgnore
    public float getXSize() {
        return lon.upperEndpoint() - lon.lowerEndpoint();
    }

    @JsonIgnore
    public float getYSize() {
        return lat.upperEndpoint() - lat.lowerEndpoint();
    }

    @JsonIgnore
    public List<Range<Float>> toList() {
        List<Range<Float>> list = new ArrayList<>(2);
        list.add(this.lon);
        list.add(this.lat);
        return list;
    }

    @JsonIgnore
    public double distanceFrom(Rectangle other) {
        double centerX = (lon.lowerEndpoint() + lon.upperEndpoint()) / 2d;
        double centerY = (lat.lowerEndpoint() + lat.upperEndpoint()) / 2d;
        double otherCenterX = (other.lon.lowerEndpoint() + other.lon.upperEndpoint()) / 2d;
        double otherCenterY = (other.lat.lowerEndpoint() + other.lat.upperEndpoint()) / 2d;
        return Math.hypot(Math.abs(centerX - otherCenterX), Math.abs(centerY - otherCenterY));
    }

    @JsonIgnore
    public double getSurfaceArea() {
        return getXSize() * getYSize();
    }
}
