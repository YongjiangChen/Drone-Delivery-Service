package uk.ac.ed.inf;

import java.util.ArrayList;

/**
 * Order contains the order constructor for each order and a compareto method for them
 */
public class Order implements Comparable<Order> {

    /** order number  */
    String orderNo;

    /** date of delivery  */
    String deliveryDate;

    /** customer matriculation no */
    String customer;

    /** three word pick up point */
    String deliverTo;

    /** items of the order */
    ArrayList<String> item;

    /** three words locations for the visiting shops */
    ArrayList<String> orderShopLocations = null;

    /** price for the order including delivery fees */
    Integer price;

    /** price per distance across all visiting points in this order */
    Integer pricePerDistance;

    /** distance across all visiting points in this order  */
    double totalDeliveryDistance;

    /** langlat list of all points visited in this order */
    ArrayList<LongLat> route;

    /** langlat format of the pick up point  */
    LongLat pickUp;

    /** is this order delivered  */
    boolean isDelivered = false;

    /** is this order delivered, another marker for  flightpathinsertion()*/
    boolean isOrderDone = false;

    /**
     * Constructor Order
     * @param orderNo order number
     * @param deliveryDate date of delivery
     * @param customer customer matriculation no
     * @param deliverTo three word pick up point
     * @param item items of the order
     * @param price price for the order including delivery fees
     */
    public Order(String orderNo, String deliveryDate, String customer, String deliverTo, ArrayList<String> item, Integer price){
        this.orderNo = orderNo;
        this.deliveryDate = deliveryDate;
        this.customer = customer;
        this.deliverTo = deliverTo;;
        this.item = item;
        this.price = price;
    }

    /**
     * orders are comparable in terms of their pricePerDistance
     */
    @Override
    public int compareTo(Order o) {
        return pricePerDistance.compareTo(o.pricePerDistance);
    }
}
