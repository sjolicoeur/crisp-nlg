package crisp.converter.grammar;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collection;

public class TAGNode extends LinkedList<TAGNode> {
    
    private String cat;
    private String index;
    private String sem;
    private String constraint;

    private String type;

    private Set<String> roles;
    private Set<String> catNames;

    private List<TAGNode> substNodes;
    private List<TAGNode> nonSubstNodes;

    /**
     * Create a new empty Node in a TAG tree.
     */
    public TAGNode(){        
        roles = new HashSet<String>(); 
        catNames = new HashSet<String>();
        substNodes = new LinkedList<TAGNode>();
        nonSubstNodes = new LinkedList<TAGNode>();
    }

    /**** Setters and getters ****/

    public void setCat(String aCat){ cat = aCat; addCatName(cat); }
    public void setIndex(String aIndex){ index = aIndex; }
    public void setSem(String aSem){ sem = aSem; addRole(aSem);}
    public void setType(String aType) { type = aType; }
    public void setConstraint(String aConstraint) { constraint = aConstraint; }

    public void addRole(String role) { if (role!=null) 
                                           roles.add(role); }
    public void addRoles(Collection<String> someRoles) { roles.addAll(someRoles); }

    public void addCatName(String catName) { if (catName!=null && !("".equals(catName))) 
                                                  catNames.add(catName); }
    public void addCatNames(Collection<String> someCatNames) { catNames.addAll(someCatNames); }

    public void addSubstNodes(Collection<TAGNode> nodes) { substNodes.addAll(nodes); }
    public void addSubstNode(TAGNode node) { substNodes.add(node); }
    public void addNonSubstNodes(Collection<TAGNode> nodes) { nonSubstNodes.addAll(nodes); }
    public void addNonSubstNode(TAGNode node) { nonSubstNodes.add(node); }
    
    
    public String getCat(){ return cat; }
    public String getIndex(){ return index; }
    public String getSem(){ return sem; }
    public String getType() { return type; }
    public String getConstraint() { return constraint; }

    public Set<String> getRoleSet(){ return roles; }
    public Set<String> getCatNames(){ return catNames; }

    /**
     * Retrieve the list of semantic roles in the subtree rooted by this node.
     * @return the list of semantic roles.
     */
    public List<String> getRoleList(){
        LinkedList<String> roleList = new LinkedList<String>();
        // Make sure "self" is added to the list of roles exactly once
        roleList.add("self");
        roles.remove("self"); 
        roleList.addAll(roles);
        return roleList;
    }

    public List<TAGNode> getSubstNodes() { return substNodes; }
    public List<TAGNode> getNonSubstNodes() { return nonSubstNodes; }          
    
    public List<TAGNode> getAllNodes() {
        LinkedList<TAGNode> nodeList= new LinkedList<TAGNode>();
        nodeList.addAll(substNodes);
        nodeList.addAll(nonSubstNodes);
        return nodeList;
    }
    
    @Override
    public boolean add(TAGNode node) {
        if (super.add(node)) {
            addRoles(node.getRoleSet());
            addCatNames(node.getCatNames());
            addSubstNodes(node.getSubstNodes());
            addNonSubstNodes(node.getNonSubstNodes());
            if ("substitution".equals(node.getType())){
                substNodes.add(node);
            }
            else 
                if (!"".equals(node.getCat()))
                    nonSubstNodes.add(node);
            return true;
        } else 
            return false;
    }
    

}
