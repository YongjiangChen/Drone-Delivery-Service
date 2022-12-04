package uk.ac.ed.inf;

import com.mapbox.geojson.Point;
import java.awt.geom.Line2D;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Drone plan the movement of the drone by producing a ArrayList<LongLat> path from the order of the given orders
 * and produce the final ArrayList<Point> pointList avoiding the No-fly-zones, for the final geojson file output.
 * It also contains several helper functions to produce the data to be inserted into the database tables.
 */
public class Drone {

    /** The maximum number of moves the drone can make */
    private static final int MAXIMUM_NO_OF_MOVES = 1500;

    /** Helps function flightpathInsertion, records the number of deliveries successfully made in the flight */
    int noOfDeliveriesMade = 0;

    /** used for menu parsing and get its port*/
    Menus menus;

    /** used for database insertion calls  */
    Database database;

    /** used to get the landmarks and NoFlyZones from the server  */
    WebServer server;

    /** used to record the arrayList<Point> pointList in terms of LongLat and with exact coordinate whenever the drone hovers,
     * so it can cast .equals to order.dliveryTo*/
    LongLat completePosition;

    /**  holds the current langlat position of the drone */
    LongLat currentPosition;

    /** holds the target langlat position in the next drone move   */
    LongLat targetPosition;

    /** given orders read from the database */
    ArrayList<Order> orders;

    /** records the points visited by the drone*/
    ArrayList<Point> pointList = new ArrayList<>();

    /** contains the LongLat coords of the shops and pick up points and home in the order the drone plans to fly,
     * and it contains double coords for hoverings */
    ArrayList<LongLat> path;

    /** record the arrayList<Point> pointList in terms of LongLat */
    ArrayList<LongLat> longPath = new ArrayList<>();

    /** This helps function checkDelivered, It contains all the longlat in longpath, it also includes the exact
     * hovering locations that the drone moved less than 0.00015 degrees */
    ArrayList<LongLat> completePath = new ArrayList<>();

    /** This helps function flightpathInsertion, it records the arrayList<Point> pointList in terms of LongLat,
     * it has the same length as pointList but it only contains two consecutive location(hovering) for the pick up points,
     * not shopping points*/
    ArrayList<LongLat> pickUpPath = new ArrayList<>();



    /**
     * constructor Drone
     * For accessing Drone movement operations
     * @param menus - web server port
     * @param database  - database we get data from
     * @param server  - WebServer we get data from
     */
    public Drone(Menus menus, Database database, WebServer server) throws SQLException {
        this.menus = menus;
        this.database = database;
        this.server = server;
        this.orders = database.readOrders();
    }

    /**
     * check whether the linestring between the given two coordinates crosses the No-Fly-Zones
     * @return true if crossed, false if not
     */
    public boolean isNoFlyZone(double lng1, double lat1, double lng2, double lat2){
        boolean isCrossed = false;
        if(lng1==lng2 && lat1 == lat2){
            return false;
        }
        ArrayList<Line2D> noFlyZone2D = server.getNoFlyZones();
        for(Line2D line2D:noFlyZone2D){
            isCrossed = line2D.intersectsLine(lng1, lat1, lng2, lat2);
            if(isCrossed){
                break;
            }
        }
        return isCrossed;
    }

