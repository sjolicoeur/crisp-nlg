package de.saar.penguin.tag.grammar;

import de.saar.chorus.term.Term;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * A TAG lexicon entry that carries semantic content, semantic requirements and
 * other information required for (situated) Crisp
 * @author dbauer
 */
public class CrispLexiconEntry extends LexiconEntry {

    private List<Term> semanticRequirements;
    private List<Term> pragmaticConditions;
    private List<Term> pragmaticEffects;

    public CrispLexiconEntry(String word, String tree, Map<String, String> auxLexicalItems, List<Term> semantics) {
	super(word,tree,auxLexicalItems, semantics);
        semanticRequirements = new ArrayList();
    }

    public void addSemanticRequirements(List<Term> requirements){
        semanticRequirements.addAll(requirements);
    }

    public List<Term> getSemanticRequirements(){
        return semanticRequirements;
    }

    public void addPragmaticPreconditions(List<Term> requirements){
        pragmaticConditions.addAll(requirements);
    }

    public List<Term> getPragmaticPreconditions(){
        return pragmaticConditions;
    }

    public void addPragmaticEffects(List<Term> requirements){
        pragmaticEffects.addAll(requirements);
    }

    public List<Term> getPragmaticEffects(){
        return pragmaticEffects;
    }

}
