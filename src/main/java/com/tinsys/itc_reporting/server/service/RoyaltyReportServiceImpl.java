package com.tinsys.itc_reporting.server.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tinsys.itc_reporting.client.service.RoyaltyReportService;
import com.tinsys.itc_reporting.dao.FXRateDAO;
import com.tinsys.itc_reporting.dao.RoyaltyDAO;
import com.tinsys.itc_reporting.dao.SalesDAO;
import com.tinsys.itc_reporting.dao.TaxDAO;
import com.tinsys.itc_reporting.model.Application;
import com.tinsys.itc_reporting.model.Sales;
import com.tinsys.itc_reporting.model.Tax;
import com.tinsys.itc_reporting.model.Zone;
import com.tinsys.itc_reporting.server.utils.DTOUtils;
import com.tinsys.itc_reporting.shared.dto.ApplicationReportSummary;
import com.tinsys.itc_reporting.shared.dto.CompanyDTO;
import com.tinsys.itc_reporting.shared.dto.FXRateDTO;
import com.tinsys.itc_reporting.shared.dto.FiscalPeriodDTO;
import com.tinsys.itc_reporting.shared.dto.RoyaltyDTO;
import com.tinsys.itc_reporting.shared.dto.SalesDTO;
import com.tinsys.itc_reporting.shared.dto.ZoneReportSummary;

@Service("royaltyReportService")
@Transactional
public class RoyaltyReportServiceImpl implements RoyaltyReportService {

   private static final Logger logger = Logger.getLogger(RoyaltyReportServiceImpl.class);
    private final static String ZONE_TOTAL_COLUMN = "Total by Zone"; 
   
    @Autowired
    @Qualifier("salesDAO")
    private SalesDAO salesDAO;

    @Autowired
    @Qualifier("fxRateDAO")
    private FXRateDAO fxRateDAO;

    @Autowired
    @Qualifier("taxDAO")
    private TaxDAO taxDAO;
    
    @Autowired
    @Qualifier("royaltyDAO")
    private RoyaltyDAO royaltyDAO;
    
    private Comparator<Sales> compareSales = new Comparator<Sales>() {

       @Override
       public int compare(Sales o1, Sales o2) {
         int c;
         c = Integer.valueOf(o1.getPeriod().getYear()).compareTo(Integer.valueOf(o2.getPeriod().getYear()));
         if (c==0) {
            c = Integer.valueOf(o1.getPeriod().getMonth()).compareTo(Integer.valueOf(o2.getPeriod().getMonth()));
         }
         if (c==0) {
            c = o1.getApplication().getVendorID().compareTo(o2.getApplication().getVendorID());
         }
         if (c==0) {
            c = o1.getZone().getCode().compareTo(o2.getZone().getCode());
         }
         
         return c;
       }
     };
    
