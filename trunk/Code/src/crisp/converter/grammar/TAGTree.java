package crisp.converter.grammar;

import java.util.List;
import java.util.Set;

public class TAGTree {
    
    private TAGNode rootNode;
    private String id;

    private List<String> roles;
    private Set<String> catNames;
    private List<TAGNode> substNodes;
    private List<TAGNode> nonSubstNodes;

    
    public TAGTree(){
    }

    /**** Setters and getters ****/

    public void setRootNode(TAGNode aRootNode) { 
        rootNode = aRootNode; 
        // Check if the root node is a substitution or 
        // adjunction node. In this case add it to its 
        // own list of substitution or adjunction nodes.
        if ("substitution".equals(rootNode.getType())){
                rootNode.addSubstNode(rootNode);
            }
            else 
                if (!"".equals(rootNode.getCat()))
                    rootNode.addNonSubstNode(rootNode);

    }

    public void setID(String aId){ id = aId;} 

    public TAGNode getRootNode() { return rootNode; }
    public String getID(){ return id; }

    public List<String> getRoles(){                
        if (roles == null)
            roles = rootNode.getRoleList();        
        return roles;
    } 

    public Set<String> getCatNames(){ 
        if (catNames == null)
            catNames = rootNode.getCatNames(); 
        return catNames;
    }

    public List<TAGNode> getSubstNodes() {
        if (substNodes==null)
           substNodes = rootNode.getSubstNodes(); 
        return substNodes;
    }

    public List<TAGNode> getNonSubstNodes() {
        if (nonSubstNodes == null)
            nonSubstNodes = rootNode.getNonSubstNodes(); 
        return nonSubstNodes;
    }
     
}
