package uk.ac.ed.inf;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * Menus calls webServer to send Http requests and gets a Http response of JSON format which it parses
 * It also contains a helper function that calculates the delivery costs for orders.
 */
public class Menus {

    /** The port where the web server is running.*/
    public final String port;

    /** This stores the body of the JSON Http response*/
    private String jsonListString;

    /** The standard delivery fees per order, unit: pence.*/
    private static final int BASIC_DELIVERY_COST = 50;

    /**
     * Constructor Menus
     *
     * @param port The port where the web server is running.
     */
    public Menus(String port){
        this.port = port;
    }

    /**
     * java objects of the Json class Menus
     */
    public static class MenusJson {
        /** The name of a store in the Menus JSON*/
        String name;

        /** The location of a store in the Menus JSON*/
        String location;

        /** The menu of a store in the Menus JSON, as a list of Item*/
        List<Item> menu;

        /** An inner class for a specific item on the menus of the stores */
        public static class Item {

            /** name of the item */
            String item;
            /** price for the item in pence */
            int pence;
        }
    }

    /**
     * sends request to the server and gets response, Deserializing JSON list to a Java object using its type.
     * @return ArrayList<MenusJson> menusJsonList
     */
    public ArrayList<MenusJson> parseMenus(){
        WebServer webServer = new WebServer(port);
        ArrayList<MenusJson> menusJsonList;
        try{
            HttpResponse<String> response = webServer.createResponse(webServer.getURLStringForMenus());
            // A statusCode of 200 means the url string is syntactically and semantically correct,
            // so we can then use response.body()
            if(response.statusCode() == 200){
                jsonListString = response.body();

                // A statusCode of 404 means the url string may be syntactically but is semantically incorrect,
                // we will need to double-check that url
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

        // Deserializing a JSON list to a Java object using its type
        Type listType = new TypeToken<ArrayList<MenusJson>>(){}.getType();
        menusJsonList = new Gson().fromJson(this.jsonListString, listType);
        return menusJsonList;
    }

    /**
     * Calculates the total delivery cost for a given order
     *
     * @param strings - Various strings of items that client ordered
     * @throws NullPointerException If strings are null
     * @return the total delivery cost
     * */
    public int getDeliveryCost(ArrayList<String> strings, Menus menus) {

        // The initial cost is equal to the basic delivery fees of 50p
        int cost = BASIC_DELIVERY_COST;
        ArrayList<Menus.MenusJson> menusList = menus.parseMenus();
        try {
            //looping around items of each store in the Menus JSON
            for (Menus.MenusJson store : menusList) {
                for (String string : strings) {
                    for (Menus.MenusJson.Item specificItem : store.menu) {

                        if (specificItem.item.equals(string)) {
                            cost += specificItem.pence;
                        }
                    }
                }
            }

        } catch (NullPointerException e) {
            System.err.println("ERROR: " + e.getMessage());
            System.exit(1);
        }
        return cost;
    }
}