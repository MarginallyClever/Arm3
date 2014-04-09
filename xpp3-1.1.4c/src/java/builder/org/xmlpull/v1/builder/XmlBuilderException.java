/* -*-             c-basic-offset: 4; indent-tabs-mode: nil; -*-  //------100-columns-wide------>|*/
// for license please see accompanying LICENSE.txt file (available also at http://www.xmlpull.org/)

package org.xmlpull.v1.builder;

/**
 * This exception is thrown to signal XB1 related exceptions.
 *
 * @version $Revision: 1.2 $
 * @author <a href="http://www.extreme.indiana.edu/~aslom/">Aleksander Slominski</a>
 */
public class XmlBuilderException extends RuntimeException {
    protected Throwable detail;
    //protected int row = -1;
    //protected int column = -1;

    public XmlBuilderException(String s) {
        super(s);
    }


    public XmlBuilderException(String s, Throwable thrwble) {
        super(s);
        this.detail = thrwble;
        }


    public Throwable getDetail() { return detail; }
    //    public void setDetail(Throwable cause) { this.detail = cause; }

    public String getMessage() {
        if(detail == null)
            return super.getMessage();
        else
            return super.getMessage() + "; nested exception is: \n\t"
                + detail.getMessage();
    }


  public void printStackTrace(java.io.PrintStream ps) {
    if (detail == null) {
        super.printStackTrace(ps);
    } else {
        synchronized(ps) {
          //ps.println(this);
          ps.println(super.getMessage() + "; nested exception is:");
          detail.printStackTrace(ps);
        }
    }
  }

  public void printStackTrace() {
    printStackTrace(System.err);
  }

  public void printStackTrace(java.io.PrintWriter pw){
    if (detail == null) {
        super.printStackTrace(pw);
    } else {
      synchronized(pw) {
        //pw.println(this);
        pw.println(super.getMessage() + "; nested exception is:");
        detail.printStackTrace(pw);
      }
    }
  }

}

