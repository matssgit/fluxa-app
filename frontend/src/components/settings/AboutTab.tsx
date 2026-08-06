import {
  CheckCircle,
  Terminal,
  Layout,
  Code2,
  Database,
  Palette,
  Shield,
  Key,
} from "lucide-react";

const GithubIcon = ({
  size = 18,
  className = "",
}: {
  size?: number;
  className?: string;
}) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
  >
    <path d="M15 22v-4a4.8 4.8 0 0 0-1-3.5c3 0 6-2 6-5.5.08-1.25-.27-2.48-1-3.5.28-1.15.28-2.35 0-3.5 0 0-1 0-3 1.5-2.64-.5-5.36-.5-8 0C6 2 5 2 5 2c-.3 1.15-.3 2.35 0 3.5A5.403 5.403 0 0 0 4 9c0 3.5 3 5.5 6 5.5-.39.49-.68 1.05-.85 1.65-.17.6-.22 1.23-.15 1.85v4" />
    <path d="M9 18c-4.51 2-5-2-7-2" />
  </svg>
);

const LinkedinIcon = ({
  size = 18,
  className = "",
}: {
  size?: number;
  className?: string;
}) => (
  <svg
    xmlns="http://www.w3.org/2000/svg"
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="2"
    strokeLinecap="round"
    strokeLinejoin="round"
    className={className}
  >
    <path d="M16 8a6 6 0 0 1 6 6v7h-4v-7a2 2 0 0 0-2-2 2 2 0 0 0-2 2v7h-4v-7a6 6 0 0 1 6-6z" />
    <rect width="4" height="12" x="2" y="9" />
    <circle cx="4" cy="4" r="2" />
  </svg>
);

