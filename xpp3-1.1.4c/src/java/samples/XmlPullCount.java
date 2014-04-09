/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import java.io.FileInputStream;

/**
 * Simple example that counts XML elements, characters and attributes.
 *
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class XmlPullCount
{
    public final static String SAMPLE_XML =
        "<?xml version=\"1.0\"?>\n"+
        "\n"+
        "<poem xmlns=\"http://www.megginson.com/ns/exp/poetry\">\n"+
        "<title>Roses are Red</title>\n"+
        "<l>Roses are red,</l>\n"+
        "<l>Violets are blue;</l>\n"+
        "<l>Sugar is sweet,</l>\n"+
        "<l>And I love you.</l>\n"+
        "</poem>";
    int countChars;
    int countAttribs;
    int countSTags;
    boolean verbose;

    public static void main (String args[])
        throws XmlPullParserException, IOException
    {


        XmlPullParserFactory factory = XmlPullParserFactory.newInstance(
            System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
        factory.setNamespaceAware(false);
        System.err.println("using factory "+factory.getClass());

        XmlPullParser xpp = factory.newPullParser();
        System.err.println("using parser "+xpp.getClass());

        XmlPullCount app = new XmlPullCount();

        app.verbose = true;
        for(int c = 0; c < 2; ++c) {
            System.err.println("run#"+c);
            app.resetCounters();
            if(args.length == 0) {
                System.err.println("Parsing simple sample XML length="+SAMPLE_XML.length());
                xpp.setInput( new StringReader( SAMPLE_XML ) );
                app.countXml(xpp);
            } else {
                //r (int i = 0; i < args.length; i++) {

                File f = new File(args[0]);
                System.err.println("Parsing file: "+args[0]+" length="+f.length());
                //xpp.setInput ( new FileReader ( args [0] ) );
                xpp.setInput ( new FileInputStream ( args [0] ), "UTF8" );
                app.countXml(xpp);
                //
            }
            app.printReport();
        }
        System.err.println("finished");
    }

    public void resetCounters() {
        countChars = countSTags = countAttribs = 0;
    }

    public void printReport() {
        System.err.println("characters="+countChars
                               +" elements="+countSTags
                               +" attributes="+countAttribs);

    }

    public void countXml(XmlPullParser xpp) throws XmlPullParserException, IOException {

        int holderForStartAndLength[] = new int[2];

        xpp.require(XmlPullParser.START_DOCUMENT, null, null);
        int eventType = xpp.next();
        xpp.require(XmlPullParser.START_TAG, null, null);
        while (eventType != XmlPullParser.END_DOCUMENT) {
            //System.err.println("pos="+xpp.getPositionDescription());
            if(eventType == XmlPullParser.START_TAG) {
                ++countSTags;
                countAttribs += xpp.getAttributeCount();
                if(verbose) {
                    System.err.println("START_TAG "+xpp.getName());
                }
            } else if(eventType == XmlPullParser.TEXT) {
                //char ch[] = xpp.getTextCharacters(holderForStartAndLength);
                xpp.getTextCharacters(holderForStartAndLength);
                //int start = holderForStartAndLength[0];
                int length = holderForStartAndLength[1];
                countChars += length;
                if(verbose) {
                    System.err.println("TEXT '"+printable(xpp.getText())+"'");
                }
            } else if(eventType == XmlPullParser.END_TAG) {
                if(verbose) {
                    System.err.println("END_TAG "+xpp.getName());
                }
            }
            eventType = xpp.next();
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

