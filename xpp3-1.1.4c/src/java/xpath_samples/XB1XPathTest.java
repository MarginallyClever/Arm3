
import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.xmlpull.v1.builder.xpath.jaxen.JaxenException;
import org.xmlpull.v1.builder.xpath.jaxen.XPath;
import org.xmlpull.v1.builder.xpath.Xb1XPath;
import org.xmlpull.v1.builder.XmlDocument;
import org.xmlpull.v1.builder.XmlElement;
import org.xmlpull.v1.builder.XmlInfosetBuilder;

public class XB1XPathTest extends TestCase
{
    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(XB1XPathTest.class));
    }
    
    private static final String BASIC_XML =
        "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n"+
        "\n"+
        "<foo>\n"+
        "    <bar>\n"+
        "        <baz/>\n"+
        "        <cheese/>\n"+
        "        <baz/>\n"+
        "        <cheese/>\n"+
        "        <baz/>\n"+
        "    </bar>\n"+
        "</foo>";
    
    
    public XB1XPathTest(String name) {
        super( name );
    }
    
    
    public void testConstruction()
    {
        try {
            XPath xpath = new Xb1XPath( "/foo/bar/baz" );
        } catch (JaxenException e) {
            fail( e.getMessage() );
        }
    }
    
    public void testSelection() throws Exception
    {
        XPath xpath = new Xb1XPath( "/foo/bar/baz" );
        
        XmlInfosetBuilder builder = XmlInfosetBuilder.newInstance();
        
        XmlDocument doc = builder.parseReader( new StringReader( BASIC_XML ));
        
        List results = xpath.selectNodes( doc );
        
        assertEquals( 3,
                     results.size() );
        
        Iterator iter = results.iterator();
        
        assertEquals( "baz",
                         ((XmlElement)iter.next()).getName() );
        
        assertEquals( "baz",
                         ((XmlElement)iter.next()).getName() );
        
        assertEquals( "baz",
                         ((XmlElement)iter.next()).getName() );
        
        assertTrue( ! iter.hasNext() );
        
    }
}

