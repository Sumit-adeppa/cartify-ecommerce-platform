package com.example.demo.controller;

import com.example.demo.dto.AnalyticsSummaryResponse;
import com.example.demo.dto.DailySalesResponse;
import com.example.demo.service.AdminService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/business")
public class AdminBusinessController {

    private final AdminService adminService;

    public AdminBusinessController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/monthly")
    public AnalyticsSummaryResponse getMonthlyBusiness(
            @RequestParam int month,
            @RequestParam int year) {

        return adminService.getMonthlyBusiness(month, year);
    }

    @GetMapping("/daily")
    public AnalyticsSummaryResponse getDailyBusiness(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return adminService.getDailyBusiness(date);
    }

    @GetMapping("/yearly")
    public AnalyticsSummaryResponse getYearlyBusiness(
            @RequestParam int year) {

        return adminService.getYearlyBusiness(year);
    }

    @GetMapping("/overall")
    public AnalyticsSummaryResponse getOverallBusiness() {
        return adminService.getOverallBusiness();
    }

    @GetMapping("/daily-sales")
    public List<DailySalesResponse> getDailySales(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {

        return adminService.getDailySales(from, to);
    }
}