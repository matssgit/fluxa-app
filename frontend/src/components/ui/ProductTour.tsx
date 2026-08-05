import type { Step } from "react-joyride";
import type { FC, ComponentProps } from "react";
import { Joyride as JoyrideOriginal } from "react-joyride";
import React, {
  useState,
  useEffect,
  useCallback,
  createContext,
  useContext,
} from "react";

export interface JoyrideCallbackData {
  action: string;
  index: number;
  lifecycle: string;
  status: string;
  step: Step;
  type: string;
}

export interface CustomTooltipProps {
  continuous: boolean;
  index: number;
  step: Step;
  backProps: React.ButtonHTMLAttributes<HTMLButtonElement>;
  closeProps: React.ButtonHTMLAttributes<HTMLButtonElement>;
  primaryProps: React.ButtonHTMLAttributes<HTMLButtonElement>;
  tooltipProps: React.HTMLAttributes<HTMLDivElement>;
  isLastStep: boolean;
}

const TourContext = createContext<{ forceFinish: () => void }>({
  forceFinish: () => {},
});

type SafeJoyrideProps = Omit<
  ComponentProps<typeof JoyrideOriginal>,
  "callback" | "tooltipComponent"
> & {
  callback?: (data: JoyrideCallbackData) => void;
  tooltipComponent?: FC<CustomTooltipProps>;
};

const Joyride = JoyrideOriginal as FC<SafeJoyrideProps>;

export function CustomTooltip({
  index,
  step,
  backProps,
  closeProps,
  primaryProps,
  tooltipProps,
  isLastStep,
}: CustomTooltipProps) {
  const { forceFinish } = useContext(TourContext);

  const handleSkip = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (closeProps.onClick) closeProps.onClick(e);
    forceFinish();
  };

  const handlePrimary = (e: React.MouseEvent<HTMLButtonElement>) => {
    if (primaryProps.onClick) primaryProps.onClick(e);
    if (isLastStep) forceFinish();
  };

  return (
    <div
      id="fluxa-tour-tooltip"
      {...tooltipProps}
      className="scroll-mt-[15vh] scroll-mb-[15vh] bg-surface border border-subtle rounded-2xl p-6 shadow-xl max-w-sm w-full font-sans z-1000"
    >
      <div className="flex flex-col gap-2">
        {step.title && (
          <h3 className="text-base font-bold text-primary tracking-tight">
            {step.title}
          </h3>
        )}
        <div className="text-sm text-secondary leading-relaxed mb-4">
          {step.content}
        </div>

        <div className="flex items-center justify-between mt-2 pt-4 border-t border-subtle/30">
          <button
            {...closeProps}
            onClick={handleSkip}
            className="text-sm font-bold text-muted hover:text-primary transition-colors cursor-pointer"
          >
            Pular tour
          </button>

          <div className="flex items-center gap-2">
            {index > 0 && (
              <button
                {...backProps}
                className="px-4 py-2 text-sm font-bold text-secondary hover:bg-elevated rounded-xl transition-colors cursor-pointer"
              >
                Voltar
              </button>
            )}

            <button
              {...primaryProps}
              onClick={handlePrimary}
              className="px-4 py-2 bg-brand hover:bg-brand-light text-white text-sm font-bold rounded-xl transition-all shadow-sm cursor-pointer"
            >
              {isLastStep ? "Começar" : "Próximo"}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

// --- MOTOR REUTILIZÁVEL ---

export interface ContextualTourProps {
  tourKey: string;
  steps: Step[];
  dependencyKeys?: string[];
}

export function ContextualTour({
  tourKey,
  steps,
  dependencyKeys = [],
}: ContextualTourProps) {
  const [isEligible, setIsEligible] = useState(false);
  const depsString = dependencyKeys.join(",");

  useEffect(() => {
    let timeoutId: ReturnType<typeof setTimeout>;

    const checkTourState = () => {
      if (localStorage.getItem(tourKey) === "true") {
        setIsEligible(false);
        return;
      }

      const keys = depsString ? depsString.split(",") : [];
      const dependenciesMet = keys.every(
        (key) => localStorage.getItem(key) === "true",
      );

      if (dependenciesMet) {
        timeoutId = setTimeout(() => {
          setIsEligible(true);
        }, 800);
      }
    };

    checkTourState();
    window.addEventListener("fluxa_tour_updated", checkTourState);

    return () => {
      window.removeEventListener("fluxa_tour_updated", checkTourState);
      if (timeoutId) clearTimeout(timeoutId);
    };
  }, [tourKey, depsString]);

  const forceFinish = useCallback(() => {
    setIsEligible(false);
    localStorage.setItem(tourKey, "true");
    window.dispatchEvent(new Event("fluxa_tour_updated"));
  }, [tourKey]);

  const stepsWithCustomScroll = steps.map((step) => ({
    ...step,
    disableScrolling: true,
  }));

  const handleJoyrideCallback = useCallback((data: JoyrideCallbackData) => {
    const { lifecycle } = data;

    if (lifecycle === "tooltip") {
      setTimeout(() => {
        const tooltip = document.getElementById("fluxa-tour-tooltip");
        if (tooltip) {
          tooltip.scrollIntoView({
            behavior: "smooth",
            block: "center",
          });
        }
      }, 100);

      setTimeout(() => {
        const tooltip = document.getElementById("fluxa-tour-tooltip");
        if (tooltip) {
          tooltip.scrollIntoView({
            behavior: "smooth",
            block: "center",
          });
        }
      }, 400);
    }
  }, []);

  if (!isEligible) return null;

  return (
    <TourContext.Provider value={{ forceFinish }}>
      <Joyride
        steps={stepsWithCustomScroll}
        run={true}
        continuous
        scrollToFirstStep={false}
        callback={handleJoyrideCallback}
        tooltipComponent={CustomTooltip}
      />
    </TourContext.Provider>
  );
}

// --- TOUR GERAL ---

const GENERAL_TOUR_KEY = "@fluxa:tour_completed";

export function ProductTour() {
  const steps: Step[] = [
    {
      target: "body",
      placement: "center",
      title: "Bem-vindo ao Fluxa! 👋",
      content:
        "Vamos fazer um tour rápido para você conhecer as principais áreas e começar a organizar sua vida financeira.",
    },
    {
      target: ".tour-dashboard-summary",
      title: "Seu resumo financeiro",
      content:
        "Aqui você acompanha suas receitas, despesas e o saldo da sua vida financeira em tempo real.",
      placement: "bottom",
    },
    {
      target: ".tour-bottom-nav",
      title: "Navegação na palma da mão",
      content:
        "Acesse suas Carteiras, Cartões de Crédito e Assinaturas através do menu inferior. Tudo pronto! 🚀",
      placement: "top",
    },
  ];

  return <ContextualTour tourKey={GENERAL_TOUR_KEY} steps={steps} />;
}
