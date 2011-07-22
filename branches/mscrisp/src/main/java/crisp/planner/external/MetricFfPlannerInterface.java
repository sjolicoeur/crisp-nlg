package crisp.planner.external;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import crisp.evaluation.ffplanparser.FfPlanParser;
import crisp.evaluation.ffplanparser.ParseException;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Problem;
import crisp.planningproblem.codec.PddlOutputCodec;
import de.saar.chorus.term.Term;

public class MetricFfPlannerInterface implements PlannerInterface {
    public static final String TEMPDOMAIN_FILE = "/tmp/tmpdomain.lisp";
    public static final String TEMPPROBLEM_FILE = "/tmp/tmpproblem.lisp";
    public static final String TEMPRESULT_FILE = "/tmp/tmpresult";

    private String metricFfFlags;

    private long preprocessingTime;
    private long searchTime;
    private String binaryLocation;

    public MetricFfPlannerInterface(String metricFfFlags) {
        this.metricFfFlags = metricFfFlags;
        preprocessingTime = 0;
        searchTime = 0;

        try {
            Properties crispProps = new Properties();
            crispProps.load(new FileReader(new File(crisp.main.GenerateSentence.PROPERTIES_FILE)));
            binaryLocation = crispProps.getProperty("MetricFfBinary");
        } catch (IOException ex) {
            throw new RuntimeException("An error occurred while trying to read the properties file: " + ex.getMessage());
        }

    }

    public MetricFfPlannerInterface() {
        this("");
    }

    public List<Term> runPlanner(Domain domain, Problem problem, PddlOutputCodec outputCodec) throws Exception {

        long start;
        long end;
        
        outputCodec.writeToDisk(domain, problem, new PrintWriter(new FileWriter(new File(TEMPDOMAIN_FILE))),
                new PrintWriter(new FileWriter(new File(TEMPPROBLEM_FILE))));


        // Run the planner
        Process metricFfplanner = Runtime.getRuntime().exec(binaryLocation + " " + metricFfFlags + " -o " + TEMPDOMAIN_FILE + " -f " + TEMPPROBLEM_FILE + " -O");
        metricFfplanner.waitFor();
        Reader resultReader = new BufferedReader(new InputStreamReader(metricFfplanner.getInputStream()));

        StringWriter str = new StringWriter();
        char[] buffer = new char[100];
        while (resultReader.read(buffer) != -1) {
            str.write(buffer);
        }
        resultReader.close();
        str.close();

        if (metricFfplanner.exitValue() != 0) {
            throw new RuntimeException("Metric FF in " + binaryLocation + " terminated inappropriately. Exit Value was " + metricFfplanner.exitValue() + ".\n" + str.toString());
        }

        List<Term> plan = parsePlan(str.toString());
        return plan;

    }


    private List<Term> parsePlan(String string) {
        Pattern p = Pattern.compile(".*found legal plan as follows(.*)time spent.*?(\\S+) seconds instantiating.*?(\\S+) seconds searching.*", Pattern.DOTALL);
        Matcher m = p.matcher(string);

        if (!m.matches()) {
            return null;
        } else {
            try {
                FfPlanParser parser = new FfPlanParser(new StringReader(m.group(1)));
                List<Term> ret = parser.plan();

                preprocessingTime = (long) (1000*Double.parseDouble(m.group(2)));
                searchTime = (long) (1000*Double.parseDouble(m.group(3)));


                return ret;
            } catch (ParseException ex) {
                ex.printStackTrace();
                return null;
            }
        }
    }

    public List<Term> runPlanner(Domain domain, Problem problem, PddlOutputCodec outputCodec, long timeout) throws Exception {
        //TODO: implement timeout for SGPlan
        return runPlanner(domain, problem, outputCodec);
    }

    public long getPreprocessingTime() {
        return preprocessingTime;
    }

    public long getSearchTime() {
        return searchTime;
    }

    public long getTotalTime() {
        return preprocessingTime + searchTime;
    }

}
