import { Check, Circle } from "lucide-react";

interface PasswordRequirementsProps {
  password?: string;
}

export function PasswordRequirements({ password = "" }: PasswordRequirementsProps) {
  const requirements = [
    { label: "Mínimo de 8 caracteres", met: password.length >= 8 },
    { label: "Uma letra maiúscula", met: /[A-Z]/.test(password) },
    { label: "Uma letra minúscula", met: /[a-z]/.test(password) },
    { label: "Um número", met: /\d/.test(password) },
    { label: "Um caractere especial", met: /[^A-Za-z0-9]/.test(password) },
  ];

  return (
    <div className="space-y-2 p-4 bg-page rounded-xl border border-subtle transition-all">
      <p className="text-sm font-medium text-primary mb-2">Sua senha deve conter:</p>
      <ul className="space-y-2">
        {requirements.map((req, index) => (
          <li key={index} className="flex items-center gap-2 text-sm">
            {req.met ? (
              <Check size={16} className="text-success transition-all duration-300" />
            ) : (
              <Circle size={14} className="text-muted transition-all duration-300" />
            )}
            <span className={req.met ? "text-primary transition-colors" : "text-muted transition-colors"}>
              {req.label}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
}