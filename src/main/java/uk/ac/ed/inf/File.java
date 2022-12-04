package uk.ac.ed.inf;

import com.mapbox.geojson.Feature;
import com.mapbox.geojson.FeatureCollection;
import com.mapbox.geojson.LineString;
import com.mapbox.geojson.Point;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * File contains the simple write to jason function that writes the flight path in geojson format to a new file in the
 * directory using a buffered writer.
 */
public class File {

    public static void writeToJSon(ArrayList<Point> flightpath, String date){
        LineString flight = LineString.fromLngLats(flightpath);
        Feature flightParseA = Feature.fromGeometry(flight);
        FeatureCollection flightParseB = FeatureCollection.fromFeature(flightParseA);
        String flightStr = flightParseB.toJson();
        java.io.File file = new java.io.File("drone-"+date+".geojson");
        try {
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(flightStr);
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
