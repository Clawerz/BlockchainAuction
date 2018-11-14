/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Auction;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * @author cfr
 */
public class Auction {
    private String auctionName;
    private int creatorID;
    private int auctionID;
    private int timeToFinish;
    private boolean auctionFinished;
    private int buyerID;
    private ArrayList<Bid> bids = new ArrayList<Bid>();

    public Auction(String auctionName, int creatorID, int auctionID, int timeToFinish, boolean auctionFinished, int buyerID) {
        this.auctionName = auctionName;
        this.creatorID = creatorID;
        this.auctionID = auctionID;
        this.timeToFinish = timeToFinish;
        this.auctionFinished = auctionFinished;
        this.buyerID = buyerID;
    }
    
    public Auction(String auctionName, int creatorID, int auctionID, int timeToFinish) {
        this.auctionName = auctionName;
        this.creatorID = creatorID;
        this.auctionID = auctionID;
        this.timeToFinish = timeToFinish;
        auctionFinished = false;
    }

    public String getAuctionName() {
        return auctionName;
    }

    public int getCreatorID() {
        return creatorID;
    }

    public int getAuctionID() {
        return auctionID;
    }

    public int getTimeToFinish() {
        return timeToFinish;
    }

    public boolean isAuctionFinished() {
        return auctionFinished;
    }

    public int getBuyerID() {
        return buyerID;
    }

    public ArrayList<Bid> getBids() {
        return bids;
    }

    public void setAuctionFinished(boolean auctionFinished) {
        this.auctionFinished = auctionFinished;
    }

    public void setBuyerID(int buyerID) {
        this.buyerID = buyerID;
    }
    
    
    
    
}