    /**
     * calculate the angle between the two Longlat coords, if they cross a no-fly-zone, give an angle that doesn't cross for the current longlat
     * @return angle between the two Longlat inputs
     */
    public int getAngle(LongLat current, LongLat target){
        int angle;
        double tan = 0;
        if(target.longitude > current.longitude && target.latitude>=current.latitude){
            tan = Math.atan(Math.abs((target.latitude-current.latitude)/(target.longitude-current.longitude))) * 180 / Math.PI; //if in First quadrant
        }else if(target.longitude <= current.longitude && target.latitude > current.latitude){
            tan = (double)180 - Math.atan(Math.abs((target.latitude-current.latitude)/(target.longitude-current.longitude))) * 180 / Math.PI; //if in second quadrant
        }else if(target.longitude <= current.longitude && target.latitude < current.latitude){
            tan = (double)180 + Math.atan(Math.abs((target.latitude-current.latitude)/(target.longitude-current.longitude))) * 180 / Math.PI; //if in third quadrant
        }else if(target.longitude >= current.longitude && target.latitude < current.latitude){
            tan = (double)360 - Math.atan(Math.abs((target.latitude-current.latitude)/(target.longitude-current.longitude))) * 180 / Math.PI; //if in forth quadrant
        }else if(target.longitude == current.longitude && target.latitude == current.latitude){                                            // if hovering
            tan = LongLat.HOVERING_ANGLE;
        }
        angle = (int)tan;
        if (angle == LongLat.HOVERING_ANGLE) {
            return angle;
        }
        //Convert angle in steps of 10 degrees
        if(angle%10>=5){
            angle = angle/10*10+10;
        }else{
            angle = angle/10*10;
        }
        //range of angele between 0 and 350
        if(angle == 360){
            angle = 0;
        }
        while(isNoFlyZone(current.longitude, current.latitude, target.longitude, target.latitude)) {
            angle += 10;
            if(angle == 360){
                angle = 0;
            }
            target = current.nextPosition(angle);
        }
        return angle;
    }

    /**
     * insert the three word locations into each order's orderShopLocations based on the order items by parsing menus.
     */
    public void findOrderShopLocations(){
        for (Order order : orders) {
            order.orderShopLocations = new ArrayList<>();
            for (int j = 0; j < order.item.size(); j++) {
                String name = order.item.get(j);
                ArrayList<Menus.MenusJson> menusList = menus.parseMenus();
                try {
                    for (Menus.MenusJson mi : menusList) {
                        for (Menus.MenusJson.Item k : mi.menu) {
                            if (k.item.equals(name)) {
                                if (!order.orderShopLocations.contains(mi.location)) {
                                    order.orderShopLocations.add(mi.location);
                                }
                            }
                        }
                    }
                } catch (IllegalArgumentException | NullPointerException e) {
                    e.printStackTrace();
                    System.exit(1);
                }
            }
        }
    }

    /**
     * get the LongLat locations of all shops/pickup points and add them to the order's route. with three Word Parser.
     * meanwhile calculate the totalDeliveryDistance from shop to shop, shop to pick up for each order
     * and their pricePerDistance based on thier price.
     */
    public void getVisitingLongLat(){
        for (Order order : orders) {
            ArrayList<LongLat> shops = new ArrayList<>();
            double distance = 0;
            order.route = new ArrayList<>();
            for (int j = 0; j < order.orderShopLocations.size(); j++) {
                String threeWord = order.orderShopLocations.get(j);
                WordParser wordParser = new WordParser(menus.port);
                WordParser.Word word = wordParser.parseWord(threeWord);
                double lng = word.coordinates.lng;
                double lat = word.coordinates.lat;
                LongLat longLat = new LongLat(lng, lat);
                shops.add(longLat);
                order.route.add(longLat);
                order.route.add(longLat);
            }
            for(int i = 0; i < shops.size()-1; i++) {
                distance += (shops.get(i).distanceTo(shops.get(i+1)));
            }
            String deliverTo = order.deliverTo;
            WordParser wordParser = new WordParser(menus.port);
            WordParser.Word word = wordParser.parseWord(deliverTo);
            double lng = word.coordinates.lng;
            double lat = word.coordinates.lat;
            LongLat longLat = new LongLat(lng, lat);
            order.route.add(longLat);
            order.route.add(longLat);
            order.pickUp = longLat;
            distance += longLat.distanceTo(shops.get(shops.size()-1));
            order.totalDeliveryDistance = distance;
            order.pricePerDistance = (int) Math.round(order.price/order.totalDeliveryDistance);
        }
    }

