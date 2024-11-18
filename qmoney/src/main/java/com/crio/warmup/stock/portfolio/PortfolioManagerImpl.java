
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.logging.log4j.core.lookup.StrSubstitutor;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  RestTemplate restTemplate;


  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  //TODO: CRIO_TASK_MODULE_REFACTOR
  // 1. Now we want to convert our code into a module, so we will not call it from main anymore.
  //    Copy your code from Module#3 PortfolioManagerApplication#calculateAnnualizedReturn
  //    into #calculateAnnualizedReturn function here and ensure it follows the method signature.
  // 2. Logic to read Json file and convert them into Objects will not be required further as our
  //    clients will take care of it, going forward.

  // Note:
  // Make sure to exercise the tests inside PortfolioManagerTest using command below:
  // ./gradlew test --tests PortfolioManagerTest

  //CHECKSTYLE:OFF




  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  //CHECKSTYLE:OFF

  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Extract the logic to call Tiingo third-party APIs to a separate function.
  //  Remember to fill out the buildUri function and use that.


  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    String url = buildUri(symbol, from, to);
    TiingoCandle[] tiingoCandle = restTemplate.getForObject(url, TiingoCandle[].class);
    return Arrays.asList(tiingoCandle);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {

    String token = getToken();
    Map<String, String> valueMap = new HashMap<>();
    valueMap.put("SYMBOL", symbol);
    valueMap.put("STARTDATE", String.valueOf(startDate));
    valueMap.put("ENDDATE", String.valueOf(endDate));
    valueMap.put("APIKEY", token);
    String uriTemplate = "https://api.tiingo.com/tiingo/daily/$SYMBOL/prices?"
        + "startDate=$STARTDATE&endDate=$ENDDATE&token=$APIKEY";
    return StrSubstitutor.replace(uriTemplate, valueMap);
  }

  public static String getToken() {
    return "7fd5c5f6930aded4fab2f587bf76771d0e25677a";
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) {
    // TODO Auto-generated method stub

    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
    for (PortfolioTrade trade : portfolioTrades) {

      List<Candle> stockQuotesList = new ArrayList<>();;
      try {
        stockQuotesList = getStockQuote(trade.getSymbol(), trade.getPurchaseDate(), endDate);
      } catch (JsonProcessingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      Candle candle = stockQuotesList.get(0);
      Candle closingCandle = stockQuotesList.get(stockQuotesList.size()-1);
      for(int i=stockQuotesList.size()-1; i>=0; i--) {
        if(stockQuotesList.get(i).getClose() != 0.0){
          closingCandle = stockQuotesList.get(i);
          break;
        }
      }
      Double buyPrice = candle.getOpen();
      Double sellPrice = closingCandle.getClose();
      Double totalReturns = (sellPrice - buyPrice) / buyPrice;
      Double total_num_years = (double) ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate) / 365;
      Double annualized_returns = Math.pow((1 + totalReturns), (1 / total_num_years)) - 1;
      AnnualizedReturn ar = new AnnualizedReturn(trade.getSymbol(), annualized_returns, totalReturns);
      annualizedReturns.add(ar);
    }
    Comparator annualizedReturnsComparator = new Comparator<AnnualizedReturn>() {
      @Override
      public int compare(AnnualizedReturn arg0, AnnualizedReturn arg1) {
        return arg1.getAnnualizedReturn().compareTo(arg0.getAnnualizedReturn());
      }
    };
    Collections.sort(annualizedReturns, annualizedReturnsComparator);
    return annualizedReturns;
  }
}
