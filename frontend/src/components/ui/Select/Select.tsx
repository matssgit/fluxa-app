import React from "react";

export interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string;
  error?: string;
  icon?: React.ElementType;
  children: React.ReactNode;
}

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  (
    { label, error, icon: Icon, className = "", id, children, ...props },
    ref,
  ) => {
    const selectId =
      id || props.name || Math.random().toString(36).substring(7);

    return (
      <div className="w-full space-y-1.5">
        {label && (
          <label
            htmlFor={selectId}
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
          <select
            ref={ref}
            id={selectId}
            className={`input appearance-none cursor-pointer ${Icon ? "pl-10" : ""} ${error ? "input-error" : ""} ${className}`}
            {...props}
          >
            {children}
          </select>
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

Select.displayName = "Select";
