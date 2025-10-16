package org.example.naverapi.controller;
import org.example.naverapi.service.NaverMailService;
import org.example.naverapi.dto.MailDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MailController {

    private final NaverMailService naverMailService;

    public MailController(NaverMailService naverMailService) {
        this.naverMailService = naverMailService;
    }

    // 사용자가 http://localhost:8080/ 경로로 접속 가능
    @GetMapping("/")
    public String getMails(Model model) {
        List<MailDto> emails = naverMailService.fetchEmails();
        model.addAttribute("emails", emails);
        return "index";
    }
}
