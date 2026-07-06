import React from "react";

export interface ModalBodyProps {
  children: React.ReactNode;
  className?: string;
}

export function ModalBody({ children, className = "" }: ModalBodyProps) {
  return <div className={`p-6 space-y-4 ${className}`}>{children}</div>;
}
