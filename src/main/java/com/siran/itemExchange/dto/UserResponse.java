package com.siran.itemExchange.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.siran.itemExchange.dataObjects.Users;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
public class UserResponse{

    private Integer id;
    private String username;
    @JsonIgnore
    private String password;
    private String email;
    private String phone;
    private String college;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date createdAt;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
    private Date updatedA;

    public UserResponse(Integer id, String username, String password, String email, String phone, String college, Date createdAt, Date updatedA) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.phone = phone;
        this.college = college;
        this.createdAt = createdAt;
        this.updatedA = updatedA;
    }

    public static UserResponse from(Users u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getPassword(), u.getEmail(),
                u.getPhone(), u.getCollege(), u.getCreatedAt(), u.getUpdatedAt());
    }


}