package com.esprit.microservice.pidev.Event.Services;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
public class GeocodingService {

    private final RestTemplate restTemplate;

    public GeocodingService(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public double[] geocodeAddress(String address) {
        try {
            String encodedAddress = URLEncoder.encode(address, StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search"
                    + "?q=" + encodedAddress
                    + "&format=json&limit=1";

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "YourAppName/1.0 contact@yourapp.com"); // OBLIGATOIRE pour Nominatim
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            List<Map<String, Object>> results = response.getBody();
            if (results != null && !results.isEmpty()) {
                Map<String, Object> first = results.get(0);
                double lat = Double.parseDouble((String) first.get("lat"));
                double lon = Double.parseDouble((String) first.get("lon"));
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
            System.err.println("Geocoding failed for: " + address + " → " + e.getMessage());
        }
        return null; // Pas trouvé
    }
}
