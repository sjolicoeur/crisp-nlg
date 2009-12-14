package crisp.converter.grammar;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Collection;

public class TAGrammar{

    /** 
     * Objects of this class represent a Tree Adjoining Grammar. 
     * A Tree Adjoining Grammar consists of a) a Map from indices to 
     * initial and auxiliary trees and b ) a lexicon. The lexicon is a Map
     * from Strings representing words and POS to TAGLexEntry objects.
     * The idea behind this is to store common types of trees (specific maybe
     * to certain subcategories of POS) only once and then associate the 
     * corresponding tree (with the word as anchor) when needed. 
     */

    private Map<String,TAGTree> trees = new HashMap<String,TAGTree>();
    private Map<String,TAGLexEntry> lexicon = new HashMap<String,TAGLexEntry>();

    /**
     * Create a new TAGrammar object.
     */
    public TAGrammar(){
    }

    /**
     * Add a new tree to the Grammar.
     * Overwrites the tree previously stored under the same index.     
     * @param tree The new tree object that should be added.
     */
    public void addTree(TAGTree tree){
        trees.put(tree.getID(),tree);
    }

    /**
     * Add a new lexical Entry to the Grammar.
     * Overwrites the tree previously stored under the same wordform and POS.
     * @param entry The new lexical entry that should be added to the grammar.     
     */
    public void addEntry(TAGLexEntry entry){
        String key = entry.getWord()+"#"+entry.getTreeRef();   
        lexicon.put(key,entry);
    }

    /**
     * @return A collection of (unlexicalized) initial and auxiliary trees in the grammar.
     */
    public Collection<TAGTree> getTrees(){
        return trees.values();
    }
    
    /**
     * @return A collection of all lexical entries in the grammar 
     */
    public Collection<TAGLexEntry> getLexicon(){
        return lexicon.values();
    }
    
    /**
     * Retrieve a specific lexical entry from the lexicon.
     * @param treeRef identefier for the tree of the entry to be retrieved.
     * @param treeWord identifier for the word for the entry to be retrieved.
     * @return The lexical entry.
     */
    public TAGLexEntry getEntry(String treeRef, String treeWord) {
        return lexicon.get(treeWord+"#"+treeRef);
    }
    
    /**
     * Retrieve an unlexicalized tree from the grammar.
     */
    public TAGTree getTree(String treeRef) {
        return trees.get(treeRef);
    }
    
}
