package gr.imsi.athenarc.xtremexpvisapi.domain.queryV2.params;

import java.io.Serializable;

import lombok.Data;

@Data
public class Point implements Serializable {
    private float x;
    private float y;
    private long fileOffset;
}
