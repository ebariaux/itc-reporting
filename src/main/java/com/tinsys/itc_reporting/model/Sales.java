package com.tinsys.itc_reporting.model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Sales implements Serializable {

  /**
     * 
     */
  private static final long serialVersionUID = 1L;
  private Long id;
  private Integer soldUnits;
  private BigDecimal individualPrice;
  private BigDecimal totalPrice;
  private BigDecimal individualProceeds;
  private BigDecimal totalProceeds;
  private String countryCode;
  private String promoCode;
  private FiscalPeriod period;
  private Zone zone;
  private Application application;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Integer getSoldUnits() {
    return soldUnits;
  }

  public void setSoldUnits(Integer soldUnits) {
    this.soldUnits = soldUnits;
  }

  public BigDecimal getIndividualPrice() {
    return individualPrice;
  }

  public void setIndividualPrice(BigDecimal individualPrice) {
    this.individualPrice = individualPrice;
  }

  public BigDecimal getTotalPrice() {
    return totalPrice;
  }

  public void setTotalPrice(BigDecimal totalPrice) {
    this.totalPrice = totalPrice;
  }

  public BigDecimal getIndividualProceeds() {
    return individualProceeds;
  }

  public void setIndividualProceeds(BigDecimal individualProceeds) {
    this.individualProceeds = individualProceeds;
  }

  public BigDecimal getTotalProceeds() {
    return totalProceeds;
  }

  public void setTotalProceeds(BigDecimal totalProceeds) {
    this.totalProceeds = totalProceeds;
  }

  public String getCountryCode() {
    return countryCode;
  }

  public void setCountryCode(String countryCode) {
    this.countryCode = countryCode;
  }

  public void setPromoCode(String promoCode) {
    this.promoCode = promoCode;
  }

  public String getPromoCode() {
    return promoCode;
  }

  public FiscalPeriod getPeriod() {
    return period;
  }

  public void setPeriod(FiscalPeriod period) {
    this.period = period;
  }

  public Zone getZone() {
    return zone;
  }

  public void setZone(Zone zone) {
    this.zone = zone;
  }

  public Application getApplication() {
    return application;
  }

  public void setApplication(Application application) {
    this.application = application;
  }

  @Override
  public String toString() {
    return "Id:" + this.getId() + "  SoldUnits:" + this.getSoldUnits() + " IndividualPrice:" + this.getIndividualPrice() + " TotaPrice:" + this.getTotalPrice() + " Country code:"
        + this.getCountryCode() + " Promo code:" + this.getPromoCode() + " Period:" + this.getPeriod() + " Zone:" + this.getZone() + " App:" + this.getApplication();
  }
}
