package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Range;

import gr.imsi.athenarc.xtremexpvisapi.service.shared.FloatRangeDeserializer;
import lombok.Data;

@Data
public class Rectangle {
    @JsonDeserialize(using = FloatRangeDeserializer.class)
    private final Range<Float> xRange;
    @JsonDeserialize(using = FloatRangeDeserializer.class)
    private final Range<Float> yRange;

    @JsonCreator
    public Rectangle(
        @JsonProperty("xRange") @JsonDeserialize(using = FloatRangeDeserializer.class) Range<Float> xRange,
        @JsonProperty("yRange") @JsonDeserialize(using = FloatRangeDeserializer.class) Range<Float> yRange
    ) {
        this.xRange = xRange;
        this.yRange = yRange;
    }

    public boolean contains(float x, float y) {
        return xRange.contains(x) && yRange.contains(y);
    }

    public boolean contains(Point point) {
        return contains(point.getX(), point.getY());
    }

    public boolean intersects(Rectangle other) {
        return this.xRange.isConnected(other.getXRange()) && !this.xRange.intersection(other.getXRange()).isEmpty()
                && this.yRange.isConnected(other.getYRange()) && !this.yRange.intersection(other.getYRange()).isEmpty();
    }

    public boolean encloses(Rectangle other) {
        return this.xRange.encloses(other.getXRange()) && this.yRange.encloses(other.getYRange());
    }

    public double getCenterX() {
        return (xRange.lowerEndpoint() + xRange.upperEndpoint())/2d;
    }

    public double getCenterY() {
        return (yRange.lowerEndpoint() + yRange.upperEndpoint())/2d;
    }

    public float getXSize() {
        return xRange.upperEndpoint() - xRange.lowerEndpoint();
    }

    public float getYSize() {
        return yRange.upperEndpoint() - yRange.lowerEndpoint();
    }

    public List<Range<Float>> toList() {
        List<Range<Float>> list = new ArrayList<>(2);
        list.add(this.xRange);
        list.add(this.yRange);
        return list;
    }

    public double distanceFrom(Rectangle other){
        double centerX = (xRange.lowerEndpoint() + xRange.upperEndpoint()) / 2d;
        double centerY = (yRange.lowerEndpoint() + yRange.upperEndpoint()) / 2d;
        double otherCenterX = (other.xRange.lowerEndpoint() + other.xRange.upperEndpoint()) / 2d;
        double otherCenterY = (other.yRange.lowerEndpoint() + other.yRange.upperEndpoint()) / 2d;
        return Math.hypot(Math.abs(centerX - otherCenterX), Math.abs(centerY - otherCenterY));
    }

    public double getSurfaceArea(){
        return getXSize() * getYSize();
    }
}
