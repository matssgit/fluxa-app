import React from "react";

export interface ModalFooterProps {
  children: React.ReactNode;
  className?: string;
}

export function ModalFooter({ children, className = "" }: ModalFooterProps) {
  return (
    <div
      className={`flex items-center justify-end gap-3 px-6 py-4 bg-elevated border-t border-subtle ${className}`}
    >
      {children}
    </div>
  );
}
