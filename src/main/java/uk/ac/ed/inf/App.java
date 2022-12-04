package uk.ac.ed.inf;

import java.sql.SQLException;

/**
 * The main class that produce the target geojson file and create deliveries& flightpath table in database
 * by calling other classes in the package
 */
public class App 
{
    public static void main( String[] args ) throws SQLException {
        String day = args[0];
        String month = args[1];
        String year = args[2];
        String webPort = args[3];
        String databasePort = args[4];
        Database database = new Database(webPort,databasePort,day,month, year);
        Menus menus = new Menus(webPort);
        WebServer server = new WebServer(webPort);
        Drone drone = new Drone(menus, database,server);
        database.createTableDeliveries();
        database.createTableFlightpath();
        drone.droneGo();
        drone.deliveriesInsertion();
        drone.flightPathInsertion();
        File.writeToJSon(drone.pointList,day+ "-" + month + "-" + year);

    }

}
