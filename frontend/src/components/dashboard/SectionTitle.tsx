import React from "react";

interface SectionTitleProps {
  title: string;
  subtitle?: string;
  eyebrow?: string; // <-- Nova propriedade de ergonomia visual!
  className?: string;
}

export function SectionTitle({
  title,
  subtitle,
  eyebrow,
  className = "",
}: SectionTitleProps) {
  return (
    <div className={`space-y-1 ${className}`}>
      {/* Âncora Visual (Eyebrow Tracker) - Destaque executivo para rolagem mobile */}
      {eyebrow && (
        <span className="inline-block text-[11px] font-extrabold uppercase tracking-[0.2em] text-brand bg-brand/10 px-2.5 py-0.5 rounded-md mb-1 border border-brand/15">
          {eyebrow}
        </span>
      )}

      <h3 className="text-lg font-bold text-primary tracking-tight">{title}</h3>

      {subtitle && (
        <p className="text-xs font-medium text-secondary leading-relaxed">
          {subtitle}
        </p>
      )}
    </div>
  );
}
