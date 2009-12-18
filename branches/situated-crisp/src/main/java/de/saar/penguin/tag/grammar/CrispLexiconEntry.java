package de.saar.penguin.tag.grammar;

import de.saar.chorus.term.Term;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A TAG lexicon entry that carries semantic content, semantic requirements and
 * other information required for (situated) Crisp
 * @author dbauer
 */
public class CrispLexiconEntry extends LexiconEntry {

    private List<Term> semanticRequirements;
    private List<Term> pragmaticPreconditions;
    private List<Term> pragmaticEffects;
    private List<Term> imperativeEffects;
    private Map<String, String> additionalParams;
    private Map<String, String> additionalVars;

    public CrispLexiconEntry(String word, String tree, Map<String, String> auxLexicalItems, List<Term> semantics) {
	super(word,tree,auxLexicalItems, semantics);
        semanticRequirements = new ArrayList<Term>();
        pragmaticPreconditions = new ArrayList<Term>();
        pragmaticEffects = new ArrayList<Term>();
        imperativeEffects = new ArrayList<Term>();
        additionalParams = new HashMap<String, String>();
        additionalVars = new HashMap<String, String>();
        
    }

    public void addSemanticRequirements(List<Term> requirements){
        semanticRequirements.addAll(requirements);
    }

    public List<Term> getSemanticRequirements(){
        return semanticRequirements;
    }

    public void addPragmaticPreconditions(List<Term> preconds){
        pragmaticPreconditions.addAll(preconds);
    }

    public List<Term> getPragmaticPreconditions(){
        return pragmaticPreconditions;
    }

    public void addPragmaticEffects(List<Term> effects){
        pragmaticEffects.addAll(effects);
    }

    public List<Term> getPragmaticEffects(){
        return pragmaticEffects;
    }

    void addAdditionalParams(Map<String, String> params) {
        additionalParams.putAll(params);
    }

    public Map<String,String> getAdditionalParams(){
        return additionalParams;
    }

    void addAdditionalVars(Map<String, String> params) {
        additionalVars.putAll(params);
    }

    public Map<String,String> getAdditionalVars(){
        return additionalVars;
    }


    void addImperativeEffects(List<Term> effects) {
        imperativeEffects.addAll(effects);
    }

    public List<Term> getImperativeEffects(){
        return imperativeEffects;
    }


}
