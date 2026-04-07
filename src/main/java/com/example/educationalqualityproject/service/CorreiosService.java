package com.example.educationalqualityproject.service;

import com.example.educationalqualityproject.entity.Address;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CorreiosService {

    private static final String VIA_CEP_API_URL = "https://viacep.com.br/ws/";
    
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CorreiosService() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    // Constructor for testing with custom OkHttpClient
    public CorreiosService(OkHttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    public Address findAddressByCep(String cep) throws IOException {
        // Clean CEP format
        String cleanCep = cep.replaceAll("[^0-9]", "");
        
        if (cleanCep.length() != 8) {
            throw new IllegalArgumentException("CEP must have 8 digits");
        }

        String url = VIA_CEP_API_URL + cleanCep + "/json/";
        
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseBody = response.body().string();
            
            // Check if API returned an error (ViaCEP returns "erro": "true" as string)
            if (responseBody.contains("\"erro\": \"true\"") || responseBody.contains("\"erro\":\"true\"") ||
                responseBody.contains("\"erro\": true") || responseBody.contains("\"erro\":true")) {
                throw new IOException("CEP not found");
            }

            return objectMapper.readValue(responseBody, Address.class);
        }
    }
}
