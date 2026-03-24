package com.dustopus.authenticationservice.common;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;

@Data
@Accessors(chain = true)
public class result<T> {

    private int code;

    private String msg;

    private T data;

    public static<T> result<T> ok(T data){
        result<T> r = new result<>();

        return r.setCode(HttpStatus.OK.value());
    }

    public static<T> result<T> databaseError(String msg){
        result<T> r = new result<>();

        return r.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value()).setMsg(msg);
    }


}
