/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package de.saar.penguin.tag.grammar;

import de.saar.chorus.term.Term;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A TAG Grammar that contains @{CrispLexiconEntry}s.
 * @author dbauer
 */
public class CrispGrammar extends Grammar<Term>{

    @Override
    public void addLexiconEntry(String word, String elementaryTreeName, Map<String, String> auxLexicalItems, List<Term> semantics) {
        List<LexiconEntry> hereTrees = lexicon.get(word);

        if (hereTrees == null) {
            hereTrees = new ArrayList<LexiconEntry>();
            lexicon.put(word, hereTrees);
        }

        hereTrees.add(new CrispLexiconEntry(word, elementaryTreeName, auxLexicalItems, semantics));
    }

    public void addLexiconEntry(CrispLexiconEntry entry) {
        List<LexiconEntry> hereTrees = lexicon.get(entry.word);

        if (hereTrees == null) {
            hereTrees = new ArrayList<LexiconEntry>();
            lexicon.put(entry.word, hereTrees);
        }

        hereTrees.add(entry);
    }
    
    public List<CrispLexiconEntry> getCrispLexiconEntries(String word) {
        List<CrispLexiconEntry> ret = new ArrayList<CrispLexiconEntry>();
        for (LexiconEntry entry : lexicon.get(word)) {
            ret.add((CrispLexiconEntry) entry);
        }
        return ret;
    }

}
