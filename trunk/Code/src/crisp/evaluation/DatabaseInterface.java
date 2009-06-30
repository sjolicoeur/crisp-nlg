package crisp.evaluation;

import java.util.Set;
import de.saar.chorus.term.Term;

public abstract class DatabaseInterface {
        
    public abstract Set<Term> getSentenceSemantics(int sentenceID) throws Exception;
    
    public abstract String getRootIndex(int sentenceID) throws Exception;    
    
}
