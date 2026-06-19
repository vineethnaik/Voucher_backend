package com.voucherpro.service;

import com.voucherpro.dto.VoucherRequest;
import com.voucherpro.model.Voucher;
import com.voucherpro.repository.VoucherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;

    public VoucherService(VoucherRepository voucherRepository) {
        this.voucherRepository = voucherRepository;
    }

    public List<Voucher> findAll() {
        return voucherRepository.findAll();
    }

    public Voucher findById(String id) {
        return voucherRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found."));
    }

    public Voucher create(VoucherRequest request) {
        validatePrices(request);

        Voucher voucher = mapToEntity(new Voucher(), request);
        voucher.setId(generateUniqueId(request.getBadge(), request.getTitle()));
        return voucherRepository.save(voucher);
    }

    public Voucher update(String id, VoucherRequest request) {
        validatePrices(request);

        Voucher existing = findById(id);
        mapToEntity(existing, request);
        return voucherRepository.save(existing);
    }

    public void delete(String id) {
        if (!voucherRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Voucher not found.");
        }
        voucherRepository.deleteById(id);
    }

    private void validatePrices(VoucherRequest request) {
        if (request.getDiscountPrice() >= request.getOriginalPrice()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Discount price must be lower than original price."
            );
        }
    }

    private Voucher mapToEntity(Voucher voucher, VoucherRequest request) {
        voucher.setTitle(request.getTitle().trim());
        voucher.setProvider(request.getProvider().trim());
        voucher.setIconName(request.getIconName().trim());
        voucher.setOriginalPrice(request.getOriginalPrice());
        voucher.setDiscountPrice(request.getDiscountPrice());
        voucher.setDescription(request.getDescription().trim());
        voucher.setBadge(request.getBadge().trim());
        voucher.setExpiryDate(request.getExpiryDate().trim());
        voucher.setVoucherAmount(request.getVoucherAmount());
        voucher.setRequirements(
                request.getRequirements().stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList()
        );
        return voucher;
    }

    private String generateUniqueId(String badge, String title) {
        String base = slugify(badge);
        if (base.isEmpty()) {
            base = slugify(title);
        }
        if (base.isEmpty()) {
            base = "voucher";
        }

        if (!voucherRepository.existsById(base)) {
            return base;
        }

        int counter = 2;
        while (voucherRepository.existsById(base + "-" + counter)) {
            counter++;
        }
        return base + "-" + counter;
    }

    private String slugify(String value) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
