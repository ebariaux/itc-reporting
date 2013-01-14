package com.tinsys.itc_reporting.client;

import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.tinsys.itc_reporting.dao.FXrateDAOTest;
import com.tinsys.itc_reporting.dao.RoyaltyDAO;
import com.tinsys.itc_reporting.dao.RoyaltyDAOTest;
import com.tinsys.itc_reporting.dao.SalesDAO;
import com.tinsys.itc_reporting.dao.SalesDAOTest;
import com.tinsys.itc_reporting.dao.TaxDAOTest;
import com.tinsys.itc_reporting.server.service.RoyaltyReportBuilderImpl;
import com.tinsys.itc_reporting.server.service.RoyaltyReportServiceImpl;
import com.tinsys.itc_reporting.shared.dto.CompanyDTO;
import com.tinsys.itc_reporting.shared.dto.FiscalPeriodDTO;
import com.tinsys.itc_reporting.shared.dto.RoyaltyReportLine;

public class RoyaltyReportServiceImplTest {

  private RoyaltyReportServiceImpl royaltyReportService;

  @Before
  public void setUp() throws Exception {
    royaltyReportService = new RoyaltyReportServiceImpl();
    RoyaltyDAO royaltyDAO = new RoyaltyDAOTest();
    royaltyReportService.setRoyaltyDAO(royaltyDAO);
    SalesDAO salesDAO = new SalesDAOTest();
    royaltyReportService.setSalesDAO(salesDAO);
    RoyaltyReportBuilderImpl royaltyReportBuilder = new RoyaltyReportBuilderImpl();
    royaltyReportService.setRoyaltyReportBuilder(royaltyReportBuilder);
    royaltyReportBuilder.setRoyaltyDAO(new RoyaltyDAOTest());
    royaltyReportBuilder.setFxRateDAO(new FXrateDAOTest());
    royaltyReportBuilder.setTaxDAO(new TaxDAOTest());
  }

  @Test
  public void testGetCompanyReportReturnsNullIfArgsAreNull() {
    Assert.assertNull(royaltyReportService.getCompanyReport(null, null, null));
  }

  @Test
  public void testGetCompanyReportReturnsNullIfNoSalesFound() {
    Assert.assertNull(royaltyReportService.getCompanyReport(new CompanyDTO(), new FiscalPeriodDTO(), new FiscalPeriodDTO()));
  }

  @Test
  public void testGetCompanyReportReturnsResultIfSalesFound() {
    CompanyDTO company = new CompanyDTO();
    company.setId(0L);
    company.setCurrencyISO("EUR");
    company.setName("Company 1");

    FiscalPeriodDTO startPeriod = new FiscalPeriodDTO();
    startPeriod.setId(0L);
    startPeriod.setMonth(1);
    startPeriod.setYear(2013);

    FiscalPeriodDTO endPeriod = new FiscalPeriodDTO();
    endPeriod.setId(1L);
    endPeriod.setMonth(3);
    endPeriod.setYear(2013);
    List<RoyaltyReportLine> test = royaltyReportService.getCompanyReport(company, startPeriod, endPeriod);
    Assert.assertNotNull(test);
  }

}
