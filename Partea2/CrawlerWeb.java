package proiect2;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class CrawlerWeb {

	private ClientHTTP Clienthttp;  //client HTTP
    private LinkedList<String> Q;   //URL FRONTIER, coada de URL-uri
    private HashSet<String> vizitatLink;  //multime cu link-urile vizitate pentru a asigura unicitatea
    private HashMap<String, String> robots;  //regulile pentru roboti
   
    public CrawlerWeb(String userAgent)
    {
        Clienthttp = new ClientHTTP(userAgent, "./http"); 

        Q = new LinkedList<>();
        
        vizitatLink = new HashSet<>();

        robots = new HashMap<>();
    }

    // sterge un domeniu din coada de explorare a crawler-ului
    private void removeDomain(String domeniu)
    {
        String url = "http://" + domeniu;

        Q.removeIf(entry -> entry.contains(url));

        vizitatLink.removeIf(entry -> entry.contains(url));

        robots.entrySet().removeIf(entry -> entry.getKey().contains(url));
    }

    public void CrawlMethod(String startingURL, int maxlinkPagees)
    { 
        int nrPaginiCrawlate = 0;

        // initializam URL Frontier, cu primul URL
        Q.add(startingURL);

        while (!Q.isEmpty() && nrPaginiCrawlate < maxlinkPagees)
        {
            String linkPage = Q.pop();
            //FETCH
            // descarcam continutul lui linkPage
            // trebuie sa parsam URL-ul linkPageinii
            try {
                URL currentURL = new URL(linkPage);  
                //PARSE
                int port = currentURL.getPort();
                if (port == -1) 
                {
                    port = 80;
                }
                
                String domeniu = currentURL.getHost();
                String cale = currentURL.getPath();
                
                if (cale.equals(""))
                {
                    cale = "/";
                }
                
               // daca protocolul nu e HTTP, ignoram URL-ul
                if (!currentURL.getProtocol().equals("http")) 
                {
                    System.out.println("[PROTOCOL_INVALID] URL: " + linkPage);
                    continue;
                }

                try {
                    //POLITETE (REP + robots.txt)
                    if (!robots.containsKey(domeniu)) // domeniu nou de explorat
                    {
                        // descarcam robots.txt de pe acest domeniu
                        String robotsRules = Clienthttp.getResource("/robots.txt", domeniu, port, 0);

                        if (robotsRules != null) // si stocam regulile din fisier, daca acestea exista
                        {
                            byte[] continut = Files.readAllBytes(Paths.get(robotsRules));
                            String robotsText = new String(continut);

                            // adaugam setul de reguli pe acest domeniu
                            robots.put(domeniu, robotsText);
                        }
                        else if (Clienthttp.getLastStatus() == 4) // nu s-a putut descarca robots.txt
                        {
                            removeDomain(domeniu);
                            continue;
                        }
                        else if (Clienthttp.getLastStatus() == 404) // nu exista robots.txt pe server
                        {
                            robots.put(domeniu, null);  // de ce aici inlocuiesc domeniul cu null si mai sus il sterg de tot????
                        }

                    } 

                    // preluam lista de reguli
                    String robotsRules = robots.get(domeniu);
                    
                    //daca avem reguli
                    if (robotsRules != null) 
                    {
                    	//daca nu avem voie sa exploram URL curent => facem skip
                        if (!isAllowed(currentURL, robotsRules))
                        {
                            System.out.println("[REP] URL interzis pentru explorare: " + linkPage);
                            continue;
                        }
                    }
                    
            
                    String P = Clienthttp.getResource(cale, domeniu, port, 0);
                    if (P != null)
                    {
                      //PARSE
                        WebsiteInfo info = new WebsiteInfo(P, "http://" + domeniu + ":" + port + cale);

                        //REP LA NIVEL DE linkPageINA
                        boolean linkExtractionAllowed = true;
                        
                        String linkPageRobots = info.getRobots();
                        
                        if (!linkPageRobots.equals("")) // daca exista mentiuni cu privire la robotii ce acceseaza pagina curenta
                        {
                            // tinem cont de ele - daca gasim "noindex" sau "nofollow", ignoram linkPageina
                            linkPageRobots = linkPageRobots.toLowerCase();
                            if (linkPageRobots.contains("noindex"))
                            {
                                System.out.println("[REP] URL interzis pentru indexare: " + linkPage);
                   
                            }
                            if (linkPageRobots.contains("nofollow"))
                            {
                                System.out.println("[REP] URL interzis pentru extragerea de legaturi: " + linkPage);
                                linkExtractionAllowed = false;
                            }
                        }
                        

                        ++nrPaginiCrawlate;

                        // extragem textul din linkPageina si il stocam in fisier
                        // generam numele fisierului text corespunzator, cu extensia txt
                        StringBuilder textFileNameBuilder = new StringBuilder(P);

                        // fisierele HTML ce contin "?" in nume vor primi extensia ".txt" alaturi de intregul nume
                        if (textFileNameBuilder.indexOf("?") != -1) 
                        {
                            textFileNameBuilder.append(".txt");
                        }
                        else // daca nu, inlocuim extensia de dupa "." cu "txt"
                        {
                            textFileNameBuilder.replace(textFileNameBuilder.lastIndexOf(".") + 1, textFileNameBuilder.length(), "txt");
                        }
                        String textFileName = textFileNameBuilder.toString();

                        // scriem rezultatul in fisierul text
                        FileWriter fw = new FileWriter(new File(textFileName), false);
                        fw.write(info.getText());
                        fw.close();
                       
                        // marcam linkPageina curenta ca fiind vizitatLinka
                        vizitatLink.add(linkPage);

                        if (linkExtractionAllowed )
                        {
                        	
                            // extrage din P o lista noua de legaturi N
                            Set<String> N = info.getLinks();

                            // adauga N la Q
                            for (String link : N)
                            {
                                if (!vizitatLink.contains(link)) // avem grija ca URL-ul adaugat sa nu fie vizitatLink deja
                                {
                                    Q.addFirst(link);
                                }
                            }
                        }

                        System.out.println("[URL_FRONTIER] (" + nrPaginiCrawlate + ") " + linkPage);
                    }
                  
                } catch (UnknownHostException uhe) 
                {
                    System.out.println("[CLIENT_HTTP] EROARE: Nu s-a putut stabili conexiunea la " + linkPage);
                    continue;
                } catch (IOException ioe)
                {
                    System.out.println("[CLIENT_HTTP] EROARE: Socket problem: " + linkPage);
                    continue;
                }
            } catch (MalformedURLException e)
            {
                System.out.println("[CLIENT_HTTP] EROARE: URL gresit: " + linkPage);
                continue;
            }
          
        }
        System.out.println("[SFARSIT] Nr de linkPageini procesate: " + nrPaginiCrawlate);
        System.out.println("[SFARSIT] Am atins limita de linkPageini sau avemm o coada goala de URL.");
       
        Clienthttp = null;
        Q.clear();
        vizitatLink.clear();
        robots.clear();
        
    }
    
    public static boolean isAllowed(URL url, String rules)
    {
        if (rules.contains("Disallow")) // nu e permis accesul
        {
            String[] split = rules.split("\n");
            ArrayList<ReguliRobot> robotRules = new ArrayList<>();
            String mostRecentUserAgent = null;
            ArrayList<String> userAgentList = new ArrayList<>();
            
            for (int i = 0; i < split.length; i++)
            {
                String line = split[i].trim();
                if (line.toLowerCase().startsWith("user-agent")) 
                {
                    int start = line.indexOf(":") + 1;
                    int end   = line.length();
                    mostRecentUserAgent = line.substring(start, end).trim();
                    userAgentList.add(mostRecentUserAgent);  //adaugam in lista cu user-agent
                }
                else if (line.startsWith("Disallow")) {
                    if (mostRecentUserAgent != null) {
                    	ReguliRobot r = new ReguliRobot();
                        r.userAgent = mostRecentUserAgent;
                        int start = line.indexOf(":") + 1;
                        int end   = line.length();
                        r.regula = line.substring(start, end).trim();
                        robotRules.add(r);
                    }
                }
            }

            for(String agent: userAgentList ) {
	            for (ReguliRobot robotRule : robotRules) 
	            {
	                String path = url.getPath();
	                if (robotRule.regula.length() == 0) return true; // permite orice actiune
	                if (robotRule.regula.equals("/")) return false; // nu permite nicio actiune in acest caz
	
	                if (robotRule.regula.length() <= path.length())
	                {
	                    String pathCompare = path.substring(0, robotRule.regula.length());
	                    if (pathCompare.equals(robotRule.regula)) return false;
	                }
	            }  
            }
        }
        return true;
    }

    public static void main(String args[])
    {
    	int linkPageeLimit = 100;
    	
        String startingURL = "http://riweb.tibeica.com/crawl/";
        
        CrawlerWeb crawler = new CrawlerWeb("RIWEB_CRAWLER");
        
        long start = System.currentTimeMillis();

        crawler.CrawlMethod(startingURL, linkPageeLimit);


        long end = System.currentTimeMillis();

        NumberFormat formatter = new DecimalFormat("#0.00000");
        System.out.print("Execution time is " + formatter.format((end - start) / 1000d) + " seconds");
    }
}
