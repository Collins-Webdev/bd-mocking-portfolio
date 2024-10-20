package com.amazon.ata.mocking;

import com.amazon.stock.InsufficientStockException;
import com.amazon.stock.Stock;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Manages and manipulates a portfolio using the stock exchange.
 */
public class PortfolioManager {
    private final Portfolio portfolio;
    private final StockExchangeClient stockExchangeClient;

    /**
     * Instantiates a new PortfolioManager with the provided portfolio and stockExchangeClient.
     * @param portfolio the portfolio that will be managed by this object
     * @param stockExchangeClient the client that will be called to retrieve stock prices, and buy/sell stocks
     */
    public PortfolioManager(Portfolio portfolio, StockExchangeClient stockExchangeClient) {
        if (portfolio == null || stockExchangeClient == null) {
            throw new IllegalArgumentException("Portfolio and StockExchangeClient cannot be null");
        }
        this.portfolio = portfolio;
        this.stockExchangeClient = stockExchangeClient;
    }

    /**
     * Returns the current market value of this portfolio as a whole.
     * @return USD value of the portfolio according to the current stock exchange prices.
     */
    public BigDecimal getMarketValue() {
        return portfolio.getStocks().entrySet().stream()
                .map(stockToQuantity -> calculateStockValue(stockToQuantity.getKey(), stockToQuantity.getValue()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Helper method to calculate the value of a specific stock in the portfolio.
     * @param stock the stock to calculate value for
     * @param quantity the quantity of shares owned
     * @return the total value of the stock in the portfolio
     */
    private BigDecimal calculateStockValue(Stock stock, int quantity) {
        BigDecimal price = stockExchangeClient.getPrice(stock);
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    /**
     * Buys stocks from the stock exchange and adds them to the portfolio.
     * @param stock the stock to buy
     * @param quantity the number of shares to buy
     * @return USD cost of buying the stocks, or null if the purchase failed
     */
    public BigDecimal buy(Stock stock, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        BigDecimal cost = stockExchangeClient.submitBuy(stock, quantity);
        if (cost != null) {
            portfolio.addStocks(stock, quantity);
        }
        return cost;
    }

    /**
     * Sells stocks from the portfolio on the stock exchange. If the portfolio does not contain enough shares,
     * it will not sell any stock.
     * @param stock the stock to sell
     * @param quantity the number of shares to sell
     * @return USD value acquired by selling the stocks or BigDecimal.ZERO if the sale failed.
     */
    public BigDecimal sell(Stock stock, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        if (!portfolio.containsStock(stock, quantity)) {
            return BigDecimal.ZERO;
        }

        try {
            portfolio.removeStocks(stock, quantity);
            return stockExchangeClient.submitSell(stock, quantity);
        } catch (InsufficientStockException e) {
            return BigDecimal.ZERO;
        }
    }
}
