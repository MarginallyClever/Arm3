/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)


/*
 SYNTAX: (// marks unimplemented)
 <tests xmlns="http://xmlpull.org/v1/tests/2002-08.xsd">
 <test-parser name="descriptive name of test">
 <input-inline>inlined xml</input-inline>?
// <input-file>name of file</input-file>?
 <set-feature>name of feature</set-feature>*
 <expect
 type="START_DOCUMENT|END_DOCUMENT|START_TAG|END_TAG|TEXT|COMMENT|..."?
 name="getName()"? namespace="getNamespace()"?
// prefix="getPrefix()"? depth="getDepth()"? empty="isEmptyElementTag"?
// namespaceCount="getNamespacesCount(getDepth())"? attributeCount="getAttributeCount()"?
// text="getText()" isWhitespace="isWhitespace():
 />*
// <check-attribute pos="position" prefix="getAttributePrefix(pos)"? namespace=...? name=...?
// value="..." is-default="0|1"?/>
// <check-namespace pos="position" prefix="getNamespacePrefix(pos)"? namespace=...?/>
 <next>*
// <nextToken>*
// <nextTag>*
// <nextText>*
// <!-- what about require(), getTextCharacters(), getLineNumber/getColumnNumber(), defineEntityReplacementText()?/>
 </test-parser>*
// <test-serializer>
// <start-tag name="..." namespace="..."/>
// </test-serializer>
 </tests>
 */

package org.xmlpull.v1.tests;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
//import org.xmlpull.v1.util.XmlPullWrapper;

//import org.xmlpull.v1.tests.UtilTestCase;

public class XmlTestCase extends UtilTestCase {
    private static final String NS_URI = "http://xmlpull.org/v1/tests/2002-08.xsd";
    //private static boolean standalone = true;
    private static final boolean verbose = false;

    //public static void main (String[] args) {
    //    junit.textui.TestRunner.run (new TestSuite(XmlTestCase.class));
    //}

    public XmlTestCase(String name) {
        super(name);
        //standalone = false;
    }

    protected void verbose(String msg)
    {
        if(verbose) {
            if(PackageTests.runnigAllTests()) {
                //PackageTests.addNote(msg+"\n");
                System.out.println(msg);
            } else {
                System.out.println(msg);
            }
        }
    }

    protected void info(String msg)
    {
        if(PackageTests.runnigAllTests()) {
            PackageTests.addNote(msg+"\n");
        } else {
            System.out.println(msg);
        }
    }

    private final static String RESOURCE_PREFIX = "/org/xmlpull/v1/tests/xml/";
    private InputStream openStream(String name) throws IOException {
        //String fullName = "src/xml/tests/"+testName;
        InputStream is = null;
        if(is == null) {
            try {
                is = new FileInputStream(name);
            } catch(Exception ex) {
            }
        }
        if(is == null) {
            try {
                is = getClass().getResourceAsStream (RESOURCE_PREFIX+name);
            } catch(Exception ex) {
            }
        }
        if(is == null) {
            try {
                is = getClass().getResourceAsStream ("/"+name);
            } catch(Exception ex) {
            }
        }
        /*if(is == null) {   // this seems to causes loading of simple.xml in the kxml samples dir
            try {
                is = getClass().getResourceAsStream (name);
            } catch(Exception ex) {
            }
         }*/


        if (is == null) {
            throw new IOException("could not open XML test for '"+name+"'");
        }
        return is;
    }
    /**
     *
     */
    public void testXml(String testName)
        throws IOException, XmlPullParserException
    {

        XmlPullParserFactory factory = factoryNewInstance();
        factory.setNamespaceAware(true);
        //XmlPullWrapper wrapper;


        XmlPullParser pp = factory.newPullParser();
        //wrapper = new XmlPullWrapper(pp);


        InputStream is = openStream(testName);
        verbose("> LOADING TESTS '"+testName+"'");
        pp.setInput(is, null);
        executeTests(pp);
        is.close();
        verbose("< FINISHED TESTS '"+testName+"'");
    }


    protected void executeTests(XmlPullParser pp) throws IOException, XmlPullParserException
    {

        //wrapper.nextStartTag(NS_URI, "tests");
        pp.nextTag();
        pp.require(XmlPullParser.START_TAG, NS_URI, "tests");
        while(pp.nextTag() == XmlPullParser.START_TAG) {
            executeTestParser(pp);
        }
        pp.require(XmlPullParser.END_TAG, NS_URI, "tests");
    }

