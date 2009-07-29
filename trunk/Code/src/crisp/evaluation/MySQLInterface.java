package crisp.evaluation;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.util.Set;
import java.util.HashSet;
import java.util.Properties;

import de.saar.chorus.term.Term;
import de.saar.chorus.term.parser.TermParser;

import de.saar.penguin.tag.derivation.DerivationTree;
import de.saar.penguin.tag.derivation.DerivedTree;

/**
 * An interface for connecting to a database that stores planning problems and result. 
 *
 * @author Daniel Bauer
 *
 */
public class MySQLInterface extends DatabaseInterface {

    private Connection connection;
    private String url;
    private String username;
    private String password;
    private Properties connectionProperties;        

    /**
     * Open a new database interface.
     */
    public MySQLInterface (String url, String username, String password) throws Exception{
        Class.forName ("com.mysql.jdbc.Driver").newInstance();
                               
        connectionProperties = new Properties();
        if( username != null ) {
            connectionProperties.put("user", username);
        }
        if( password != null ) {
            connectionProperties.put("password", password);
        }
                        
        connection = DriverManager.getConnection(url, connectionProperties);
    }
    
     /**
      * Execute a given SQL query on the database and return a result set.
      */
     private ResultSet executeQuery(String sql) throws SQLException{
        Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,  ResultSet.CONCUR_READ_ONLY);
        ResultSet srs = stmt.executeQuery(sql);
        return srs;
     }


     private int executeUpdate(String sql) throws SQLException{
         Statement stmt = connection.createStatement();
         int updatecount = stmt.executeUpdate(sql);
         return updatecount;
     }
     
    /**
     * Retrieve a semantic representation for a sentence in the database.
     * @return Semantic representation for the sentence as a set of ground positive literals.
     * @param sentenceID the id of the sentence for which to return semantics.
     */
    public Set<Term> getSentenceSemantics(String semanticsTableName, int sentenceID) throws Exception{
        
        Set<Term> literalSet = new HashSet<Term>();
        
        String sql = "SELECT DISTINCT atom FROM "+semanticsTableName+" WHERE sentence_id = " + sentenceID;         
        ResultSet srs = executeQuery(sql); 
        while (srs.next()){
            literalSet.add(TermParser.parse(srs.getString("atom")));
        }
        
        return literalSet;
    }
    
    /**
     * Return the individual associated with the root of the derivation for a sentence. 
     * @param SentenceID the id of the sentence for which to return the root index.
     * @return The individual that is associated with the root of the derivation for the sentence. 
     */
    public String getRootIndex(String semanticsTableName, int sentenceID) throws Exception{
                
        String sql = "SELECT DISTINCT root_index FROM "+semanticsTableName+" WHERE sentence_id = " + sentenceID;         
        
        ResultSet srs = executeQuery(sql);
        
        srs.first();
        String root_index = srs.getString("root_index");
        
        return root_index;
    }

    private void setResultString(String resultTableName, String column, int sentenceID, String text) throws Exception {
        String sql = "INSERT INTO " + resultTableName + " (sentence_id, "+column+") VALUES (" + sentenceID + ", '"+text+"');";
        executeUpdate(sql);
    }
   
    /**
     * Write a derivation to the result database table. 
     */
    public void setResultDerivation(String resultTableName, int sentenceID, DerivationTree derivation) throws Exception {
        setResultString(resultTableName, "derivation", sentenceID, derivation.toString());
    }

    /**
     * Write a derived tree to the result database table. 
     */
    public void setResultDerivedTree(String resultTableName, int sentenceID, DerivedTree derivedTree ) throws Exception {
        setResultString(resultTableName, "derived_tree", sentenceID, derivedTree.toString());
    }

    /**
     * Write a derived tree yield to the result database table. 
     */
    public void setResultSurface(String resultTableName, int sentenceID, String surfaceString) throws Exception{
        setResultString(resultTableName, "surface", sentenceID, surfaceString);
    }
    
    private void setResultTime(String resultTableName, String timeColumn, int sentenceID, long time) throws Exception {
        String sql = "INSERT INTO " + resultTableName + " (sentence_id, "+ timeColumn + ") VALUES (" + sentenceID + ", " + time + ");";
        executeUpdate(sql);
    }
    

    public void setDomainSize(String resultTableName, int sentenceID, int size) throws Exception {
        String sql = "INSERT INTO " + resultTableName + " (sentence_id, domain_size) VALUES (" + sentenceID + ", " + size + ");";
        executeUpdate(sql);
    }

    /**
     * Write the time needed to create the planning problem to the result database table. 
     */
    public void setResultCreationTime(String resultTableName, int sentenceID, long time) throws Exception{
        setResultTime(resultTableName, "creation_time", sentenceID, time);
    }
    
    /**
     * Write the time needed for preprocessing the planning problem to the result database table. 
     */
    public void setResultPreprocessingTime(String resultTableName, int sentenceID, long time) throws Exception{
        setResultTime(resultTableName, "preprocessing_time", sentenceID, time);
    }

    /**
     * Write the time needed for solving the planning problem to the result database table. 
     */
    public void setResultSearchTime(String resultTableName, int sentenceID, long time) throws Exception {
        setResultTime(resultTableName, "search_time", sentenceID, time);
    }

    
    public void setResultTimes(String resultTableName, int sentenceID, long creationTime, long preprocessingTime, long searchTime) throws Exception{
        setResultCreationTime(resultTableName, sentenceID, creationTime);
        setResultPreprocessingTime(resultTableName, sentenceID, preprocessingTime);
        setResultSearchTime(resultTableName, sentenceID, searchTime);
    }
    

    public void setResultError(String resultTableName, int sentenceID, String errormsg) throws Exception{
        setResultString(resultTableName, "error", sentenceID, errormsg);    
    }


    public void writeResults(String resultTableName, int sentenceID, int domainSize, DerivationTree derivation, DerivedTree derivedTree, long creationTime, long preprocessingTime, long searchTime, String error) throws SQLException {

        String sql;
        if (derivation==null){
            sql = "INSERT INTO "+resultTableName+" (sentence_id, domain_size, derivation, derived_tree, surface, creation_time, preprocessing_time, search_time, error) VALUES (" +
                     sentenceID +"," + domainSize +",'','','',"+creationTime+","+preprocessingTime+","+searchTime+",'"+error+"');";
        } else {
         sql = "INSERT INTO "+resultTableName+" (sentence_id, domain_size, derivation, derived_tree, surface, creation_time, preprocessing_time, search_time, error) VALUES (" + sentenceID +"," + domainSize + ",'" + derivation + "','"+derivedTree+"','"+derivedTree.yield()+"',"+creationTime+","+preprocessingTime+","+searchTime+",'"+error+"');";
        }
        executeUpdate(sql);                                          
    }
    
    /*
    public static void main(String[] args) throws Exception{
        MySQLInterface database = new MySQLInterface("jdbc:mysql://forbin/penguin" ,"penguin_rw","xohD9xei");
        Set<Term> sem = database.getSentenceSemantics(1);
        String rootIndex = database.getRootIndex(1);
        System.out.println(sem);
        System.out.println("Root index: "+rootIndex);
    }*/

}
