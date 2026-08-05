export interface LoadingProps {
  size?: "sm" | "md" | "lg";
  text?: string;
  className?: string;
}

export function Loading({ size = "md", text, className = "" }: LoadingProps) {
  const sizeMap = {
    sm: "w-4 h-4 border-2",
    md: "w-6 h-6 border-2",
    lg: "w-8 h-8 border-3",
  };

  return (
    <div
      className={`flex flex-col items-center justify-center gap-3 py-6 ${className}`}
    >
      <div
        className={`${sizeMap[size]} border-brand border-t-transparent rounded-full animate-spin`}
      />
      {text && (
        <span className="text-xs font-medium text-muted animate-pulse">
          {text}
        </span>
      )}
    </div>
  );
}
