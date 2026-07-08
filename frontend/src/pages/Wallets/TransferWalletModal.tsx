import { useState } from "react";
import {
  Modal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  Button,
  Input,
} from "../../components/ui";
import { useTransferWallet } from "../../hooks/useWallets";
import { useAccounts } from "../../hooks/useAccounts";
import type { Wallet } from "../../types/wallet";

interface TransferWalletModalProps {
  isOpen: boolean;
  onClose: () => void;
  wallet: Wallet | null;
  type: "deposit" | "withdraw" | null;
}

const INPUT_CLASS =
  "w-full h-11 px-3.5 rounded-xl bg-elevated/80 border border-subtle/30 text-primary text-sm font-medium focus:outline-none focus:border-brand focus:ring-1 focus:ring-brand/30 transition-all placeholder:text-muted/60";

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

  if (!wallet || !type) return null;

  const isDeposit = type === "deposit";
  const title = isDeposit
    ? `Aportar em: ${wallet.title}`
    : `Resgatar de: ${wallet.title}`;
  const subtitle = isDeposit
    ? "Envie recursos de uma conta bancária para progredir rumo a esta meta."
    : "Resgate recursos acumulados neste objetivo de volta para a sua liquidez imediata.";

  const maxWithdraw = Number(wallet.current_amount) || 0;

  function handleSubmit(e: React.FormEvent<HTMLFormElement>): void {
    e.preventDefault();
    if (!wallet || !type || !accountId || !amount) return;

    const numericAmount = Number(amount.replace(",", "."));
    if (isNaN(numericAmount) || numericAmount <= 0) return;

    if (!isDeposit && numericAmount > maxWithdraw) {
      alert(
        "O valor de resgate não pode ser maior que o saldo acumulado no objetivo.",
      );
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
      },
    );
  }

  function handleClose(): void {
    setAccountId("");
    setAmount("");
    onClose();
  }

  return (
    <Modal isOpen={isOpen} onClose={handleClose}>
      <ModalHeader title={title} onClose={handleClose} />

      <form onSubmit={handleSubmit}>
        <ModalBody className="space-y-4">
          <p className="text-xs font-medium text-muted -mt-2 mb-2">
            {subtitle}
          </p>

          {/* Seleção de Conta Bancária */}
          <div>
            <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
              {isDeposit
                ? "Conta de Origem (Sai o dinheiro)"
                : "Conta de Destino (Entra o dinheiro)"}
            </label>
            <div className="relative">
              <select
                value={accountId}
                onChange={(e) => setAccountId(e.target.value)}
                className={INPUT_CLASS}
                required
              >
                <option value="" disabled>
                  Selecione uma conta bancária...
                </option>
                {accounts.map((acc) => (
                  <option key={acc.id} value={acc.id}>
                    {acc.name}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* Valor da Transferência */}
          <div>
            <div className="flex justify-between items-baseline mb-1.5">
              <label className="block text-xs font-bold uppercase tracking-wider text-muted">
                Valor do {isDeposit ? "Aporte" : "Resgate"} (R$)
              </label>
              {!isDeposit && (
                <span className="text-[11px] font-bold text-secondary">
                  Disponível: R$ {maxWithdraw.toFixed(2)}
                </span>
              )}
            </div>
            <div className="relative">
              <span className="absolute left-3.5 top-1/2 -translate-y-1/2 text-muted font-bold text-sm select-none">
                R$
              </span>
              <Input
                type="number"
                step="0.01"
                max={!isDeposit ? maxWithdraw : undefined}
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className={`${INPUT_CLASS} pl-10`}
                required
              />
            </div>
          </div>
        </ModalBody>

        <ModalFooter>
          <Button
            type="button"
            variant="ghost"
            onClick={handleClose}
            disabled={isPending}
          >
            Cancelar
          </Button>
          <Button
            type="submit"
            variant={isDeposit ? "primary" : "secondary"}
            isLoading={isPending}
          >
            {isDeposit ? "Confirmar Aporte" : "Confirmar Resgate"}
          </Button>
        </ModalFooter>
      </form>
    </Modal>
  );
}
