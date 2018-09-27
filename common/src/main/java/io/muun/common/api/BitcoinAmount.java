package io.muun.common.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import javax.money.MonetaryAmount;
import javax.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class BitcoinAmount {

    @NotNull
    public Long inSatoshis;

    @NotNull
    public MonetaryAmount inInputCurrency;

    @NotNull
    public MonetaryAmount inPrimaryCurrency;

    /**
     * Json constructor.
     */
    public BitcoinAmount() {
    }

    /**
     * Constructor.
     */
    public BitcoinAmount(Long inSatoshis,
                         MonetaryAmount inInputCurrency,
                         MonetaryAmount inPrimaryCurrency) {

        this.inSatoshis = inSatoshis;
        this.inInputCurrency = inInputCurrency;
        this.inPrimaryCurrency = inPrimaryCurrency;
    }
}
