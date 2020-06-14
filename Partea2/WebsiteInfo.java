package proiect2;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class WebsiteInfo {
	private String FisierHtml;
    private String baseUri;
    private Document document;

    WebsiteInfo(String htmlFile, String baseUri)
    {
        this.FisierHtml = htmlFile;
        this.baseUri = baseUri;
        try {
        	document = Jsoup.parse(new File(htmlFile), null, baseUri);
        } catch (IOException e)
        {
        	document = null;
        }
    }

    public Document getDoc()
    {
        return document;
    }

    public String getBaseUri()
    {
        return baseUri;
    }

    public String getText()
    {
        StringBuilder stringbuider = new StringBuilder();
        stringbuider.append(getTitle()); // titlul
        stringbuider.append(System.lineSeparator());
        stringbuider.append(document.body().text());
        return stringbuider.toString();
    }

    public String getTitle() // preia titlul documentului
    {
        String titlu = document.title();
           return titlu;
    }

  
    public String getRobots() // preia lista de robots
    {
        Element robots = document.selectFirst("meta[name=robots]");
        String robotsString = "";
        if (robots == null) {
           
        } else {
            robotsString = robots.attr("content");
           
        }
        return robotsString;
    }

    public Set<String> getLinks() // preia link-urile de pe site (ancorele)
    {
        Elements linkuri = document.select("a[href], A[href]");
        Set<String> URLuri = new HashSet<String>();
        for (Element link : linkuri) {
            String LinkAbsolut = link.attr("abs:href"); // facem link-urile relative sa fie absolute

            // cautam eventuale ancore in link-uri
            int PozitiaAncorei = LinkAbsolut.indexOf('#');
            if (PozitiaAncorei != -1) // daca exista o ancora (un #)
            {
                // stergem partea cu ancora din link
                StringBuilder LinkTemporar = new StringBuilder(LinkAbsolut);
                LinkTemporar.replace(PozitiaAncorei, LinkTemporar.length(), "");
                LinkAbsolut = LinkTemporar.toString();
            }

            // vrem doar link-uri care contin documente HTML
            try {
                URL absoluteLinkURL = new URL(LinkAbsolut);
                String path = absoluteLinkURL.getPath();
                String extension = path.substring(path.lastIndexOf(".") + 1);
                if (!extension.isEmpty()) // daca link-ul are extensie de document
                {
                    // verificam sa fie document HTML
                    if (!(path.endsWith("html") || path.endsWith("htm")))
                    {
                        continue;
                    }
                }

                // nu vrem sa adaugam duplicate, asa incat folosim o colectie de tip Set
                URLuri.add(LinkAbsolut);
            } catch (MalformedURLException e)
            {
                 continue;
            }
        }
          return URLuri;
    }
}
