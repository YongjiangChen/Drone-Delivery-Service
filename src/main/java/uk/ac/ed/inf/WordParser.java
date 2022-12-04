package uk.ac.ed.inf;

import com.google.gson.Gson;
import java.net.http.HttpResponse;

/**
 * calls web server for the three word locations and pack the corresponding longitude and latitude as coordinates
 */
public class WordParser {

    /** The port where the web server is running.*/
    public final String port;

    /** THold the jsonListString.*/
    private String jsonListString;

    /**
     * Constructor WordParser
     * @param port The port where the web server is running.
     */
    public WordParser(String port){
        this.port = port;
    }

    /**
     * Constructor Word
     * With coordinates longitude and latitude
     */
    public static class Word {
        Coordinates coordinates;
        public static class Coordinates {
            double lng;
            double lat;
        }
    }

    /**
     * calls web server for the three word locations and packed in Word
     * @return Word word, the parsed three word locations with valid coordinates
     */
    public Word parseWord(String threeWord){
        Word word = null;
        WebServer webServer = new WebServer(port);
        try{
            HttpResponse<String> response = webServer.createResponse(webServer.getURLStringForThreeWordsLocation(threeWord));
            if(response.statusCode() == 200){
                this.jsonListString = response.body();
            }else if(response.statusCode() == 404) {
                System.err.println("URL Not Found: Unable to connect to localhost at port " + port + "." +
                        "\nStatus Code: 404");
                System.exit(1);
            }else {
                System.err.println("Fatal error: Unable to connect to localhost at port " + port + ".");
                System.exit(1);
            }
        }catch (Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        word = new Gson().fromJson(jsonListString, Word.class);
        return word;
    }
}
