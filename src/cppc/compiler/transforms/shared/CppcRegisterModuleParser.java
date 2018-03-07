package cppc.compiler.transforms.shared;

import cetus.hir.Identifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CppcRegisterModuleParser extends DefaultHandler
{
  private static final String XML_FUNCTION_TAG = "function";
  private static final String XML_NAME_ATTR = "name";
  private static final String XML_INPUT_TAG = "input";
  private static final String XML_INPUT_OUTPUT_TAG = "input-output";
  private static final String XML_OUTPUT_TAG = "output";
  private static final String XML_PARAMETER_ATTR = "parameters";
  private static final String XML_VARARGS_VALUE = "...";
  private static final String XML_SEMANTICS_TAG = "semantics";
  private static final String XML_SEMANTIC_TAG = "semantic";
  private static final String XML_ROLE_ATTR = "role";
  private static final String XML_PARAMETER_TAG = "attribute";
  private static final String XML_VALUE_ATTR = "value";
  private SAXParser parser;
  private Set<ProcedureParameter> currentConsumed;
  private Set<ProcedureParameter> currentGenerated;
  private String currentProcedure;
  private String currentRole;
  private Hashtable<String, String> currentParameters;
  private Hashtable<String, Hashtable<String, String>> currentSemantics;
  
  private CppcRegisterModuleParser()
    throws ParserConfigurationException, SAXException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    this.parser = factory.newSAXParser();
    
    this.currentConsumed = null;
    
    this.currentGenerated = null;
    this.currentProcedure = null;
    
    this.currentRole = null;
    this.currentParameters = null;
    this.currentSemantics = null;
  }
  
  public static void parse(File file)
  {
    try
    {
      FileInputStream inputStream = new FileInputStream(file);
      InputSource inputSource = new InputSource(inputStream);
      CppcRegisterModuleParser parser = new CppcRegisterModuleParser();
      parser.parse(inputSource);
    }
    catch (FileNotFoundException e)
    {
      System.err.println("WARNING: cannot open file " + file.getPath() + ": " + 
        e.getMessage());
    }
    catch (SecurityException e)
    {
      System.err.println("WARNING: access denied reading " + file.getPath() + 
        ": " + e.getMessage());
    }
    catch (ParserConfigurationException e)
    {
      System.err.println("WARNING: error parsing file " + file.getPath() + ": " + 
        e.getMessage());
    }
    catch (SAXException e)
    {
      System.err.println("WARNING: error parsing file " + file.getPath() + ": " + 
        e.getMessage());
    }
    catch (IOException e)
    {
      System.err.println("WARNING: error reading file " + file.getPath() + ": " + 
        e.getMessage());
    }
  }
  
  public void parse(InputSource inputSource)
    throws SAXException, IOException
  {
    this.parser.parse(inputSource, this);
  }
  
  public void startElement(String uri, String localName, String qName, Attributes atts)
  {
    if (qName.equals("function"))
    {
      String functionName = atts.getValue("name");
      startFunction(functionName);
    }
    if (qName.equals("input")) {
      startInput(atts);
    }
    if (qName.equals("output")) {
      startOutput(atts);
    }
    if (qName.equals("input-output")) {
      startIO(atts);
    }
    if (qName.equals("semantic")) {
      startSemantic(atts);
    }
    if (qName.equals("attribute")) {
      startParameter(atts);
    }
  }
  
  private void startFunction(String functionName)
  {
    this.currentProcedure = functionName;
    this.currentConsumed = new HashSet();
    this.currentGenerated = new HashSet();
    this.currentParameters = new Hashtable();
    this.currentSemantics = new Hashtable();
  }
  
  private void startInput(Attributes atts)
  {
    registerParameters(atts, this.currentConsumed);
  }
  
  private void startOutput(Attributes atts)
  {
    registerParameters(atts, this.currentGenerated);
  }
  
  private void startIO(Attributes atts)
  {
    registerParameters(atts, this.currentConsumed);
    registerParameters(atts, this.currentGenerated);
  }
  
  private void startSemantic(Attributes atts)
  {
    this.currentRole = atts.getValue("role");
  }
  
  private void startParameter(Attributes atts)
  {
    String name = atts.getValue("name");
    String value = atts.getValue("value");
    
    this.currentParameters.put(name, value);
  }
  
  private void registerParameters(Attributes atts, Set<ProcedureParameter> set)
  {
    Set<ProcedureParameter> newElems = parseParameters(atts);
    set.addAll(newElems);
  }
  
  private Set<ProcedureParameter> parseParameters(Attributes atts)
  {
    String value = atts.getValue("parameters");
    String[] parameters = value.trim().split(",");
    Set<ProcedureParameter> returnSet = new HashSet(
      parameters.length);
    for (int i = 0; i < parameters.length; i++)
    {
      String parameter = parameters[i].trim();
      if (parameter.equals("...")) {
        returnSet.add(ProcedureParameter.VARARGS);
      } else {
        returnSet.add(new ProcedureParameter(new Integer(
          parameters[i].trim()).intValue() - 1));
      }
    }
    return returnSet;
  }
  
  public void endElement(String uri, String localName, String qName)
  {
    if (qName.equals("function")) {
      endFunction();
    }
    if (qName.equals("semantic")) {
      endSemantic();
    }
  }
  
  private void endSemantic()
  {
    this.currentSemantics.put(this.currentRole, this.currentParameters);
  }
  
  private void endFunction()
  {
    Identifier name = new Identifier(this.currentProcedure);
    ProcedureCharacterization characterization = 
      new ProcedureCharacterization(name);
    characterization.setGenerated(this.currentGenerated);
    characterization.setConsumed(this.currentConsumed);
    characterization.setSemantics(this.currentSemantics);
    characterization.statementCount = 1;
    
    CppcRegisterManager.addProcedure(name, characterization);
  }
}
