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


/**
 * An interface for connecting to a database that stores planning problems and result. 
 *
 * @author Daniel Bauer
 *
 */
public class MySQLInterface {

    private static final String semanticsTableName = "dbauer_PTB_semantics";
    
    private Connection connection;
    private String url;
    private String username;
    private String password;
    private Properties connectionProperties;        
    
    /**
     * Open a new database interface.
     */
    public void MySQLInterface (String url, String username, String password, String filename) throws Exception{
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
     * Retrieve a semantic representation for a sentence in the database.
     * @return Semantic representation for the sentence as a set of ground positive literals.
     * @param sentenceID the id of the sentence for which to return semantics.
     */
    public Set<Term> getSentenceSemantics(int sentenceID) throws SQLException{
        
        Set<Term> literalSet = new HashSet<Term>();
        
        String sql = "SELECT DISTINCT atom FROM "+semanticsTableName+" WHERE sentence_id = " + sentenceID;         
        
        Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,  ResultSet.CONCUR_READ_ONLY);
        ResultSet srs = stmt.executeQuery(sql);
        
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
    public String getRootIndex(int sentenceID) throws SQLException{
                
        String sql = "SELECT DISTINCT root_index FROM "+semanticsTableName+" WHERE sentence_id = " + sentenceID;         
        
        Statement stmt = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,  ResultSet.CONCUR_READ_ONLY);
        ResultSet srs = stmt.executeQuery(sql);
        
        srs.first();
        String root_index = srs.getString("root_index");
        
        return root_index;
    }
    
}