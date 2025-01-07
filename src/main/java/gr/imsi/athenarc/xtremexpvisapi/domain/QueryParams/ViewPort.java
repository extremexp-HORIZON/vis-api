package gr.imsi.athenarc.xtremexpvisapi.domain.QueryParams;

import lombok.Data;

@Data
public class ViewPort {
    
    private int width;
    private int height;
    public ViewPort() {}

    public ViewPort(int width, int height) {
        this.width = width;
        this.height = height;
    }
}
