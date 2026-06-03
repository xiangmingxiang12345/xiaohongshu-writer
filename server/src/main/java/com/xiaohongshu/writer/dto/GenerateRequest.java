package com.xiaohongshu.writer.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class GenerateRequest {

    private String type;

    @NotBlank(message = "主题不能为空")
    private String topic;

    private List<String> images;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
}
