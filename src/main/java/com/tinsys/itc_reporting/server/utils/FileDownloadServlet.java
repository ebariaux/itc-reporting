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

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PrintSetup;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.tinsys.itc_reporting.server.service.SalesReportServiceImpl;
import com.tinsys.itc_reporting.shared.dto.ApplicationReportSummary;
import com.tinsys.itc_reporting.shared.dto.FiscalPeriodDTO;
import com.tinsys.itc_reporting.shared.dto.MonthReportSummary;

public class FileDownloadServlet extends HttpServlet {

  /**
   * 
   */
  private static final long serialVersionUID = -7595446460370406470L;
  private SalesReportServiceImpl salesReportService;
  private FileOutputStream outPutToFile;


  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);

    ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
    salesReportService = (SalesReportServiceImpl) ctx.getBean("salesReportService");
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

     FiscalPeriodDTO period = new FiscalPeriodDTO();
     int month = Integer.parseInt(req.getParameter("month"));
     int year = Integer.parseInt(req.getParameter("year"));
     period.setMonth(month);
     period.setYear(year);
     List<MonthReportSummary> report = salesReportService.getMonthlyReport(period);

    

    String basePath = req.getSession().getServletContext().getRealPath("");
    File xlsFile;
    try {
       xlsFile = new File(basePath + "/resources/"+req.getParameter("year")+"_"+req.getParameter("month")+".xls");
    
    
    outPutToFile = new FileOutputStream(xlsFile);
    List<String> applications = new ArrayList<String>();
    for (MonthReportSummary monthReportSummary : report) {
       for (ApplicationReportSummary applicationReportSummary : monthReportSummary
             .getApplications()) {
          if (!applications.contains(applicationReportSummary
                .getApplicationName())) {
             applications.add(applicationReportSummary
                   .getApplicationName());
          }
       }
    }
    
    
    HSSFWorkbook workbook = new HSSFWorkbook();
    Map<String, CellStyle> styles = createStyles(workbook);
    HSSFSheet sheet = workbook.createSheet(req.getParameter("year")+"_"+req.getParameter("month"));
    PrintSetup printSetup = sheet.getPrintSetup();
    printSetup.setLandscape(true);
    sheet.setFitToPage(true);
    sheet.setHorizontallyCenter(true);
    
    Row firstHeaderRow = sheet.createRow(0);
    firstHeaderRow.setHeight((short) (16*20));
    Cell firstHeaderCell;
    for (int j = 0; j < applications.size(); j++) {
       firstHeaderCell = firstHeaderRow.createCell((j*3)+1);
       firstHeaderCell.setCellValue(applications.get(j));
       CellRangeAddress region = new CellRangeAddress(0,0, (j*3)+1, (j*3)+3);
       sheet.addMergedRegion(region);
       firstHeaderCell.setCellStyle(styles.get("appFirstHeader"));
       sheet.setColumnWidth(0, 30*256);
   }

    Row secondHeaderRow = sheet.createRow(1);
    secondHeaderRow.setHeight((short) (14*20));
    Cell secondHeaderCell;
    for (int j = 0; j < applications.size(); j++) {
       secondHeaderCell = secondHeaderRow.createCell((j*3)+1);
       secondHeaderCell.setCellValue("Sales #");
       secondHeaderCell.setCellStyle(styles.get("secondHeader"));
       secondHeaderCell = secondHeaderRow.createCell((j*3)+2);
       secondHeaderCell.setCellValue("Total orig. currency");
       secondHeaderCell.setCellStyle(styles.get("secondHeader"));
       secondHeaderCell = secondHeaderRow.createCell((j*3)+3);
       secondHeaderCell.setCellValue("Total ref. currency");
       secondHeaderCell.setCellStyle(styles.get("secondHeader"));
       sheet.setColumnWidth(0, 30*256);
   }
    int currentIndex=0;
    for (MonthReportSummary reportLine : report) {
       Row dataRow = sheet.createRow(2+currentIndex);
       dataRow.setHeight((short) (14*20));
       Cell dataCell;
       dataCell = dataRow.createCell(0);
       dataCell.setCellValue(reportLine.getZoneName());

       for (int i = 0; i < applications.size(); i++) {
           
           for (ApplicationReportSummary applicationReportSummary : reportLine
                   .getApplications()) {
               if (applicationReportSummary
                       .getApplicationName().equals(
                               applications.get(i))) {
                   dataCell = dataRow.createCell((i*3)+1);
                   dataCell.setCellValue(applicationReportSummary.getSalesNumber());
                   dataCell = dataRow.createCell((i*3)+2);
                   dataCell.setCellValue(applicationReportSummary.getOriginalCurrencyAmount().doubleValue());
                   dataCell = dataRow.createCell((i*3)+3);
                   dataCell.setCellValue(applicationReportSummary.getReferenceCurrencyAmount().doubleValue());

               }
           }
           
    }
       currentIndex +=1;
   }
    
    workbook.write(outPutToFile);
    outPutToFile.flush();
    outPutToFile.close();
 
    ServletContext ctx = getServletContext();
    InputStream is = ctx.getResourceAsStream("/resources/"+req.getParameter("year")+"_"+req.getParameter("month")+".xls");
    System.out.println("/resources/"+req.getParameter("year")+"_"+req.getParameter("month")+".xls");
    resp.addHeader("Content-Disposition", "attachment; filename=\"" + req.getParameter("year")+"_"+req.getParameter("month")+".xls" + "\"");
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
  private static Map<String, CellStyle> createStyles(Workbook wb){
      Map<String, CellStyle> styles = new HashMap<String, CellStyle>();
      CellStyle style;
      Font firstHeaderFont = wb.createFont();
      firstHeaderFont.setFontHeightInPoints((short)12);
      style = wb.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
      style.setFont(firstHeaderFont);
      styles.put("appFirstHeader", style);

      Font secondHeaderFont = wb.createFont();
      secondHeaderFont.setFontHeightInPoints((short)10);
      style = wb.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
      style.setFont(secondHeaderFont);
      styles.put("secondHeader", style);
      
      Font monthFont = wb.createFont();
      monthFont.setFontHeightInPoints((short)11);
      monthFont.setColor(IndexedColors.WHITE.getIndex());
      style = wb.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
      style.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
      style.setFillPattern(CellStyle.SOLID_FOREGROUND);
      style.setFont(monthFont);
      style.setWrapText(true);
      styles.put("header", style);

      style = wb.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      style.setWrapText(true);
      style.setBorderRight(CellStyle.BORDER_THIN);
      style.setRightBorderColor(IndexedColors.BLACK.getIndex());
      style.setBorderLeft(CellStyle.BORDER_THIN);
      style.setLeftBorderColor(IndexedColors.BLACK.getIndex());
      style.setBorderTop(CellStyle.BORDER_THIN);
      style.setTopBorderColor(IndexedColors.BLACK.getIndex());
      style.setBorderBottom(CellStyle.BORDER_THIN);
      style.setBottomBorderColor(IndexedColors.BLACK.getIndex());
      styles.put("cell", style);

      style = wb.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
      style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
      style.setFillPattern(CellStyle.SOLID_FOREGROUND);
      style.setDataFormat(wb.createDataFormat().getFormat("0.00"));
      styles.put("formula", style);

      style = wb.createCellStyle();
      style.setAlignment(CellStyle.ALIGN_CENTER);
      style.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
      style.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
      style.setFillPattern(CellStyle.SOLID_FOREGROUND);
      style.setDataFormat(wb.createDataFormat().getFormat("0.00"));
      styles.put("formula_2", style);

      return styles;
  }
  
}
