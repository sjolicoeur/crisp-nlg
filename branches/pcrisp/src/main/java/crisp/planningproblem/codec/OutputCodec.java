package crisp.planningproblem.codec;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;

abstract public class OutputCodec {
    public void writeToDisk(Domain domain, Problem problem, String filenamePrefix, String problemname) throws IOException {
        PrintWriter dw = new PrintWriter(new FileWriter(filenamePrefix + problemname + "-domain.lisp"));
        PrintWriter pw = new PrintWriter(new FileWriter(filenamePrefix + problemname + "-problem.lisp"));

        writeToDisk(domain, problem, dw, pw);

        dw.close();
        pw.close();
    }


    abstract public void writeToDisk(Domain domain, Problem problem, PrintWriter domainWriter, PrintWriter problemWriter) throws IOException;
    
    public void writeToDisk(Domain domain, Problem problem, Writer domainWriter, Writer problemWriter) throws IOException {
                writeToDisk(domain, problem, new PrintWriter(domainWriter), new PrintWriter(problemWriter));        
    }

    public void writeToDisk(Domain domain, Problem problem, String filenamePrefix) throws IOException {
        writeToDisk(domain, problem, filenamePrefix, domain.getName());
    }
}
