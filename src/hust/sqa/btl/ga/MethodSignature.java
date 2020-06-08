package hust.sqa.btl.ga;

import java.util.List;

public class MethodSignature {
  
    private String name;
    
    private List<String> parameters;

   
    public String getName() {
        return name;
    }

    public MethodSignature(String name, List<String> parameters) {
        super();
        this.name = name;
        this.parameters = parameters;
    }

    public List<String> getParameters() {
        return parameters;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }
}