export function AboutTab() {
  return (
    <div className="space-y-10 animate-fade-in max-w-3xl">
      <div className="flex flex-col items-center text-center pb-8 border-b border-subtle/20">
        <div className="w-20 h-20 rounded-3xl bg-brand text-white flex items-center justify-center shadow-lg mb-6 rotate-3 hover:rotate-0 transition-transform">
          <span className="text-4xl font-black italic">F</span>
        </div>
        <h2 className="text-3xl font-extrabold text-primary tracking-tight mb-2">
          Fluxa
        </h2>
        <span className="px-3 py-1 bg-brand/10 text-brand text-xs font-bold rounded-full border border-brand/20">
          MVP Versão 1.0
        </span>
        <p className="text-sm text-secondary mt-6 max-w-xl leading-relaxed">
          O Fluxa nasceu da necessidade de ter uma ferramenta financeira
          realmente simples, rápida e confiável para o controle das finanças
          pessoais. Ao longo do desenvolvimento, o projeto deixou de ser apenas
          um gerenciador financeiro e passou a servir também como laboratório
          para aplicação de boas práticas de arquitetura de software,
          autenticação, segurança e experiência do usuário.
        </p>
      </div>

      <div>
        <h3 className="text-lg font-bold text-primary mb-6 flex items-center gap-2">
          <CheckCircle size={20} className="text-brand" /> Funcionalidades
        </h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {[
            "Controle de receitas e despesas",
            "Categorias e contas personalizadas",
            "Gestão de cartões de crédito",
            "Parcelamentos",
            "Dashboard financeiro",
            "Autenticação segura com verificação em duas etapas (2FA)",
            "Recuperação de senha por e-mail",
            "Diversas camadas de proteção contra fraudes e acessos indevidos",
          ].map((feature, i) => (
            <div
              key={i}
              className="flex items-start gap-3 p-4 rounded-2xl bg-elevated/40 border border-subtle/30"
            >
              <div className="p-1 rounded-full bg-brand/10 text-brand shrink-0 mt-0.5">
                <CheckCircle size={14} />
              </div>
              <span className="text-sm font-medium text-primary">
                {feature}
              </span>
            </div>
          ))}
        </div>
        <p className="text-sm font-medium text-secondary mt-6">
          Este projeto continua em constante evolução e novas funcionalidades
          serão adicionadas ao longo do tempo.
        </p>
      </div>

      <div>
        <h3 className="text-lg font-bold text-primary mb-6 flex items-center gap-2">
          <Terminal size={20} className="text-brand" /> Sobre o Desenvolvedor
        </h3>
        <div className="p-6 sm:p-8 rounded-3xl bg-surface border border-subtle/30 shadow-sm relative overflow-hidden">
          <div className="absolute top-0 right-0 w-32 h-32 bg-brand/5 rounded-bl-full -mr-4 -mt-4" />
          <h4 className="text-xl font-bold text-primary">
            Olá! Eu sou Matheus Santana.
          </h4>
          <p className="text-sm font-bold text-brand mb-6 uppercase tracking-wider">
            Desenvolvedor Full Stack
          </p>

          <div className="space-y-4">
            <p className="text-sm text-secondary leading-relaxed max-w-2xl relative z-10">
              Sou desenvolvedor Full Stack com foco em aplicações web utilizando
              Node.js, TypeScript e React. Gosto de transformar problemas do
              mundo real em soluções simples, seguras e bem estruturadas.
            </p>

            <p className="text-sm text-secondary leading-relaxed max-w-2xl relative z-10">
              O Fluxa é um projeto totalmente autoral, desenvolvido como forma
              de aprofundar meus conhecimentos em arquitetura de software,
              autenticação, segurança, banco de dados e experiência do usuário.
            </p>

            <p className="text-sm text-secondary leading-relaxed max-w-2xl relative z-10">
              Cada funcionalidade foi desenvolvida priorizando arquitetura
              limpa, segurança, experiência do usuário e facilidade de
              manutenção, sempre seguindo práticas utilizadas em aplicações
              reais.
            </p>
          </div>

          <p className="text-sm font-bold text-primary mt-6 mb-8 relative z-10">
            Espero que o Fluxa seja útil para organizar suas finanças e que a
            experiência de uso seja tão agradável quanto foi desenvolvê-lo.
          </p>

          <div className="mt-8 relative z-10">
            <p className="text-xs font-extrabold uppercase tracking-widest text-secondary mb-4">
              Stack Tecnológico
            </p>
            <div className="flex flex-wrap gap-2">
              {[
                { name: "React", icon: Layout },
                { name: "TypeScript", icon: Code2 },
                { name: "Node.js", icon: Terminal },
                { name: "Fastify", icon: Terminal },
                { name: "PostgreSQL", icon: Database },
                { name: "SQLite", icon: Database },
                { name: "React Query", icon: Code2 },
                { name: "Knex.js", icon: Database },
                { name: "Zod", icon: Shield },
                { name: "JWT + 2FA", icon: Key },
                { name: "Tailwind CSS", icon: Palette },
              ].map((tech, i) => {
                const Icon = tech.icon;
                return (
                  <div
                    key={i}
                    className="flex items-center gap-2 px-3 py-1.5 rounded-lg bg-elevated/60 border border-subtle/30 text-xs font-bold text-primary"
                  >
                    <Icon size={12} className="text-muted" /> {tech.name}
                  </div>
                );
              })}
            </div>
          </div>

          <div className="mt-10 relative z-10">
            <p className="text-xs font-extrabold uppercase tracking-widest text-secondary mb-4">
              Contato & Repositório
            </p>
            <div className="flex flex-col sm:flex-row gap-4 mb-6">
              <a
                href="https://github.com/matssgit"
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 min-w-0 flex items-center gap-4 p-4 rounded-2xl bg-elevated/40 border border-subtle/30 hover:bg-surface hover:border-subtle/50 hover:shadow-sm transition-all group"
              >
                <GithubIcon
                  size={24}
                  className="group-hover:scale-110 transition-transform text-primary shrink-0"
                />
                <div className="min-w-0">
                  <span className="block text-sm font-bold text-primary truncate">
                    GitHub
                  </span>
                  <span className="block text-[11px] font-medium text-muted truncate">
                    github.com/matssgit
                  </span>
                </div>
              </a>

              <a
                href="https://linkedin.com/in/matheussantanadev"
                target="_blank"
                rel="noopener noreferrer"
                className="flex-1 min-w-0 flex items-center gap-4 p-4 rounded-2xl bg-elevated/40 border border-subtle/30 hover:bg-surface hover:border-subtle/50 hover:shadow-sm transition-all group"
              >
                <LinkedinIcon
                  size={24}
                  className="group-hover:scale-110 transition-transform text-primary shrink-0"
                />
                <div className="min-w-0">
                  <span className="block text-sm font-bold text-primary truncate">
                    LinkedIn
                  </span>
                  <span className="block text-[11px] font-medium text-muted truncate">
                    linkedin.com/in/matheussantanadev
                  </span>
                </div>
              </a>
            </div>

            <a
              href="https://github.com/matssgit/fluxa"
              target="_blank"
              rel="noopener noreferrer"
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 px-6 py-3 bg-[#24292e] hover:bg-[#2f363d] text-white text-sm font-bold rounded-xl transition-all shadow-sm group"
            >
              <GithubIcon
                size={18}
                className="group-hover:scale-110 transition-transform shrink-0"
              />
              <span className="truncate">Ver projeto no GitHub</span>
            </a>
          </div>
        </div>
      </div>

      <div className="pt-8 mt-8 border-t border-subtle/20 text-center pb-4">
        <p className="text-sm font-extrabold text-primary">Fluxa v1.0.0</p>
        <p className="text-xs font-medium text-muted mt-1.5">
          Desenvolvido e mantido por Matheus Santana.
        </p>
        <p className="text-[11px] font-medium text-muted/60 mt-3">
          Construído com Node.js, TypeScript, React e muito café ☕
        </p>

        <div className="mt-12 flex justify-center animate-fade-in">
          <div className="bg-[#1e1e1e] border border-subtle/30 rounded-lg p-5 text-left shadow-md max-w-280px w-full relative overflow-hidden group">
            <div className="absolute top-0 left-0 w-full h-1 bg-brand/50"></div>
            <div className="flex items-center gap-1.5 mb-4">
              <div className="w-2.5 h-2.5 rounded-full bg-red-500/80"></div>
              <div className="w-2.5 h-2.5 rounded-full bg-yellow-500/80"></div>
              <div className="w-2.5 h-2.5 rounded-full bg-green-500/80"></div>
            </div>
            <p className="font-mono text-[11px] text-[#a0a0a0] mb-3 flex items-center gap-2">
              <span className="text-brand">~/fluxa</span> $ git log --oneline
            </p>
            <ul className="font-mono text-[11px] text-[#d4d4d4] space-y-1.5 opacity-80 group-hover:opacity-100 transition-opacity">
              <li>
                <span className="text-[#ce9178]">feat:</span> Fluxa v1.0
              </li>
              <li>
                <span className="text-[#ce9178]">feat:</span> Recuperação de
                senha
              </li>
              <li>
                <span className="text-[#ce9178]">feat:</span> 2FA
              </li>
              <li>
                <span className="text-[#ce9178]">feat:</span> Parcelamentos
              </li>
              <li>
                <span className="text-[#ce9178]">feat:</span> Cartões
              </li>
              <li>
                <span className="text-[#ce9178]">feat:</span> Dashboard
              </li>
              <li className="text-[#6a9955] pt-2">Initial Public Release 🚀</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
