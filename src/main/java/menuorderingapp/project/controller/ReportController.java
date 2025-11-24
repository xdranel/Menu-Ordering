package menuorderingapp.project.controller;

import jakarta.servlet.http.HttpSession;
import menuorderingapp.project.model.dto.ApiResponse;
import menuorderingapp.project.model.dto.ReportRequest;
import menuorderingapp.project.service.ReportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController extends BaseController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }


    @PostMapping("/sales")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateSalesReport(
            @RequestBody ReportRequest reportRequest,
            HttpSession session) {

        if (session.getAttribute("cashier") == null) {
            return unauthorized("Not authenticated");
        }

        try {
            LocalDateTime startDate = reportRequest.getStartDate().atStartOfDay();
            LocalDateTime endDate = reportRequest.getEndDate().atTime(23, 59, 59);

            Map<String, Object> report = reportService.getSalesReport(startDate, endDate);
            return success(report);

        } catch (Exception e) {
            return error("Failed to generate sales report: " + e.getMessage());
        }
    }


    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDailyReport(
            @RequestParam String date,
            HttpSession session) {

        if (session.getAttribute("cashier") == null) {
            return unauthorized("Not authenticated");
        }

        try {
            LocalDate reportDate = LocalDate.parse(date);
            Map<String, Object> report = reportService.getDailySalesReport(reportDate);
            return success(report);

        } catch (Exception e) {
            return error("Failed to generate daily report: " + e.getMessage());
        }
    }


    @GetMapping("/top-items")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopSellingItems(
            @RequestParam String startDate,
            @RequestParam String endDate,
            HttpSession session) {

        if (session.getAttribute("cashier") == null) {
            return unauthorized("Not authenticated");
        }

        try {
            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);

            var topItems = reportService.getTopSellingItems(start, end);
            return success(topItems);

        } catch (Exception e) {
            return error("Failed to get top items: " + e.getMessage());
        }
    }


    @GetMapping("/export")
    public ResponseEntity<Resource> exportReport(
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String format,
            HttpSession session) {

        if (session.getAttribute("cashier") == null) {
            return ResponseEntity.status(401).build();
        }

        try {

            LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
            LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
            Map<String, Object> report = reportService.getSalesReport(start, end);


            String pdfContent = generatePdfContent(report);
            byte[] pdfBytes = pdfContent.getBytes();

            ByteArrayResource resource = new ByteArrayResource(pdfBytes);

            String filename = String.format("sales-report-%s-to-%s.pdf", startDate, endDate);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String generatePdfContent(Map<String, Object> report) {


        StringBuilder content = new StringBuilder();
        content.append("SALES REPORT\n");
        content.append("Generated on: ").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("\n\n");

        if (report.containsKey("totalRevenue")) {
            content.append("Total Revenue: ").append(report.get("totalRevenue")).append("\n");
        }
        if (report.containsKey("totalOrders")) {
            content.append("Total Orders: ").append(report.get("totalOrders")).append("\n");
        }
        if (report.containsKey("averageOrderValue")) {
            content.append("Average Order Value: ").append(report.get("averageOrderValue")).append("\n");
        }

        return content.toString();
    }
}
