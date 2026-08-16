package com.nailic.sproochencoach.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageBody {
    private AIRoleEnum role;
    public String content;
}
