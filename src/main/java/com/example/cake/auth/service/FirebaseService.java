package com.example.cake.auth.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Service
public class FirebaseService {

    @PostConstruct
    public void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) { // tránh khởi tạo nhiều lần
                InputStream serviceAccount = null;

                // 1. Kiểm tra biến môi trường custom
                String externalPath = System.getenv("FIREBASE_CREDENTIAL_PATH");
                
                // 2. Kiểm tra đường dẫn Secret File mặc định của Render (/etc/secrets/serviceAccountKey.json)
                File renderSecretFile = new File("/etc/secrets/serviceAccountKey.json");

                if (externalPath != null && !externalPath.isEmpty()) {
                    System.out.println("🚀 [FirebaseService] Load key từ biến môi trường: " + externalPath);
                    serviceAccount = new FileInputStream(externalPath);
                } else if (renderSecretFile.exists()) {
                    System.out.println("🚀 [FirebaseService] Load key từ Render Secret File (/etc/secrets/serviceAccountKey.json)");
                    serviceAccount = new FileInputStream(renderSecretFile);
                } else {
                    ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
                    if (resource.exists()) {
                        System.out.println("🚀 [FirebaseService] Load key từ local resources.");
                        serviceAccount = resource.getInputStream();
                    } else {
                        System.out.println("⚠️ [FirebaseService] Không tìm thấy serviceAccountKey.json. Bỏ qua khởi tạo Firebase Admin.");
                        return;
                    }
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("✅ [FirebaseService] Firebase Admin SDK đã khởi tạo thành công!");
            }
        } catch (Exception e) {
            System.err.println("⚠️ [FirebaseService] Lỗi khởi tạo Firebase: " + e.getMessage());
        }
    }

    public FirebaseToken verifyToken(String idToken) throws FirebaseAuthException {
        if (FirebaseApp.getApps().isEmpty()) {
            throw new IllegalStateException("Firebase App chưa được cấu hình key dịch vụ trên server cloud.");
        }
        return FirebaseAuth.getInstance().verifyIdToken(idToken);
    }
}
