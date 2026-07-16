import React from "react";

export function ModalBody({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={`flex-1 overflow-y-auto p-4 sm:p-6 ${className}`}>
      {children}
    </div>
  );
}
