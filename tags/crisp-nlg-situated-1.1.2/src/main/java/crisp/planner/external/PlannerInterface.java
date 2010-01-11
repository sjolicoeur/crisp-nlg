package crisp.planner.external;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;


import de.saar.chorus.term.Term; 

import java.util.List;

/**
 * Specify an interface for planners.
 */
public interface PlannerInterface {            
    
    public List<Term> runPlanner(Domain domain, Problem problem) throws Exception;
    public List<Term> runPlanner(Domain domain, Problem problem,long timeout) throws Exception;
        
    public long getPreprocessingTime();
    public long getSearchTime();
    public long getTotalTime();    
    
}
