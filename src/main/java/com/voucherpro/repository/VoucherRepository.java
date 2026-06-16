package com.voucherpro.repository;

import com.voucherpro.model.Voucher;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VoucherRepository extends MongoRepository<Voucher, String> {

    boolean existsByBadgeIgnoreCase(String badge);
}
