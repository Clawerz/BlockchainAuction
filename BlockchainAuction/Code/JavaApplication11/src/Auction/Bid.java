/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Auction;
import java.util.Date;

/**
 * Bid Class
 * <br>
 * Contem toda a informação sobre uma bid
 * 
 */
public class Bid {
    private Date date;
    private double value;
    private int clientID;
    
    /**
     * Construtor básico de um bid
     * 
     * @param date Data do bid 
     * @param value Valor do bid
     * @param clientID ID do cliente
     */
    public Bid(double value, int clientID) {
        this.date = new Date();
        this.value = value;
        this.clientID = clientID;
    }
    
    /**
     * 
     * @return Retorna a data do bid
     */
    public Date getDate() {
        return date;
    }

    /**
     * 
     * @return Retorna o valor do bid
     */
    public double getValue() {
        return value;
    }

    /**
     * 
     * @return Retorna o ID do cliente que fez o bid
     */
    public int getClientID() {
        return clientID;
    }

    @Override
    public String toString() {
        return "Bid{" + "date=" + date + ", value=" + value + ", clientID=" + clientID + '}';
    }
    
    
    
}
