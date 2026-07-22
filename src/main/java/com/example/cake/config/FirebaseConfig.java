package com.example.cake.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() {
        try {
            InputStream serviceAccount = null;

            // 1. đọc biến môi trường
            String externalPath = System.getenv("FIREBASE_CREDENTIAL_PATH");
            // 2. Render Secret File
            File renderSecretFile = new File("/etc/secrets/serviceAccountKey.json");

            if (externalPath != null && !externalPath.isEmpty()) {
                System.out.println("🚀 Firebase đang chạy với file bên ngoài Docker: " + externalPath);
                serviceAccount = new FileInputStream(externalPath);
            } else if (renderSecretFile.exists()) {
                System.out.println("🚀 Firebase đang chạy với Render Secret File.");
                serviceAccount = new FileInputStream(renderSecretFile);
            } else {
                ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
                if (resource.exists()) {
                    System.out.println("🚀 Firebase đang chạy với file trong resources.");
                    serviceAccount = resource.getInputStream();
                } else {
                    System.out.println("⚠️ serviceAccountKey.json không tìm thấy. Bỏ qua khởi tạo Firebase Admin.");
                    return;
                }
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                System.out.println("✅ Firebase đã được khởi tạo thành công!");
            }
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khởi tạo Firebase Admin (có thể bỏ qua nếu dùng Client Auth): " + e.getMessage());
        }
    }
}
