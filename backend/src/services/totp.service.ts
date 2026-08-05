import { authenticator } from "otplib";
import QRCode from "qrcode";
import crypto from "node:crypto";

authenticator.options = {
  window: 1,
};

export const totpService = {
  generateSecret(): string {
    return authenticator.generateSecret();
  },

  async generateQRCode(userEmail: string, secret: string): Promise<string> {
    const serviceName = "Fluxa";
    const otpauth = authenticator.keyuri(userEmail, serviceName, secret);
    return QRCode.toDataURL(otpauth);
  },

  verifyToken(token: string, secret: string): boolean {
    try {
      return authenticator.verify({ token, secret });
    } catch (err) {
      return false;
    }
  },

  generateRecoveryCodes(): string[] {
    const codes: string[] = [];
    for (let i = 0; i < 10; i++) {
      codes.push(crypto.randomBytes(4).toString("hex"));
    }
    return codes;
  },
};
