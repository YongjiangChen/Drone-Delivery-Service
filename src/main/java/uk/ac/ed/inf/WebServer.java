package uk.ac.ed.inf;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.Point;
import com.mapbox.geojson.Polygon;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * WebServer initiates an HttpClient, create http requests and get information for landmarks/no fly zones/ three words locations/
 *  off the server
 */
public class WebServer {

    /** Initialising one Http Client that's shared between all HttpRequests*/
    private static final HttpClient client = HttpClient.newHttpClient();

    /** The port where the web server is running.*/
    public final String port;

    /** A list of landmark coords in langlat*/
    public ArrayList<LongLat> landmarks = new ArrayList<>();

    /**
     * Constructor WebServer
     *
     * @param port The port where the web server is running.
     */
    public WebServer(String port){
        this.port = port;
    }


    /**
     * simple helper function to put up a valid server string to get menus
     * @return valid url
     */
    public String getURLStringForMenus(){
        return "http://localhost:" + port + "/menus/menus.json";
    }

    /**
     * simple helper function to put up a valid server string to get no fly zones
     * @return valid url
     */
    public String getURLStringForNoFlyZones(){ return "http://localhost:" + port + "/buildings/no-fly-zones.geojson"; }

    /**
     * simple helper function to put up a valid server string to get landmarks
     * @return valid url
     */
    public String getURLStringForLandmarks(){
        return "http://localhost:" + port + "/buildings/landmarks.geojson";
    }

    /**
     * simple helper function to put up a valid server string to get three word references
     * @return valid url
     */
    public String getURLStringForThreeWordsLocation(String threeWordLocation){
        String[] wordList = threeWordLocation.split("[.]");
        String threeWordURL = "/words/" + wordList[0] + "/" + wordList[1] + "/" + wordList[2] + "/details.json";
        return "http://localhost:" + port + threeWordURL;
    }

    /**
     * A helper function that constructs web server connection
     * @return  HttpResponse<String> response
     */
    public HttpResponse<String> createResponse(String urlString){
        HttpRequest request;
        HttpResponse<String> response = null;
        try{
            request = HttpRequest.newBuilder().uri(URI.create(urlString)).build();
            response = client.send(request, BodyHandlers.ofString());
        }catch (Exception e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return response;
    }

    /**
     * get landmark coords from server to the landmarks list
     */
    public void getLandMarks(){
        ArrayList<Feature> lfLandmarks = new ArrayList<>();
        HttpResponse<String> response = createResponse(getURLStringForLandmarks());
        FeatureCollection fc = FeatureCollection.fromJson(response.body());
        lfLandmarks = (ArrayList<Feature>) fc.features();
        for(Feature feature: Objects.requireNonNull(lfLandmarks)) {
            Point point = (Point) feature.geometry();
            double lng = Objects.requireNonNull(point).coordinates().get(0);
            double lat = Objects.requireNonNull(point).coordinates().get(1);
            LongLat landmark = new LongLat(lng, lat);
            landmarks.add(landmark);
        }
    }

    /**
     * Get no fly zones from the server
     * @return ArrayList<Line2D> no fly zones in Line2D
     */
    public ArrayList<Line2D> getNoFlyZones(){
        ArrayList<Line2D> line2DArrayList = new ArrayList<>();
        HttpResponse<String> response = createResponse(getURLStringForNoFlyZones());
        FeatureCollection featureCollection = FeatureCollection.fromJson(response.body());
        List<Feature> features = featureCollection.features();
        try{
            for(Feature feature: Objects.requireNonNull(features)){
                Polygon polygon = (Polygon)feature.geometry();
                for(List<Point> listPoint: Objects.requireNonNull(polygon).coordinates()){
                    ArrayList<Point2D> point2DS = new ArrayList<>();
                    for (Point point:listPoint){
                        Point2D point2D = new Point2D.Double();
                        point2D.setLocation(point.coordinates().get(0),point.coordinates().get(1));
                        point2DS.add(point2D);             //Put every point into ArrayList<Point2D> ArrayList
                    }
                    for (int i = 0;i<point2DS.size();i++){
                        Line2D line2D = new Line2D.Double();
                        if (i == point2DS.size()-1){    //if last point, connect with the first point
                            line2D.setLine(point2DS.get(i),point2DS.get(0));
                        }else {
                            line2D.setLine(point2DS.get(i),point2DS.get(i+1));
                        }
                        line2DArrayList.add(line2D);
                    }
                }
            }
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        return line2DArrayList;
    }
}

