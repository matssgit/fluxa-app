import React from "react";

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  icon?: React.ElementType;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  ({ label, error, icon: Icon, className = "", id, ...props }, ref) => {
    const inputId = id || props.name || Math.random().toString(36).substring(7);

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label
            htmlFor={inputId}
            className="block text-sm font-semibold text-primary"
          >
            {label}
          </label>
        )}

        <div className="relative flex items-center">
          {Icon && (
            <Icon
              size={18}
              className="absolute left-3.5 text-muted pointer-events-none transition-colors group-focus-within:text-brand"
            />
          )}
          <input
            ref={ref}
            id={inputId}
            className={`input ${Icon ? "pl-10" : ""} ${error ? "input-error" : ""} ${className}`}
            {...props}
          />
        </div>

        {error && (
          <span className="block text-xs font-medium text-danger animate-fade-in">
            {error}
          </span>
        )}
      </div>
    );
  },
);

Input.displayName = "Input";
