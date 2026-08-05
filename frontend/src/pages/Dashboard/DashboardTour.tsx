import type { Step } from "react-joyride";
import { ContextualTour } from "../../components/ui/ProductTour";

export function DashboardTour() {
  const steps: Step[] = [
    {
      target: ".tour-dash-health",
      title: "Saúde Financeira",
      content:
        "Este banner analisa suas receitas e despesas em tempo real para avisar se sua liquidez está saudável ou se exige atenção.",
      placement: "bottom",
    },
    {
      target: ".tour-dash-telemetry",
      title: "Termômetro Operacional",
      content:
        "Acompanhe de perto sua taxa de poupança e o quanto da sua renda já foi comprometida no mês.",
      placement: "bottom",
    },
    {
      target: ".tour-dash-radar",
      title: "Radar de Vencimentos",
      content:
        "Nunca mais perca um prazo. Seus próximos compromissos, assinaturas e faturas aparecem aqui para sua atenção imediata.",
      placement: "bottom",
    },
  ];

  return (
    <ContextualTour tourKey="@fluxa:tour_dashboard_completed" steps={steps} />
  );
}
