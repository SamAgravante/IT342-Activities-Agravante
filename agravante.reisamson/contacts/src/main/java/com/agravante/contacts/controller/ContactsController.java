package com.agravante.contacts.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.util.List;
import java.util.Map;

@Controller
public class ContactsController {
	private final OAuth2AuthorizedClientService clientService;
	
    @GetMapping("/user-info")
    public Map<String, Object> getUser(@AuthenticationPrincipal OAuth2User principal){
        return principal.getAttributes();
    }
    
    public ContactsController(OAuth2AuthorizedClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping("/contacts")
    public String getContacts(Model model, @AuthenticationPrincipal OAuth2User user) throws Exception {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("google", user.getName());
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(
                request -> request.setHeaders(new HttpHeaders().setAuthorization("Bearer " + client.getAccessToken().getTokenValue()))
        );

        GenericUrl url = new GenericUrl("https://people.googleapis.com/v1/people/me/connections?personFields=names,phoneNumbers");
        HttpResponse response = requestFactory.buildGetRequest(url).execute();
        
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Object> contacts = mapper.readValue(response.getContent(), new TypeReference<Map<String, Object>>() {});
        
        model.addAttribute("contacts", contacts);
        return "contacts";
    }
    
    
    
    //test
    @PostMapping("/contacts/create")
    public String createContact(@RequestParam String name, @RequestParam String phone, Model model, @AuthenticationPrincipal OAuth2User user) throws Exception {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("google", user.getName());
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(
            request -> request.setHeaders(new HttpHeaders().setAuthorization("Bearer " + client.getAccessToken().getTokenValue()))
        );

        GenericUrl url = new GenericUrl("https://people.googleapis.com/v1/people:createContact");
        Map<String, Object> requestBody = Map.of(
            "names", List.of(Map.of("givenName", name)),
            "phoneNumbers", List.of(Map.of("value", phone))
        );

        HttpContent content = new JsonHttpContent(new JacksonFactory(), requestBody);
        requestFactory.buildPostRequest(url, content).execute();

        return "redirect:/contacts";
    }

    @PostMapping("/contacts/update")
    public String updateContact(@RequestParam String resourceName, @RequestParam String name, @RequestParam String phone, Model model, @AuthenticationPrincipal OAuth2User user) throws Exception {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("google", user.getName());
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(
            request -> request.setHeaders(new HttpHeaders().setAuthorization("Bearer " + client.getAccessToken().getTokenValue()))
        );

        GenericUrl url = new GenericUrl("https://people.googleapis.com/v1/" + resourceName + "?updatePersonFields=names,phoneNumbers");
        Map<String, Object> requestBody = Map.of(
            "names", List.of(Map.of("givenName", name)),
            "phoneNumbers", List.of(Map.of("value", phone))
        );

        HttpContent content = new JsonHttpContent(new JacksonFactory(), requestBody);
        requestFactory.buildPatchRequest(url, content).execute();

        return "redirect:/contacts";
    }

    @PostMapping("/contacts/delete")
    public String deleteContact(@RequestParam String resourceName, Model model, @AuthenticationPrincipal OAuth2User user) throws Exception {
        OAuth2AuthorizedClient client = clientService.loadAuthorizedClient("google", user.getName());
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory(
            request -> request.setHeaders(new HttpHeaders().setAuthorization("Bearer " + client.getAccessToken().getTokenValue()))
        );

        GenericUrl url = new GenericUrl("https://people.googleapis.com/v1/" + resourceName + ":deleteContact");
        requestFactory.buildDeleteRequest(url).execute();

        return "redirect:/contacts";
    }
}

