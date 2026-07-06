interface SkeletonProps {
  className?: string;
}

export function Skeleton({ className = "" }: SkeletonProps) {
  // Usa bg-elevated (que é nosso cinza ultra suave da paleta oficial) para o efeito de piscar
  return (
    <div className={`animate-pulse bg-elevated rounded-xl ${className}`} />
  );
}
