package crisp.planner.external;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PddlOutputCodec;


import de.saar.chorus.term.Term; 

import java.util.List;

/**
 * Specify an interface for planners.
 */
public interface PlannerInterface {            
    
    public List<Term> runPlanner(Domain domain, Problem problem, PddlOutputCodec outputCodec) throws Exception;
    public List<Term> runPlanner(Domain domain, Problem problem, PddlOutputCodec outputCodec, long timeout) throws Exception;
        
    public long getPreprocessingTime();
    public long getSearchTime();
    public long getTotalTime();    
    
}
