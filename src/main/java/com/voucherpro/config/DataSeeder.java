package com.voucherpro.config;

import com.voucherpro.model.User;
import com.voucherpro.model.UserRole;
import com.voucherpro.model.Voucher;
import com.voucherpro.repository.UserRepository;
import com.voucherpro.repository.VoucherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final VoucherRepository voucherRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminProperties adminProperties;

    public DataSeeder(
            UserRepository userRepository,
            VoucherRepository voucherRepository,
            PasswordEncoder passwordEncoder,
            AdminProperties adminProperties
    ) {
        this.userRepository = userRepository;
        this.voucherRepository = voucherRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void seedData() {
        try {
            seedAdminUser();
            seedVouchers();
        } catch (Exception ex) {
            log.error("Database seeding skipped: {}", ex.getMessage());
            log.error("Verify MONGODB_URI in backend/.env matches your Atlas username and password.");
        }
    }

    private void seedAdminUser() {
        String email = adminProperties.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                if (user.getRole() != UserRole.ADMIN) {
                    user.setRole(UserRole.ADMIN);
                    userRepository.save(user);
                    log.info("Promoted existing user {} to ADMIN", email);
                }
            });
            return;
        }

        User admin = new User(
                email,
                adminProperties.getName(),
                passwordEncoder.encode(adminProperties.getPassword()),
                UserRole.ADMIN
        );
        userRepository.save(admin);
        log.info("Seeded admin user: {}", email);
    }

    private void seedVouchers() {
        if (voucherRepository.count() > 0) {
            return;
        }

        List<Voucher> defaults = List.of(
                buildVoucher("aws-saa", "AWS Certified Solutions Architect - Associate", "AWS", "Cloud",
                        150, 75, "Validates ability to design and deploy secure and robust applications on AWS technologies.",
                        "SAA-C03", List.of("Basic cloud architecture knowledge", "1 year AWS hands-on recommended")),
                buildVoucher("gcp-pca", "Google Cloud Professional Cloud Architect", "Google Cloud", "Server",
                        200, 100, "Enables organizations to leverage Google Cloud technologies. Validates proficiency in designing cloud solutions.",
                        "PCA-2026", List.of("Google Cloud console familiarity", "3+ years industry experience recommended")),
                buildVoucher("comptia-sec", "CompTIA Security+", "CompTIA", "Shield",
                        392, 196, "The premier global baseline cybersecurity credential verifying core knowledge required of any cybersecurity role.",
                        "SY0-701", List.of("Core security protocol understanding", "9 months network security experience recommended")),
                buildVoucher("sf-admin", "Salesforce Certified Administrator", "Salesforce", "Users",
                        200, 100, "Designed for those who have experience with Salesforce administration, configuration, and data management.",
                        "ADM-201", List.of("Salesforce lightning platform familiarity", "6 months admin experience recommended")),
                buildVoucher("az-900", "Microsoft Certified: Azure Fundamentals", "Azure", "Cpu",
                        99, 49, "Validates foundational knowledge of cloud services and how those services are provided with Microsoft Azure.",
                        "AZ-900", List.of("General IT concepts", "Basic cloud service models")),
                buildVoucher("aws-ccp", "AWS Certified Cloud Practitioner", "AWS", "Globe",
                        100, 50, "Provides an overall understanding of AWS Cloud platform core services, pricing models, and security structures.",
                        "CLF-C02", List.of("No prior technical experience required", "6 months basic AWS knowledge recommended"))
        );

        voucherRepository.saveAll(defaults);
        log.info("Seeded {} default vouchers", defaults.size());
    }

    private Voucher buildVoucher(
            String id,
            String title,
            String provider,
            String iconName,
            double originalPrice,
            double discountPrice,
            String description,
            String badge,
            List<String> requirements
    ) {
        Voucher voucher = new Voucher();
        voucher.setId(id);
        voucher.setTitle(title);
        voucher.setProvider(provider);
        voucher.setIconName(iconName);
        voucher.setOriginalPrice(originalPrice);
        voucher.setDiscountPrice(discountPrice);
        voucher.setDescription(description);
        voucher.setBadge(badge);
        voucher.setRequirements(requirements);
        return voucher;
    }
}
