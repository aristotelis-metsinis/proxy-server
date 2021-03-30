/*
 * @(#)ProHTTPCompressStatus.java	1.00 10th March, 2000
 * 
 * Modification Log:
 * 10th Mar 2000 : Tanmay : Original Version.
 */

package proproxy;

import java.net.*;
import java.io.*;
/**
 * ProHTTPCompressStatus<br>
 * Class to store intermediate header parsing results. Contains common functions used
 * for manipulating headers.
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

public class ProHTTPCompressStatus
{
    static int GZIP_COMPRESS = 0x1;
    static int DEFLATE_COMPRESS = 0x2;
    static int ALL_COMPRESS = GZIP_COMPRESS | DEFLATE_COMPRESS;
    
    int iSetHeaders=0, iRespHeaders=0;
    String  []aHeaders = new String[50];
    String sRespStatus = "";
    
    boolean         bOutputParsingRequired = true;
        
    static String readLine(InputStream stream) throws IOException
    {
        StringWriter    sLine = new StringWriter();
        int             iByteRead = 0;

        do
        {
            try
            {
                iByteRead = stream.read();
                sLine.write(iByteRead);
                if ('\r' == iByteRead)  continue;
                if ('\n' == iByteRead)  break;
            }
            catch(InterruptedIOException ie)
            {
                // ignore
            }
        } while(-1 != iByteRead);
        return sLine.toString();
    }
    
    // Reads an input stream and parses out the headers.
    // Can be used for both request and response streams.
    // Reads the newline also.
    // Returns the number of headers present or -1 if more than iNumElements headers are present.
    static int readHeaders(InputStream stream, String [] aHeaders) throws IOException
    {
        boolean         bEmptyLine = false;
        int             iIndex;
        int             iNumElements = aHeaders.length;
        
        for(iIndex=0; !bEmptyLine && (iIndex <= iNumElements); iIndex+=2)
        {
            String  sThisLine = readLine(stream).trim();
            int     iLength = sThisLine.length();
            
            if (iLength == 0)
            {
                bEmptyLine = true;
            }
            else
            {
                int iSeparator = sThisLine.indexOf(":");
                if (-1 != iSeparator)
                {
                    aHeaders[iIndex] = sThisLine.substring(0, iSeparator).trim();
                    aHeaders[iIndex+1] = sThisLine.substring(iSeparator+1).trim();
                    //System.out.println("Read: " + aHeaders[iIndex] + ": " + aHeaders[iIndex+1]);
                }
                else
                {
                    // I couldn't make out anything
                    aHeaders[iIndex] = sThisLine;
                    aHeaders[iIndex+1] = null;
                    //System.out.println("Read-: " + aHeaders[iIndex]);
                }
            }
        }
        
        return  (iIndex > iNumElements) ? -1 : iIndex;
    }

    // Writes the headers back into the output stream.
    // Can be used for both request and response streams.
    // Writes the newline also.
    static void writeHeaders(OutputStream stream, String [] aHeaders) throws IOException
    {
        int iIndex;
        int iNumElements = aHeaders.length;
        for(iIndex=0; iIndex<iNumElements; iIndex+=2)
        {
            if (null != aHeaders[iIndex+1])
            {
                stream.write((aHeaders[iIndex]+": "+aHeaders[iIndex+1]+"\n").getBytes());
                //System.out.println("Wrote: " + aHeaders[iIndex] + ": " + aHeaders[iIndex+1]);
            }
            else if (null != aHeaders[iIndex])
            {
                // Header format was not what we expected. Just in case. :)
                stream.write((aHeaders[iIndex]+"\n").getBytes());
                //System.out.println("Wrote-: " + aHeaders[iIndex]);
            }
        }
        stream.write("\n".getBytes());
    }

    static void unsetHeader(String sUnsetHdr, String [] aHeaders)
    {
        int     iIndex;
        int     iNumElements = aHeaders.length;
        
        for (iIndex=0; iIndex < iNumElements; iIndex+=2)
        {
            if ((null != aHeaders[iIndex]) && aHeaders[iIndex].equalsIgnoreCase(sUnsetHdr))
            {
                aHeaders[iIndex] = null;
                aHeaders[iIndex+1] = null;
                break;
            }
        }
    }

    static String getHeader(String sGetHdr, String [] aHeaders)
    {
        int     iIndex;
        int     iNumElements = aHeaders.length;
        
        for (iIndex=0; iIndex < iNumElements; iIndex+=2)
        {
            if ((null != aHeaders[iIndex]) && aHeaders[iIndex].equalsIgnoreCase(sGetHdr))
            {
                return aHeaders[iIndex+1];
            }
        }
        return null;
    }
    
    static void setHeader(String sName, String sValue, String [] aHeaders)
    {
        int     iIndex;
        int     iNumElements = aHeaders.length;
        
        for (iIndex=0; iIndex<iNumElements; iIndex+=2)
        {
            if ((null != aHeaders[iIndex]) && aHeaders[iIndex].equalsIgnoreCase(sName))
            {
                aHeaders[iIndex+1] = sValue;
                break;
            }
        }
        if(iIndex >= iNumElements)
        {
            // add new header
            for (iIndex=0; iIndex<iNumElements; iIndex+=2)
            {
                if (null == aHeaders[iIndex])
                {
                    aHeaders[iIndex] = sName;
                    aHeaders[iIndex+1] = sValue;
                    break;
                }
            }
        }
    }
    
    static void unsetAll(String [] aHeaders)
    {
        int iIndex;
        int iNumElements = aHeaders.length;
        for (iIndex=0; iIndex < iNumElements; iIndex+=2)
        {
            aHeaders[iIndex] = null;
            aHeaders[iIndex+1] = null;
        }
    }
}
