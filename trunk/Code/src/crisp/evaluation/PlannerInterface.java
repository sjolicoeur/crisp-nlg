package crisp.evaluation;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;

import java.io.IOException;

/**
 * Specify an interface for planners.
 */
public interface PlannerInterface {            
    
    public void runPlanner(Domain domain, Problem problem) throws Exception;
        
    public long getPreprocessingTime();
    public long getSearchTime();    
    
}
