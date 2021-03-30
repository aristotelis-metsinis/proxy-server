/*
 * @(#)ProHTTPProcessor.java	1.10 10th Mar 2000
 * 
 * Modification Log:
 * 01st Jul 1999: Tanmay : Original Version
 * 10th Mar 2000: Tanmay : Added support for HTTP compression
 */


package proproxy;

import java.util.Stack;
import java.net.*;
import java.io.*;

/**
 * ProHTTPProcessor<br>
 * The HTTP connection processor. Takes up a new connection, determines the server
 * and port to connect to, opens a new connection and keeps passing 
 * data to and fro till the connection breaks.
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
 * @version     1.10, 10th Mar, 2000
 */

public class ProHTTPProcessor extends ProAbstractTCPProcessor
{
    String  sCommandLine;
    String  sMethod, sPath, sAfterPath, sOnlyPath;
    String  sServer, sProtocol;
    int     iPort;
    static boolean bDoCompression = true;
    
    /**
     * save the cache stack.
     */
    public ProHTTPProcessor(Stack cache)
    {
        super(cache);
    }

    public void openConnection() throws IOException
    {
        URL     reqUrl;
        int     iSepIndex, iSepNum;
        
        sServer = sProtocol = sMethod = sPath = sAfterPath = sOnlyPath = sCommandLine = null;
        iPort = -1;
        // read the url
        // set some timeout on read
        conn.setSoTimeout(50);
        // get incoming streams
        inStream  = conn.getInputStream();
        outStream = conn.getOutputStream();
        readCommandLine(inStream);
        // split the url to extract the host and port
        reqUrl = new URL(sPath);
        sServer = reqUrl.getHost();
        sProtocol = reqUrl.getProtocol();
        if (-1 == (iPort = reqUrl.getPort())) iPort = 80;
        if ( (null == sServer) || !sProtocol.equalsIgnoreCase("http") )
        {
            throw new IOException("Unsupported protocol");
        }
        // extract the url sans host and port
        for(iSepNum=0,iSepIndex=-1; iSepNum<3; )
        {
            if (-1 != (iSepIndex = sPath.indexOf("/", iSepIndex+1))) 
                iSepNum++;
            else
                break;
        }
        sOnlyPath = (iSepNum<3) ? "/" : sPath.substring(iSepIndex);
        
        // create the cascading connection
        cascade = new Socket(sServer, iPort);
        // set some timeout on read
        cascade.setSoTimeout(50);

        // get all streams
        inCascade = cascade.getInputStream();
        outCascade = cascade.getOutputStream();
    }

    public void processConnection() throws IOException
    {
        String sCommand = sMethod + " " + sOnlyPath + " " + sAfterPath + "\r\n";

        if(bDoCompression)
        {
            ProHTTPCompressStatus ppStatus = new ProHTTPCompressStatus();
            outCascade = new ProHTTPCompressOutputStream(outCascade, ppStatus);
            inCascade = new ProHTTPCompressInputStream(inCascade, ppStatus);
        }
        
        // write the request line
        outCascade.write(sCommand.getBytes());

        // pass data to and fro
        tunnelProcess();
    }

    private String readCommandLine(InputStream inStream) throws IOException
    {
        int iByteRead = 0;
        
        StringWriter sLine = new StringWriter();
        do
        {
            try
            {
                iByteRead = inStream.read();
            }
            catch(InterruptedIOException ioe)
            {
                // ignore
                continue;
            }
            sLine.write(iByteRead);
            if ('\r' == iByteRead)  continue;
            if ('\n' == iByteRead)  break;
            
            if (Character.isWhitespace((char)iByteRead))
            {
                if (null == sMethod)
                {
                    sMethod = sLine.toString();
                    sMethod = sMethod.trim();
                }
                else if (null == sPath)
                {
                    sPath = sLine.toString();
                    sPath = sPath.substring(sMethod.length());
                    sPath = sPath.trim();
                }
                else if (null == sAfterPath)
                {
                    int iIndex;
                    sAfterPath = sLine.toString();
                    iIndex = sAfterPath.indexOf(sPath);
                    sAfterPath = sAfterPath.substring(iIndex+sPath.length());
                    sAfterPath.trim();
                }
            }
        } while(-1 != iByteRead);
        
        if (-1 == iByteRead)
        {
            return null;
        }
        String sRet = sLine.toString();
        if (null == sAfterPath)
        {
            int iIndex;
            iIndex = sRet.indexOf(sPath);
            sAfterPath = sRet.substring(iIndex+sPath.length());
            sAfterPath = sAfterPath.trim();
        }
        sLine.close();
        return sRet;
    }
    
    public static void setCompression(boolean bCompress)
    {
        bDoCompression = bCompress;
    }
    
    public static boolean getCompression()
    {
        return bDoCompression;
    }
}
