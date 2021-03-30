/*
 * @(#)ProTCPListener.java	1.10 28th Jan 2001
 * 
 * Modification Log:
 * 14th Feb 1999: Tanmay : Original Version
 * 18th Feb 1999: Tanmay : The stack is no longer private, to let ProProxy query its size.
 * 01st Jul 1999: Tanmay : Extends abstract class ProAbstractTCPListener.
 * 28th Jan 2001 : Tanmay : Added connection filter.
 */


package proproxy;

import java.net.*;
import java.util.Stack;
import java.util.EmptyStackException;
import java.io.*;

/**
 * ProTCPListener<br>
 * The TCP connection listener. Waits for a new connection, creates a new 
 * ProTCPProcessor for each new connection. ProTCPProcessors are cached in a
 * stack. Each ProTCPProcessor puts itself back to the cache when it is done
 * so that it can be used for the next request.
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
 * @version     1.10, 28th Jan, 2001
 */

public class ProTCPListener extends ProAbstractTCPListener
{
    String                  sCascadeServer;
    int                     iCascadePort;

    /**
     * Save the connection parameters
     */
    public ProTCPListener (int iListenPort, String sCascadeServer, int iCascadePort, String sIncludeList, String sExcludeList)
    {
        super(iListenPort, sIncludeList, sExcludeList);
        this.sCascadeServer = sCascadeServer;
        this.iCascadePort = iCascadePort;
    }
    
    public void createAndProcessRequest(Socket newSocket)
    {
        ProTCPProcessor processor = new ProTCPProcessor(this.processorStack, sCascadeServer, iCascadePort);
        processor.processReq(newSocket);
    }
    
    public void processRequest(Object processor, Socket newSocket)
    {
        ((ProTCPProcessor)processor).processReq(newSocket);
    }
}
