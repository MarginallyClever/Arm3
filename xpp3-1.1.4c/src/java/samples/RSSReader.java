// modified by Aleksander Slominski
// based on http://www.xml.com/pub/a/2002/05/22/parsing.html?page=2

import java.io.*;
import java.net.*;

import org.xmlpull.v1.*;

public class RSSReader {

    public static void main(String [] args)
    {

        // create an instance of RSSReader
        RSSReader rssreader = new RSSReader();

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser parser = factory.newPullParser();
            String url = args[0];
            InputStreamReader stream = new InputStreamReader(
                new URL(url).openStream());
            parser.setInput(stream);
            XmlSerializer writer = factory.newSerializer();
            writer.setOutput(new OutputStreamWriter(System.out));
            rssreader.convertRSSToHtml(parser, writer);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public void convertRSSToHtml(XmlPullParser parser, XmlSerializer writer)
        throws IOException, XmlPullParserException
    {
        // <!ELEMENT rss (channel)>
        if (parser.nextTag() == XmlPullParser.START_TAG
            && parser.getName().equals("rss"))
        {
            writer.startTag(null,"html");
            if (parser.nextTag() == XmlPullParser.START_TAG
                && parser.getName().equals("channel"))
            {
                convertChannelToHtml(parser, writer);
                parser.require(XmlPullParser.END_TAG, null, "channel");
            } else {
                new RuntimeException("expectd channel start tag not "+parser.getPositionDescription());
            }
            parser.nextTag();
            parser.require(XmlPullParser.END_TAG, null, "rss");
            writer.endTag(null, "html");
            writer.flush();
        } else {
            throw new RuntimeException("expectd an RSS document at" + parser.getPositionDescription());
        }
    }

    public void convertChannelToHtml(XmlPullParser parser, XmlSerializer writer)
        throws IOException, XmlPullParserException
    {
        // <!ELEMENT channel (title | description | link | language | item+ | rating? | image? | textinput? | copyright? | pubDate? | lastBuildDate? | docs? | managingEditor? | webMaster? | skipHours? | skipDays?)*>
        boolean seenBody = false; //assumption that title is before items ...
        while (parser.nextTag() != XmlPullParser.END_TAG) { // this guranteed by well formednes of XML && parser.getName().equals("channel"))) {
            // if (parser.getEventType() == XmlPullParser.START_TAG) { //guranteed by nextTag
            // <!ELEMENT title (#PCDATA)>
            if(parser.getName().equals("title") && !seenBody) {
                writer.startTag(null,"head");
                writer.startTag(null,"title").text(parser.nextText()).endTag(null,"title");
                writer.endTag(null,"head");
            } else if(parser.getName().equals("item")) {
                if(!seenBody) {
                    writer.startTag(null, "body");
                    seenBody = true;
                }
                convertItemToHtml(parser, writer);
            } else {
                // skip any element content including sub elements...
                int level = 1;
                while (level > 0) {
                    switch(parser.next()) {
                        case XmlPullParser.START_TAG: ++level; break;
                        case XmlPullParser.END_TAG: --level; break;
                    }
                }
            }
        }
        if(seenBody) writer.endTag(null, "body");
    }

    public void convertItemToHtml(XmlPullParser parser, XmlSerializer writer)
        throws IOException, XmlPullParserException
    {
        writer.startTag(null, "p");
        //<!ELEMENT item (title | link | description)*>
        String title = null, link = null, description = null;
        while (parser.nextTag() != XmlPullParser.END_TAG) {
            if (parser.getName().equals("title")) {
                title = parser.nextText();
            } else if (parser.getName().equals("link")) {
                link = parser.nextText();
            } else if (parser.getName().equals("description")) {
                description = parser.nextText();
            }
        }

        //HashMap attributes = new HashMap(1);
        //if(link != null) attributes.put("href", link);
        //writer.beginElement("a",attributes);
        writer.startTag(null, "a");
        writer.attribute(null, "href", link);
        if(title != null) {
            writer.text(title);
        } else {
            writer.text(link);
        }
        writer.endTag(null,"a");

        //writer.writeEmptyElement("br");
        writer.startTag(null, "br").endTag(null, "br");

        if(description != null) writer.text(description);

        writer.endTag(null, "p"); // end the "p" element
    }
}

