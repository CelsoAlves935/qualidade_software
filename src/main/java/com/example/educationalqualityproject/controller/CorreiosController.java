package com.example.educationalqualityproject.controller;

import com.example.educationalqualityproject.entity.Address;
import com.example.educationalqualityproject.service.CorreiosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
public class CorreiosController {

    @Autowired
    private CorreiosService correiosService;

    @GetMapping("/correios")
    public String showCepLookupForm() {
        return "correios/cep-lookup";
    }

    @PostMapping("/correios/buscar")
    public String searchAddressByCep(@RequestParam String cep, Model model, RedirectAttributes redirectAttributes) {
        try {
            Address address = correiosService.findAddressByCep(cep);
            model.addAttribute("address", address);
            model.addAttribute("success", true);
            return "correios/cep-lookup";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", "CEP inválido. O CEP deve ter 8 dígitos.");
            return "redirect:/correios";
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("error", "Erro ao buscar CEP: " + e.getMessage());
            return "redirect:/correios";
        }
    }
}