    protected void executeTestParser(XmlPullParser pp) throws IOException, XmlPullParserException
    {
        pp.require(XmlPullParser.START_TAG, NS_URI, "test-parser");
        String testName = pp.getAttributeValue("", "name");
        verbose(">> START TEST '"+testName+"'");
        //testFactory = XmlPullParserFactory.newInstance();
        XmlPullParserFactory testFactory = factoryNewInstance();
        verbose(">> using testFactory='"+testFactory.getClass()+"'");
        XmlPullParser testParser = null;
        boolean testInputReady = false;
        while(pp.nextTag() == XmlPullParser.START_TAG) {
            String action = pp.getName();
            if("create-parser".equals(action)) {
                testParser = testFactory.newPullParser();
                verbose(">> using testParser='"+testParser.getClass()+"'");
                pp.nextText();
            } else if("input-inline".equals(action)) {
                if(testInputReady) {
                    throw new RuntimeException("input already set"+pp.getPositionDescription());
                }
                testInputReady = true;
                String input = pp.nextText();
                verbose(">>> "+action+" to '"+printable(input)+"'");
                verbose(">>> "+action+" to '"+input+"'");
                testParser.setInput(new StringReader( input ));
            } else if("expect".equals(action)) {
                String type = pp.getAttributeValue("", "type");
                String namespace = pp.getAttributeValue("", "namespace");
                String name = pp.getAttributeValue("", "name");
                String empty = pp.getAttributeValue("", "empty");
                verbose(">>> "+action
                            +" type="+type
                            +(namespace != null ? " namespace="+namespace : "")
                            +(name != null ? " name="+name : "")
                            +(empty != null ? " empty="+empty : ""));
                if(type != null) {
                    int event = convertToEventType(pp,type);
                    // comapring string give better error messages
                    assertEquals(XmlPullParser.TYPES[ event ], XmlPullParser.TYPES[ testParser.getEventType() ]);
                    // now compare actual int values
                    assertEquals(event, testParser.getEventType());
                }
                if(namespace != null) assertEquals(namespace, testParser.getNamespace());
                if(name != null) assertEquals(name, testParser.getName());
                if(empty != null) {
                    boolean isEmpty = Boolean.valueOf( empty ).booleanValue();
                    assertEquals(isEmpty, testParser.isEmptyElementTag());
                }
                pp.nextText();
            } else if("next".equals(action)) {
                verbose(">>> "+action);
                testParser.next();
                pp.nextText();
            } else if("next-tag".equals(action)) {
                verbose(">>> "+action);
                testParser.nextTag();
                pp.nextText();
            } else if("next-text".equals(action)) {
                verbose(">>> "+action); //+" on "+testParser.getPositionDescription()+"");
                String expected = pp.getAttributeValue("", "text");
                String testText = testParser.nextText();
                if(expected != null) {
                    verbose(">>> "+action+" testParser="+testParser.getPositionDescription()
                           +" excpectecd='"+printable(expected)+"' test='"+printable(testText)+"'");
                    assertEquals(testParser.getPositionDescription(), printable(expected), printable(testText));
                    assertEquals(expected, testText);
                }
                pp.nextText();
            } else if("set-feature".equals(action)) {
                String feature = pp.nextText();
                verbose(">>> "+action+" "+feature);
                testParser.setFeature(feature, true);
            } else {
                verbose(">>> UNKNONW ACTION '"+action+"' ("+pp.getPositionDescription()+")");
                String content = pp.nextText();
                if(content.length() > 0) {
                    if(verbose) System.err.println(">>> CONTENT='"+printable(content)+"'");
                }

            }
        }

        pp.require(XmlPullParser.END_TAG, NS_URI, "test-parser");
        verbose(">> END TEST '"+testName+"'");
    }


    private int convertToEventType(XmlPullParser pp, String eventName) {
        for (int i = 0; i < XmlPullParser.TYPES.length; i++)
        {
            if( eventName.equals(XmlPullParser.TYPES[ i ]) ) {
                return i;
            }
        }
        throw new RuntimeException("unknown event type "+eventName+pp.getPositionDescription());
    }

}

