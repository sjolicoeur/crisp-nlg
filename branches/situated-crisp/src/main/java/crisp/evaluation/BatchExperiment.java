package crisp.evaluation;

import crisp.converter.BackoffModelProbCRISPConverter;
import crisp.converter.FancyBackoffProbCRISPConverter;
import crisp.converter.FastCRISPConverter;
import crisp.planningproblem.Problem;
import crisp.planningproblem.Domain;
import crisp.result.DerivationTreeBuilder;
import crisp.result.CrispDerivationTreeBuilder;
import crisp.result.PCrispDerivationTreeBuilder;

import de.saar.penguin.tag.grammar.Grammar;
import de.saar.penguin.tag.grammar.ProbabilisticGrammar;
import de.saar.penguin.tag.codec.PCrispXmlInputCodec;
import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;

import de.saar.chorus.term.Term;

import crisp.converter.ProbCRISPConverter;

import crisp.converter.TreeModelProbCRISPConverter;
import de.saar.penguin.tag.grammar.LinearInterpolationProbabilisticGrammar;
import de.saar.penguin.tag.grammar.TreeModelProbabilisticGrammar;
import java.util.Set;
import java.util.Queue;
import java.util.LinkedList;
import java.util.List;

import java.io.StringReader;
import java.io.StringWriter;    
import java.io.File;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;

import java.util.Properties;

/**
 * Provides basic functionality for large scale experiments with the generation system. 
 */
public class BatchExperiment {         

    private static final String GRAMMAR_NAME = "dummy";
    private static final String DATABASE_PROPERTIES_FILE = "database.properties";
    private static final long TIMEOUT = 300000;
    
    Grammar grammar; 
    DatabaseInterface database;
    String resultTable;
    Queue<Integer> batch;        
    
    Properties properties;    

    public BatchExperiment(Grammar grammar, DatabaseInterface database, String resultTable) {


        this.grammar = grammar;
        this.database = database;
        this.resultTable = resultTable;
        this.batch = new LinkedList<Integer>();       
    }
    
    public void addToBatch(int sentenceID){
        batch.offer(sentenceID);
    }
    
    public void runExperiment(){
        while (! batch.isEmpty()) {                        
                processNextSentence();            
        }
    }
    
