package de.saar.penguin.tag.grammar;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import de.saar.chorus.term.Constant;
import de.saar.chorus.term.Term;
import de.saar.chorus.term.parser.TermParser;
import de.saar.penguin.tag.codec.InputCodec;
import de.saar.penguin.tag.codec.ParserException;

public class SituatedCrispXmlInputCodec implements InputCodec<Term> {

    private CrispGrammar grammar;

    public void parse(Reader reader, CrispGrammar grammar) throws ParserException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        this.grammar = grammar;

        try {
            SAXParser parser = factory.newSAXParser();
            parser.parse(new InputSource(reader), new Handler());
        } catch (ParserConfigurationException e) {
            throw new ParserException(e);
        } catch (SAXException e) {
            throw new ParserException(e);
        }
    }

    public void parse(Reader reader, Grammar<Term> grammar) throws ParserException, IOException {
        parse(reader, (CrispGrammar) grammar);
    }

    private class Handler extends DefaultHandler {

        private List<String> currentNodes;
        private ElementaryTree<Term> currentTree;
        private String currentLexTree;
        private Map<String, String> currentAux;
        private String currentLexWord;
        private boolean withinEntry = false;
        private List<Term> currentSem;
        private List<Term> currentSemReq;
        private List<Term> currentPragCond;
        private List<Term> currentPragEff;
        private List<Term> currentImpEff;
        private Map<String, String> additionalParams;
        private Map<String, String> additionalVars;
        private String currentParamType;
        private StringWriter characterBuffer = new StringWriter();

        private String getParent() {
            if (currentNodes.isEmpty()) {
                return null;
            } else {
                return currentNodes.get(currentNodes.size() - 1);
            }
        }

        @Override
        public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
            if (name.equals("tree")) {
                if (withinEntry) {
                    currentLexTree = attributes.getValue("refid");
                    currentAux = new HashMap<String, String>();
                    currentSem = new ArrayList<Term>();
                    currentSemReq = new ArrayList<Term>();
                    currentPragCond = new ArrayList<Term>();
                    currentPragEff = new ArrayList<Term>();
                    currentImpEff = new ArrayList<Term>();
                    additionalParams = new HashMap<String,String>();
                    additionalVars = new HashMap<String,String>();                    
                } else {
                    currentTree = new ElementaryTree<Term>();
                    currentTree.setType(ElementaryTreeType.INITIAL);
                    grammar.addTree(attributes.getValue("id"), currentTree);
                    currentNodes = new ArrayList<String>();
                }
            } else if (name.equals("node") || name.equals("leaf")) {
                NodeType type = decodeNodeType(attributes.getValue("type"));
                String node = currentTree.addNode(attributes.getValue("cat"), type, Constraint.NONE, decodeSemRole(attributes.getValue("sem")), getParent(), attributes.getValue("id"));

                currentNodes.add(node);

                if (type == NodeType.FOOT) {
                    currentTree.setType(ElementaryTreeType.AUXILIARY);
                }
            } else if (name.equals("entry")) {
                currentLexWord = attributes.getValue("word");
                withinEntry = true;
            } else if (name.equals("lex")) {
                currentAux.put(attributes.getValue("pos"), attributes.getValue("word"));
            } else if (name.equals("semcontent") || name.equals("semreq") ||
                       name.equals("pragcond") || name.equals("prageff") ||
                       name.equals("impeff") ) {
                characterBuffer = new StringWriter();
            } else if (name.equals("param")) {
                characterBuffer = new StringWriter();
                currentParamType = attributes.getValue("type") ;
                if (currentParamType == null)
                    currentParamType = "individual"; // default to individual
            } else if (name.equals("var")) {
                characterBuffer = new StringWriter();
                currentParamType = attributes.getValue("type");
                if (currentParamType == null)
                    currentParamType = "individual"; // default to individual
            }
        }

        @Override
        public void endElement(String uri, String localName, String name) throws SAXException {
            if (name.equals("node") || name.equals("leaf")) {
                currentNodes.remove(currentNodes.size() - 1);
            } else if (name.equals("tree")) {
                if (withinEntry) {
                    CrispLexiconEntry newEntry = new CrispLexiconEntry(currentLexWord, currentLexTree, currentAux, currentSem);
                    newEntry.addSemanticRequirements(currentSemReq);
                    newEntry.addPragmaticPreconditions(currentPragCond);
                    newEntry.addPragmaticEffects(currentPragEff);
                    newEntry.addImperativeEffects(currentImpEff);
                    newEntry.addAdditionalParams(additionalParams);
                    newEntry.addAdditionalVars(additionalVars);
                    grammar.addLexiconEntry(newEntry);
                }
            } else if (name.equals("entry")) {
                withinEntry = false;
            } else if (name.equals("semcontent")) {
                currentSem.add(TermParser.parse(characterBuffer.toString()));
            } else if (name.equals("semreq")) {
                currentSemReq.add(TermParser.parse(characterBuffer.toString()));
            } else if (name.equals("pragcond")) {
                currentPragCond.add(TermParser.parse(characterBuffer.toString()));
            } else if (name.equals("prageff")) {
                currentPragEff.add(TermParser.parse(characterBuffer.toString()));
            } else if (name.equals("impeff")) {
                currentImpEff.add(TermParser.parse(characterBuffer.toString()));
            } else if (name.equals("param")) {
                additionalParams.put(characterBuffer.toString(), currentParamType);
            } else if (name.equals("var")) {
                additionalVars.put(characterBuffer.toString(), currentParamType);
            }
        }

        private NodeType decodeNodeType(String nodeTypeString) {
            if (nodeTypeString == null) {
                return NodeType.INTERNAL;
            } else if (nodeTypeString.equals("substitution")) {
                return NodeType.SUBSTITUTION;
            } else if (nodeTypeString.equals("anchor")) {
                return NodeType.ANCHOR;
            } else if (nodeTypeString.equals("foot")) {
                return NodeType.FOOT;
            } else if (nodeTypeString.equals("lex")) {
                return NodeType.AUX_LEXICAL;
            } else {
                return NodeType.INTERNAL;
            }
        }

        private Term decodeSemRole(String semrole) {
            return new Constant(semrole);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            characterBuffer.write(ch, start, length);
        }
    }
}
