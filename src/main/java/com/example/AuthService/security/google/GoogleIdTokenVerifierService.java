package com.example.AuthService.security.google;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GoogleIdTokenVerifierService {

    private final GoogleOAuthProperties props;

    public GoogleIdToken.Payload verify(String idTokenString) throws Exception {
        List<String> audience = props.getClientIdList();
        if (audience.isEmpty()) {
            throw new IllegalStateException("google.client-ids is empty");
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance())
                .setAudience(audience)
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            return null; // invalid
        }
        return idToken.getPayload();
    }
}
