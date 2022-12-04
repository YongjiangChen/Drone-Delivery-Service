package uk.ac.ed.inf;

// all of java.sql is imported because they are all useful in this class
import java.sql.*;
import java.util.ArrayList;

/**
 * Database reads the databases orders and orderdetails at the database port and interpret the database content at
 * a particular time. This class includes helper functions to read the database data in to arraylist<orders> with
 * corresponding parameters. And also functions to create databases deliveries and flightpath and insert values
 */
public class Database {

    /** date string used to filter the sql database  */
    private static String dateString;

    /** data base port */
    private final String dataBasePort;

    /** menus used to calculate delivery cost for order.price  */
    private final Menus menus;

    /**
     * constructor Database
     * For accessing database operations
     * @param port - web server port
     * @param dataBasePort  - database port
     * @param day  - day of the date string
     * @param month  - month of the date string
     * @param year  - year the date string
     */
    public Database(String port,String dataBasePort, String day, String month, String year){
        this.menus = new Menus(port);
        menus.parseMenus();
        this.dataBasePort = dataBasePort;
        dateString = year + "-" + month + "-" + day;
    }

    /**
     * get JDBC String for the database
     * @return JDBC String for the database.
     */
    public String getJDBCString(){
        return "jdbc:derby://localhost:" + dataBasePort + "/derbyDB";
    }

    /**
     * read table orders
     * @return Arraylist<order> orders for each row in the database orders on this specific date
     */
    public ArrayList<Order> readOrders() throws SQLException {
        ArrayList<Order> orders = new ArrayList<>();
        final String ordersQuery = "select * from orders where deliveryDate='" + dateString + "'";
        Connection conn = DriverManager.getConnection(getJDBCString());
        PreparedStatement psOrdersQuery = conn.prepareStatement(ordersQuery);
        ResultSet rs = psOrdersQuery.executeQuery();
        while (rs.next()) { //for all rows in the database, add particular info to order and Arraylist<order> orders
            String ordersNo = rs.getString("orderNo");
            String deliverysDate = rs.getString("deliveryDate");
            String customers = rs.getString("customer");
            String deliverT = rs.getString("deliverTo");
            ArrayList<String> items = readOrderDetails(ordersNo);
            int price = menus.getDeliveryCost(items, menus);
            Order order = new Order(ordersNo,deliverysDate,customers,deliverT,items,price);
            orders.add(order);
        }
        return orders;
    }

    /**
     * read table orderdetails where orderNo = given orderNo
     * @return Arraylist<String> items for the given orderNo
     */
    public ArrayList<String> readOrderDetails(String orderNo) throws SQLException {
        final String orderDetailsQuery = "select * from orderDetails where orderNo=(?)";
        Connection conn = DriverManager.getConnection(getJDBCString());
        PreparedStatement psOrdersQuery = conn.prepareStatement(orderDetailsQuery);
        psOrdersQuery.setString(1,orderNo);
        ResultSet rs = psOrdersQuery.executeQuery();
        ArrayList<String> its = new ArrayList<>();
        while (rs.next()){
            String it = rs.getString("item");
            its.add(it);
        }
        return its;
    }

    /**
     * create an empty table FLIGHTPATH in the database
     */
    public void createTableFlightpath() {
        DatabaseMetaData databaseMetadata = null;
        try {
            Connection conn = DriverManager.getConnection(getJDBCString());
            Statement statement = conn.createStatement();
            databaseMetadata = conn.getMetaData();
            // Note: must capitalise deliveries in the call to getTables
            ResultSet resultSet =databaseMetadata.getTables(null, null,"FLIGHTPATH", null);
            // If the resultSet is not empty then the table exists, so we can drop it
            if (resultSet.next()){
                statement.execute("drop table FLIGHTPATH");
            }
            statement.execute("create table flightpath("+"orderNo varchar(8), "+"fromLongitude double, "+"fromLatitude double, "+"angle int, "+"toLongitude double, "+"toLatitude double)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * create an empty table DELIVERIES in the database
     */
    public void createTableDeliveries() {
        DatabaseMetaData databaseMetadata = null;
        try {
            Connection conn = DriverManager.getConnection(getJDBCString());
            Statement statement = conn.createStatement();
            databaseMetadata = conn.getMetaData();
            // Note: must capitalise deliveries in the call to getTables
            ResultSet resultSet =databaseMetadata.getTables(null, null,"DELIVERIES", null);
            // If the resultSet is not empty then the table exists, so we can drop it
            if (resultSet.next()){
                statement.execute("drop table DELIVERIES");
            }
            statement.execute("create table deliveries("+"orderNo varchar(8), "+"deliveredTo varchar(19), "+"costInPence int)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * insert into the empty table DELIVERIES in the database
     * @param orderno  - order number
     * @param deliverTo  - pick up point
     * @param costInPence - cost for the delivery
     */
    public void insertToTableDelivery(String orderno, String deliverTo, int costInPence){
        PreparedStatement delivery = null;
        try {
            Connection conn = DriverManager.getConnection(getJDBCString());
            delivery = conn.prepareStatement("insert into deliveries values (?, ?, ?)");
            delivery.setString(1, orderno);
            delivery.setString(2, deliverTo);
            delivery.setInt(3, costInPence);
            delivery.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * insert into the empty table DELIVERIES in the database
     * @param orderno  - order number
     * @param fromLongitude  - start long for this flight
     * @param fromLatitude - start lat for this flight
     * @param angle  - flight angle
     * @param toLongitude  - end long for this flight
     * @param toLatitude  - end lat for this flight
     */
    public void insertToTableflightPath(String orderno, double fromLongitude, double fromLatitude, int angle, double toLongitude, double toLatitude){
        PreparedStatement flightpath = null;
        try {
            Connection conn = DriverManager.getConnection(getJDBCString());
            flightpath = conn.prepareStatement("insert into flightpath values (?, ?, ?, ?, ?, ?)");
            flightpath.setString(1, orderno);
            flightpath.setDouble(2,fromLongitude);
            flightpath.setDouble(3,fromLatitude);
            flightpath.setInt(4,angle);
            flightpath.setDouble(5,toLongitude);
            flightpath.setDouble(6,toLatitude);
            flightpath.execute();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
