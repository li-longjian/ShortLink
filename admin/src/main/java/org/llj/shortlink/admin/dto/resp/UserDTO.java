package org.llj.shortlink.admin.dto.resp;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import org.llj.shortlink.admin.common.UserInfoDesensitization.PhoneDesensitizationSerializer;

@Data
public class UserDTO {
    private long id;
    private String username;
    private String realName;

    @JsonSerialize(using = PhoneDesensitizationSerializer.class)
    private String phone;
    private String mail;
}
