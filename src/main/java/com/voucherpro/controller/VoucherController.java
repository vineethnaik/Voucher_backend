package com.voucherpro.controller;

import com.voucherpro.dto.VoucherRequest;
import com.voucherpro.model.Voucher;
import com.voucherpro.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @GetMapping
    public List<Voucher> listVouchers() {
        return voucherService.findAll();
    }

    @GetMapping("/{id}")
    public Voucher getVoucher(@PathVariable String id) {
        return voucherService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public Voucher createVoucher(@Valid @RequestBody VoucherRequest request) {
        return voucherService.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Voucher updateVoucher(@PathVariable String id, @Valid @RequestBody VoucherRequest request) {
        return voucherService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVoucher(@PathVariable String id) {
        voucherService.delete(id);
    }
}
