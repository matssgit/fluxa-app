import nodemailer from "nodemailer";

const transporter = nodemailer.createTransport({
  host: process.env.SMTP_HOST,
  port: Number(process.env.SMTP_PORT) || 587,
  secure: process.env.SMTP_SECURE === "true",
  auth: {
    user: process.env.SMTP_USER,
    pass: process.env.SMTP_PASSWORD,
  },
});

export const emailService = {
  async sendVerificationEmail(to: string, token: string): Promise<void> {
    const frontendUrl = process.env.FRONTEND_URL || "http://localhost:5173";
    const verificationLink = `${frontendUrl}/verify-email?token=${token}`;

    console.log({
      host: process.env.SMTP_HOST,
      port: process.env.SMTP_PORT,
      user: process.env.SMTP_USER,
    });

    await transporter.sendMail({
      from: process.env.EMAIL_FROM || '"Fluxa" <noreply@fluxa.com>',
      to,
      subject: "Fluxa - Confirme seu endereço de e-mail",
      html: `
        <h2>Bem-vindo ao Fluxa!</h2>
        <p>Para ativar sua conta, por favor confirme que este e-mail pertence a você clicando no link abaixo:</p>
        <a href="${verificationLink}" style="display:inline-block;padding:10px 20px;background:#007ACC;color:#fff;text-decoration:none;border-radius:5px;">Verificar meu e-mail</a>
        <p>Se você não criou uma conta no Fluxa, desconsidere esta mensagem.</p>
      `,
    });

    await transporter.verify();
    console.log("SMTP OK");
  },

  async sendPasswordReset(to: string, token: string): Promise<void> {
    const frontendUrl = process.env.FRONTEND_URL || "http://localhost:5173";
    const resetLink = `${frontendUrl}/reset-password?token=${token}`;

    await transporter.sendMail({
      from: process.env.EMAIL_FROM || '"Fluxa" <noreply@fluxa.com>',
      to,
      subject: "Fluxa - Redefinição de senha",
      html: `
        <h2>Redefinição de senha</h2>
        <p>Você solicitou a redefinição da sua senha no Fluxa. Clique no botão abaixo para criar uma nova senha:</p>
        <a href="${resetLink}" style="display:inline-block;padding:10px 20px;background:#007ACC;color:#fff;text-decoration:none;border-radius:5px;">Criar nova senha</a>
        <p>Este link é válido por 30 minutos. Se você não solicitou esta alteração, desconsidere este e-mail.</p>
      `,
    });
  },
};
