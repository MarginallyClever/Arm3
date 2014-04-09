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
 * More complete test to verify paring.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestEvent extends UtilTestCase {
    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestEvent.class));
    }

    public TestEvent(String name) {
        super(name);
    }

    protected XmlPullParserFactory newFactory() throws XmlPullParserException {
        XmlPullParserFactory factory = factoryNewInstance();
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        assertEquals(true, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
        assertEquals(false, factory.getFeature(XmlPullParser.FEATURE_VALIDATION));
        return factory;
    }

    protected void tearDown() {
    }

    public void testEvent() throws Exception {
        XmlPullParserFactory factory = newFactory();
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader(TEST_XML));

        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);

        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "root", null, false, 0);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, "\n", false, -1);

        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, null, 0, "", "foo", null, false, 0);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.TEXT, null, 0, null, null, "bar", false, -1);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, null, 0, "", "foo", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, "\n", false, -1);

        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG,
                           null, 1, "http://www.xmlpull.org/temp", "hugo", null, false, 0);
        checkNamespace(xpp, 0, null, "http://www.xmlpull.org/temp", true);

        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.TEXT, null, 1, null, null, " \n\n \n  ", false, -1);

        xpp.next();
        checkParserStateNs(xpp, 3, XmlPullParser.START_TAG,
                           null, 1, "http://www.xmlpull.org/temp", "hugochild", null, false, 0);
        xpp.next();
        checkParserStateNs(xpp, 3, XmlPullParser.TEXT, null, 1, null, null,
                           "This is in a new namespace", false, -1);
        xpp.next();
        checkParserStateNs(xpp, 3, XmlPullParser.END_TAG,
                           null, 1, "http://www.xmlpull.org/temp", "hugochild", null, false, -1);

        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG,
                           null, 1, "http://www.xmlpull.org/temp", "hugo", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.TEXT, null, 0, null, null, "\t\n", false, -1);

        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, null, 0, "", "bar", null, true, 1);
        checkAttribNs(xpp, 0, null, "", "testattr", "123abc");
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, null, 0, "", "bar", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "root", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);
    }

    public void testMultiNs() throws Exception {
        XmlPullParserFactory factory = newFactory();
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput(new StringReader("<foo><bar xmlns=''/><char xmlns=''></char></foo>"));

        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);

        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, false, 0);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, null, 1, "", "bar", null, true, 0);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, null, 1, "", "bar", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, null, 1, "", "char", null, false, 0);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, null, 1, "", "char", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "foo", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);

        xpp.setInput(new StringReader("<foo><bar xmlns=''></bar><char xmlns=''></char></foo>"));

        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, false, 0);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, null, 1, "", "bar", null, false, 0);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, null, 1, "", "bar", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.START_TAG, null, 1, "", "char", null, false, 0);
        xpp.next();
        checkParserStateNs(xpp, 2, XmlPullParser.END_TAG, null, 1, "", "char", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 1, XmlPullParser.END_TAG, null, 0, "", "foo", null, false, -1);
        xpp.next();
        checkParserStateNs(xpp, 0, XmlPullParser.END_DOCUMENT, null, 0, null, null, null, false, -1);

    }

}

