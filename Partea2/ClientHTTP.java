package proiect2;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

public class ClientHTTP {

	 private String userAgent;
	 private String FolderulResursa;
	 private int lastStatus;
	 /*
	 lastStatus = cod de eroare intern
	 Semnificatii:
	 1 - nu se poate gasi IP-ul pentru domeniu folosind DNS 
	 2 - prea multe redirectionari
	 3 - pagina mutata, protocol nou nesuportat
	 4 - timp de asteptare depasit
	 5 - socket-ul nu poate determina automat IP-ul pentru domeniu
	 6 - problema I/O cu socket-ul
	 */

	 public ClientHTTP(String userAgent, String resSaveFolder)
	 {
		 	this.userAgent = userAgent;
	        FolderulResursa = resSaveFolder;
	 }
	 
	// preia o resursa web folosind protocolul HTTP 1.1
	 public String getResource(String NumeResursa, String NumeDomeniu, int port, int NumarDeReincercari) throws UnknownHostException
	 {
	        // construim cererea HTTP
	        StringBuilder requestBuilder = new StringBuilder();

	        requestBuilder.append("GET " + NumeResursa + " HTTP/1.1\r\n");   // linia de cerere

	        requestBuilder.append("Host: " + NumeDomeniu + "\r\n");     // antetul Host
	        
	        requestBuilder.append("User-Agent: " + userAgent + "\r\n"); // antetul User-Agent
     
	        requestBuilder.append("Connection: close\r\n");  // antetul Connection

	        requestBuilder.append("\r\n");  // final de cerere

	        String httpRequest = requestBuilder.toString();
	       
	        InetAddress address = InetAddress.getByName(NumeDomeniu); 
	        
	        // aflam host-ul folosind DNS 
	        String host =address.getHostAddress();
	        if (host == null) // DNS-ul nu a reusit preluarea IP-ului
	        {
	            lastStatus = 1;
	            return null;
	        }

	        Socket tcpSocket = null;
	        try {
	            // deschidem socket-ul TCP
	            tcpSocket = new Socket(host, port);
	            tcpSocket.setSoTimeout(2000); 

	            DataOutputStream outToServer = new DataOutputStream(tcpSocket.getOutputStream()); // buffer de iesire (pt cerere)
	            BufferedReader inFromServer = new BufferedReader(new InputStreamReader(tcpSocket.getInputStream())); // buffer de intrare (pt raspuns)

	            // trimitem cererea
	            outToServer.writeBytes(httpRequest);
	           
	            String responseLine;
	            boolean res_OK = false;
	            boolean res_Moved_Permanently = false;
	            boolean res_Found= false;

	            // prima linie este linia de stare, ce contine codul de raspuns
	            responseLine = inFromServer.readLine();
	            if (responseLine.contains("200 OK"))
	            {
	            	res_OK = true;
	                lastStatus = 200;
	            }
	            else if (responseLine.contains("301")) // Moved Permanently
	            {
	            	res_Moved_Permanently = true;
	                lastStatus = 301;
	            }
	            else if (responseLine.contains("302")) // Found
	            {
	            	res_Found= true;
	                lastStatus = 302;
	            }
	            else if (responseLine.contains("404"))
	            {
	                lastStatus = 404;
	                return null;
	            }
	          
	            // afisam si restul liniilor din antet
	            String location = ""; // ne intereseaza noua locatie in caz de raspuns 301
	            while ((responseLine = inFromServer.readLine()) != null)
	            {
	                if (responseLine.equals("")) // sfarsit de antet -> deci urmeaza continutul raspunsului
	                {
	                    break;
	                }
	                if (responseLine.startsWith("Location:")) // avem un raspuns 301 Moved Permanently sau 302 Found
	                {
	                    location = responseLine.replace("Location: ", "");
	                }
	              
	            }

	            String htmlFilePath;
	            if (res_OK) // salvam continutul paginii doar daca raspunsul este 200 OK
	            {
	                // construim continutul paginii trimise de server
	                StringBuilder pageBuilder = new StringBuilder();
	                while ((responseLine = inFromServer.readLine()) != null) {
	                    pageBuilder.append(responseLine + System.lineSeparator());
	                }

	                // construim calea de salvare a resursei
	                htmlFilePath = FolderulResursa + "/" + NumeDomeniu + NumeResursa;
	                if (!(htmlFilePath.endsWith(".html") || htmlFilePath.endsWith("htm")) && !NumeResursa.equals("/robots.txt"))
	                {
	                    if (!htmlFilePath.endsWith("/"))
	                    {
	                        htmlFilePath += "/";
	                    }
	                    htmlFilePath += "index.html";
	                }

	                File file = new File(htmlFilePath);
	                File parentDirectory = file.getParentFile();
	                if (!parentDirectory.exists())
	                {
	                    parentDirectory.mkdirs();
	                }

	                // salvam resursa
	                BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFilePath));
	                writer.write(pageBuilder.toString());
	                writer.close();
	               
	                if (NumarDeReincercari > 0)
	                {
	                    System.out.println("[CLIENT_HTTP] Pagina gasita la locatia noua \"http://" + NumeDomeniu + NumeResursa + "\".");
	                }
	            }
	            else if (res_Moved_Permanently || res_Found) // 301 Moved Permanently sau 302 Found
	            {
	                // inchidem socket-ul
	                tcpSocket.close();

	                if (NumarDeReincercari > 3)
	                {
	                    System.out.println("[CLIENT_HTTP] EROARE: Prea multe redirectionari pe domeniul \"" + NumeDomeniu + "\".");
	                    lastStatus = 2;
	                    return null;
	                }

	                // refacem cererea pentru noua locatie
	                URL NouaLocatie = new URL(location);
	                if (!NouaLocatie.getProtocol().equals("http"))
	                {
	                    System.out.println("[CLIENT_HTTP] EROARE: Pagina mutata permanent. Protocol nou nesuportat! (" + location + ")");
	                    lastStatus = 3;
	                    return null;
	                }
	                String NouaCale = NouaLocatie.getPath();
	                String NoulDomeniu = NouaLocatie.getHost();
	                int NoulPort = NouaLocatie.getPort();
	                if (NoulPort == -1)
	                {
	                	NoulPort = 80;
	                }
	                System.out.println("[CLIENT_HTTP] Pagina mutata permanent la locatia \"" + location + "\". Se incearca refacerea cererii HTTP...");
	                return getResource(NouaCale, NoulDomeniu, NoulPort, NumarDeReincercari + 1);
	            }
	            else {
	                // inchidem socket-ul
	                tcpSocket.close();
	                return null;
	            }

