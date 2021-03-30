/*
 * @(#)ProHTTPCompressOutputStream.java	1.00 10th March, 2000
 * 
 * Modification Log:
 * 10th Mar 2000 : Tanmay : Original Version.
 */

package proproxy;

import java.io.*;


/**
 * ProHTTPCompressOutputStream<br>
 * Encapsulates the connection between the Proxy server and the Webserver.
 * Writes to webserver. Checks for the Accept-Encoding header. If a compression method can
 * be supported, adds support. If no Accept-Encoding header was found, adds one before 
 * headers are over.
 * 
 * <br>
 * You are free to use this code and to make modifications provided
 * this notice is retained.
 * <p>
 * If you found this useful, please add a note of acknowledgement to my 
 * <a href="http://htmlgear.lycos.com/guest/control.guest?u=tanmaykm&i=1&a=sign" alt="guest book">guestbook</a>. 
 * If you would like to report a bug or suggest some improvements, 
 * you are most welcome. I will be happy to help you use this piece of code.
 * <p>
 *
 * @author 	    Tanmay K. Mohapatra
 * @version     1.00, 10th Mar, 2000
 */

public class ProHTTPCompressOutputStream extends OutputStream
{
    private ProHTTPCompressStatus   ComprStatus;
    private OutputStream            BaseStream;
    private StringBuffer            sbHeader = new StringBuffer();
    private boolean                 bHeadersStarted;
    
    public ProHTTPCompressOutputStream(OutputStream BaseStreamIn, ProHTTPCompressStatus ComprStatusIn)
    {
        ComprStatus = ComprStatusIn;
        BaseStream = BaseStreamIn;
        bHeadersStarted = false;
    }
    
    /**
     * Write to the underlying stream and keep appending to the string buffer. 
     * Once a new-line is received, check if the header is a Accept-Encoding header, add
     * some extra support if possible, set flag in ComprStatus to ignore further monitoring.
     * If headers ended without Accept-Encoding flag, add full support and set flag in 
     * ComprStatus.
     */
    public void write(int iData) throws IOException
    {
        if (!ComprStatus.bOutputParsingRequired)
        {
            BaseStream.write(iData);
            //System.out.write(iData);
            return;
        }
        
        if ('\n' == iData)
        {
            if(bHeadersStarted)
            {
                String sHeader = sbHeader.toString();
                sHeader = sHeader.trim();
                sbHeader.setLength(0);
                if(0 == sHeader.length())
                {
                    // reached end of headers
                    // If we reached till here, it means that we didn't encounter 
                    // accept-encoding header before. Output accept-encoding header and
                    // set status
                    BaseStream.write(new String("Accept-Encoding: gzip, deflate\n\n").getBytes());
                    //System.out.println("Accept-Encoding: gzip, deflate\n");
                    ComprStatus.iSetHeaders = ComprStatus.ALL_COMPRESS;
                    ComprStatus.bOutputParsingRequired = false;
                }
                else
                {
                    String sTrimmed = sHeader.toLowerCase();
                    if(sTrimmed.startsWith("accept-encoding"))
                    {
                        if(-1 == sTrimmed.indexOf("gzip"))
                        {
                            // add gzip support
                            sHeader += ", gzip";
                            ComprStatus.iSetHeaders |= ComprStatus.GZIP_COMPRESS;
                        }
                        else if (-1 == sTrimmed.indexOf("deflate"))
                        {
                            // add deflate support
                            sHeader += ", deflate";
                            ComprStatus.iSetHeaders |= ComprStatus.DEFLATE_COMPRESS;
                        }
                        ComprStatus.bOutputParsingRequired = false;
                    }
                    BaseStream.write(new String(sHeader + "\n").getBytes());
                    //System.out.println(sHeader);
                }
            }
            else
            {
                BaseStream.write(iData);
                //System.out.write(iData);
            }
        }
        else
        {
            bHeadersStarted = true;
            sbHeader.append((char)iData);
        }
    }
}
