package cppc.compiler.transforms.shared;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

//处理xml parser
public class CppcRegisterParser extends DefaultHandler
{
  private static final String XML_MODULE_TAG_NAME = "module";
  private static final String XML_MODULE_FILE_ATTRIBUTE = "file";
  private SAXParser parser;
  
  private CppcRegisterParser() throws ParserConfigurationException, SAXException
  {
    SAXParserFactory factory = SAXParserFactory.newInstance();
    this.parser = factory.newSAXParser();
  }
  
  public static void parse(File file)
  {
    try
    {
      FileInputStream inputStream = new FileInputStream(file);
      InputSource inputSource = new InputSource(inputStream);
      CppcRegisterParser parser = new CppcRegisterParser();
      parser.parse(inputSource);
    }
    catch (FileNotFoundException e)
    {
      System.err.println("WARNING: cannot open file " + file.getPath() + ": " + e.getMessage());
    }
    catch (SecurityException e)
    {
      System.err.println("WARNING: access denied reading " + file.getPath() + ": " + e.getMessage());
    }
    catch (ParserConfigurationException e)
    {
      System.err.println("WARNING: error parsing file " + file.getPath() + ": " +  e.getMessage());
    }
    catch (SAXException e)
    {
      System.err.println("WARNING: error parsing file " + file.getPath() + ": " +  e.getMessage());
    }
    catch (IOException e)
    {
      System.err.println("WARNING: error reading file " + file.getPath() + ": " +  e.getMessage());
    }
  }
  
  public void parse(InputSource inputSource) throws SAXException, IOException
  {
    this.parser.parse(inputSource, this);
  }
  
  //重载函数
  public void startElement(String uri, String localName, String qName, Attributes attributes)
  {
    if (qName.equals("module"))
    {
      String fileName = attributes.getValue("file");
      
      File file = new File(fileName);
      if (file == null) {
        System.err.println("WARNING: cannot access file " + fileName + " for reading");
      } else {
        CppcRegisterModuleParser.parse(file);
      }
    }
  }



}
