/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

import java.io.IOException;

import junit.framework.TestCase;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Some common utilities to help with XMLPULL tests.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class UtilTestCase extends TestCase {
    protected static final String FEATURE_XML_ROUNDTRIP=
        "http://xmlpull.org/v1/doc/features.html#xml-roundtrip";
    protected final static String PROPERTY_XMLDECL_VERSION =
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-version";
    protected final static String PROPERTY_XMLDECL_STANDALONE =
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-standalone";
    protected final static String PROPERTY_XMLDECL_CONTENT =
        "http://xmlpull.org/v1/doc/properties.html#xmldecl-content";


    protected final static String TEST_XML =
        "<root>\n"+
        "<foo>bar</foo>\r\n"+
        "<hugo xmlns=\"http://www.xmlpull.org/temp\"> \n\r \n"+
        "  <hugochild>This is in a <!-- comment -->new namespace</hugochild>"+
        "</hugo>\t\n"+
        "<bar testattr='123abc' />"+
        "</root>\n"+
        "\n"+
        "<!-- an xml sample document without meaningful content -->\n";

    //private static XmlPullParserFactory factory;
    private static boolean printedFactoryName;

    public UtilTestCase(String name) {
        super(name);
    }

    public static XmlPullParserFactory factoryNewInstance() throws XmlPullParserException {
        String property = System.getProperty(XmlPullParserFactory.PROPERTY_NAME);
        //property = "org.xmlpull.mxp1.MXParserFactory";
        //"org.xmlpull.v1.xni2xmlpull1.X2ParserFactory",
        //property = "org.xmlpull.mxp1.MXParser,org.xmlpull.mxp1_serializer.MXSerializer";
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance(
            property,
            null //Thread.currentThread().getContextClassLoader().getClass(), //NOT ON JDK 1.1
        );
        //System.out.println("factory="+factory+" property="+property);
        if(PackageTests.runnigAllTests() == false && printedFactoryName == false) {
            System.out.println("factory="+factory+" property="+property);
            printedFactoryName = true;
        }
        return factory;
    }

    /**
     *
     * Mpve to next token and gather getText() for successive tokens of the same type.
     * Parser will be positioned on next token of different type.
     */
    public String nextTokenGathered(XmlPullParser xpp, int type, boolean expectedWhitespaces)
        throws XmlPullParserException, IOException
    {

        xpp.nextToken();
        return gatherTokenText(xpp, type, expectedWhitespaces);

    }

    /**
     * Gathers getText() for successive tokens of the same type.
     * Parser will be positioned on next token of different type.
     */
    public String gatherTokenText(XmlPullParser xpp, int type, boolean expectedWhitespaces)
        throws XmlPullParserException, IOException
    {
        StringBuffer buf = new StringBuffer();
        assertEquals(XmlPullParser.TYPES[ type ], XmlPullParser.TYPES[ xpp.getEventType() ]);
        do {
            buf.append(xpp.getText());
            if(expectedWhitespaces) {
                assertTrue(xpp.isWhitespace());
            }
        } while(xpp.nextToken() == type);
        return buf.toString();

    }

    public void checkParserState(
        XmlPullParser xpp,
        int depth,
        int type,
        String name,
        String text,
        boolean isEmpty,
        int attribCount
    ) throws XmlPullParserException, IOException
    {
        assertTrue("line number must be -1 or >= 1 not "+xpp.getLineNumber(),
                   xpp.getLineNumber() == -1 || xpp.getLineNumber() >= 1);
        assertTrue("column number must be -1 or >= 0 not "+xpp.getColumnNumber(),
                   xpp.getColumnNumber() == -1 || xpp.getColumnNumber() >= 0);

        assertEquals("PROCESS_NAMESPACES", false, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        assertEquals("TYPES[getType()]", XmlPullParser.TYPES[type], XmlPullParser.TYPES[xpp.getEventType()]);
        assertEquals("getType()", type, xpp.getEventType());
        assertEquals("getDepth()", depth, xpp.getDepth());
        assertEquals("getPrefix()", null, xpp.getPrefix());
        assertEquals("getNamespacesCount(getDepth())", 0, xpp.getNamespaceCount(depth));
        if(xpp.getEventType() == XmlPullParser.START_TAG || xpp.getEventType() == XmlPullParser.END_TAG) {
            assertEquals("getNamespace()", "", xpp.getNamespace());
        } else {
            assertEquals("getNamespace()", null, xpp.getNamespace());
        }
        assertEquals("getName()", name, xpp.getName());

        if(xpp.getEventType() != XmlPullParser.START_TAG && xpp.getEventType() != XmlPullParser.END_TAG) {
            assertEquals("getText()", printable(text), printable(xpp.getText()));

            int [] holderForStartAndLength = new int[2];
            char[] buf = xpp.getTextCharacters(holderForStartAndLength);
            if(buf != null) {
                String s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                assertEquals("getText(holder)", printable(text), printable(s));
            } else {
                assertEquals("getTextCharacters()", null, text);
            }
        }
        if(type == XmlPullParser.START_TAG) {
            assertEquals("isEmptyElementTag()", isEmpty, xpp.isEmptyElementTag());
        } else {
            try {
                xpp.isEmptyElementTag();
                fail("isEmptyElementTag() must throw exception if parser not on START_TAG");
            } catch(XmlPullParserException ex) {
            }
        }
        assertEquals("getAttributeCount()", attribCount, xpp.getAttributeCount());
    }

    public void checkParserStateNs(
        XmlPullParser xpp,
        int depth,
        int type,
        int nsCount,
        String namespace,
        String name,
        boolean isEmpty,
        int attribCount
    ) throws XmlPullParserException, IOException
    {
        assertTrue("line number must be -1 or >= 1 not "+xpp.getLineNumber(),
                   xpp.getLineNumber() == -1 || xpp.getLineNumber() >= 1);
        assertTrue("column number must be -1 or >= 0 not "+xpp.getColumnNumber(),
                   xpp.getColumnNumber() == -1 || xpp.getColumnNumber() >= 0);

        // this methid can be used with enabled and not enabled namespaces
        //assertEquals("PROCESS_NAMESPACES", true, xpp.getFeature(xpp.FEATURE_PROCESS_NAMESPACES));
        assertEquals("TYPES[getEventType()]", XmlPullParser.TYPES[type], XmlPullParser.TYPES[xpp.getEventType()]);
        assertEquals("getEventType()", type, xpp.getEventType());
        assertEquals("getName()", name, xpp.getName());

        assertEquals("getDepth()", depth, xpp.getDepth());
        assertEquals("getNamespacesCount(getDepth())", nsCount, xpp.getNamespaceCount(depth));
        assertEquals("getNamespace()", namespace, xpp.getNamespace());

        if(type == XmlPullParser.START_TAG) {
            assertEquals("isEmptyElementTag()", isEmpty, xpp.isEmptyElementTag());
        } else {
            try {
                xpp.isEmptyElementTag();
                fail("isEmptyElementTag() must throw exception if parser not on START_TAG");
            } catch(XmlPullParserException ex) {
            }
        }
        assertEquals("getAttributeCount()", attribCount, xpp.getAttributeCount());
    }

    public void checkParserStateNs(
        XmlPullParser xpp,
        int depth,
        int type,
        String prefix,
        int nsCount,
        String namespace,
        String name,
        boolean isEmpty,
        int attribCount
    ) throws XmlPullParserException, IOException
    {
        checkParserStateNs(xpp, depth, type, nsCount, namespace, name, isEmpty, attribCount);
        assertEquals("getPrefix()", prefix, xpp.getPrefix());
    }

    public void checkParserStateNs(
        XmlPullParser xpp,
        int depth,
        int type,
        String prefix,
        int nsCount,
        String namespace,
        String name,
        String text,
        boolean isEmpty,
        int attribCount
    ) throws XmlPullParserException, IOException
    {
        checkParserStateNs(xpp, depth, type, prefix, nsCount, namespace, name, isEmpty, attribCount);

        if(xpp.getEventType() != XmlPullParser.START_TAG && xpp.getEventType() != XmlPullParser.END_TAG) {
            assertEquals("getText()", printable(text), printable(xpp.getText()));

            int [] holderForStartAndLength = new int[2];
            char[] buf = xpp.getTextCharacters(holderForStartAndLength);
            if(buf != null) {
                String s = new String(buf, holderForStartAndLength[0], holderForStartAndLength[1]);
                // ENTITY_REF is a special case when getText != (getTextCharacters == getName)
                if(xpp.getEventType() != XmlPullParser.ENTITY_REF) {
                    assertEquals("getText(holder)", printable(text), printable(s));
                } else {
                    assertEquals("getText(holder) ENTITY_REF", printable(name), printable(s));
                }
            } else {
                assertEquals("getTextCharacters()", null, text);
            }

        }


    }

    public void checkAttrib(
        XmlPullParser xpp,
        int pos,
        String name,
        String value
    ) throws XmlPullParserException, IOException
    {
        assertEquals("must be on START_TAG", XmlPullParser.START_TAG, xpp.getEventType());
        assertEquals("getAttributePrefix()",null, xpp.getAttributePrefix(pos));
        assertEquals("getAttributeNamespace()","", xpp.getAttributeNamespace(pos));
        assertEquals("getAttributeName()",name, xpp.getAttributeName(pos));
        assertEquals("getAttributeValue()",value, xpp.getAttributeValue(pos));
        assertEquals("getAttributeValue(name)",value, xpp.getAttributeValue(null, name));
        assertEquals("getAttributeType()","CDATA", xpp.getAttributeType(pos));
        assertEquals("isAttributeDefault()",false, xpp.isAttributeDefault(pos));
    }


    public void checkAttribNs(
        XmlPullParser xpp,
        int pos,
        String namespace,
        String name,
        String value
    ) throws XmlPullParserException, IOException
    {
        assertEquals("must be on START_TAG", XmlPullParser.START_TAG, xpp.getEventType());
        assertEquals("getAttributeNamespace()",namespace, xpp.getAttributeNamespace(pos));
        assertEquals("getAttributeName()",name, xpp.getAttributeName(pos));
        assertEquals("getAttributeValue()",printable(value), printable(xpp.getAttributeValue(pos)));
        assertEquals("getAttributeValue(ns,name)",
                     printable(value), printable(xpp.getAttributeValue(namespace, name)));
        assertEquals("getAttributeType()","CDATA", xpp.getAttributeType(pos));
        assertEquals("isAttributeDefault()",false, xpp.isAttributeDefault(pos));
    }

    public void checkAttribNs(
        XmlPullParser xpp,
        int pos,
        String prefix,
        String namespace,
        String name,
        String value
    ) throws XmlPullParserException, IOException
    {
        checkAttribNs(xpp, pos, namespace, name, value);
        assertEquals("getAttributePrefix()",prefix, xpp.getAttributePrefix(pos));
    }

    public void checkNamespace(
        XmlPullParser xpp,
        int pos,
        String prefix,
        String uri,
        boolean checkMapping
    ) throws XmlPullParserException, IOException
    {
        assertEquals("getNamespacePrefix(pos)",prefix, xpp.getNamespacePrefix(pos));
        assertEquals("getNamespaceUri(pos)",uri, xpp.getNamespaceUri(pos));
        if(checkMapping) {
            assertEquals("getNamespace(prefix)", uri, xpp.getNamespace (prefix));
        }
    }

    protected String printable(char ch) {
        if(ch == '\n') {
            return "\\n";
        } else if(ch == '\r') {
            return "\\r";
        } else if(ch == '\t') {
            return "\\t";
        } if(ch > 127 || ch < 32) {
            StringBuffer buf = new StringBuffer("\\u");
            String hex = Integer.toHexString((int)ch);
            for (int i = 0; i < 4-hex.length(); i++)
            {
                buf.append('0');
            }
            buf.append(hex);
            return buf.toString();
        }
        return ""+ch;
    }

    protected String printable(String s) {
        if(s == null) return null;
        StringBuffer buf = new StringBuffer();
        for(int i = 0; i < s.length(); ++i) {
            buf.append(printable(s.charAt(i)));
        }
        s = buf.toString();
        return s;
    }

}

