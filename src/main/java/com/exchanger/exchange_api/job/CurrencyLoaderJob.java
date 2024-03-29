package com.exchanger.exchange_api.job;

import com.exchanger.exchange_api.client.internal.ApiLayerClientImpl;
import com.exchanger.exchange_api.client.internal.CoinGeckoClientImpl;
import com.exchanger.exchange_api.enumeration.CurrencyProvider;
import com.exchanger.exchange_api.exception.HttpResponseException;
import com.exchanger.exchange_api.model.CurrencyModel;
import com.exchanger.exchange_api.repository.CurrencyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@EnableScheduling
public class CurrencyLoaderJob {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    private final ApiLayerClientImpl apiLayerClient;
    private final CoinGeckoClientImpl coinGeckoClient;
    private final CurrencyRepository currencyRepository;

    @Autowired
    public CurrencyLoaderJob(ApiLayerClientImpl apiLayerClient,
                             CoinGeckoClientImpl coinGeckoClient,
                             CurrencyRepository currencyRepository) {
        this.apiLayerClient = apiLayerClient;
        this.coinGeckoClient = coinGeckoClient;
        this.currencyRepository = currencyRepository;
    }


    @Scheduled(fixedRate = 3_600_000)
    public void loadCurrencies() {
        this.logger.info("CURRENCY GENERATING STARTED...");
        try {
            Set<String> currencies = this.apiLayerClient.listCurrencies().getCurrencies().keySet();
//            Set<String> currencies = Arrays.stream((new String[]{"USD", "BGN", "EUR"})).collect(Collectors.toSet());

            Set<CurrencyModel> apiLayerCurrencies = currencies.stream().map(s ->
                    new CurrencyModel(s, CurrencyProvider.ApiLayer)).collect(Collectors.toSet());

            Set<CurrencyModel> coinGeckoCurrencies = Arrays.stream(coinGeckoClient.listCurrencies())
                    .filter(coin -> !coin.getSymbol().isEmpty())
                    .map(coin -> new CurrencyModel(coin.getSymbol().toUpperCase(), coin.getId(),
                            CurrencyProvider.CoinGecko)).collect(Collectors.toSet());

            this.currencyRepository.deleteAll();
            this.currencyRepository.saveAll(apiLayerCurrencies);
            this.currencyRepository.saveAll(coinGeckoCurrencies);

            this.logger.info("CURRENCY GENERATING COMPLETED!");
        } catch (HttpResponseException e) {
            e.printStackTrace();
        }
    }
}
