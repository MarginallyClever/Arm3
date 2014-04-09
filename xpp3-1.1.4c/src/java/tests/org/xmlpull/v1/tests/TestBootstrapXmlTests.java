/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

//import junit.framework.Test;
//import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Make minimal checks that tests described in XML can be processed.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestBootstrapXmlTests extends UtilTestCase {
    private XmlPullParserFactory factory;


    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestBootstrapXmlTests.class));
    }


    public TestBootstrapXmlTests(String name) {
        super(name);
    }

    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        assertEquals(true, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
    }

    protected void tearDown() {
    }

    final String TEST_NS = "http://xmlpull.org/v1/tests/2002-08.xsd";

    final String MINI_TEST_XML =
	"<tests xmlns=\""+TEST_NS+"\">\n"+
	" <test-parser name=\"initial test\">\r"+
	"  <create-parser/> <input-inline>"+
	"&lt;foo/&gt; "+
	"</input-inline>\n"+
	"  <next/><expect type=\"START_TAG\" namespace=\"\" name=\"foo\" empty=\"false\"/>\n"+
	"  <next/><expect type=\"END_TAG\"/>"+
	"  <next/><expect type=\"END_DOCUMENT\"/>"+
	" </test-parser>"+
	"</tests>\n  \n"
	;

    public void testMini() throws Exception {
        XmlPullParser pp = factory.newPullParser();
        pp.setInput( new StringReader( MINI_TEST_XML ) );
        pp.next();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "tests");
        pp.next();
        pp.require( XmlPullParser.TEXT, null, null);
	String text = pp.getText();
	assertEquals("\n ", text);
        pp.next();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "test-parser");
	// check name

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "create-parser");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "create-parser");


        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "input-inline");
	text = "";
	if(pp.next() == XmlPullParser.TEXT) {
	    text = pp.getText();
	    pp.next();
	}
	assertEquals("<foo/> ", text);
        pp.require( XmlPullParser.END_TAG, TEST_NS, "input-inline");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "next");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "next");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "expect");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "expect");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "next");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "next");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "expect");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "expect");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "next");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "next");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "expect");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "expect");

        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "test-parser");

	pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "tests");

        pp.next();
        pp.require( XmlPullParser.END_DOCUMENT, null, null);

    }


    final String TYPICAL_TEST_XML =
	"<tests xmlns=\""+TEST_NS+"\">\n"+
	" <test-parser name=\"initial test\">\r"+
	"  <create-parser/> <input-inline>"+
	"&lt;foo att=&quot;t&quot; att2=&apos;a&apos; &gt;<![CDATA[ bar&baz ]]>&amp;&lt;/foo&gt;"+
	//"&lt;foo/&gt; "+
	"</input-inline>\r\n"+
	"  <set-feature>http://xmlpull.org/v1/doc/features.html#process-namespaces</set-feature>\r\n"+
	"  <expect type=\"START_DOCUMENT\"/>\n"+
	"  <next/><expect type=\"START_TAG\" namespace=\"\" name=\"foo\" empty=\"false\"/>\n"+
	"  <next-text text=\"bar\"/>"+
	"  <next/><expect type=\"END_DOCUMENT\"/>"+
	" </test-parser>"+
	"</tests>\n  \n"
	;
    public void testTypical() throws Exception {
        XmlPullParser pp = factory.newPullParser();
        pp.setInput( new StringReader( TYPICAL_TEST_XML ) );
        pp.next();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "tests");
        pp.next();
        pp.require( XmlPullParser.TEXT, null, null);
	String text = pp.getText();
	assertEquals("\n ", text);
        pp.next();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "test-parser");
	// check name

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "create-parser");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "create-parser");


        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "input-inline");
	text = "";
	if(pp.next() == XmlPullParser.TEXT) {
	    text = pp.getText();
	    pp.next();
	}
	assertEquals("<foo att=\"t\" att2='a' > bar&baz &</foo>", text);
        pp.require( XmlPullParser.END_TAG, TEST_NS, "input-inline");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "set-feature");
        text = pp.nextText();
	assertEquals("http://xmlpull.org/v1/doc/features.html#process-namespaces", text);
        pp.require( XmlPullParser.END_TAG, TEST_NS, "set-feature");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "expect");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "expect");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "next");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "next");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "expect");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "expect");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "next-text");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "next-text");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "next");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "next");

        pp.nextTag();
        pp.require( XmlPullParser.START_TAG, TEST_NS, "expect");
        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "expect");

        pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "test-parser");

	pp.nextTag();
        pp.require( XmlPullParser.END_TAG, TEST_NS, "tests");

        pp.next();
        pp.require( XmlPullParser.END_DOCUMENT, null, null);

    }


}

