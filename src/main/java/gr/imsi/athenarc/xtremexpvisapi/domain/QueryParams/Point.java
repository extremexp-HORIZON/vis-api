package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams;

import java.io.Serializable;

import lombok.Data;

@Data
public class Point implements Serializable {
    private float x;
    private float y;
    private long fileOffset;
}
