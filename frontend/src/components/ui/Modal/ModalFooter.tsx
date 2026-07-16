import React from "react";

export function ModalFooter({
  children,
  className = "",
}: {
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <div
      className={`shrink-0 sticky bottom-0 z-10 bg-surface border-t border-subtle/20 p-4 sm:p-6 flex gap-3 justify-end ${className}`}
    >
      {children}
    </div>
  );
}
