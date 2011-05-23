/**
 * 
 */
package crisp.profile;

import java.util.Map;

import crisp.converter.CrispConverter;
import crisp.planner.external.PlannerInterface;
import crisp.planningproblem.codec.PddlOutputCodec;
import de.saar.penguin.tag.codec.InputCodec;

/**
 * Specifies a profile, i.e. a set of parameters, for CRISP.
 *
 */
public interface CrispProfile {
    
    public InputCodec getInputCodec();
    public CrispConverter getCrispConverter();
    public PddlOutputCodec getPddlOutputCodec();
    public PlannerInterface getPlannerInterface();
    
}
