package com.lahat.muolana.web;

import com.lahat.muolana.analytics.AnalyticsAPI;
import com.lahat.muolana.analytics.domain.QuerySummaryVM;
import com.lahat.muolana.legaldocuments.LegalDocumentsAPI;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class LandingController {

    private final LegalDocumentsAPI legalDocumentsAPI;
    private final AnalyticsAPI analyticsAPI;

    LandingController(LegalDocumentsAPI legalDocumentsAPI, AnalyticsAPI analyticsAPI) {
        this.legalDocumentsAPI = legalDocumentsAPI;
        this.analyticsAPI = analyticsAPI;
    }

    @GetMapping("/")
    String landing(Model model) {
        long ingested = legalDocumentsAPI.countIngested();
        model.addAttribute("actsIngested", ingested > 0 ? ingested + "+" : "0");

        try {
            QuerySummaryVM summary = analyticsAPI.allTimeSummary();
            long total = summary.total();
            long answered = summary.answered();
            long notFound = summary.notFound();

            String citedPct = total > 0
                    ? Math.round(answered * 100.0 / total) + "%"
                    : "100%";
            String notFoundPct = total > 0
                    ? Math.round(notFound * 100.0 / total) + "%"
                    : "0%";

            model.addAttribute("citedAnswers", citedPct);
            model.addAttribute("noAnswers", notFoundPct);
            model.addAttribute("totalQueries", total > 0 ? total + "+" : "0");
        } catch (Exception ignored) {
            model.addAttribute("citedAnswers", "100%");
            model.addAttribute("noAnswers", "0%");
            model.addAttribute("totalQueries", "0");
        }

        return "landing";
    }

    @GetMapping("/chat")
    String chat() {
        return "chat";
    }

    @GetMapping("/how-it-works")
    String howItWorks() {
        return "how-it-works";
    }

    @GetMapping("/lawyers")
    String lawyers() {
        return "lawyers";
    }

    @GetMapping("/lawyers/{lawyerId}")
    String lawyerProfile() {
        return "lawyer-profile";
    }

    @GetMapping("/admin")
    String adminRoot() {
        return "redirect:/admin/login";
    }

    @GetMapping("/admin/login")
    String adminLogin() {
        return "admin/login";
    }

    @GetMapping("/admin/dashboard")
    String adminDashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/admin/documents")
    String adminDocuments() {
        return "admin/documents";
    }

    @GetMapping("/admin/lawyers")
    String adminLawyers() {
        return "admin/lawyers";
    }

    @GetMapping("/admin/analytics")
    String adminAnalytics() {
        return "admin/analytics";
    }
}
