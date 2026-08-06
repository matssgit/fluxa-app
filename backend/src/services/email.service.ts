import nodemailer from "nodemailer";
import { env } from "../env/index.js";

const smtpConfig = {
  host: env.SMTP_HOST,
  port: env.SMTP_PORT,
  secure: false,
  auth: {
    user: env.SMTP_USER,
    pass: env.SMTP_PASSWORD,
  },
};

const transporter = nodemailer.createTransport(smtpConfig);

transporter
  .verify()
  .then(() => {
    console.log("SMTP conectado com sucesso!");
  })
  .catch((error) => {
    console.error("SMTP ERROR:", error);
  });

export const emailService = {
  async sendVerificationEmail(to: string, token: string): Promise<void> {
    const verificationLink = `${env.FRONTEND_URL}/verify-email?token=${token}`;

    try {
      const info = await transporter.sendMail({
        from: env.EMAIL_FROM,
        to,
        subject: "Fluxa - Confirme seu endereço de e-mail",
        html: `
          <h2>Bem-vindo ao Fluxa!</h2>
          <p>Para ativar sua conta, por favor confirme que este e-mail pertence a você clicando no link abaixo:</p>
          <a href="${verificationLink}" style="display:inline-block;padding:10px 20px;background:#007ACC;color:#fff;text-decoration:none;border-radius:5px;">Verificar meu e-mail</a>
          <p>Se você não criou uma conta no Fluxa, desconsidere esta mensagem.</p>
        `,
      });
      console.log("Email enviado:", info);
    } catch (error) {
      console.error("[SMTP ERROR] Falha ao enviar verificação:", error);
      throw error;
    }
  },

  async sendPasswordReset(to: string, token: string): Promise<void> {
    const resetLink = `${env.FRONTEND_URL}/reset-password?token=${token}`;

    const info = await transporter.sendMail({
      from: env.EMAIL_FROM,
      to,
      subject: "Fluxa - Redefinição de senha",
      html: `
        <h2>Redefinição de senha</h2>
        <p>Você solicitou a redefinição da sua senha no Fluxa. Clique no botão abaixo para criar uma nova senha:</p>
        <a href="${resetLink}" style="display:inline-block;padding:10px 20px;background:#007ACC;color:#fff;text-decoration:none;border-radius:5px;">Criar nova senha</a>
        <p>Este link é válido por 30 minutos. Se você não solicitou esta alteração, desconsidere este e-mail.</p>
      `,
    });
    console.log("Email enviado:", info);
  },
};
