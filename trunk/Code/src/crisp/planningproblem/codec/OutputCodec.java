package crisp.planningproblem.codec;

import java.io.IOException;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;

abstract public class OutputCodec {
    abstract public void writeToDisk(Domain domain, Problem problem, String filenamePrefix, String problemname) throws IOException;

    public void writeToDisk(Domain domain, Problem problem, String filenamePrefix) throws IOException {
        writeToDisk(domain, problem, filenamePrefix, domain.getName());
    }
}