    public List<ZoneReportSummary> getMonthlyReport(FiscalPeriodDTO period) {
       logger.debug("Preparing report");
        List<Sales> sales = salesDAO.getAllSales(period);
        //get tax rates for period
        List<Tax> taxes = taxDAO.getTaxesForPeriod(period);
        ArrayList<FXRateDTO> fxRates = fxRateDAO.getAllFXRatesForPeriod(period);
        BigDecimal changeRate = new BigDecimal(0);
        String currency = new String();
        Zone currentZone = null;
        Application currentApplication = null;
        List<ZoneReportSummary> monthReportList = null;
        ApplicationReportSummary applicationSumary = null;
        ZoneReportSummary monthReportLine = new ZoneReportSummary();
        monthReportLine
                .setApplications(new ArrayList<ApplicationReportSummary>());
        ZoneReportSummary monthReportLineTotal = new ZoneReportSummary();
        monthReportLineTotal.setApplications(new ArrayList<ApplicationReportSummary>());
        if (sales != null && sales.size() > 0) {
           logger.debug("Processing  "+sales.size()+" lines");
           BigDecimal taxRate = new BigDecimal(0);
            for (Sales sale : sales) {
                Zone zone = sale.getZone();

                
                // get tax rate for zone
                if (zone != currentZone && monthReportList != null) {
                    if (applicationSumary != null) {
                        monthReportLine.getApplications()
                                .add(applicationSumary);
                        applicationSumary = null;
                    }
                    monthReportList.add(monthReportLine);
                    monthReportLineTotal = appsTotal(monthReportLineTotal, monthReportLine);
                    monthReportLine = new ZoneReportSummary();
                    monthReportLine
                            .setApplications(new ArrayList<ApplicationReportSummary>());
                    changeRate = new BigDecimal(0);
                    currency = new String();
                    for (FXRateDTO fxRate : fxRates) {
                        if (fxRate.getId() != null
                                && fxRate.getZone().getId() == zone.getId()) {
                            changeRate = fxRate.getRate();
                            currency = fxRate.getCurrencyIso();
                            break;
                        }
                    }
                    taxRate = new BigDecimal(0);
                    for (Tax tax : taxes) {
                        if (tax.getZone().equals(zone)){
                           taxRate = tax.getRate();
                           break;
                        }
                    }
                } else {
                    if (currentZone == null) {
                        changeRate = new BigDecimal(0);
                        for (FXRateDTO fxRate : fxRates) {
                            if (fxRate.getId() != null
                                    && fxRate.getZone().getId() == zone.getId()) {
                                changeRate = fxRate.getRate();
                                currency = fxRate.getCurrencyIso();
                                break;
                            }
                        }
                        taxRate = new BigDecimal(0);
                        for (Tax tax : taxes) {
                            if (tax.getZone().equals(zone)){
                               taxRate = tax.getRate();
                               break;
                            }
                        }
                    }
                }
                Application application = sale.getApplication();
                if (application != currentApplication
                        && applicationSumary != null) {
                    monthReportLine.getApplications().add(applicationSumary);
                    applicationSumary = new ApplicationReportSummary();
                    applicationSumary.setOriginalCurrencyAmount(new BigDecimal(
                            0));
                    applicationSumary.setOriginalCurrencyProceeds(new BigDecimal(
                            0));
                    applicationSumary
                            .setReferenceCurrencyAmount(new BigDecimal(0));
                    applicationSumary.setOriginalCurrencyProceedsAfterTax(new BigDecimal(
                            0));
                    } else {
                    if (applicationSumary == null) {
                        applicationSumary = new ApplicationReportSummary();
                        applicationSumary
                                .setOriginalCurrencyAmount(new BigDecimal(0));
                        applicationSumary
                        .setOriginalCurrencyProceeds(new BigDecimal(0));
                        applicationSumary
                                .setReferenceCurrencyAmount(new BigDecimal(0));
                        applicationSumary
                        .setOriginalCurrencyProceedsAfterTax(new BigDecimal(0));                    }
                }
                applicationSumary.setApplicationName(application.getName());
                applicationSumary.setSalesNumber(applicationSumary
                        .getSalesNumber() + sale.getSoldUnits());
                applicationSumary.setOriginalCurrency(sale.getZone()
                        .getCurrencyISO());
                if (applicationSumary.getOriginalCurrencyAmount()==null){
                   applicationSumary.setOriginalCurrencyAmount(new BigDecimal(0));
                }

                
                applicationSumary.setOriginalCurrencyAmount(applicationSumary
                        .getOriginalCurrencyAmount().add(sale.getTotalPrice()));
                if (applicationSumary.getOriginalCurrencyProceeds()==null){
                    applicationSumary.setOriginalCurrencyProceeds(new BigDecimal(0));
                 }
                 applicationSumary.setOriginalCurrencyProceeds(applicationSumary
                         .getOriginalCurrencyProceeds().add((sale.getTotalProceeds()!=null)?sale.getTotalProceeds():new BigDecimal(0)));
                 if (applicationSumary.getOriginalCurrencyProceedsAfterTax()==null){
                     applicationSumary.setOriginalCurrencyProceedsAfterTax(new BigDecimal(0));
                  }
                  applicationSumary.setOriginalCurrencyProceedsAfterTax(applicationSumary
                          .getOriginalCurrencyProceedsAfterTax().add((sale.getTotalProceeds()!=null)?(sale.getTotalProceeds().multiply((new BigDecimal(1).subtract(taxRate)))):new BigDecimal(0)));
                applicationSumary.setReferenceCurrency(currency);
                if (applicationSumary.getReferenceCurrencyAmount()==null){
                   applicationSumary.setReferenceCurrencyAmount(new BigDecimal(0));
                }
                applicationSumary.setReferenceCurrencyAmount(applicationSumary
                        .getReferenceCurrencyAmount().add(
                                ((sale.getTotalPrice()!=null)?sale.getTotalPrice():new BigDecimal(0)).multiply(changeRate)));

                if (applicationSumary.getReferenceCurrencyProceeds()==null){
                    applicationSumary.setReferenceCurrencyProceeds(new BigDecimal(0));
                 }
                 applicationSumary.setReferenceCurrencyProceeds(applicationSumary
                         .getReferenceCurrencyProceeds().add(
                                 ((sale.getTotalProceeds()!=null)?sale.getTotalProceeds():new BigDecimal(0)).multiply(changeRate)));
                 
                 if (applicationSumary.getReferenceCurrencyProceedsAfterTax()==null){
                     applicationSumary.setReferenceCurrencyProceedsAfterTax(new BigDecimal(0));
                  }
                  applicationSumary.setReferenceCurrencyProceedsAfterTax(applicationSumary
                          .getReferenceCurrencyProceedsAfterTax().add(
                                  ((sale.getTotalProceeds()!=null)?(sale.getTotalProceeds().multiply((new BigDecimal(1).subtract(taxRate)))):new BigDecimal(0)).multiply(changeRate))); 
                monthReportLine.setZoneName(zone.getName());
                currentApplication = application;
                currentZone = zone;

                if (monthReportList == null) {
                    monthReportList = new ArrayList<ZoneReportSummary>();
                }

            }
            if (applicationSumary != null && monthReportLine != null) {
                monthReportLine.getApplications().add(applicationSumary);
            }
            if (monthReportLine != null && monthReportList != null) {
                monthReportList.add(monthReportLine);
                monthReportLineTotal = appsTotal(monthReportLineTotal, monthReportLine);
            }
            monthReportLineTotal = appsTotal(monthReportLineTotal, monthReportLineTotal);
            monthReportList.add(monthReportLineTotal);
            return monthReportList;
        } else {
           logger.debug("No sales found for period  "+period.getMonth()+"/"+period.getYear());

        }
        monthReportList = new ArrayList<ZoneReportSummary>();
        return monthReportList;
    }

