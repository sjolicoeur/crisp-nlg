package crisp.converter;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;


import crisp.converter.grammar.TAGNode;
import crisp.converter.grammar.TAGLeaf;
import crisp.converter.grammar.TAGTree;
import crisp.converter.grammar.TAGLexEntry;
import crisp.converter.grammar.TAGrammar;

import crisp.planningproblem.Action;
import crisp.planningproblem.Domain;
import crisp.planningproblem.Predicate;
import crisp.planningproblem.Problem;
import crisp.planningproblem.TypedVariableList;
import crisp.planningproblem.effect.Effect;
import crisp.planningproblem.goal.Goal;
import crisp.termparser.TermParser;
import de.saar.chorus.term.Compound;
import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Substitution;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.Variable;

/**
 * This class provides a parser for CRISP grammar XML files.
 * More specifically it provides an XML content handler for these files, creates
 * and uses a SAX parser using the handler. 
 */

// Default Handler already does a lot of work like
// parse error handling and registering the handler
// with a parser.
public class GrammarParser extends DefaultHandler {  


    private Stack elementStack; 
    private LinkedList<TAGTree> trees;
    private TAGrammar grammar;

    private StringWriter characterBuffer;
    
    // Some flags and values we have to remember during parsing
    private boolean withinEntry;
    

    String probType; 
    String probLex;
    String probTree;
    String probTargetTreeID;                           
    String probTargetNode; 
    String probTargetCat;
                            
    
    
    /**
     * Create a new instance of a Grammar Converter to be used as a Content Handler 
     * for the XML parser.
     */
    public GrammarParser() {
       trees = new LinkedList<TAGTree>(); 
       elementStack = new Stack<TAGTree>();
       grammar = new TAGrammar();

       withinEntry = false;
       characterBuffer = new StringWriter();
    }

    /************************ Methods for the content handler *****************/
    
    // All of these methods are specified in the ContentHandler interface.    

    public void startDocument() throws SAXException {
        
    }

    public void startElement(String namespaceURI, String localName, String qName, 
        Attributes atts) throws SAXException {

        // Namespace is ingored for now, use prefixed name and assume prefixes are empty.
        
        if (qName.equals("crisp-grammar")){
        } else 
        
        if (qName.equals("tree")){
            if (withinEntry) {
                TAGLexEntry entry = (TAGLexEntry) elementStack.peek();
                entry.setTreeRef(atts.getValue("refid"));
            } else {
                // Create a new tree instance and store it on the stack
                TAGTree newTree = new TAGTree();
                newTree.setID(atts.getValue("id"));
                elementStack.push(newTree);
            }
        } else 
        
        if (qName.equals("node")){
            // Create a new node instance and store it on the stack 
            TAGNode newNode = new TAGNode();

            String index = atts.getValue("index");
            if (index!=null) 
                newNode.setIndex(index);
    
            newNode.setCat(atts.getValue("cat"));
            newNode.setSem(atts.getValue("sem"));
            newNode.setType(atts.getValue("type"));
            newNode.setConstraint(atts.getValue("constraint"));
            elementStack.push(newNode);
        } else 
        
        if (qName.equals("leaf")){
            // Create a new leaf instance and store it on the stack 
            TAGNode newLeaf = new TAGNode();

            String index = atts.getValue("index");
            if (index!=null) 
                newLeaf.setIndex(index);
    
            newLeaf.setCat(atts.getValue("cat"));
            newLeaf.setSem(atts.getValue("sem"));
            newLeaf.setType(atts.getValue("type"));
            newLeaf.setConstraint(atts.getValue("constraint"));
   
            elementStack.push(newLeaf);
        }
         
        else
        if (qName.equals("entry")){
            TAGLexEntry entry = new TAGLexEntry();
            entry.setPOS(atts.getValue("pos"));
            entry.setWord(atts.getValue("word"));
            entry.setID(atts.getValue("id"));
            elementStack.push(entry);
            withinEntry = true;
        }
        
        else
        if (qName.equals("semreq") || qName.equals("semcontent")){
            characterBuffer = new StringWriter(); // Expect characted data: 
                                                  //   clear the character buffer
        } else
        if (qName.equals("lex")){
            TAGLexEntry entry = (TAGLexEntry) elementStack.peek();
            entry.addLexValue(atts.getValue("pos"), atts.getValue("word"));
        } else 
        
        if (qName.equals("prob")){
            // All of this has to be remembered: it is needed when the </prob> tag appears
            probType = atts.getValue("type");
            probLex = atts.getValue("lex");
            probTree = atts.getValue("tree");
            probTargetTreeID = atts.getValue("targetid"); // ID of the entry in which to 
                                                         //   substitute or adjoin this entry
            probTargetNode = atts.getValue("sem");    // Substitution/Adjunction node in 
                                                         //   the target tree where 
            probTargetCat = atts.getValue("cat");
                                                         
            // if no ID exists create new target ID from tree reference and lex
            if (!probType.equals("init") && (probTargetTreeID==null))            
                probTargetTreeID = probTree+"-"+probLex;           
            
                                                         
            characterBuffer = new StringWriter(); // Expect characted data: 
                                                  //   clear the character buffer
        }
        
    }
    
