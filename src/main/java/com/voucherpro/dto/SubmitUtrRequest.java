package com.voucherpro.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SubmitUtrRequest {

    @NotBlank(message = "UTR is required")
    @Size(min = 8, max = 50, message = "UTR must be between 8 and 50 characters")
    private String utr;

    public SubmitUtrRequest() {
    }

    public SubmitUtrRequest(String utr) {
        this.utr = utr;
    }

    public String getUtr() {
        return utr;
    }

    public void setUtr(String utr) {
        this.utr = utr;
    }
}
