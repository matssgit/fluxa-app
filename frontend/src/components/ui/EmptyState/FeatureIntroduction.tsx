import { type LucideIcon, Lightbulb, CheckCircle2, Plus } from "lucide-react";

interface Example {
  label: string;
  category?: string;
}

interface FeatureIntroductionProps {
  icon: LucideIcon;
  title: string;
  subtitle: string;
  whatIs: string;
  examples: Example[];
  tip?: string;
  actionLabel: string;
  onAction: () => void;
}

export function FeatureIntroduction({
  icon: Icon,
  title,
  subtitle,
  whatIs,
  examples,
  tip,
  actionLabel,
  onAction,
}: FeatureIntroductionProps) {
  return (
    <div className="w-full max-w-2xl mx-auto mt-4 sm:mt-8 animate-fade-in">
      <div className="bg-surface border border-subtle/30 rounded-3xl p-6 sm:p-10 shadow-sm text-left relative overflow-hidden">
        {/* Efeito de fundo sutil para parecer um card Premium */}
        <div className="absolute top-0 right-0 p-12 opacity-5 pointer-events-none">
          <Icon size={160} />
        </div>

        {/* Cabeçalho */}
        <div className="flex items-center gap-4 mb-8 relative z-10">
          <div className="w-14 h-14 rounded-2xl bg-brand/10 text-brand flex items-center justify-center shrink-0 shadow-inner">
            <Icon size={28} />
          </div>
          <div>
            <h2 className="text-xl sm:text-2xl font-black text-primary tracking-tight">
              {title}
            </h2>
            <p className="text-sm font-medium text-muted mt-0.5">{subtitle}</p>
          </div>
        </div>

        {/* O que é? */}
        <div className="mb-8 relative z-10">
          <h3 className="text-sm font-extrabold text-primary mb-2 tracking-wide uppercase">
            O que é isso?
          </h3>
          <p className="text-sm sm:text-base text-secondary leading-relaxed font-medium">
            {whatIs}
          </p>
        </div>

        {/* Exemplos Práticos */}
        <div className="mb-8 relative z-10">
          <h3 className="text-sm font-extrabold text-primary mb-3 tracking-wide uppercase">
            Exemplos na prática:
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {examples.map((example, i) => (
              <div
                key={i}
                className="flex items-center gap-3 p-3.5 rounded-xl bg-elevated/40 border border-subtle/30 shadow-2xs"
              >
                <CheckCircle2 size={18} className="text-brand shrink-0" />
                <div>
                  <span className="text-sm font-bold text-secondary block leading-none">
                    {example.label}
                  </span>
                  {example.category && (
                    <span className="text-[10px] font-extrabold uppercase tracking-widest text-muted mt-1 block">
                      {example.category}
                    </span>
                  )}
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Dica de Ouro */}
        {tip && (
          <div className="flex items-start gap-3 p-4 sm:p-5 rounded-2xl bg-amber-500/10 border border-amber-500/20 mb-8 relative z-10">
            <Lightbulb size={20} className="text-amber-600 shrink-0 mt-0.5" />
            <p className="text-xs sm:text-sm font-semibold text-amber-700/90 dark:text-amber-500/90 leading-relaxed">
              {tip}
            </p>
          </div>
        )}

        {/* Call to Action */}
        <div className="relative z-10 pt-2">
          <button
            onClick={onAction}
            className="w-full sm:w-auto px-8 py-4 rounded-xl bg-brand hover:bg-brand-light text-white text-sm font-bold shadow-sm transition-all cursor-pointer active:scale-95 flex items-center justify-center gap-2"
          >
            <Plus size={18} strokeWidth={3} />
            <span>{actionLabel}</span>
          </button>
        </div>
      </div>
    </div>
  );
}
