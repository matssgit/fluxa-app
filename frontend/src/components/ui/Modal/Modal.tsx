import React, { useEffect } from "react";

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  size?: "sm" | "md" | "lg" | "xl";
  children: React.ReactNode;
}

export function Modal({ isOpen, onClose, size = "md", children }: ModalProps) {
  // Lock no scroll e fechamento com a tecla ESC
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };

    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = "unset";
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const sizeMap = {
    sm: "max-w-sm",
    md: "max-w-md",
    lg: "max-w-lg",
    xl: "max-w-xl",
  };

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-primary/20 backdrop-blur-xs animate-fade-in"
      onClick={onClose}
    >
      <div
        className={`w-full ${sizeMap[size]} bg-surface border border-subtle shadow-md rounded-2xl overflow-hidden animate-scale-in`}
        onClick={(e) => e.stopPropagation()} // Impede que o clique dentro do card feche o modal
      >
        {children}
      </div>
    </div>
  );
}
