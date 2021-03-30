/*
 * @(#)ProHTTPCompressInputStream.java	1.00 10th March, 2000
 * 
 * Modification Log:
 * 10th Mar 2000 : Tanmay : Original Version.
 */
package proproxy;

import java.io.*;
import java.util.zip.DataFormatException;


/**
 * ProHTTPCompressInputStream<br>
 * Encapsulates the actual HTTP connection between the Proxy and the WebServer.
 * Handles header modification and compression of data transparently.
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

public class ProHTTPCompressInputStream extends InputStream
{
    private ProHTTPCompressStatus   ComprStatus;
    private InputStream             BaseStream;
    private ByteArrayOutputStream   baos = null;
    private ByteArrayInputStream    bais = null;

    public ProHTTPCompressInputStream(InputStream BaseStreamIn, ProHTTPCompressStatus ComprStatusIn)
    {
        ComprStatus = ComprStatusIn;
        BaseStream = BaseStreamIn;
    }
    
    /**
     * Wrapper read function that parses and modifies the underlying stream also.
     * 
     * Read from the underlying stream. Write to the StringBuffer. When a new-line is obtained,
     * check if the header is a Content-Encoding header. If yes, check in ComprStatus and 
     * modify it if required. If the header is a content-length header, read the whole output
     * and store it in a local buffer. Henceforth, give from local buffer.
     * If no intervention is required, ignore all the above and return whatever the base stream
     * returns.
     */
    public int read() throws IOException
    {
        int iFirstByte;
        
        if(null != bais)
        {
            if (-1 != (iFirstByte = bais.read()))
            {
                return iFirstByte;
            }
            bais.close();
            bais = null;
        }
        if(0 == ComprStatus.iSetHeaders)    return BaseStream.read(); // Everything handled by browser
        if(null == baos)    baos = new ByteArrayOutputStream();
        
        if(-1 == (iFirstByte = BaseStream.read()))  return -1;
        
        ComprStatus.sRespStatus = new Character((char)iFirstByte).toString();
        do
        {
            ComprStatus.sRespStatus += ProHTTPCompressStatus.readLine(BaseStream);
            ComprStatus.sRespStatus = ComprStatus.sRespStatus.trim();
        } while(ComprStatus.sRespStatus.length() <= 0);
        
        //System.out.println("Resp Status: [" + ComprStatus.sRespStatus + "]");
        ProHTTPCompressStatus.readHeaders(BaseStream, ComprStatus.aHeaders);
        ComprStatus.iRespHeaders = getResponseCompression();
        baos.write((ComprStatus.sRespStatus+"\n").getBytes());
        
        byte []contentData;
        if (0 != (ComprStatus.iRespHeaders & ComprStatus.iSetHeaders))
        {
            ProHTTPCompressStatus.unsetHeader("content-encoding", ComprStatus.aHeaders);
            if(null != (contentData = readContentData(BaseStream)))
            {
                // uncompress appropriately
                try
                {
                    contentData = (ProHTTPCompressStatus.GZIP_COMPRESS == ComprStatus.iRespHeaders) ? ProCompressor.gunzip(contentData) : ProCompressor.uncompress(contentData);
                }
                catch (DataFormatException dfe)
                {
                    throw new IOException(dfe.toString());
                }
                ProHTTPCompressStatus.setHeader("Content-Length", Integer.toString(contentData.length), ComprStatus.aHeaders);
            }
        }
        else
        {
            contentData = readContentData(BaseStream);
        }

        String sContType = ProHTTPCompressStatus.getHeader("Content-Type", ComprStatus.aHeaders);
        if( (null != sContType) && (null != contentData) && (contentData.length > 1024) && 
            ((-1 != sContType.indexOf("text")) || (-1 != sContType.indexOf("html"))) )
        {
            // compress only if it is text/html type        
            if (ProHTTPCompressStatus.ALL_COMPRESS != ComprStatus.iSetHeaders)
            {
                // compress appropriately
                if( (ProHTTPCompressStatus.GZIP_COMPRESS == ComprStatus.iSetHeaders) &&
                    (ProHTTPCompressStatus.DEFLATE_COMPRESS != ComprStatus.iRespHeaders) )
                {
                    contentData = ProCompressor.compress(contentData);
                    ProHTTPCompressStatus.setHeader("Content-Encoding", "deflate", ComprStatus.aHeaders);
                }
                else if( (ProHTTPCompressStatus.DEFLATE_COMPRESS == ComprStatus.iSetHeaders) &&
                         (ProHTTPCompressStatus.GZIP_COMPRESS != ComprStatus.iRespHeaders) )
                {
                    contentData = ProCompressor.gzip(contentData);
                    ProHTTPCompressStatus.setHeader("Content-Encoding", "gzip", ComprStatus.aHeaders);
                }
                ProHTTPCompressStatus.setHeader("Content-Length", Integer.toString(contentData.length), ComprStatus.aHeaders);
            }
        }

        ProHTTPCompressStatus.writeHeaders(baos, ComprStatus.aHeaders);
        if (null != contentData) 
        {
            baos.write(contentData);
        }
        baos.flush();
        baos.close();
        bais = new ByteArrayInputStream(baos.toByteArray());
        ComprStatus.iSetHeaders = 0;
        return bais.read();
    }

    /**
     * Reads content-length amount of data into a byte array.
     * If content-length is not set, returns null.
     */
    private byte [] readContentData(InputStream inStream) throws IOException
    {
        int iRead, iReadNow, iLen;
        byte [] bRead;
        String sLen;
        
        // get content length.
        sLen = ProHTTPCompressStatus.getHeader("content-length", ComprStatus.aHeaders);
        if(null == sLen) return null;
        
        iLen = Integer.parseInt(sLen);
        // read content length amount of data
        bRead = new byte[iLen];
        for(iRead=0; iRead<iLen; iRead+=iReadNow)
        {
            iReadNow = 0;
            try
            {
                if(0 > (iReadNow = inStream.read(bRead, iRead, iLen-iRead)))
                {
                    break;
                }
            }
            catch(InterruptedIOException ie)
            {
                // ignore
            }
        }
        return bRead;
    }
    
    /** Examines the headers for Content-Encoding: gzip, deflate
     * If present, returns the encoding type
     */
    private int getResponseCompression()
    {
        int iIndex;
        int iNumElements = ComprStatus.aHeaders.length;
        int iRespSet = 0;
        for (iIndex=0; iIndex < iNumElements; iIndex+=2)
        {
            if ((null != ComprStatus.aHeaders[iIndex]) && ComprStatus.aHeaders[iIndex].equalsIgnoreCase("content-encoding"))
            {
                if(ComprStatus.aHeaders[iIndex+1].equalsIgnoreCase("gzip")) iRespSet |= ProHTTPCompressStatus.GZIP_COMPRESS;
                if(ComprStatus.aHeaders[iIndex+1].equalsIgnoreCase("deflate")) iRespSet |= ProHTTPCompressStatus.DEFLATE_COMPRESS;
                break;
            }
        }
        return iRespSet;
    }
}
