/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license see accompanying LICENSE_TESTS.txt file (available also at http://www.xmlpull.org)

package org.xmlpull.v1.tests;

import junit.framework.TestSuite;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Tests to determine that setInput works as expected for different encodings.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class TestSetInput extends UtilTestCase {
    private XmlPullParserFactory factory;

    public TestSetInput(String name) {
        super(name);
    }

    protected void setUp() throws XmlPullParserException {
        factory = factoryNewInstance();
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        assertEquals(true, factory.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));
    }

    protected void tearDown() {
    }

    public void testSetReader() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));

        xpp.setInput(null);
        assertEquals(XmlPullParser.START_DOCUMENT, xpp.getEventType());
        try {
            xpp.next();
            fail("exception was expected of next() if no input was set on parser");
        } catch(XmlPullParserException ex) {}


        // make input suspectible to read ...
        ReaderWrapper reader = new ReaderWrapper(
            new StringReader("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><foo/>"));
        assertEquals("no read() called in just contructed reader", false, reader.calledRead());

        xpp.setInput(reader);
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        assertEquals("read() not called before next()", false, reader.calledRead());

        xpp.next();
        assertEquals("read() must be called after next()", true, reader.calledRead());
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, true/*empty*/, 0);
    }

    public void testSetInput() throws Exception {
        XmlPullParser xpp = factory.newPullParser();
        assertEquals(true, xpp.getFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES));

        // another test

        byte[] binput = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><foo/>").getBytes("UTF-8");
        InputStreamWrapper isw = new InputStreamWrapper(
            new ByteArrayInputStream( binput ));
        assertEquals("no read() called in just contructed reader", false, isw.calledRead());

        xpp.setInput(isw, "UTF-8");
        assertEquals("UTF-8", xpp.getInputEncoding());
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        assertEquals("read() not called before next()", false, isw.calledRead());

        xpp.nextToken();
        assertEquals("read() must be called after next()", true, isw.calledRead());

        //needs to resolve:
        // java.lang.InternalError: Converter malfunction (UTF-16) -- please submit a bug report via http://java.sun.com/cgi-bin/bugreport.cgi

        //
        //      // add BOM
        //        //byte[] binput1 = new byte[]{((byte)'\u00FE'), ((byte)'\u00FF')};
        //        //byte[] binput1 = new byte[]{((byte)'\u00FF'), ((byte)'\u00FE')};
        //      byte[] binput1 = new byte[0];
        //        byte[] binput2 =
        //            ("<?xml version=\"1.0\" encoding=\"UTF16\"?><foo/>").getBytes("UTF16");
        //        binput = new byte[ binput1.length + binput2.length ] ;
        //        System.arraycopy(binput1, 0, binput, 0, binput1.length);
        //        System.arraycopy(binput2, 0, binput, binput1.length, binput2.length);
        //        isw = new InputStreamWrapper(
        //            new ByteArrayInputStream( binput ));
        //        assertEquals("no read() called in just contructed reader", false, isw.calledRead());
        //
        //        //xpp.setInput(isw, "UTF-16" ); //TODO why Xerces2 causes java Unicode decoder to fail ????
        //      xpp.setInput(isw, "UTF16" );
        //        //assertEquals("UTF-16", xpp.getInputEncoding());
        //        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        //        assertEquals("read() not called before next()", false, isw.calledRead());
        //
        //        xpp.nextToken();
        //        assertEquals("read() must be called after next()", true, isw.calledRead());
        //
        // check input detecting  -- for mutlibyte sequences ...
        final String FEATURE_DETECT_ENCODING =
            "http://xmlpull.org/v1/doc/features.html#detect-encoding";
        if(xpp.getFeature(FEATURE_DETECT_ENCODING)) {

            PackageTests.addNote("* optional feature  "+FEATURE_DETECT_ENCODING+" is supported\n");

            isw = new InputStreamWrapper(
                new ByteArrayInputStream( binput ));
            assertEquals("no read() called in just contructed reader", false, isw.calledRead());

            xpp.setInput(isw, null );
            assertEquals(null, xpp.getInputEncoding());

            checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
            //assertEquals("read() not called before next()", false, isw.calledRead());

            xpp.nextToken();
            assertEquals("read() must be called after next()", true, isw.calledRead());
            assertEquals("UTF16", xpp.getInputEncoding());

        }

        // check input detecting  -- default
        binput =
            ("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><foo/>").getBytes("ISO-8859-1");
        isw = new InputStreamWrapper(
            new ByteArrayInputStream( binput ));
        assertEquals("no read() called in just contructed reader", false, isw.calledRead());

        xpp.setInput(isw, null );
        assertEquals(null, xpp.getInputEncoding());
        checkParserStateNs(xpp, 0, XmlPullParser.START_DOCUMENT, null, 0, null, null, null, false, -1);
        //assertEquals("read() not called before next()", false, isw.calledRead());

        xpp.next();
        assertEquals("read() must be called after next()", true, isw.calledRead());
        checkParserStateNs(xpp, 1, XmlPullParser.START_TAG, null, 0, "", "foo", null, true/*empty*/, 0);

        if(xpp.getFeature(FEATURE_DETECT_ENCODING)) {
            assertEquals("ISO-8859-1", xpp.getInputEncoding());
        }
    }

    // ------ allow to detect if input was read

    private static class InputStreamWrapper extends InputStream {
        InputStream is;
        boolean calledRead;
        public boolean calledRead() { return calledRead; }
        public InputStreamWrapper(InputStream inputStream) {
            is = inputStream;
        }
        public int read() throws IOException {
            calledRead = true;
            return is.read();
        }
    }

    private static class ReaderWrapper extends Reader {
        Reader r;
        boolean calledRead;
        public boolean calledRead() { return calledRead; }
        public ReaderWrapper(Reader reader) {
            r = reader;
        }
        public int read(char[]ch, int off, int len) throws IOException {
            calledRead = true;
            return r.read(ch, off, len);
        }
        public void close() throws IOException {
            r.close();
        }
    }

    public static void main (String[] args) {
        junit.textui.TestRunner.run (new TestSuite(TestSetInput.class));
    }

}

