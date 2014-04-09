/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

//import junit.framework.Test;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import junit.framework.TestSuite;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * Test if entity replacement works ok.
 * This test is designe to work bboth for validating and non validating parsers!
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestEntityReplacement extends UtilTestCase {
    private XmlPullParserFactory factory;
    
    public TestEntityReplacement(String name) {
        super(name);
    }
    
    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        assertEquals(true, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        //assertEquals(false, factory.getFeature(XmlPullParser.FEATURE_VALIDATION));
    }
    
    protected void tearDown() {
    }
    
    public void testEntityReplacement() throws IOException, XmlPullParserException
    {
        // taken from http://www.w3.org/TR/REC-xml#sec-entexpand
        final String XML_ENTITY_EXPANSION =
            "<?xml version='1.0'?>\n"+
            "<!DOCTYPE test [\n"+
            "<!ELEMENT test (#PCDATA) >\n"+
            "<!ENTITY % xx '&#37;zz;'>\n"+
            "<!ENTITY % zz '&#60;!ENTITY tricky \"error-prone\" >' >\n"+
            "%xx;\n"+
            "]>"+
            "<test>This sample shows a &tricky; method.</test>";
        
        XmlPullParser pp = factory.newPullParser();
        // default parser must work!!!!
        pp.setInput(new StringReader( XML_ENTITY_EXPANSION ) );
        if(pp.getFeature( XmlPullParser.FEATURE_PROCESS_DOCDECL ) == false) {
            pp.defineEntityReplacementText("tricky", "error-prone");
        }
        testEntityReplacement(pp);
        
        // now we try for no FEATURE_PROCESS_DOCDECL
        pp.setInput(new StringReader( XML_ENTITY_EXPANSION ) );
        try {
            pp.setFeature( XmlPullParser.FEATURE_PROCESS_DOCDECL, false );
        } catch( Exception ex ){
        }
        if( pp.getFeature( XmlPullParser.FEATURE_PROCESS_DOCDECL ) == false ) {
            pp.defineEntityReplacementText("tricky", "error-prone");
            testEntityReplacement(pp);
        }
        
        // try to use FEATURE_PROCESS_DOCDECL if supported
        pp.setInput(new StringReader( XML_ENTITY_EXPANSION ) );
        try {
            pp.setFeature( XmlPullParser.FEATURE_PROCESS_DOCDECL, true );
            //PackageTests.addNote("* feature "+pp.FEATURE_PROCESS_DOCDECL+" is supported\n");
        } catch( Exception ex ){
        }
        if( pp.getFeature( XmlPullParser.FEATURE_PROCESS_DOCDECL ) ) {
            testEntityReplacement(pp);
        }
        
        // try to use FEATURE_VALIDATION if supported
        pp.setInput(new StringReader( XML_ENTITY_EXPANSION ) );
        try {
            pp.setFeature( XmlPullParser.FEATURE_VALIDATION, true );
            //PackageTests.addNote("* feature "+pp.FEATURE_VALIDATION+" is supported\n");
        } catch( Exception ex ){
        }
        if( pp.getFeature( XmlPullParser.FEATURE_VALIDATION ) ) {
            testEntityReplacement(pp);
        }
        
    }
    
    public void testEntityReplacement(XmlPullParser pp) throws IOException, XmlPullParserException
    {
        pp.next();
        checkParserStateNs(pp, 1, XmlPullParser.START_TAG,
                           null, 0, "", "test", null, false, 0);
        pp.next();
        checkParserStateNs(pp, 1, XmlPullParser.TEXT, null, 0, null, null,
                           "This sample shows a error-prone method.", false, -1);
        pp.next();
        checkParserStateNs(pp, 1, XmlPullParser.END_TAG,
                           null, 0, "", "test", null, false, -1);
        pp.nextToken();
        checkParserStateNs(pp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
        
    }
    
    /**
     * Additional tests to confirm that
     * <a href="http://www.extreme.indiana.edu/bugzilla/show_bug.cgi?id=192">Bug 192
     * Escaped characters disappear in certain cases</a>
     * is not present in XmlPull implementations.
     */
    public void testLastEntity(String xml, String expectedText) throws Exception {
        InputStream stream = new ByteArrayInputStream(xml.getBytes());
        XmlPullParser pp = factory.newPullParser();
        // default parser must work!!!!
        pp.setInput(stream, "UTF-8" );
        pp.nextTag();
        assertEquals(XmlPullParser.START_TAG, pp.getEventType());
        //assertEquals("UTF-8", pp.getCharacterEncodingScheme());
        //assertEquals("1.0", startdoc.getVersion());
        String c = pp.nextText();
        assertEquals(expectedText, c);
        assertEquals(XmlPullParser.END_TAG, pp.getEventType());
    }
    
    public void testLastEntity() throws Exception {
        testLastEntity(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><document>&lt;text&gt;</document>",
            "<text>"
        );
        testLastEntity(
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><document>&lt;text&gt; </document>",
            "<text> "
        );
    }
    
    public void testDefineEntityBeforeSetInput() throws IOException, XmlPullParserException
    {
        XmlPullParser pp = factory.newPullParser();
        pp.defineEntityReplacementText("foo", "bar");
        pp.setInput(new StringReader( "<foo/>" ) );
        
    }
    
    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestEntityReplacement.class));
    }
    
}

