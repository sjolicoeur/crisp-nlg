package crisp.evaluation;

import java.util.Set;
import de.saar.chorus.term.Term;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;


public abstract class DatabaseInterface {
        
    public abstract Set<Term> getSentenceSemantics(String semanticsTableName, int sentenceID) throws Exception;
    
    public abstract String getRootIndex(String semanticsTableName, int sentenceID) throws Exception;    

    public abstract void setResultDerivation(String resultTableName, int sentenceID, DerivationTree derivation) throws Exception;
    public abstract void setResultDerivedTree(String resultTableName, int sentenceID, DerivedTree derivedTree) throws Exception;
    public abstract void setResultSurface(String resultTableName, int sentenceID, String surface) throws Exception;
    public abstract void setResultCreationTime(String resultTableName, int sentenceID, long time) throws Exception;
    public abstract void setResultPreprocessingTime(String resultTableName, int sentenceID, long time) throws Exception;
    public abstract void setResultSearchTime(String resultTableName, int sentenceID, long time) throws Exception;

}
