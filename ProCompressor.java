/*
 * @(#)ProCompressor.java	1.00 10th Mar 2000
 * 
 * Modification Log:
 * 10th Mar 2000: Tanmay : Original Version
 */


package proproxy;

import java.net.*;
import java.io.*;
import java.util.zip.*;

/**
 * ProCompressor<br>
 * Contains methods that assist in compressing HTTP responses.
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
final public class ProCompressor
{    
    public static byte[] gzip(byte[] unCompData) throws IOException
    {
        ByteArrayOutputStream baos;
        GZIPOutputStream gos = new GZIPOutputStream(baos=new ByteArrayOutputStream());
        gos.write(unCompData);
        gos.close();
        baos.close();
        return baos.toByteArray();
    }

    public static byte[] gunzip(byte[] compData) throws DataFormatException, IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(compData));
        int iByteRead;
        while(-1 != (iByteRead=gis.read()))
        {
            baos.write(iByteRead);
        }
        gis.close();
        baos.close();
        return baos.toByteArray();
    }
    
    public static byte[] compress(byte[] unCompData)
    {
        byte[]      deflatedData = new byte[1024];
        Deflater    defl = new Deflater(Deflater.DEFAULT_COMPRESSION, true);

        int iBytesDeflated=0, iStart, iEnd;
        defl.setInput(unCompData);
        for(iStart=iEnd=0; !defl.finished(); iStart=iEnd)
        {
            if (deflatedData.length < iStart+1024)
            {
                // expand by 1024 every time
                byte []expandedBuf = new byte[iStart+1024];
                System.arraycopy(deflatedData, 0, expandedBuf, 0, deflatedData.length);
                deflatedData = expandedBuf;
            }
            iBytesDeflated = defl.deflate(deflatedData, iStart, deflatedData.length-iStart);
            iEnd+=iBytesDeflated;
        }
        defl.finish();
        defl.reset();

        byte []deflatedDataOut = new byte[iEnd];
        System.arraycopy(deflatedData, 0, deflatedDataOut, 0, iEnd);
        return deflatedDataOut;
    }

    public static byte[] uncompress(byte[] compData) throws DataFormatException
    {
        Inflater    infl=new Inflater(true);
        byte[]      inflatedData=new byte[1024];
        int         iBytesInflated=0, iStart, iEnd;
        
        infl.setInput(compData);
        for(iStart=iEnd=0; !infl.finished(); iStart=iEnd)
        {
            if (inflatedData.length < iStart+1024)
            {
                // expand by 1024 every time
                byte []expandedBuf = new byte[iStart+1024];
                System.arraycopy(inflatedData, 0, expandedBuf, 0, inflatedData.length);
                inflatedData = expandedBuf;
            }
            iBytesInflated = infl.inflate(inflatedData, iStart, inflatedData.length-iStart);
            iEnd+=iBytesInflated;
        }
        infl.end();
        infl.reset();

        byte []inflatedDataOut = new byte[iEnd];
        System.arraycopy(inflatedData, 0, inflatedDataOut, 0, iEnd);
        return inflatedDataOut;
    }
}
