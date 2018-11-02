/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Auction;

import java.util.Date;

/**
 *
 * @author cfr
 */
public class Bid {
    private Date date;
    private double value;
    private int clientID;

    public Bid(Date date, double value, int clientID) {
        this.date = new Date();
        this.value = value;
        this.clientID = clientID;
    }
    
    public Date getDate() {
        return date;
    }

    public double getValue() {
        return value;
    }

    public int getClientID() {
        return clientID;
    }
    
}
