/*
 * @(#)ProAbstractTCPProcessor.java	1.00 1st Jul 1999
 * 
 * Modification Log:
 * 01st Feb 1999: Tanmay : Original Version
 * 28th Jan 2001: Tanmay : Added optimization loop for grabbing large chunks of
 *                         data from either end. This will fix the problem of
 *                         50 ms delays when used in high traffic situation. In
 *                         low traffic situation, it will add a delay of 50ms,
 *                         which is bad, but won't be noticeable.
 * 31st Oct 2001: Tin Le tinle@cisco.com (http://tin.le.org): Modifed tunnelProcess() to check for 
 *                         Global debugflg and act appropriately.  We want to 
 *                         log contents going through the proxy for later analysis.
 */


package proproxy;

import java.util.Stack;
import java.net.*;
import java.io.*;

/**
 * ProAbstractTCPProcessor<br>
 * The base TCP connection processor. Takes up a new connection, and keeps passing 
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
 * @version     1.10, 31st Oct, 2001
 */

public abstract class ProAbstractTCPProcessor implements Runnable
{
    Stack           cache;
    Socket          conn, cascade;
    InputStream     inStream, inCascade;
    OutputStream    outStream, outCascade;
    static int      iNumProcessors=0;
    
    /**
     * save the connection properties and the cache stack.
     */
    public ProAbstractTCPProcessor(Stack cache)
    {
        this.cache = cache;
    }
    
    public abstract void openConnection() throws IOException;
    public abstract void processConnection() throws IOException;
    
    public void tunnelProcess() throws IOException
    {
        byte    bytesRead[] = new byte[1024*5];
        int     iRead=0, iRepeatCount;

		FileOutputStream	finStream=null, finCascade=null;
		FileOutputStream	foutStream=null, foutCascade=null;

		if (ProProxy.debugflg) 
		{
			try 
			{
				finCascade = new FileOutputStream("Server."+ProProxy.requests_counter);
				//foutCascade = new FileOutputStream("outServer."+ProProxy.requests_counter);
				finStream = new FileOutputStream("Client."+ProProxy.requests_counter);
				//foutStream = new FileOutputStream("outClient."+ProProxy.requests_counter);
			} 
			catch (IOException ioe) 
			{
				ioe.printStackTrace();
			}
		}
		
        // continue till connections are not closed
        while(true)
        {
            // read from the client and write to server
            try
            {
				for(iRepeatCount=0; iRepeatCount<5; iRepeatCount++)
				{
					if ((iRead = inStream.read(bytesRead)) > 0)
					{
					    outCascade.write(bytesRead, 0, iRead);
						if (ProProxy.debugflg) 
						{
							//System.out.println("Client -> Server\n----------------------------");
							//System.out.println(new String(bytesRead, 0, iRead));
							finStream.write(bytesRead, 0, iRead);
							//foutCascade.write(bytesRead, 0, iRead);
						}
					}
					else if (iRead <= 0)
					{
					    break;
					}
				}
				if (iRead < 0) break;
            }
            catch (InterruptedIOException iioe)
            {
                // ignore
            }

            // read from the server and write to client
            try
            {
				for(iRepeatCount=0; iRepeatCount<5; iRepeatCount++)
				{
					if ((iRead = inCascade.read(bytesRead)) > 0)
					{
					    outStream.write(bytesRead, 0, iRead);
						if (ProProxy.debugflg) 
						{
							//System.out.println("Server -> Client\n----------------------------");
							//System.out.println(new String(bytesRead, 0, iRead));
							//foutStream.write(bytesRead, 0, iRead);
							finCascade.write(bytesRead, 0, iRead);
						}
					}
					else if (iRead <= 0)
					{
					    break;
					}
				}
				if (iRead < 0) break;
            }
            catch (InterruptedIOException iioe)
            {
                // ignore
            }
        }
		
		if (ProProxy.debugflg) 
		{
        	try{finStream.close();}   catch (IOException ioe){}
        	try{finCascade.close();}  catch (IOException ioe){}
        	//try{foutStream.close();}  catch (IOException ioe){}
        	//try{foutCascade.close();} catch (IOException ioe){}
		}
    }

    public void closeConnection()
    {
        try{inStream.close();}   catch (IOException ioe){}
        try{inCascade.close();}  catch (IOException ioe){}
        try{outStream.close();}  catch (IOException ioe){}
        try{outCascade.close();} catch (IOException ioe){}
        try{cascade.close();}    catch (IOException ioe){}
        try{conn.close();}       catch (IOException ioe){}
        inStream   = null;
        inCascade  = null;
        outStream  = null;
        outCascade = null;
    }
    
    /**
     * The processor thread run routine
     */
    public void run()
    {
        iNumProcessors++;
        
        try
        {
            // create the cascading connection
            openConnection();
        }
        catch (IOException ioe)
        {
            System.err.println("");
            System.err.println("Error opening connection: " + ioe.toString());
            //ioe.printStackTrace();
            iNumProcessors--;
            return;
        }
        
        try
        {
            processConnection();
        }
        catch (IOException ioe)
        {
            // ioe.printStackTrace();
            // somebody closed connection, session over
        }

        // close everything
        closeConnection();
        cascade    = null;
        conn       = null;
        
        cache.push(this);   // push yourself back to the cache
        iNumProcessors--;
    }
    
    /**
     * Process a new connection. Create a thread for it ans start it.
     */
    public void processReq(Socket conn)
    {
        Thread procThread;
        
        this.conn = conn;
        procThread = new Thread(this);
        procThread.setPriority(procThread.getPriority() - 1); // low priority
        procThread.start();
    }
}
