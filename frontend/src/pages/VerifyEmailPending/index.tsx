import { Link, useLocation } from "react-router-dom";

export function VerifyEmailPending() {
  const location = useLocation();
  const email = location.state?.email || "seu e-mail";

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-page p-4 transition-colors duration-300">
      <div className="w-full max-w-md card-default text-center animate-fade-in">
        <div className="flex items-center justify-center w-16 h-16 rounded-2xl bg-surface shadow-sm text-brand mb-6 mx-auto border border-subtle">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            className="h-8 w-8"
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
            strokeWidth={2}
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
            />
          </svg>
        </div>

        <h2 className="text-2xl font-bold text-primary mb-4">
          Verifique sua caixa de entrada
        </h2>

        <p className="text-secondary mb-6 leading-relaxed">
          Enviamos um link de confirmação para{" "}
          <strong className="text-primary font-medium">{email}</strong>. Por
          favor, clique no link para ativar sua conta e acessar o sistema.
        </p>

        <div className="space-y-4">
          <Link to="/login" className="block w-full">
            <button className="w-full flex justify-center items-center py-3 px-4 bg-brand text-white rounded-xl font-medium shadow-sm hover:opacity-90 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2 transition-all">
              Voltar para o Login
            </button>
          </Link>
        </div>
      </div>
    </div>
  );
}
