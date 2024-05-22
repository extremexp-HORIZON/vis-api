package gr.imsi.athenarc.xtremexpvisapi.domain.ExplabilityProcedure;

    
    import java.util.List;

    public class Axis {
        private String axisName;       // Corresponds to `string axis_name = 1;`
        private List<String> axisValues; // Corresponds to `repeated string axis_values = 2;`
        private String axisType;       // Corresponds to `string axis_type = 3;`
    
        // Getters
        public String getAxisName() {
            return axisName;
        }
    
        public List<String> getAxisValues() {
            return axisValues;
        }
    
        public String getAxisType() {
            return axisType;
        }
    
        // Setters
        public void setAxisName(String axisName) {
            this.axisName = axisName;
        }
    
        public void setAxisValues(List<String> axisValues) {
            this.axisValues = axisValues;
        }
    
        public void setAxisType(String axisType) {
            this.axisType = axisType;
        }
    

    @Override
    public String toString() {
        return "Axis{" +
                "axisName='" + axisName + '\'' +
                ", axisValues=" + axisValues +
                ", axisType='" + axisType + '\'' +
                '}';
    }
}