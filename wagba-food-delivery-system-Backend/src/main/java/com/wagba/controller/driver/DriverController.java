package com.wagba.controller.driver;

import com.wagba.dto.driver.DriverLocationRequest;
import com.wagba.dto.driver.DriverProfileRequest;
import com.wagba.dto.driver.DriverResponse;
import com.wagba.dto.wallet.WalletResponse;
import com.wagba.dto.wallet.WithdrawRequest;
import com.wagba.security.SecurityUtil;
import com.wagba.service.DriverService;
import com.wagba.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/driver")
@PreAuthorize("hasRole('DRIVER')")
public class DriverController {

    private final DriverService driverService;
    private final WalletService walletService;

    public DriverController(DriverService driverService, WalletService walletService) {
        this.driverService = driverService;
        this.walletService = walletService;
    }

    @PostMapping("/profile")
    public ResponseEntity<String> completeDriverProfile(@Valid @RequestBody DriverProfileRequest request) {
        // The userId is taken from the JWT, never from the request.
        driverService.completeDriverProfile(SecurityUtil.getCurrentUserEmail(), request);
        return ResponseEntity.ok("Driver profile submitted successfully");
    }

    @PostMapping("/location")
    public ResponseEntity<String> updateLocation(@RequestBody DriverLocationRequest request) {
        driverService.updateLocation(SecurityUtil.getCurrentUserEmail(), request.getLatitude(), request.getLongitude());
        return ResponseEntity.ok("Location updated");
    }

    @PutMapping("/profile")
    public ResponseEntity<String> updateProfile(@RequestBody DriverProfileRequest request) {
        driverService.updateDriverProfile(SecurityUtil.getCurrentUserEmail(), request);
        return ResponseEntity.ok("Driver profile updated");
    }

    @GetMapping("/profile")
    public DriverResponse getProfile() {
        return driverService.getDriverProfile(SecurityUtil.getCurrentUserEmail());
    }

    @GetMapping("/wallet")
    public WalletResponse myWallet() {
        return walletService.getWalletInfo(SecurityUtil.getCurrentUserEmail());
    }

    @PostMapping("/wallet/withdraw")
    public Map<String, Object> withdraw(@RequestBody WithdrawRequest request) {
        return walletService.withdraw(SecurityUtil.getCurrentUserEmail(), request.getAmount());
    }
}