	            // inchidem socket-ul
	            tcpSocket.close();

	            return htmlFilePath;
	        }
	        catch (SocketTimeoutException ste)
	        {
	            System.out.println("[CLIENT_HTTP] EROARE: Timp de asteptare depasit pentru descarcarea \"http://" + NumeDomeniu + NumeResursa + "\".");
	            lastStatus = 4;
	            return null;
	        }
	        catch (UnknownHostException uhe)
	        {
	            System.out.println("[CLIENT_HTTP] EROARE: Socket-ul nu poate determina IP-ul pentru domeniul \"" + NumeDomeniu + "\".");
	            lastStatus = 5;
	            return null;
	        }
	        catch (IOException ioe)
	        {
	            System.out.println("[CLIENT_HTTP] EROARE: Problema socket I/O.");
	            lastStatus = 6;
	            return null;
	        }
	        catch (ArrayIndexOutOfBoundsException aioobe) // pachet DNS corupt
	        {
	            return null;
	        }
	        finally
	        {
	            if (tcpSocket != null)
	            {
	                try
	                {
	                    tcpSocket.close();
	                } catch (IOException ioe)
	                {
	                    System.out.println("[CLIENT_HTTP] EROARE: Nu s-a putut inchide socket-ul.");
	                }
	            }
	        }
	 }

	 public int getLastStatus()
	 {
	        return lastStatus;
	 }
	 
	 
}
