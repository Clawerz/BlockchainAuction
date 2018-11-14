package Auction;

import java.util.ArrayList;

/**
 * Auction class
 * <br>
 * Contem toda a informação de um leilão
 * 
 */
public class Auction {
    private String auctionName;
    private int creatorID;
    private int auctionID;
    private int timeToFinish;
    private boolean auctionFinished;
    private int buyerID;
    private Bid winnerBid;
    private ArrayList<Bid> bids = new ArrayList<Bid>();
    
    /**
     * Construtor de um leilão
     * 
     * @param auctionName Nome do leilão
     * @param creatorID ID do cliente criador
     * @param auctionID ID do leilão
     * @param timeToFinish  Tempo até ao leilão terminar
     */
    public Auction(String auctionName, int creatorID, int auctionID, int timeToFinish) {
        this.auctionName = auctionName;
        this.creatorID = creatorID;
        this.auctionID = auctionID;
        this.timeToFinish = timeToFinish;
        auctionFinished = false;
    }

    /**
     * 
     * @return  Retorna nome do leilão
     */
    public String getAuctionName() {
        return auctionName;
    }

    /**
     * 
     * @return Retorna ID do criador
     */
    public int getCreatorID() {
        return creatorID;
    }

    /**
     * 
     * @return Retorna ID do leilão
     */
    public int getAuctionID() {
        return auctionID;
    }

    /**
     * 
     * @return Retorna tempo até fim do leilão
     */
    public int getTimeToFinish() {
        return timeToFinish;
    }

    /**
     * 
     * @return Retorna true caso o leilão já tenha acabado, false se não
     */
    public boolean isAuctionFinished() {
        return auctionFinished;
    }

    /**
     * 
     * @return Retorna ID do comprador
     */
    public int getBuyerID() {
        return buyerID;
    }

    /**
     * 
     * @return Retorna lista de bids feitos
     */
    public ArrayList<Bid> getBids() {
        return bids;
    }

    /**
     * 
     * @return Retorna bid vencedor
     */
    public Bid getWinnerBid() {
        return winnerBid;
    }

    /**
     * 
     * @param auctionFinished True caso já tenha acabado, false se não
     */
    public void setAuctionFinished(boolean auctionFinished) {
        this.auctionFinished = auctionFinished;
    }

    /**
     * 
     * @param buyerID ID do comprador
     */
    public void setBuyerID(int buyerID) {
        this.buyerID = buyerID;
    }

    /**
     * 
     * @param auctionName Nome do leilão
     */
    public void setAuctionName(String auctionName) {
        this.auctionName = auctionName;
    }

    /**
     * 
     * @param creatorID ID do criador
     */
    public void setCreatorID(int creatorID) {
        this.creatorID = creatorID;
    }

    /**
     * 
     * @param auctionID ID do leilão
     */
    public void setAuctionID(int auctionID) {
        this.auctionID = auctionID;
    }

    /**
     * 
     * @param timeToFinish Tempo até ao fim
     */
    public void setTimeToFinish(int timeToFinish) {
        this.timeToFinish = timeToFinish;
    }

    /**
     * 
     * @param winnerBid Bid vencedor
     */
    public void setWinnerBid(Bid winnerBid) {
        this.winnerBid = winnerBid;
    }

    /**
     * 
     * @param bids Lista de bids
     */
    public void setBids(ArrayList<Bid> bids) {
        this.bids = bids;
    }
    
    
    
    
}
