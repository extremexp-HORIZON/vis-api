package gr.imsi.athenarc.xtremexpvisapi.service.shared;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.collect.Range;

import java.io.IOException;

public class FloatRangeSerializer extends JsonSerializer<Range<Float>> {
    @Override
    public void serialize(Range<Float> range, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (range == null) {
            gen.writeNull();
            return;
        }
        
        // Convert Range to float array format [lower, upper]
        float lower = range.lowerEndpoint();
        float upper = range.upperEndpoint();
        
        gen.writeStartArray();
        gen.writeNumber(lower);
        gen.writeNumber(upper);
        gen.writeEndArray();
    }
}
