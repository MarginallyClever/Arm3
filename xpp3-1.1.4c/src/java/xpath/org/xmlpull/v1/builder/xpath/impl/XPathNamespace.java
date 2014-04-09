
package org.xmlpull.v1.builder.xpath.impl;

import org.xmlpull.v1.builder.XmlElement;
import org.xmlpull.v1.builder.XmlNamespace;

/**
 * Wrap namespace with parent so we can find parent as required by JAXEN ....
 *
 *  @author
 */
public class XPathNamespace
{
    private XmlElement element;

    private XmlNamespace namespace;

    public XPathNamespace( XmlNamespace namespace )
    {
        this.namespace = namespace;
    }

    public XPathNamespace( XmlElement element, XmlNamespace namespace )
    {
        this.element = element;
        this.namespace = namespace;
    }

    public XmlElement getElement()
    {
        return element;
    }

//    public void setElement( XmlElement element )
//    {
//        this.element = element;
//    }

    public XmlNamespace getNamespace()
    {
        return namespace;
    }

    public String toString()
    {
        return ( "[xmlns:" + namespace.getPrefix() + "=\"" +
                 namespace.getNamespaceName() + "\", element=" +
                 element.getName() + "]" );
    }
}