    private ZoneReportSummary appsTotal(
            ZoneReportSummary monthReportLineTotal,
            ZoneReportSummary monthReportLine) {
        monthReportLineTotal.setZoneName("Total by App :");
        ApplicationReportSummary total = new  ApplicationReportSummary();
        total.setReferenceCurrencyAmount(new BigDecimal(0));
        total.setReferenceCurrencyProceeds(new BigDecimal(0));
        total.setReferenceCurrencyProceedsAfterTax(new BigDecimal(0));
        total.setOriginalCurrencyAmount(new BigDecimal(0));
        total.setOriginalCurrencyProceeds(new BigDecimal(0));
        total.setOriginalCurrencyProceedsAfterTax(new BigDecimal(0));
        for ( ApplicationReportSummary reportSummary: monthReportLine.getApplications()) {
            total.setApplicationName(ZONE_TOTAL_COLUMN);
            if (reportSummary.getOriginalCurrencyAmount()!=null){
            total.setOriginalCurrencyAmount(total.getOriginalCurrencyAmount().setScale(2, RoundingMode.HALF_EVEN).add(reportSummary.getOriginalCurrencyAmount().setScale(2, RoundingMode.HALF_EVEN)));
            }
            if (reportSummary.getOriginalCurrencyProceeds()!=null){
            total.setOriginalCurrencyProceeds(total.getOriginalCurrencyProceeds().setScale(2, RoundingMode.HALF_EVEN).add(reportSummary.getOriginalCurrencyProceeds().setScale(2, RoundingMode.HALF_EVEN)));
            }
            if (reportSummary.getOriginalCurrencyProceedsAfterTax()!=null){
            total.setOriginalCurrencyProceedsAfterTax(total.getOriginalCurrencyProceedsAfterTax().setScale(2, RoundingMode.HALF_EVEN).add(reportSummary.getOriginalCurrencyProceedsAfterTax().setScale(2, RoundingMode.HALF_EVEN)));
            }
            total.setReferenceCurrencyAmount(total.getReferenceCurrencyAmount().setScale(2, RoundingMode.HALF_EVEN).add(reportSummary.getReferenceCurrencyAmount().setScale(2, RoundingMode.HALF_EVEN)));
            total.setReferenceCurrencyProceeds(total.getReferenceCurrencyProceeds().setScale(2, RoundingMode.HALF_EVEN).add(reportSummary.getReferenceCurrencyProceeds().setScale(2, RoundingMode.HALF_EVEN)));
            total.setReferenceCurrencyProceedsAfterTax(total.getReferenceCurrencyProceedsAfterTax().setScale(2, RoundingMode.HALF_EVEN).add(reportSummary.getReferenceCurrencyProceedsAfterTax().setScale(2, RoundingMode.HALF_EVEN)));
            
            total.setReferenceCurrency(reportSummary.getReferenceCurrency());
            total.setOriginalCurrency(reportSummary.getOriginalCurrency());
            total.setSalesNumber(total.getSalesNumber()+reportSummary.getSalesNumber());
            boolean appFound = false;
            if (monthReportLineTotal == monthReportLine){
                total.setOriginalCurrencyAmount(null);
                total.setOriginalCurrencyProceeds(null); 
                total.setOriginalCurrencyProceedsAfterTax(null);
            }
            if (monthReportLineTotal != monthReportLine){
            for (ApplicationReportSummary reportSummaryTotal : monthReportLineTotal.getApplications()){
                if (reportSummaryTotal.getApplicationName().equals(reportSummary.getApplicationName())){
                    appFound = true;
                    reportSummaryTotal.setSalesNumber(reportSummaryTotal.getSalesNumber()+reportSummary.getSalesNumber());
                    reportSummaryTotal.setReferenceCurrencyAmount(reportSummaryTotal.getReferenceCurrencyAmount().setScale(2, RoundingMode.HALF_EVEN).add(reportSummary.getReferenceCurrencyAmount().setScale(2, RoundingMode.HALF_EVEN)));
                    reportSummaryTotal.setReferenceCurrencyProceeds(reportSummaryTotal.getReferenceCurrencyProceeds().setScale(2, RoundingMode.HALF_EVEN).add(reportSummary.getReferenceCurrencyProceeds().setScale(2, RoundingMode.HALF_EVEN)));
                    reportSummaryTotal.setReferenceCurrencyProceedsAfterTax(reportSummaryTotal.getReferenceCurrencyProceedsAfterTax().setScale(2, RoundingMode.HALF_EVEN).add(reportSummary.getReferenceCurrencyProceedsAfterTax().setScale(2, RoundingMode.HALF_EVEN)));
                    reportSummaryTotal.setReferenceCurrency(reportSummary.getReferenceCurrency());
                }
            }
            if (!appFound){
                ApplicationReportSummary reportSummaryTotal = new ApplicationReportSummary();
                reportSummaryTotal.setApplicationName(reportSummary.getApplicationName());
                reportSummaryTotal.setSalesNumber(reportSummary.getSalesNumber());
                reportSummaryTotal.setReferenceCurrencyAmount(reportSummary.getReferenceCurrencyAmount().setScale(2, RoundingMode.HALF_EVEN));
                reportSummaryTotal.setReferenceCurrencyProceeds(reportSummary.getReferenceCurrencyProceeds().setScale(2, RoundingMode.HALF_EVEN));
                reportSummaryTotal.setReferenceCurrencyProceedsAfterTax(reportSummary.getReferenceCurrencyProceedsAfterTax().setScale(2, RoundingMode.HALF_EVEN));
                reportSummaryTotal.setReferenceCurrency(reportSummary.getReferenceCurrency());
                monthReportLineTotal.getApplications().add(reportSummaryTotal);
            }}
        }
        monthReportLine.getApplications().add(total);
        return monthReportLineTotal;
    }

   @Override
   public List<SalesDTO> getCompanyReport(CompanyDTO company,
         FiscalPeriodDTO startPeriod, FiscalPeriodDTO endPeriod) {
      logger.debug("Preparing report");
      List<RoyaltyDTO> royalties = royaltyDAO.getAllRoyalty(company);
      List<Sales> sales = salesDAO.getAllSales(startPeriod,endPeriod,royalties);
      Collections.sort(sales,compareSales);
      List<SalesDTO> salesDTOList = new ArrayList<SalesDTO>();
      for (Sales sale : sales) {
         salesDTOList.add(DTOUtils.salesToSalesDTO(sale));
      }
      return salesDTOList;
   }

}
