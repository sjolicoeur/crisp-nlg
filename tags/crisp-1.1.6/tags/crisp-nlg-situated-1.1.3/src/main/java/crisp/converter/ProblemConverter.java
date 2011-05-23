package crisp.converter;


import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;

import de.saar.penguin.tag.grammar.Grammar;

import de.saar.chorus.term.Term;

import java.io.Reader;

public interface ProblemConverter{

    public void convert(Grammar<Term> grammar, Reader problemreader, Domain domain, Problem problem) throws Exception;
    
}
