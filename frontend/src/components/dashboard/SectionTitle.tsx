import React from "react";

interface SectionTitleProps {
  title: string;
  subtitle?: string;
  icon?: React.ElementType;
}

export function SectionTitle({
  title,
  subtitle,
  icon: Icon,
}: SectionTitleProps) {
  return (
    <div className="mb-6">
      <h3 className="flex items-center gap-2 font-semibold text-lg text-primary tracking-tight">
        {Icon && <Icon size={18} className="text-accent" />}
        {title}
      </h3>
      {subtitle && (
        <p className="text-xs font-medium text-muted mt-0.5">{subtitle}</p>
      )}
    </div>
  );
}
