package crisp.profile;

import crisp.converter.CrispConverter;
import crisp.converter.CurrentNextConverterWithCosts;
import crisp.converter.MscrispREConverter;
import crisp.planner.external.MetricFfPlannerInterface;
import crisp.planner.external.PlannerInterface;
import crisp.planningproblem.codec.FluentsPddlOutputCodec;
import crisp.planningproblem.codec.PddlOutputCodec;
import de.saar.penguin.tag.codec.InputCodec;
import de.saar.penguin.tag.grammar.SituatedCrispXmlInputCodec;

public class MscrispREProfile implements CrispProfile {

    public InputCodec getInputCodec() {
	return new SituatedCrispXmlInputCodec();
    }

    public CrispConverter getCrispConverter() {
	return new MscrispREConverter();
    }

    public PddlOutputCodec getPddlOutputCodec() {
	return new FluentsPddlOutputCodec();
    }

    public PlannerInterface getPlannerInterface() {
	//return new MetricFfPlannerInterface("-B -g 5 -h 1");
	return new MetricFfPlannerInterface("-B");
    }

}
