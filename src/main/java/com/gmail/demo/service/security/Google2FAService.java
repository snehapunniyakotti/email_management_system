package com.gmail.demo.service.security;

import org.springframework.stereotype.Service;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrData.Builder;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.util.Utils;

@Service
public class Google2FAService {

    private static final String ISSUER = "GmailProject";

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final CodeVerifier verifier =
            new DefaultCodeVerifier(new DefaultCodeGenerator(HashingAlgorithm.SHA1), new SystemTimeProvider());

   
	/* Generate a new secret key for the user. */
    public String generateSecretKey() { 
        return secretGenerator.generate();  
    }

	/* Generate a QR Code URL (base64 image) for Google Authenticator setup. */
    public String getQrCodeUrl(String userEmail, String secret) throws Exception {
        QrData data = new Builder()
                .label(userEmail)
                .secret(secret)
                .issuer(ISSUER) 
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        ZxingPngQrGenerator qrGenerator = new ZxingPngQrGenerator();
        byte[] imageData = qrGenerator.generate(data);
        return Utils.getDataUriForImage(imageData, qrGenerator.getImageMimeType());
    }

    
	/* Verify the TOTP code entered by user. */
    public boolean verifyCode(String secret, String code) {
        return verifier.isValidCode(secret, code);
    }
}
