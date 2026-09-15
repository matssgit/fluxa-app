import React from "react";

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: "default" | "elevated" | "interactive" | "metric";
  children: React.ReactNode;
}

export function Card({
  variant = "default",
  className = "",
  children,
  ...props
}: CardProps) {
  const variantMap = {
    default: "card-default",
    elevated: "card-elevated",
    interactive: "card-interactive",
    metric:
      "bg-surface border border-border shadow-sm p-6 rounded-3xl relative overflow-hidden",
  };

  return (
    <div className={`${variantMap[variant]} ${className}`} {...props}>
      {children}
    </div>
  );
}
