package com.xiaohongshu.writer.dto;

public class GenerateResponse {

    private int code;
    private String content;
    private String error;

    public static GenerateResponse success(String content) {
        GenerateResponse r = new GenerateResponse();
        r.code = 0;
        r.content = content;
        return r;
    }

    public static GenerateResponse error(int code, String msg) {
        GenerateResponse r = new GenerateResponse();
        r.code = code;
        r.error = msg;
        return r;
    }

    public int getCode() { return code; }
    public String getContent() { return content; }
    public String getError() { return error; }
}
