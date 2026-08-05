export interface DividerProps {
  text?: string;
  className?: string;
}

export function Divider({ text, className = "" }: DividerProps) {
  if (!text) {
    return <hr className={`border-t border-subtle w-full my-4 ${className}`} />;
  }

  return (
    <div className={`relative flex items-center my-6 ${className}`}>
      <div className="grow border-t border-subtle" />
      <span className="shrink mx-4 text-xs font-semibold text-muted uppercase tracking-wider">
        {text}
      </span>
      <div className="grow border-t border-subtle" />
    </div>
  );
}
