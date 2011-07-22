package crisp.profile;

import crisp.converter.CrispConverter;
import crisp.converter.CurrentNextCrispConverter;
import crisp.planner.external.FfPlannerInterface;
import crisp.planner.external.PlannerInterface;
import crisp.planningproblem.codec.PddlOutputCodec;
import de.saar.penguin.tag.codec.InputCodec;
import de.saar.penguin.tag.grammar.SituatedCrispXmlInputCodec;

public class ScrispProfile implements CrispProfile {

    public InputCodec getInputCodec() {
	return new SituatedCrispXmlInputCodec();
    }

    public CrispConverter getCrispConverter() {
	return new CurrentNextCrispConverter();
    }

    public PddlOutputCodec getPddlOutputCodec() {
	return new PddlOutputCodec();
    }

    public PlannerInterface getPlannerInterface() {
	return new FfPlannerInterface("-B -T -H");
    }

}