    public void processNextSentence(){
        processSentence(batch.poll());
    }
    
    
    protected void processSentence(int sentenceID) {
        System.out.println("Processing sentence #"+sentenceID);
        
        DerivationTree derivTree = null;
        DerivedTree derivedTree = null;
        String yield = null;
        long preprocessingTime = 0;
        long searchTime = 0;
        long creationTime = 0;
        int domainSize = 0;
        
        //TreeModelProbCRISPConverter converter = new TreeModelProbCRISPConverter();
        //FancyBackoffProbCRISPConverter converter = new FancyBackoffProbCRISPConverter();
        //ProbCRISPConverter converter = new ProbCRISPConverter();
        FastCRISPConverter converter = new FastCRISPConverter();
        //PlannerInterface planner = new LamaPlannerInterface();
        LazyFfInterface planner = new LazyFfInterface();
        List<Term> plan = null;

        Domain domain = new Domain();
        Problem problem = new Problem();

        try{
            Set<Term> semantics = database.getSentenceSemantics("dbauer_PTB_semantics",sentenceID);
            String rootIndex = database.getRootIndex("dbauer_PTB_semantics",sentenceID);
        
            String xmlProblem = createXMLProblem(semantics, rootIndex, "problem-"+sentenceID, GRAMMAR_NAME);
            
            System.out.print("  converting...");
                            
            long start = System.currentTimeMillis();
            converter.convert(grammar, new StringReader(xmlProblem), domain, problem);
            //converter.convert_backoff((LinearInterpolationProbabilisticGrammar) grammar, new StringReader(xmlProblem), domain, problem);
            domainSize = domain.getActions().size();



            creationTime = System.currentTimeMillis()-start;
            
            System.out.println("done in "+creationTime+"ms.");
            System.out.println("  Starting planner... ");
            plan = planner.runPlanner(domain, problem, TIMEOUT);

            long time = planner.getSearchTime();
            if (plan == null || plan.isEmpty()){
                if (planner.isTimedOut() && time > 5000) {
                    database.writeResults(resultTable, sentenceID, domainSize, null, null, creationTime, planner.getPreprocessingTime(), planner.getSearchTime(),"Timed out after "+TIMEOUT);
                    return;
                } else {
                System.err.println("Couldn't find plan!");
                database.writeResults(resultTable, sentenceID, domainSize, null, null, creationTime, planner.getPreprocessingTime(), planner.getSearchTime(),"No solution.");
                return;
                }
            }

            System.out.println(plan);
            // Build derivation and derived tree
            //DerivationTreeBuilder derivationTreeBuilder = new PCrispDerivationTreeBuilder(grammar);
            DerivationTreeBuilder derivationTreeBuilder = new CrispDerivationTreeBuilder(grammar);
            derivTree = derivationTreeBuilder.buildDerivationTreeFromPlan(plan, domain);            
            System.out.println(derivTree);
            derivedTree = derivTree.computeDerivedTree(grammar);
            yield = derivedTree.yield();
            preprocessingTime = planner.getPreprocessingTime();
            searchTime = planner.getSearchTime();
            System.out.println("   Result is: "+yield);
            database.writeResults(resultTable, sentenceID, domainSize, derivTree, derivedTree, creationTime, preprocessingTime, searchTime, null);
        } catch (SQLException e) {            
            System.err.println("Couldn't process sentence #"+sentenceID);
            System.err.println("Error in SQL connection: "+e);
            return;
        } catch (Exception e) {         
            System.err.println("Couldn't process sentence #"+sentenceID);
            e.printStackTrace();
            // Write error tag to database
            try {
                database.writeResults(resultTable, sentenceID, domainSize, null, null, creationTime, 0, 0 , e.toString());
            }    catch (SQLException f) {            
                System.err.println("Couldn't write error message to database.");
                System.err.println("Error in SQL connection: "+f);                
            }         
        } finally {
           //converter = null;
           planner = null;
           plan = null;
           problem = null;
           domain = null;
         //  System.gc();
        }
    }
          
    
    private String createXMLProblem(Set<Term> semantics, String rootIndex, String name, String grammar) {
        StringWriter writer = new StringWriter();
        
        writer.write("<crispproblem name=\"");
        writer.write(name);
        writer.write("\" grammar=\"");
        writer.write(grammar);
        writer.write("\" cat=\"S\" index=\"");
        writer.write(rootIndex);
        writer.write("\" plansize=\"");
        writer.write(new Integer(semantics.size()).toString());
        writer.write("\">\n");

        // Everything in the KB becomes communicative goal
        for (Term t : semantics) {
            writer.write("<world>");
            writer.write(t.toString());
            writer.write("</world>\n");
            writer.write("<commgoal>");
            writer.write(t.toString());
            writer.write("</commgoal>\n");                        
        }
        
        writer.write("</crispproblem>\n");
        return writer.toString();        
    }
    
    
    public static void main(String[] args) throws Exception{
        
        System.out.print("Parsing grammar...");
        PCrispXmlInputCodec codec = new PCrispXmlInputCodec();
        //TreeModelProbabilisticGrammar<Term> grammar = new TreeModelProbabilisticGrammar<Term>();
        ProbabilisticGrammar  grammar = new ProbabilisticGrammar<Term>();
        //LinearInterpolationProbabilisticGrammar<Term> grammar = new LinearInterpolationProbabilisticGrammar<Term>(new Double(args[1]), new Double(args[2]) ,1000);


        codec.parse(new File(args[0]), grammar);
        //grammar.initBackoff();


        System.out.println("done");

        // Read properties file.
        Properties props = new Properties();

        try {
            props.load(new FileInputStream(DATABASE_PROPERTIES_FILE));
        } catch (IOException e) {
            System.err.print("Couldn't read properties file "+DATABASE_PROPERTIES_FILE);
        }

        String database = props.getProperty("databaseName");
        String username = props.getProperty("userName");
        String password = props.getProperty("password");
        String resulttable = props.getProperty("resultTable");

        System.out.println("Will connect to "+database+" as user "+username);

        System.out.print("Initializing experiment ...");
        BatchExperiment exp1 = new BatchExperiment(grammar,                                                   
                                                   new MySQLInterface(database, username, password),
                                                   resulttable);
                                                           
        int start = new Integer(args[1]);
        int end = new Integer(args[2]);


        int[] testset = //{2,7,8,23,25,29,30,39,42,48,52,53,54,58,64,66,71,76,79,90,94,96,102,105,112,113,117,141,145,149,150,151,155,166,170,171,179,180,181,183,188,190,193,194,195,196,198,199,200,206,210,211,221,224,226,227,228,231,233,234,239,241,246,247,248,249,254,257,264,271,273,278,279,293,294,296,299,302,303,305,308,310,312,316,317,320,333,340,342,345,346,350,354,355,356,359,363,364,367,369,371,375,379,381,386,388,389,391,393,399,405,407,411,413,424,428,431,435,437,451,461,466,469,471,476,482,483,484,485,486,487,492,499,501,502,505,522,527,528,529,530,531,532,534,535,539,545,556,558,559,561,563,564,565,566,570,573,575,587,588,589,591,593,612,616,617,618,619,623,626,627,628,634,636,643,649,655,661,668,669,680,681,682,685,693,695,696,698,699,700,701,702,705,706,707,708,709,712,714,715,721,723,724,729,733,735,738,743,753,754,756,759,760,766,772,773,789,793,799,802,806, 807, 811,816,823,831,835,845,848,862,865,869,873,877,879,882,884,890,895,898,899,902,905,911,914,919,922,923,925,933,936,938,939,941,942,943,945,953,954,961,964,967,969,970,971,973,981,993,995,1002,1009,1013,1015,1017,1019,1028,1037,1039,
        {1040,1045,1046,1051,1052,1054,1055,1058,1059,1062,1064,1066,1075,1086,1092,1094,1098,1100,1103,1107,1110,1116,1122,1125,1127,1135,1138,1141,1143,1145,1147,1155,1158,1163,1169,1179,1191,1201,1204,1205,1206,1220,1226,1227,1230,1233,1237,1240,1241,1242,1243,1245,1246,1250,1252,1255,1256,1259,1262,1263,1265,1275,1279,1284,1287,1291,1298,1300,1304,1305,1308,1310,1312,1317,1318,1319,1321,1325,1328,1333,1346,1349,1353,1365,1366,1369,1374,1375,1376,1378,1379,1390,1393,1403,1405,1408,1409,1415,1416,1418,1421,1422,1423,1424,1425,1426,1427,1433,1434,1435,1438,1439,1444,1455,1457,1469,1475,1476,1482,1483,1488,1490,1494,1505,1507,1520,1525,1531,1540,1550,1554,1555,1558,1566,1581,1583,1584,1585,1589,1592,1593,1595,1605,1613,1617,1619,1620,1627,1629,1631,1632,1634,1635,1639,1646,1647,1648,1652,1654,1656,1657,1660,1664,1665,1668,1670,1672,1682,1684,1687,1693,1697,1700,1704,1709,1711,1713,1714,1715,1720,1722,1723,1732,1739};

        //8,30,42,
        /*int[] testset = {8, 30, 42,48,66,71,90,94,96,149,179,180,183,188,190,194,198,200,
        210,221,228,231,233,234,239,246,248,249,254,257,271,273,279,296,299,302,303,305,
        308,310,316,317,340,356,359,371,375,381,388,389,393,399,405,407,411,413,428,435,
        466,471,476,482,483,484,486,487,499,502,505,522,530,531,532,534,535,539,545,556,
        561,563,565,566,570,575,587,616,617,623,627,628,634,643,649,655,661,668,681,682,
        701,702,707,708,712,714,723,724,729,733,735,738,743,753,754,756,759,760,766,772,
        773,806,816,823,831,835,845,862,873,884,890,895,898,899,902,905,922,939,941,943,
        945,953,954,961,967,970,995,1002,1009,1013,1045,1046,1052,1054,1055,1058,1059,
        1062,1064,1066,1086,1092,1107,1110,1125,1127,1138,1143,1158,1163,1179,1191,1204,
        1205,1220,1227,1230,1233,1237,1240,1241,1242,1243,1245,1246,1250,1252,1256,1259,
        1265,1279,1298,1300,1304,1305,1308,1319,1328,1333,1346,1349,1366,1375,1378,1379,
        1390,1393,1403,1408,1416,1418,1421,1423,1424,1425,1426,1427,1433,1434,1435,1439,
        1444,1476,1482,1483,1488,1505,1507,1525,1531,1555,1566,1581,1583,1584,1585,1589,
        1605,1613,1617,1627,1629,1631,1632,1634,1639,1648,1652,1654,1656,1660,1664,
        1665,1668,1682,1693,1700,1704,1709,1711,1714,1715,1720};
        */
        for (int i=start; i<=end; i++ ){
        //for (int i=0; i<testset.length; i++) {
            //exp1.addToBatch(testset[i]);
            exp1.addToBatch(i);
        }

        //exp1.addToBatch(30);

        System.out.println("Done.");
        System.out.println("Running experiment...");
        exp1.runExperiment();
        System.out.println("Finished Experiment.");
        System.exit(0);
    }
    
}