    /**
     * Sort the orders which are read from the database in terms of decreasing pricePerDistance.
     */
    public void sortOrders() {
        orders.sort(Collections.reverseOrder());
    }

    /**
     * add all order's route to one arraylist path, with appleton tower at the start and end.
     */
    public void createLangLatPath(){
        path = new ArrayList<>();
        path.add(new LongLat(LongLat.APPLETON_LONGITUDE ,LongLat.APPLETON_LATITUDE));
        for(Order order:orders){
            path.addAll(order.route);
        }
        path.add(new LongLat(LongLat.APPLETON_LONGITUDE ,LongLat.APPLETON_LATITUDE));
    }

    /**
     * if there is noflyzone in between two coords in path, add a valid landmark between the two, to avoid the noflyzone
     */
    public void avoidNoFlyZone() {
        for(int counter = 0; counter < path.size()-1; counter++){
            if(isNoFlyZone(path.get(counter).longitude, path.get(counter).latitude, path.get(counter+1).longitude, path.get(counter+1).latitude)){
                for(int i = 0; i< server.landmarks.size(); i++) {
                    if(!(isNoFlyZone(path.get(counter).longitude, path.get(counter).latitude, server.landmarks.get(i).longitude, server.landmarks.get(i).latitude)) &&
                            !(isNoFlyZone(path.get(counter+1).longitude, path.get(counter+1).latitude, server.landmarks.get(i).longitude, server.landmarks.get(i).latitude))) {
                        path.add(counter+1, new LongLat(server.landmarks.get(i).longitude, server.landmarks.get(i).latitude));
                        break;
                    }
                }
                counter ++; // if the drone moves to a landmark, then we increment that as well
            }
        }
    }

    /**
     *  Based on the Arraylist<LongLat> path, add all visiting points to ArrayList<Point> pointList
     *  in steps of a single drone move of 0.00015.
     *  Meanwhile, handle other helper holders, longPath, completePath, pickUpPath
     */
    public void planPath(){
        int TargetPositionFromHome = 0;

        pointList.add(Point.fromLngLat(LongLat.APPLETON_LONGITUDE ,LongLat.APPLETON_LATITUDE));
        longPath.add(new LongLat(LongLat.APPLETON_LONGITUDE ,LongLat.APPLETON_LATITUDE));
        pickUpPath.add(new LongLat(LongLat.APPLETON_LONGITUDE ,LongLat.APPLETON_LATITUDE));
        currentPosition = path.get(0);

        //System.out.println("miao: "+ isNoFlyZone(-3.1911,55.9455, -3.1885,55.9455));
        for(int i = 1; i < path.size(); i++){
            int marker = 0;
            completePosition = path.get(i-1);
            targetPosition = path.get(i);
            if(currentPosition.closeTo(targetPosition)) {
                pointList.add(Point.fromLngLat(currentPosition.longitude,currentPosition.latitude));
                longPath.add(currentPosition);

                for(Order order : orders) {
                    if(targetPosition.equals(order.pickUp)) {
                        pickUpPath.add(currentPosition);
                        marker = 1;
                        break;
                    }
                }
                if(marker == 0) {
                    pickUpPath.add(new LongLat(LongLat.FALSE_LONGITUDE,LongLat.FALSE_LATITUDE));
                }
            }
            completePath.add(completePosition);

            TargetPositionFromHome = (int)(currentPosition.distanceTo(targetPosition) / LongLat.DISTANCE_TOLERANCE +
                   targetPosition.distanceTo(path.get(path.size()-1)) / LongLat.DISTANCE_TOLERANCE);
            if(pointList.size() < (MAXIMUM_NO_OF_MOVES - TargetPositionFromHome)) {
                while(!currentPosition.closeTo(targetPosition)){
                    LongLat thiscurrent = currentPosition;
                    currentPosition = currentPosition.nextPosition(getAngle(currentPosition,targetPosition));
                    LongLat nextcurrent = currentPosition;
                    if(isNoFlyZone(nextcurrent.longitude,nextcurrent.latitude,thiscurrent.longitude,thiscurrent.latitude)) {
                        currentPosition = thiscurrent.nextPosition(getAngle(thiscurrent,nextcurrent));
                    }
                    pointList.add(Point.fromLngLat(currentPosition.longitude,currentPosition.latitude));
                    longPath.add(currentPosition);
                    pickUpPath.add(currentPosition);
                    completePath.add(currentPosition);
                }
            } else{
                targetPosition = path.get(path.size()-1);
                while(!currentPosition.closeTo(targetPosition)){
                    LongLat thiscurrent = currentPosition;
                    currentPosition = currentPosition.nextPosition(getAngle(currentPosition,targetPosition));
                    LongLat nextcurrent = currentPosition;
                    if(isNoFlyZone(nextcurrent.longitude,nextcurrent.latitude,thiscurrent.longitude,thiscurrent.latitude)) {
                        currentPosition = thiscurrent.nextPosition(getAngle(thiscurrent,nextcurrent));
                    }
                    pointList.add(Point.fromLngLat(currentPosition.longitude,currentPosition.latitude));
                    longPath.add(currentPosition);
                    pickUpPath.add(currentPosition);
                    completePath.add(currentPosition);
                }
                break;
            }
        }
        completePath.add(new LongLat(LongLat.APPLETON_LONGITUDE ,LongLat.APPLETON_LATITUDE));
    }

