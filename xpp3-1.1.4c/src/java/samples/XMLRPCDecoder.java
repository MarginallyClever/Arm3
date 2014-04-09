//
//      ===========================================================================
//
//      Title:          XMLRPCDecoder.java
//      Description:    [Description]
//      Author:         Raphael Szwarc <zoe_info@mac.com>
//                      http://guests.evectors.it/zoe/
//      Creation Date:  Sun Nov 24 2002
//      Legal:          Copyright (c) 2002-2004 Raphael Szwarc. All Rights Reserved.
//
//      For license information please see XPP3's LICENSE.txt file
//      Also available at http://www.xmlpull.org/
//
//      ---------------------------------------------------------------------------
//

import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParser;

import java.io.Reader;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.OutputStream;
import java.io.BufferedOutputStream;

import java.net.URL;
import java.net.URLConnection;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public abstract class XMLRPCDecoder extends Object
{

    //  ===========================================================================
    //  Constant(s)
    //  ---------------------------------------------------------------------------

    private static final Class[]        Classes =
    {
        FaultDecoder.class,
            MethodDecoder.class,
            ParametersDecoder.class,
            ArrayDecoder.class,
            StructureDecoder.class,
            ValueDecoder.class,
            IntegerDecoder.class,
            BooleanDecoder.class,
            StringDecoder.class,
            DoubleDecoder.class,
            DateDecoder.class,
            Base64Decoder.class
    };


    //  ===========================================================================
    //  Class variable(s)
    //  ---------------------------------------------------------------------------

    private final static Map    _decoders = new HashMap();

    //  ===========================================================================
    //  Instance variable(s)
    //  ---------------------------------------------------------------------------

    //  ===========================================================================
    //  Constructor method(s)
    //  ---------------------------------------------------------------------------

    protected XMLRPCDecoder()
    {
        super();
    }

    //  ===========================================================================
    //  Class initialization method(s)
    //  ---------------------------------------------------------------------------

    static
    {
        Class[] someClasses = XMLRPCDecoder.Classes;
        int     count = someClasses.length;

        for ( int index = 0; index < count; index++ )
        {
            Class       aClass = someClasses[ index ];

            try
            {
                XMLRPCDecoder   aDecoder = (XMLRPCDecoder) aClass.newInstance();

                aDecoder.register( _decoders );
            }
            catch (Exception anException)
            {
                throw new RuntimeException( "XMLRPCDecoder.decoders: " + anException );
            }
        }
    }

    //  ===========================================================================
    //  Class method(s)
    //  ---------------------------------------------------------------------------

    private static Map decoders()
    {
        return _decoders;
    }

    public static Object decode(Reader aReader) throws Exception
    {
        if ( aReader != null )
        {
            Map                 someDecoders = XMLRPCDecoder.decoders();
            XmlPullParserFactory        aFactory = XmlPullParserFactory.newInstance();
            XmlPullParser               aParser = aFactory.newPullParser();

            aParser.setInput( aReader );

            return XMLRPCDecoder.decode( aParser );
        }

        throw new IllegalArgumentException( "XMLRPCDecoder.decode: null reader." );
    }

    private static XMLRPCDecoder decoderWithName(String aName)
    {
        if ( aName != null )
        {
            return (XMLRPCDecoder) XMLRPCDecoder.decoders().get( aName.toUpperCase() );
        }

        throw new IllegalArgumentException( "XMLRPCDecoder.decoderWithName: null name." );
    }

    private static Object decode(XmlPullParser aParser) throws Exception
    {
        if ( aParser != null )
        {
            int anEventType = aParser.getEventType();

            while ( anEventType != XmlPullParser.END_DOCUMENT )
            {
                if ( anEventType == XmlPullParser.START_TAG )
                {
                    String              aName = aParser.getName();
                    XMLRPCDecoder       aDecoder = XMLRPCDecoder.decoderWithName( aName );

                    if ( aDecoder != null )
                    {
                        return aDecoder.decodeWithParser( aParser );
                    }
                    else
                    {
                        anEventType = aParser.next();
                    }
                }
                else
                {
                    anEventType = aParser.next();
                }
            }

            return null;
        }

        throw new IllegalArgumentException( "XMLRPCDecoder.decode: null reader." );
    }

    //  ===========================================================================
    //  Instance method(s)
    //  ---------------------------------------------------------------------------

    protected abstract String[] names();

    protected void register(Map aMap)
    {
        if ( aMap != null )
        {
            String[]    someNames = this.names();

            if ( someNames != null )
            {
                int     count = someNames.length;

                for ( int index = 0; index < count; index++ )
                {
                    String      aName = someNames[ index ];

                    aMap.put( aName.toUpperCase(), this );
                }

                return;
            }

            throw new IllegalStateException( "XMLRPCDecoder.register: null names." );
        }

        throw new IllegalArgumentException( "XMLRPCDecoder.register: null map." );
    }

    protected abstract Object decodeWithParser(XmlPullParser aParser) throws Exception;

    //  ===========================================================================
    //  FaultDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class FaultDecoder extends XMLRPCDecoder
    {
        private static final String     Name = "fault";
        private static final String[]   Names = { FaultDecoder.Name };
        private static final String     FaultStringKey = "faultString";
        private static final String     FaultCodeKey = "faultCode";

        protected FaultDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return FaultDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                aParser.require( XmlPullParser.START_TAG, null, FaultDecoder.Name );
                aParser.nextTag();
                Map     aMap =  (Map) XMLRPCDecoder.decode( aParser );
                Object  aValue = null;

                if ( aMap != null )
                {
                    String      aDescription = (String) aMap.get( FaultDecoder.FaultStringKey );
                    Number      aCode = (Number) aMap.get( FaultDecoder.FaultCodeKey );

                    aValue = new RuntimeException( aDescription + "(" + aCode + ")" );
                }

                aParser.require( XmlPullParser.END_TAG, null, FaultDecoder.Name );
                aParser.nextTag();

                return aValue;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.FaultDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  MethodDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class MethodDecoder extends XMLRPCDecoder
    {
        private static final String     Name = "methodCall";
        private static final String[]   Names = { MethodDecoder.Name };

        protected MethodDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return MethodDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                aParser.require( XmlPullParser.START_TAG, null, MethodDecoder.Name );
                aParser.nextTag();
                String          aMethodName = aParser.nextText();
                Collection      someArguments = (Collection) XMLRPCDecoder.decode( aParser );
                Map             aValue = new HashMap();

                aValue.put( "methodName",  aMethodName );
                aValue.put( "parameters",  someArguments );

                if ( aParser.getEventType() != XmlPullParser.END_DOCUMENT )
                {
                    aParser.require( XmlPullParser.END_TAG, null, MethodDecoder.Name );
                    aParser.next();
                }

                return aValue;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.MethodDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  ParametersDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class ParametersDecoder extends XMLRPCDecoder
    {
        private static final String     Name = "params";
        private static final String     ParamName = "param";
        private static final String[]   Names = { ParametersDecoder.Name };

        protected ParametersDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return ParametersDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                Collection      aCollection = new ArrayList();

                aParser.require( XmlPullParser.START_TAG, null, ParametersDecoder.Name );
                aParser.nextTag();

                while ( ( aParser.getEventType() != XmlPullParser.END_DOCUMENT ) && ( aParser.getEventType() == XmlPullParser.START_TAG ) )
                {
                    aParser.require( XmlPullParser.START_TAG, null, ParametersDecoder.ParamName );
                    aParser.nextTag();

                    aCollection.add( XMLRPCDecoder.decode( aParser ) );

                    aParser.require( XmlPullParser.END_TAG, null, ParametersDecoder.ParamName );
                    aParser.nextTag();
                }

                aParser.require( XmlPullParser.END_TAG, null, ParametersDecoder.Name );
                aParser.nextTag();

                if ( aCollection.isEmpty() == true )
                {
                    aCollection = null;
                }

                return aCollection;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.ParametersDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  ArrayDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class ArrayDecoder extends XMLRPCDecoder
    {
        private static final String     Name = "array";
        private static final String     DataName = "data";
        private static final String[]   Names = { ArrayDecoder.Name };

        protected ArrayDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return ArrayDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                Collection      aCollection = new ArrayList();

                aParser.require( XmlPullParser.START_TAG, null, ArrayDecoder.Name );
                aParser.nextTag();
                aParser.require( XmlPullParser.START_TAG, null, ArrayDecoder.DataName );
                aParser.nextTag();

                while ( ( aParser.getEventType() != XmlPullParser.END_DOCUMENT ) && ( aParser.getEventType() == XmlPullParser.START_TAG ) )
                {
                    Object aValue = XMLRPCDecoder.decode( aParser );

                    aCollection.add( aValue );
                }

                if ( aCollection.isEmpty() == true )
                {
                    aCollection = null;
                }

                aParser.require( XmlPullParser.END_TAG, null, ArrayDecoder.DataName );
                aParser.nextTag();
                aParser.require( XmlPullParser.END_TAG, null, ArrayDecoder.Name );
                aParser.nextTag();

                return aCollection;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.ArrayDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  StructureDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class StructureDecoder extends XMLRPCDecoder
    {
        private static final String     Name = "struct";
        private static final String     MemberName = "member";
        private static final String     NameName = "name";
        private static final String[]   Names = { StructureDecoder.Name };

        protected StructureDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return StructureDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                Map     aMap = new HashMap();

                aParser.require( XmlPullParser.START_TAG, null, StructureDecoder.Name );
                aParser.nextTag();

                while ( ( aParser.getEventType() != XmlPullParser.END_DOCUMENT ) && ( aParser.getEventType() == XmlPullParser.START_TAG ) )
                {
                    Object      aKey = null;
                    Object      aValue = null;

                    aParser.require( XmlPullParser.START_TAG, null, StructureDecoder.MemberName );
                    aParser.nextTag();
                    aParser.require( XmlPullParser.START_TAG, null, StructureDecoder.NameName );

                    aKey = aParser.nextText();

                    aParser.require( XmlPullParser.END_TAG, null, StructureDecoder.NameName );

                    aValue = XMLRPCDecoder.decode( aParser );

                    aParser.require( XmlPullParser.END_TAG, null, StructureDecoder.MemberName );
                    aParser.nextTag();

                    if ( ( aKey != null ) && ( aValue != null ) )
                    {
                        aMap.put( aKey, aValue );
                    }
                }

                aParser.require( XmlPullParser.END_TAG, null, StructureDecoder.Name );
                aParser.nextTag();

                if ( aMap.isEmpty() == true )
                {
                    aMap = null;
                }

                return aMap;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.StructureDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  ArrayDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class ValueDecoder extends XMLRPCDecoder
    {
        private static final String     Name = "value";
        private static final String[]   Names = { ValueDecoder.Name };

        protected ValueDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return ValueDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                Object  aValue = null;

                aParser.require( XmlPullParser.START_TAG, null, ValueDecoder.Name );
                aParser.next();

                if ( aParser.getEventType() == XmlPullParser.END_TAG )
                {
                    aValue = "";
                }
                else
                    //if ( aParser.getEventType() == aParser.TEXT)
                    //{
                    //  aValue = aParser.getText();
                    //  aParser.next();
                    //}
                    //else
                {
                    aValue = XMLRPCDecoder.decode( aParser );
                }

                aParser.require( XmlPullParser.END_TAG, null, ValueDecoder.Name );
                aParser.nextTag();

                return aValue;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.ValueDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  IntegerDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class IntegerDecoder extends XMLRPCDecoder
    {
        private static final String     IntName = "int";
        private static final String     I4Name = "i4";
        private static final String[]   Names = { IntegerDecoder.IntName, IntegerDecoder.I4Name };

        protected IntegerDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return IntegerDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                String  aName = aParser.getName();
                aParser.require( XmlPullParser.START_TAG, null, aName );
                String  aString = aParser.nextText();
                Object  aValue = null;

                if ( aString != null )
                {
                    try
                    {
                        aValue = new Integer( aString );
                    }
                    catch(Exception anException)
                    {
                        System.err.println( anException );
                    }
                }

                aParser.require( XmlPullParser.END_TAG, null, aName );
                aParser.nextTag();

                return aValue;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.IntegerDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  BooleanDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class BooleanDecoder extends XMLRPCDecoder
    {
        private static final String     Name = "boolean";
        private static final String[]   Names = { BooleanDecoder.Name };
        private static final String     False = "0";

        protected BooleanDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return BooleanDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                aParser.require( XmlPullParser.START_TAG, null, BooleanDecoder.Name );

                String  aString = aParser.nextText();
                Object  aValue =  Boolean.TRUE;

                if ( BooleanDecoder.False.equals( aString ) == true )
                {
                    aValue = Boolean.FALSE;
                }

                aParser.require( XmlPullParser.END_TAG, null, BooleanDecoder.Name );
                aParser.next();

                return aValue;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.BooleanDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  DoubleDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class DoubleDecoder extends XMLRPCDecoder
    {
        private static final String     Name = "double";
        private static final String[]   Names = { DoubleDecoder.Name };

        protected DoubleDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return DoubleDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                aParser.require( XmlPullParser.START_TAG, null, DoubleDecoder.Name );

                String  aString = aParser.nextText();
                Object  aValue = null;

                if ( aString != null )
                {
                    try
                    {
                        aValue = new Double( aString );
                    }
                    catch(Exception anException)
                    {
                        System.err.println( anException );
                    }
                }

                aParser.require( XmlPullParser.END_TAG, null, DoubleDecoder.Name );
                aParser.nextTag();

                return aValue;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.DoubleDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  StringDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class StringDecoder extends XMLRPCDecoder
    {
        private static final String     Name = "string";
        private static final String[]   Names = { StringDecoder.Name };

        protected StringDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return StringDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                aParser.require( XmlPullParser.START_TAG, null, StringDecoder.Name );

                String  aValue = aParser.nextText();

                aParser.require( XmlPullParser.END_TAG, null, StringDecoder.Name );

                aParser.next();

                if ( ( aValue != null ) && ( aValue.length() == 0 ) )
                {
                    aValue = null;
                }

                return aValue;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.StringDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  DateDecoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class DateDecoder extends XMLRPCDecoder
    {
        private static final String     Name = "dateTime.iso8601";
        private static final String[]   Names = { DateDecoder.Name };
        private static final String     Format = "yyyyMMdd'T'HH:mm:ss";

        protected DateDecoder()
        {
            super();
        }

        protected String[] names()
        {
            return DateDecoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                aParser.require( XmlPullParser.START_TAG, null, DateDecoder.Name );

                String  aString = aParser.nextText();
                Date    aValue = null;

                if ( aString != null )
                {
                    try
                    {
                        DateFormat      aFormat = new SimpleDateFormat( DateDecoder.Format );

                        aValue = aFormat.parse( aString );
                    }
                    catch(Exception anException)
                    {
                        System.err.println( anException );
                    }
                }

                aParser.require( XmlPullParser.END_TAG, null, DateDecoder.Name );
                aParser.nextTag();

                return aValue;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.DateDecoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  Base64Decoder method(s)
    //  ---------------------------------------------------------------------------

    private static final class Base64Decoder extends XMLRPCDecoder
    {
        private static final String     Name = "base64";
        private static final String[]   Names = { Base64Decoder.Name };

        protected Base64Decoder()
        {
            super();
        }

        protected String[] names()
        {
            return Base64Decoder.Names;
        }

        protected Object decodeWithParser(XmlPullParser aParser) throws Exception
        {
            if ( aParser != null )
            {
                aParser.require( XmlPullParser.START_TAG, null, Base64Decoder.Name );

                String  aString = aParser.nextText();
                Object  aValue = null;

                if ( aString != null )
                {
                    try
                    {
                        aValue = Base64.decode( aString.getBytes() );
                    }
                    catch(Exception anException)
                    {
                        System.err.println( aValue );
                    }
                }

                aParser.require( XmlPullParser.END_TAG, null, Base64Decoder.Name );
                aParser.nextTag();

                return aValue;
            }

            throw new IllegalArgumentException( "XMLRPCDecoder.Base64Decoder.decodeWithParser: null parser." );
        }

    }

    //  ===========================================================================
    //  Base64 method(s)
    //  ---------------------------------------------------------------------------

    public static final class Base64 extends Object
    {

        /**
         *  Returns an array of bytes which were encoded in the passed
         *  character array.
         *
         *  @param data the array of base64-encoded characters
         *  @return decoded data array
         */

        static public byte[] decode( byte[] data )
        {
            int len = ( ( data.length + 3 ) / 4 ) * 3;

            if ( data.length > 0 && data[ data.length - 1 ] == '=' )
            {
                --len;
            }

            if ( data.length > 1 && data[ data.length - 2 ] == '=' )
            {
                --len;
            }

            byte[] out = new byte[ len ];

            int shift = 0; // # of excess bits stored in accum
            int accum = 0; // excess bits
            int index = 0;

            for ( int ix = 0; ix < data.length; ix++ )
            {
                int value = codes[ data[ ix ] & 0xFF ]; // ignore high byte of char

                if ( value >= 0 )
                {
                    // skip over non-code

                    accum <<= 6;    // bits shift up by 6 each time thru
                    shift += 6;     // loop, with new bits being put in
                    accum |= value; // at the bottom.

                    if ( shift >= 8 )
                    {
                        // whenever there are 8 or more shifted in,
                        shift -= 8; // write them out (from the top, leaving any

                        // excess at the bottom for next iteration.
                        out[ index++ ] = ( byte )( ( accum >> shift ) & 0xff );
                    }
                }
            }

            if ( index != out.length )
            {
                throw new RuntimeException(
                    "Error decoding BASE64 element: Miscalculated data length." );
            }

            return out;
        }


        /** Code characters for values 0..63 */
        static private char[] alphabet =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".toCharArray();

        /** Lookup table for converting base64 characters to value in range 0..63 */
        static private byte[] codes = new byte[256];

        /** Initialize look-up table */
        static
        {
            for ( int i = 0; i < 256; i++ )
            {
                codes[ i ] = -1;
            }

            for ( int i = 'A'; i <= 'Z'; i++ )
            {
                codes[ i ] = ( byte )( i - 'A' );
            }

            for ( int i = 'a'; i <= 'z'; i++ )
            {
                codes[ i ] = ( byte )( 26 + i - 'a' );
            }

            for ( int i = '0'; i <= '9'; i++ )
            {
                codes[ i ] = ( byte )( 52 + i - '0' );
            }

            codes[ '+' ] = 62;
            codes[ '/' ] = 63;
        }
    }


    //  ===========================================================================
    //  Main method(s)
    //  ---------------------------------------------------------------------------

    public static void main (String args[])
    {
        args= new String[] {"http://www.oreillynet.com/meerkat/xml-rpc/server.php"};
        if ( args.length == 1 )
        {
            System.out.println( "Decoding \"" + args[ 0 ] + "\"" );

            try
            {
                URL             anURL = new URL( args[ 0 ] );
                byte[]          aRequest = "<methodCall><methodName>system.listMethods</methodName><params><param><value></value></param></params></methodCall>".getBytes();
                URLConnection   aConnection = anURL.openConnection();
                aConnection.setDoInput( true );
                aConnection.setDoOutput( true );
                aConnection.setUseCaches( false );
                aConnection.setAllowUserInteraction( false );
                aConnection.setRequestProperty( "Content-Type", "text/xml" );
                aConnection.setRequestProperty( "Content-Length", Integer.toString( aRequest.length ) );
                OutputStream    anOutputStream = new BufferedOutputStream( aConnection.getOutputStream() );
                anOutputStream.write( aRequest );
                anOutputStream.flush();
                InputStream     anInputStream = new BufferedInputStream( aConnection.getInputStream() );
                Reader          aReader = new InputStreamReader( anInputStream );
                Object          anObject = XMLRPCDecoder.decode( aReader );

                System.out.println( "anObject: " + anObject.getClass() );
                System.out.println( "anObject: " + anObject );
            }
            catch(Exception anException)
            {
                System.err.println( anException );
            }

            return;
        }

        System.out.println( "XMLRPCDecoder maps an XML-RPC document into its Java equivalent." );
        System.out.println( "This example will send a canned 'system.listMethods' to the target url." );
        System.out.println( "Usage: <url>" );
        System.out.println( "Example: \"http://www.oreillynet.com/meerkat/xml-rpc/server.php\"" );
    }

}