    public void endElement(String namespaceURI, String localName, String qName)
         throws SAXException {
        
        if (qName.equals("crisp-grammar")){
        } else 
        
        if (qName.equals("tree")){
            
            if (withinEntry) {
                 
            } else {
              TAGTree tree = (TAGTree) elementStack.pop();
              grammar.addTree(tree);         
            }

        } else 
        
        if (qName.equals("node")){
            TAGNode node = (TAGNode) elementStack.pop();
            Object previous = elementStack.peek();
         
            if (previous instanceof TAGNode)    //previous element on stack was
                ((TAGNode) previous).add(node); //  a node: Add daughter node

            else if (previous instanceof TAGTree) { // previous element was a
                TAGTree tree = (TAGTree) previous; //  TAG tree:
                tree.setRootNode(node);             //  Set it's root node 
            }   

            else throw 
               new SAXParseException("Unexpected element around <node>.",null);
            
        } else 
        
        if (qName.equals("leaf")){
            TAGNode node = (TAGNode) elementStack.pop();
            Object previous = elementStack.peek();
           
            if (previous instanceof TAGNode)    //previous element on stack was
                ((TAGNode) previous).add(node); //  a node: Add daughter node
            
            else if (previous instanceof TAGTree) { // previous element was a
                TAGTree tree = (TAGTree) previous;  //  TAG tree:
                tree.setRootNode(node);             //  Set it's root node 
            }

            else 
               new SAXParseException("Unexpected element around <leaf>.",null);
        }
        else 

        if (qName.equals("entry")){
            withinEntry = false;
            TAGLexEntry entry = (TAGLexEntry) elementStack.pop();
	    // Check if this entry has an ID, otherwise create one
	    if (entry.getID()==null) {
            entry.setID(entry.getTreeRef()+"-"+entry.getWord());
	    }
            grammar.addEntry(entry);         
        } else
        
        if (qName.equals("semreq")){
            String semReq =  characterBuffer.toString();
            TAGLexEntry entry = (TAGLexEntry) elementStack.peek();
            entry.addSemReq(semReq);
        } else
        
        if (qName.equals("semcontent")){
            String semContent =  characterBuffer.toString();
            TAGLexEntry entry = (TAGLexEntry) elementStack.peek();
            entry.setSemContent(semContent);
        }
        
        if (qName.equals("pragcondition")){
            String pragCond =  characterBuffer.toString();
            TAGLexEntry entry = (TAGLexEntry) elementStack.peek();
            entry.addPragCond(pragCond);
        } else
        
        if (qName.equals("prageffect")){
            String pragEff =  characterBuffer.toString();
            TAGLexEntry entry = (TAGLexEntry) elementStack.peek();
            entry.addPragEffect(pragEff);
        } else  
        
        if (qName.equals("prob")){
            float prob = Float.parseFloat(characterBuffer.toString());
           
            TAGLexEntry entry = (TAGLexEntry) elementStack.peek();
           
            if (probType.equals("init"))
                entry.setInitProb(prob);    
            else if (probType.equals("subst"))
                entry.addSubstProb(probTargetTreeID,probTargetNode, probTargetCat, prob);
            else if (probType.equals("adjoin"))
                entry.addAdjProb(probTargetTreeID,probTargetNode, probTargetCat,prob);
            
           
        }
    }


    public void characters(char[] ch, int start, int length){ 
        characterBuffer.write(ch,start,length);
    }

    

    /******** input, output, main program **********/

    public TAGrammar getParsedGrammar(){ return grammar; }

    /**
     * Parses a grammar specification from an XML file and returns a 
     * TAGrammar object containing the parsed Tree Adjoining Grammar.
     *
     * @param grammarfile The grammar file to be parsed.
     * @return a {@link crisp.converter.grammar.TAGrammar} object containing the 
     *         parsed TAG.
     * @throws ParserConfigurationException If the parser is misconfigured.
     * @throws SAXException If the grammar file could not be parsed.
     * @throws IOException If the grammar file cannot be opened or read.
     */
    public static TAGrammar parseGrammar(File grammarfile) throws SAXException, ParserConfigurationException, IOException {
        // run the parser on the grammar specification file
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();

        GrammarParser handler = new GrammarParser();

        parser.parse(grammarfile, handler);

        return handler.getParsedGrammar();
    }


    public static void main(String[] argv){
        try {

            TAGrammar grammar = parseGrammar(new File(argv[1]));

            System.out.println("Read the following trees:");
            for (TAGTree tree : grammar.getTrees()) {
                System.out.println(tree.getID());
                      
            }
            
            System.out.println("Read the following lexical entries:");
            for (TAGLexEntry entry : grammar.getLexicon()) {
                System.out.println(entry.getWord());
            }

        } catch (ParserConfigurationException e) {
            System.out.println("Parser misconfigured: "+e);
        } catch (SAXException e) {
            System.out.println("Parse error: "+e);
        } catch (IOException e) {
            System.out.println("Couldn't read grammar file: "+e);
        }
    }

}
