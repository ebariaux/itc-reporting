package com.tinsys.itc_reporting.server.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFDataFormat;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.tinsys.itc_reporting.server.service.SalesReportServiceImpl;
import com.tinsys.itc_reporting.shared.dto.ApplicationReportSummary;
import com.tinsys.itc_reporting.shared.dto.FiscalPeriodDTO;
import com.tinsys.itc_reporting.shared.dto.ZoneReportSummary;

public class FileDownloadServlet extends HttpServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -7595446460370406470L;
  private final static String ZONE_TOTAL_COLUMN = "Total by Zone";
  private SalesReportServiceImpl salesReportService;
  private FileOutputStream outPutToFile;
  private String filename;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
    salesReportService = (SalesReportServiceImpl) ctx.getBean("salesReportService");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException, RuntimeException {

    FiscalPeriodDTO startPeriod = new FiscalPeriodDTO();
    int startMonth = Integer.parseInt(req.getParameter("startMonth"));
    int startYear = Integer.parseInt(req.getParameter("startYear"));
    startPeriod.setMonth(startMonth);
    startPeriod.setYear(startYear);
    
    FiscalPeriodDTO endPeriod = new FiscalPeriodDTO();
    int endMonth = Integer.parseInt(req.getParameter("endMonth"));
    int endYear = Integer.parseInt(req.getParameter("endYear"));
    endPeriod.setMonth(endMonth);
    endPeriod.setYear(endYear);
    List<ZoneReportSummary> report = salesReportService.getMonthlyReport(startPeriod,endPeriod);

    String basePath = req.getSession().getServletContext().getRealPath("");
    File xlsFile;
    filename = req.getParameter("startYear") + "_" + req.getParameter("startMonth")+ "_to_" + req.getParameter("endYear") + "_" + req.getParameter("endMonth") +".xls";
    try {
      xlsFile = new File(basePath + "/resources/" + filename);

      outPutToFile = new FileOutputStream(xlsFile);
      List<String> applications = new ArrayList<String>();
      for (ZoneReportSummary monthReportSummary : report) {
        for (ApplicationReportSummary applicationReportSummary : monthReportSummary.getApplications()) {
          if (!applications.contains(applicationReportSummary.getApplicationName()) && !applicationReportSummary.getApplicationName().equals(ZONE_TOTAL_COLUMN)) {
            applications.add(applicationReportSummary.getApplicationName());
          }
        }
      }
      applications.add(ZONE_TOTAL_COLUMN);

      HSSFWorkbook workbook = new HSSFWorkbook();
      Map<String, CellStyle> styles = createStyles(workbook);
      HSSFSheet sheet = workbook.createSheet(""+startYear + "_" +startMonth + "_to_"+endYear+ "_" +endMonth);
      PrintSetup printSetup = sheet.getPrintSetup();
      printSetup.setLandscape(true);
      sheet.setFitToPage(true);
      sheet.setHorizontallyCenter(true);

      Row firstHeaderRow = sheet.createRow(0);
      firstHeaderRow.setHeight((short) (16 * 20));
      Cell firstHeaderCell;
      for (int j = 0; j < applications.size(); j++) {
        firstHeaderCell = firstHeaderRow.createCell((j * 7) + 1);
        firstHeaderCell.setCellValue(applications.get(j));
        CellRangeAddress region = new CellRangeAddress(0, 0, (j * 7) + 1, (j * 7) + 7);
        sheet.addMergedRegion(region);
        if ((j % 2) == 0) {
          firstHeaderCell.setCellStyle(styles.get("appFirstHeader1"));
        } else {
          firstHeaderCell.setCellStyle(styles.get("appFirstHeader2"));
        }
        sheet.setColumnWidth(0, 30 * 256);
      }

      Row secondHeaderRow = sheet.createRow(1);
      secondHeaderRow.setHeight((short) (14 * 20));
      Cell secondHeaderCell;
      for (int j = 0; j < applications.size(); j++) {
        secondHeaderCell = secondHeaderRow.createCell((j * 7) + 1);
        secondHeaderCell.setCellValue("Sales #");
        secondHeaderCell.setCellStyle(styles.get("secondHeader"));
        secondHeaderCell = secondHeaderRow.createCell((j * 7) + 2);
        secondHeaderCell.setCellValue("Total orig. currency");
        secondHeaderCell.setCellStyle(styles.get("secondHeader"));
        secondHeaderCell = secondHeaderRow.createCell((j * 7) + 3);
        secondHeaderCell.setCellValue("Total ref. currency");
        secondHeaderCell.setCellStyle(styles.get("secondHeader"));
        secondHeaderCell = secondHeaderRow.createCell((j * 7) + 4);
        secondHeaderCell.setCellValue("Proceeds orig. currency");
        secondHeaderCell.setCellStyle(styles.get("secondHeader"));
        secondHeaderCell = secondHeaderRow.createCell((j * 7) + 5);
        secondHeaderCell.setCellValue("Proceeds ref. currency");
        secondHeaderCell.setCellStyle(styles.get("secondHeader"));
        secondHeaderCell = secondHeaderRow.createCell((j * 7) + 6);
        secondHeaderCell.setCellValue("Proceeds orig. curr. after Taxes");
        secondHeaderCell.setCellStyle(styles.get("secondHeader"));
        secondHeaderCell = secondHeaderRow.createCell((j * 7) + 7);
        secondHeaderCell.setCellValue("Proceeds ref. curr. after Taxes");
        secondHeaderCell.setCellStyle(styles.get("secondHeader"));
        sheet.setColumnWidth(0, 30 * 256);
      }
      int currentIndex = 0;
      HSSFCellStyle cs = workbook.createCellStyle();
      HSSFDataFormat df = workbook.createDataFormat();
      cs.setDataFormat(df.getFormat("#####.##"));
      for (ZoneReportSummary reportLine : report) {
        Row dataRow = sheet.createRow(2 + currentIndex);
        dataRow.setHeight((short) (14 * 20));
        Cell dataCell;
        dataCell = dataRow.createCell(0);
        dataCell.setCellValue(reportLine.getZoneName());

        for (int i = 0; i < applications.size(); i++) {

          for (ApplicationReportSummary applicationReportSummary : reportLine.getApplications()) {
            if (applicationReportSummary.getApplicationName().equals(applications.get(i))) {
              dataCell = dataRow.createCell((i * 7) + 1);
              dataCell.setCellValue(applicationReportSummary.getSalesNumber());
              dataCell = dataRow.createCell((i * 7) + 2);
              if (applicationReportSummary.getOriginalCurrencyAmount() != null) {
                cs = workbook.createCellStyle();
                cs.setDataFormat(df.getFormat("#,##0.00 \"" + applicationReportSummary.getOriginalCurrency() + "\"_"));
                dataCell.setCellStyle(cs);
                dataCell.setCellValue(applicationReportSummary.getOriginalCurrencyAmount().doubleValue());
              }
              ;
              dataCell = dataRow.createCell((i * 7) + 3);
              cs = workbook.createCellStyle();
              cs.setDataFormat(df.getFormat("#,##0.00 \"" + applicationReportSummary.getReferenceCurrency() + "\"_"));
              dataCell.setCellStyle(cs);
              dataCell.setCellValue(applicationReportSummary.getReferenceCurrencyAmount().doubleValue());

              dataCell = dataRow.createCell((i * 7) + 4);
              if (applicationReportSummary.getOriginalCurrencyProceeds() != null) {
                cs = workbook.createCellStyle();
                cs.setDataFormat(df.getFormat("#,##0.00 \"" + applicationReportSummary.getOriginalCurrency() + "\"_"));
                dataCell.setCellStyle(cs);
                dataCell.setCellValue(applicationReportSummary.getOriginalCurrencyProceeds().doubleValue());
              }
              ;
              dataCell = dataRow.createCell((i * 7) + 5);
              cs = workbook.createCellStyle();
              cs.setDataFormat(df.getFormat("#,##0.00 \"" + applicationReportSummary.getReferenceCurrency() + "\"_"));
              dataCell.setCellStyle(cs);
              dataCell.setCellValue(applicationReportSummary.getReferenceCurrencyProceeds().doubleValue());

              dataCell = dataRow.createCell((i * 7) + 6);
              if (applicationReportSummary.getOriginalCurrencyProceedsAfterTax() != null) {
                cs = workbook.createCellStyle();
                cs.setDataFormat(df.getFormat("#,##0.00 \"" + applicationReportSummary.getOriginalCurrency() + "\"_"));
                dataCell.setCellStyle(cs);
                dataCell.setCellValue(applicationReportSummary.getOriginalCurrencyProceedsAfterTax().doubleValue());
              }
              ;
              dataCell = dataRow.createCell((i * 7) + 7);
              cs = workbook.createCellStyle();
              cs.setDataFormat(df.getFormat("#,##0.00 \"" + applicationReportSummary.getReferenceCurrency() + "\"_"));
              dataCell.setCellStyle(cs);
              dataCell.setCellValue(applicationReportSummary.getReferenceCurrencyProceedsAfterTax().doubleValue());
            }
          }

        }
        currentIndex += 1;
      }
      for (int i = 0; i < ((applications.size() * 7) + 1); i++) {
        sheet.autoSizeColumn(i);
      }
      workbook.write(outPutToFile);
      outPutToFile.flush();
      outPutToFile.close();

      ServletContext ctx = getServletContext();
      InputStream is = ctx.getResourceAsStream("/resources/" + filename);
      resp.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
      resp.setContentType("application/vnd.ms-excel");
      ServletOutputStream outToClient = resp.getOutputStream();
      resp.setBufferSize(32768);
      int bufSize = resp.getBufferSize();
      byte[] buffer = new byte[bufSize];
      BufferedInputStream bis = new BufferedInputStream(is, bufSize);
      int bytes;
      while ((bytes = bis.read(buffer, 0, bufSize)) >= 0)
        outToClient.write(buffer, 0, bytes);
      bis.close();
      is.close();
      outToClient.flush();
      outToClient.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    super.doPost(req, resp);
  }

  /**
   * Create a library of cell styles
   */
  private static Map<String, CellStyle> createStyles(HSSFWorkbook wb) {
    Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
    CellStyle style;
    HSSFFont firstHeaderFont = wb.createFont();
    firstHeaderFont.setFontHeightInPoints((short) 12);
    style = wb.createCellStyle();
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    style.setFont(firstHeaderFont);
    styles.put("appFirstHeader1", style);

    style = wb.createCellStyle();
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    style.setFont(firstHeaderFont);
    style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
    style.setFillPattern(CellStyle.SOLID_FOREGROUND);
    styles.put("appFirstHeader2", style);

    Font secondHeaderFont = wb.createFont();
    secondHeaderFont.setFontHeightInPoints((short) 10);
    style = wb.createCellStyle();
    style.setAlignment(CellStyle.ALIGN_CENTER);
    style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
    style.setFont(secondHeaderFont);
    styles.put("secondHeader", style);

    return styles;
  }

}
