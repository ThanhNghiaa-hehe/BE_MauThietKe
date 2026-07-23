package com.example.cake.auth.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String id;                          //id user
    @org.springframework.data.mongodb.core.index.Indexed(unique = true)
    private String email;                       //email
    private String fullname ;                   // Tên uer
    private String password ;                   // mật khẩu
    private String phoneNumber;                 // số điện thoại
    private String role;
    private boolean active;
    private String gender;                      // Giới tính
    private LocalDate dateOfBirth;              // ngay sinh nam sinh
    private LocalDate createdAt;                // Ngày tao tai khoan
    private String avatarUrl;                    // Link ảnh đại diện
    private Address address;                     // địa chỉ ;
    private AutheProvider authProvider;          // loại tài khoản


}
