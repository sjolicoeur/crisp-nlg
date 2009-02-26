package crisp.converter.grammar;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Collection;

public class TAGrammar{

    /** Objects of this class represent a Tree Adjoining Grammar. 
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
     * @param index The index for the new tree. Conventionally use XTAG tree names.
     * @param tree The new tree object.
     */
    public void addTree(TAGTree tree){
        trees.put(tree.getID(),tree);
    }

    /**
     * Add a new lexical Entry to the Grammar.
     * Overwrites the tree previously stored under the same wordform and POS.
     * @param word Surface form for the word for which an entry is added.
     * @param pos POS for the word for which an entry is added.
     */
    public void addEntry(TAGLexEntry entry){
        String key = entry.getWord()+"#"+entry.getPOS();   
        lexicon.put(key,entry);
    }

    public Collection<TAGTree> getTrees(){
        return trees.values();
    }
    
    public Collection<TAGLexEntry> getLexicon(){
        return lexicon.values();
    }
    

}
