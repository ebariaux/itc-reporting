package com.tinsys.itc_reporting.shared.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class FXRateDTO implements Serializable, Comparable<FXRateDTO> {

  /**
     * 
     */
  private static final long serialVersionUID = 1L;

  private Long id;
  private BigDecimal rate;
  private ZoneDTO zone;
  private FiscalPeriodDTO period;
  private String currencyIso;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public BigDecimal getRate() {
    return rate;
  }

  public void setRate(BigDecimal rate) {
    this.rate = rate;
  }

  public ZoneDTO getZone() {
    return zone;
  }

  public void setZone(ZoneDTO zone) {
    this.zone = zone;
  }

  public FiscalPeriodDTO getPeriod() {
    return period;
  }

  public void setPeriod(FiscalPeriodDTO period) {
    this.period = period;
  }

  public void setCurrencyIso(String currencyIso) {
    this.currencyIso = currencyIso;
  }

  public String getCurrencyIso() {
    return currencyIso;
  }

  @Override
  public int compareTo(FXRateDTO fxRateDTO) {
    String zoneCode = fxRateDTO.getZone().getCode();
    return this.getZone().getCode().compareTo(zoneCode);
  }

}
