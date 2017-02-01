/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

private Socket socket;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      String html = "";
      html = readHTTPRequest(is);
      writeHTTPHeader(os,"text/html");
      writeContent(os,html);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
}

/**
* Read the HTTP request header.
**/
private String readHTTPRequest(InputStream is)
{
   String line;
   String html = null;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
         System.err.println("Request line: ("+line+")");
         
         //determines the GET line and calls new functions
         if(line.substring(0, 3).equals("GET")){
        	 String fileLocate = chopAtWhitespace(line);
        	 System.err.println(fileLocate);
        	 html = fileGrabber(fileLocate);
         }
         
         if (line.length()==0) break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return html;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();
   DateFormat df = DateFormat.getDateTimeInstance();
   df.setTimeZone(TimeZone.getTimeZone("GMT"));
   os.write("HTTP/1.1 200 OK\n".getBytes());
   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jon's very own server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
// edited to display the contents of a string formated as an html doc
private void writeContent(OutputStream os, String html) throws Exception
{
	
	os.write(html.getBytes());
}

//chops GET into the readable file location
private String chopAtWhitespace(String s){
	String newS = "";
	boolean chopping = false;
	for(int i=0; i<s.length(); i++){
		if( s.charAt(i) == ' '){
			chopping = !chopping;
		}
		if(chopping == true){
			newS = newS + s.charAt(i);
		}
		
	}
	newS = newS.substring(2);
	return newS;
}

//generates the text to send to the readHTTP function
private String fileGrabber(String file) throws IOException{
	String finalHTML = null;
	Path filePath = null;
	try{
		filePath = Paths.get(file);
		finalHTML = readFile(file) ;
	}
	catch(Exception NoSuchFileException){
		finalHTML ="<html><head></head><body>\n<h2>404 error: The page you were looking for cannont be found.</h2>\n <h3> Sorry <h3/>\n</body></html>\n";
		//finalHTML = "<html><head></head><body>\n<h3>My web server works!</h3>\n</body></html>\n";
	}
	System.err.println(filePath);
	
	finalHTML = splicer(finalHTML);
	
	System.err.println(finalHTML);
	
	return finalHTML;
	
}

// grabs the .html file and returns it's contents as a string
private String readFile(String filePath)throws IOException{
	byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
	String fileWords = new String(fileBytes);
	return fileWords;
}

private String splicer(String fileText){
	System.err.println(fileText);
	String newText = fileText;
	DateFormat dateFormat = new SimpleDateFormat("MM/dd/YY");
	Date date = new Date();
	
	for(int i=0; i<fileText.length()-11; i++){
		if(fileText.substring(i, i+11).equals("<cs371date>")){
			newText = newText.substring(0, i)+ "<h3>" +dateFormat.format(date) + "</h3>";
		}
		
	}
	
	for(int i=0; i<fileText.length()-13; i++){
		if(fileText.substring(i, i+13).equals("<cs371server>")){
			newText = newText.substring(0, i)+ "<h3>I am very tired. On my desk I have a Coke Zero bottle. It is glass. It is smaller than the plastic Coke Zero bottles, but I like it more.</h3>";
		}
		System.err.println(fileText.substring(i, i+13));
	}
	
	return newText;
}

} // end class
