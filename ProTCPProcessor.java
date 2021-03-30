/*
 * @(#)ProTCPProcessor.java	1.02 1st Jul 1999
 * 
 * Modification Log:
 * 14th Feb 1999: Tanmay : Original Version
 * 18th Feb 1999: Tanmay : The streams and sockets are closed properly when the thread dies.
 * 01st Jul 1999: Tanmay : Extends ProAbstractTCPProcessor.
 */


package proproxy;

import java.util.Stack;
import java.net.*;
import java.io.*;

/**
 * ProTCPProcessor<br>
 * The TCP connection processor. Takes up a new connection, and keeps passing 
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
 * @version     1.02, 01st Jul, 1999
 */

public class ProTCPProcessor extends ProAbstractTCPProcessor
{
    String      sCascadeServer;
    int         iCascadePort;
    
    /**
     * save the connection properties and the cache stack.
     */
    public ProTCPProcessor(Stack cache, String sCascadeServer, int iCascadePort)
    {
        super(cache);
        this.sCascadeServer = sCascadeServer;
        this.iCascadePort = iCascadePort;
    }

    public void openConnection() throws IOException
    {
        // create the cascading connection
        cascade = new Socket(sCascadeServer, iCascadePort);
        // set some timeout on read
        conn.setSoTimeout(50);
        cascade.setSoTimeout(50);

        // get all streams
        inStream  = conn.getInputStream();
        outStream = conn.getOutputStream();
        inCascade = cascade.getInputStream();
        outCascade = cascade.getOutputStream();
    }
    
    public void processConnection() throws IOException
    {
        tunnelProcess();
    }
}
