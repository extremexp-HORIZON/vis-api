package gr.imsi.athenarc.xtremexpvisapi.service.shared;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.collect.Range;

import java.io.IOException;

public class FloatRangeDeserializer extends JsonDeserializer<Range<Float>> {
    @Override
    public Range<Float> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        float[] arr = p.readValueAs(float[].class);
        if (arr.length != 2)
            throw new IOException("Range must have exactly 2 elements");
        return Range.closed(arr[0], arr[1]);
    }
}