    /**
     * A summary function that calls all necessary functions to make the drone move
     */
    public void droneGo() {
        findOrderShopLocations();
        getVisitingLongLat();
        sortOrders();
        createLangLatPath();
        server.getLandMarks();
        avoidNoFlyZone();
        planPath();
    }

    /**
     * Mark order objects order.isDelivered true if it's indeed delivered.
     */
    public void checkDelivered() {
        ArrayList<LongLat> hoverings = new ArrayList<>();
        for(int i=0; i < completePath.size()-1; i++) {
            if(completePath.get(i).equals(completePath.get(i+1))) {
                hoverings.add(completePath.get(i));
                i++;
            }
        }
        for(Order order : orders) {
            for (LongLat hover : hoverings) {
                if (order.pickUp.equals(hover) && !order.isDelivered) {
                    order.isDelivered = true;
                    break;
                }
            }
        }
    }

    /**
     * Insert orders into database deliveries, based on their isDelivered field.
     */
    public void deliveriesInsertion() {
        checkDelivered();
        for(Order order : orders) {
            if(order.isDelivered) {
                database.insertToTableDelivery(order.orderNo, order.deliverTo, order.price);
                noOfDeliveriesMade += 1;
            }
        }
    }

    /**
     * Insert the flight path into database flightpath, and assign the correct orderno to completed
     * deliveries and BACKHOME to the last move back to appleton tower.
     */
    public void flightPathInsertion() {
        int angle;
        int j = 0;
        String orderStr;
        for(int i = 0; i < longPath.size()-1; i++) {
            angle = getAngle(longPath.get(i),longPath.get(i+1));
            if(pickUpPath.get(i).equals(pickUpPath.get(i+1))) {
                orders.get(j).isOrderDone = true;
            }
            orderStr = orders.get(j).orderNo;
            if(j == noOfDeliveriesMade || orders.get(orders.size()-1).isOrderDone) {
                orderStr = "BACKHOME";
            } else if(orders.get(j).isOrderDone && j < (orders.size() - 1)) {
                j++;
            }
            database.insertToTableflightPath(orderStr, longPath.get(i).longitude, longPath.get(i).latitude, angle,
                    longPath.get(i+1).longitude, longPath.get(i+1).latitude);
        }

    }

}
