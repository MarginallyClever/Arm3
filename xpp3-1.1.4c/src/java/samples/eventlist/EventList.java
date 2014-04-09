/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)

package eventlist;

import java.io.*;

import org.xmlpull.v1.*;

public class EventList {

    public static void main (String [] args) throws IOException, XmlPullParserException{
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);
        Reader reader = (args.length > 0) ?
            new FileReader (args [0]) :
            (Reader) new StringReader("<sample>Hello World!</sample>");
        XmlPullParser xpp = factory.newPullParser();
        xpp.setInput (reader);
        int eventType;
        while ((eventType = xpp.next()) != XmlPullParser.END_DOCUMENT) {
            if(eventType == XmlPullParser.START_TAG) {
                System.out.println("START_TAG "+xpp.getName());
            } else if(eventType == XmlPullParser.END_TAG) {
                System.out.println("END_TAG   "+xpp.getName());
            } else if(eventType == XmlPullParser.TEXT) {
                System.out.println("TEXT      "+xpp.getText());
            }
        }

    }

}

