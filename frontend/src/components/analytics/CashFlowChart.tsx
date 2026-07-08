import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";
import type { CashFlowDataPoint } from "../../types/analytics";

interface CashFlowChartProps {
  data: CashFlowDataPoint[];
}

export function CashFlowChart({ data }: CashFlowChartProps) {
  const formatCurrency = (value: number) =>
    new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL", maximumFractionDigits: 0 }).format(value);

  return (
    <div className="card-default p-6 border-subtle/30 flex flex-col justify-between h-full">
      <div className="mb-6">
        <h3 className="font-bold text-lg text-primary tracking-tight">Evolução de Fluxo de Caixa</h3>
        <p className="text-xs text-muted mt-0.5">Comparativo de entradas e saídas nos últimos meses</p>
      </div>

      <div className="w-full h-64 sm:h-72">
        <ResponsiveContainer width="100%" height="100%">
          <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
            <defs>
              <linearGradient id="colorIncome" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#10B981" stopOpacity={0.3} />
                <stop offset="95%" stopColor="#10B981" stopOpacity={0} />
              </linearGradient>
              <linearGradient id="colorExpense" x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%" stopColor="#EF4444" stopOpacity={0.2} />
                <stop offset="95%" stopColor="#EF4444" stopOpacity={0} />
              </linearGradient>
            </defs>
            <XAxis dataKey="month" stroke="#64748B" fontSize={12} tickLine={false} axisLine={false} />
            <YAxis stroke="#64748B" fontSize={12} tickLine={false} axisLine={false} tickFormatter={(val) => `R$ ${val / 1000}k`} />
            <Tooltip
              contentStyle={{ backgroundColor: "#1E293B", borderColor: "#334155", borderRadius: "12px", color: "#F8FAFC", fontSize: "12px" }}
              formatter={(value: unknown) => [formatCurrency(Number(value || 0)), ""]}
              labelStyle={{ fontWeight: "bold", marginBottom: "4px" }}
            />
            <Area type="monotone" dataKey="income" name="Entradas" stroke="#10B981" strokeWidth={2} fillOpacity={1} fill="url(#colorIncome)" />
            <Area type="monotone" dataKey="expense" name="Saídas" stroke="#EF4444" strokeWidth={2} fillOpacity={1} fill="url(#colorExpense)" />
          </AreaChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}