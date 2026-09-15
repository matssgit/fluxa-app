import React from "react";

export interface EmptyStateProps {
  icon?: React.ElementType;
  title: string;
  description: string;
  action?: React.ReactNode;
  className?: string;
}

export function EmptyState({
  icon: Icon,
  title,
  description,
  action,
  className = "",
}: EmptyStateProps) {
  return (
    <div
      className={`flex flex-col items-center justify-center px-5 py-12 bg-surface border border-border rounded-3xl text-center shadow-sm animate-fade-in ${className}`}
    >
      {Icon && (
        <div className="w-14 h-14 bg-elevated shadow-inner rounded-2xl flex items-center justify-center text-brand mb-4 border border-border">
          <Icon size={28} />
        </div>
      )}
      <h3 className="text-base font-semibold text-primary tracking-tight mb-1">
        {title}
      </h3>
      <p className="text-sm text-secondary max-w-xs leading-relaxed mb-6">
        {description}
      </p>
      {action && <div className="mt-2">{action}</div>}
    </div>
  );
}
