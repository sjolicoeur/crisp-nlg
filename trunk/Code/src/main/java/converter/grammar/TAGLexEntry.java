package crisp.converter.grammar;

import java.util.LinkedList;
import java.util.HashMap;

/**
 * This class represents a lexical entry for a tree adjoining grammar. 
 * It includes a reference to an (unlexicalized) TAGTree tree structure,
 * stores information about the word, pos and semantic content, initial probability
 * for this entry and the substitution and adjunction probabilities of this entry
 * in other entries.
 */
public class TAGLexEntry{

    private String treeRef; // reference to the tree for this entry
    private String id;	    // a unique id for this entry
    private String pos;     
    private String word;
    private String semContent;
    private LinkedList<String> semReqs;
    private LinkedList<String> pragConds;
    private LinkedList<String> pragEffects;
    
    /* Store the probabilities to substitute/adjoin the tree for this entry
       into the tree for a specific other entry at the node with a certain
       label. 
       These are HashMaps from ids for TAGLexEntries to a HashMap from node
       labels (here semantic roles) to the probabilities.*/
    private HashMap<String, HashMap<String, Float>> substProb;
    private HashMap<String, HashMap<String, Float>> adjProb;
    private HashMap<String, Float> noAdjProb; 
    
    private float initProb;
    
    // Store instantiations for leaf nodes (marked with 'lex')
    // in a mapping POS -> word
    private HashMap<String,String> lexValues; 

    /**
     * Create a new lexical entry for a tree adjoining grammar.
     */
    public TAGLexEntry(){
        semReqs = new LinkedList<String>();
        pragConds = new LinkedList<String>();
        pragEffects = new LinkedList<String>();
        lexValues = new HashMap<String,String>();
        substProb = new HashMap<String, HashMap<String, Float>>();
        adjProb = new HashMap<String, HashMap<String, Float>>();
        noAdjProb = new HashMap<String, Float>();
    }

    /************ Setter methods **********************/
    public void setID(String aID){ id = aID; }
    public void setTreeRef(String treeID){ treeRef = treeID; }
    public void setPOS(String aPos){ pos = aPos; }
    public void setWord(String aWord){ word = aWord; }
    public void setSemContent(String aSemContent) { semContent = aSemContent; }
    public void addSemReq(String semReq) { semReqs.add(semReq); }
    public void addPragCond(String pragCond) { pragConds.add(pragCond); }
    public void addPragEffect(String pragEffect) { pragEffects.add(pragEffect); }
    public void addLexValue(String pos, String word) { lexValues.put(pos,word); }

    public void setInitProb(float prob){
        initProb = prob;
    }
    
    public void addSubstProb(String tree, String lex, String targetNode, float prob){
        String key = tree+"-"+lex;
        if (substProb.containsKey(key)) 
            substProb.get(key).put(targetNode,new Float(prob));
        else {
            HashMap<String, Float> nodesToProbs = new HashMap<String, Float>();
            nodesToProbs.put(targetNode, new Float(prob));
            substProb.put(key,nodesToProbs);
        }
    }
    
    public void addAdjProb(String tree, String lex, String targetNode, float prob){
        String key = tree+"-"+lex;
        if (adjProb.containsKey(key)) 
            adjProb.get(key).put(targetNode,new Float(prob));
        else {
            HashMap<String, Float> nodesToProbs = new HashMap<String, Float>();
            nodesToProbs.put(targetNode, new Float(prob));
            adjProb.put(key,nodesToProbs);
        }
    }
    
    public void addNoAdjProb(String targetNode, float prob){
        noAdjProb.put(targetNode, new Float(prob));
    }
    
    /************ Getter methods *******************/
    public String getID(){ return id; }
    public String getTreeName(){ return normalizeTreename(treeRef+"-"+word);}
    public String getTreeRef(){ return treeRef; }
    public String getPOS(){ return pos; }
    public String getWord(){ return word; }
    public String getSemContent() {
        if (semContent==null)
            return null;
        
        if (semContent.equals(""))
            return null;
        
        return semContent;
    }
    public LinkedList<String> getSemReqs(){ return semReqs; }
    public LinkedList<String> getPragConds(){ return pragConds; }
    public LinkedList<String> getPragEffects(){ return pragEffects; }

    public HashMap<String,String> getAuxAnchors(){ return lexValues; };

    public float getInitProb() { return initProb; };
    public HashMap<String, HashMap<String, Float>> getSubstProbs(){
            return substProb;
    }
    public HashMap<String, HashMap<String, Float>> getAdjProbs(){
            return adjProb;
    }
    public HashMap<String, Float> getNoAdjProbs(){
            return noAdjProb;
    }
    
    /**
    * Translates XTAG style tree names into tree names that PDDL will accept.
    *
    * @param treename
    * @return
    */
    private String normalizeTreename(String treename) {
        return treename.replace("i.", "init_").replace("a.", "aux_");
    }
    
}
