import { usePrivacy } from "../../../hooks/usePrivacy";

interface PrivacyMaskProps {
  amount: number;
  currency?: string;
  className?: string;
}

export function PrivacyMask({
  amount,
  currency = "R$",
  className = "",
}: PrivacyMaskProps) {
  const { hideBalance } = usePrivacy();

  if (hideBalance) {
    return (
      <span className={`tracking-widest opacity-60 ${className}`}>
        ••••••••
      </span>
    );
  }

  const formattedAmount = new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
  }).format(amount);

  return (
    <span className={className}>{formattedAmount.replace("R$", currency)}</span>
  );
}
