import { useState } from "react";
import { X, Building, DollarSign, ChevronDown } from "lucide-react";
import { useTransferWallet } from "../../hooks/useWallets";
import { useAccounts } from "../../hooks/useAccounts";
import type { Wallet } from "../../types/wallet";
import { PickerModal } from "../../components/ui/PickerModal";

interface TransferWalletModalProps {
  isOpen: boolean;
  onClose: () => void;
  wallet: Wallet | null;
  type: "deposit" | "withdraw" | null;
}

export function TransferWalletModal({
  isOpen,
  onClose,
  wallet,
  type,
}: TransferWalletModalProps) {
  const { mutate: transfer, isPending } = useTransferWallet();
  const { accounts = [] } = useAccounts();

  const [accountId, setAccountId] = useState("");
  const [amount, setAmount] = useState("");
  
  const [isAccountPickerOpen, setIsAccountPickerOpen] = useState(false);

  if (!isOpen || !wallet || !type) return null;

  const isDeposit = type === "deposit";
  const title = isDeposit ? `Aportar` : `Resgatar`;
  const subtitle = isDeposit
    ? `Enviando recursos para: ${wallet.title}`
    : `Resgatando de: ${wallet.title}`;

  const maxWithdraw = Number(wallet.current_amount) || 0;
  const selectedAccountName = accounts.find((a) => a.id === accountId)?.name || "Selecione uma conta...";

  function handleSubmit(e: React.FormEvent<HTMLFormElement>): void {
    e.preventDefault();
    if (!wallet || !type || !accountId || !amount) return;

    const numericAmount = Number(amount.replace(",", "."));
    if (isNaN(numericAmount) || numericAmount <= 0) return;

    if (!isDeposit && numericAmount > maxWithdraw) {
      alert("O valor de resgate não pode ser maior que o saldo acumulado no objetivo.");
      return;
    }

    transfer(
      {
        wallet_id: wallet.id,
        account_id: accountId,
        amount: numericAmount,
        type: type,
      },
      {
        onSuccess: () => {
          handleClose();
        },
      }
    );
  }

  function handleClose(): void {
    setAccountId("");
    setAmount("");
    onClose();
  }

  return (
    <>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-4 bg-page/80 backdrop-blur-md animate-fade-in">
        <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-md max-h-[82vh] sm:max-h-[88vh] flex flex-col overflow-hidden border border-subtle/30 transition-all duration-300">
          
          <div className="flex justify-between items-center p-4 sm:p-6 border-b border-subtle/20 shrink-0">
            <div>
              <h2 className="text-base sm:text-xl font-bold text-primary tracking-tight">{title}</h2>
              <p className="text-xs font-medium text-muted mt-0.5">{subtitle}</p>
            </div>
            <button onClick={handleClose} className="p-2 text-muted hover:text-primary hover:bg-elevated rounded-full transition-colors cursor-pointer">
              <X size={18} />
            </button>
          </div>

          <form id="transfer-wallet-form" onSubmit={handleSubmit} className="p-4 sm:p-6 space-y-4 overflow-y-auto flex-1">
            
            <div>
              <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary mb-1 pl-1">
                <Building size={13} className="text-muted" />
                <span>{isDeposit ? "Conta de Origem (Sai o dinheiro) *" : "Conta de Destino (Entra o dinheiro) *"}</span>
              </label>
              {/* ✨ TRIGGER BUTTON: Conta (Lei da Adaptação Mobile - Touch Friendly) */}
              <button
                type="button"
                onClick={() => setIsAccountPickerOpen(true)}
                className={`w-full flex items-center justify-between rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 hover:bg-surface outline-none transition-all text-xs sm:text-sm font-semibold shadow-2xs cursor-pointer ${!accountId ? "text-muted" : "text-primary"}`}
              >
                <span className="truncate pr-2">{selectedAccountName}</span>
                <ChevronDown size={16} className="text-muted shrink-0" />
              </button>
            </div>

            <div>
              <div className="flex justify-between items-baseline mb-1">
                <label className="flex items-center gap-1.5 text-xs font-extrabold uppercase tracking-widest text-secondary pl-1">
                  <DollarSign size={13} className="text-muted" />
                  <span>Valor do {isDeposit ? "Aporte" : "Resgate"} (R$) *</span>
                </label>
                {!isDeposit && (
                  <span className="text-[10px] font-extrabold text-amber-500 uppercase tracking-wider bg-amber-500/10 px-1.5 py-0.5 rounded">
                    Disp: R$ {maxWithdraw.toFixed(2)}
                  </span>
                )}
              </div>
              <input
                type="number"
                step="0.01"
                max={!isDeposit ? maxWithdraw : undefined}
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full rounded-xl border border-subtle/30 px-3.5 py-2.5 sm:py-3 bg-elevated/40 focus:bg-surface text-primary placeholder:text-muted/60 focus:border-brand outline-none transition-all text-sm sm:text-base font-extrabold shadow-2xs tracking-tight"
                required
              />
            </div>

          </form>

          <div className="p-4 sm:p-6 bg-surface border-t border-subtle/20 flex gap-3 shrink-0">
            <button type="button" onClick={handleClose} disabled={isPending} className="flex-1 px-4 py-2.5 sm:py-3 rounded-xl border border-subtle/30 bg-elevated hover:bg-subtle/40 text-secondary text-xs sm:text-sm font-bold transition-all shadow-2xs cursor-pointer">
              Cancelar
            </button>
            <button type="submit" form="transfer-wallet-form" disabled={isPending || !accountId || !amount} className={`flex-1 px-4 py-2.5 sm:py-3 rounded-xl text-white text-xs sm:text-sm font-bold shadow-sm transition-all disabled:opacity-50 cursor-pointer ${isDeposit ? "bg-brand hover:bg-brand-light" : "bg-amber-500 hover:bg-amber-400"}`}>
              {isPending ? "Processando..." : (isDeposit ? "Confirmar Aporte" : "Confirmar Resgate")}
            </button>
          </div>
        </div>
      </div>

      <PickerModal
        isOpen={isAccountPickerOpen}
        onClose={() => setIsAccountPickerOpen(false)}
        title={isDeposit ? "Selecione a Conta de Origem" : "Selecione a Conta de Destino"}
        selectedValue={accountId}
        onSelect={(val) => setAccountId(val as string)}
        options={accounts.map((acc) => ({
          label: acc.name,
          value: acc.id,
        }))}
      />
    </>
  );
}