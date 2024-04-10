package gr.imsi.athenarc.xtremexpvisapi.domain;

public class ViewPort {
    
    private int width;
    private int height;
    public ViewPort() {}

    public ViewPort(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    @Override
    public String toString() {
        return "{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
