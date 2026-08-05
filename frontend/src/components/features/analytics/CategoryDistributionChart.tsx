import type { CategoryDistributionPoint } from "../../../types/analytics";
import { PieChart, Pie, Cell, ResponsiveContainer, Tooltip } from "recharts";

interface CategoryDistributionChartProps {
  data: CategoryDistributionPoint[];
}

export function CategoryDistributionChart({
  data,
}: CategoryDistributionChartProps) {
  const formatCurrency = (value: number) =>
    new Intl.NumberFormat("pt-BR", {
      style: "currency",
      currency: "BRL",
    }).format(value);

  return (
    <div className="card-default p-6 border-subtle/30 flex flex-col justify-between h-full">
      <div>
        <h3 className="font-bold text-lg text-primary tracking-tight">
          Despesas por Categoria
        </h3>
        <p className="text-xs text-muted mt-0.5">
          Distribuição percentual do orçamento no ciclo atual
        </p>
      </div>

      <div className="my-4 flex flex-col sm:flex-row items-center justify-center gap-6">
        {/* Gráfico Doughnut */}
        <div className="w-48 h-48 sm:w-52 sm:h-52 shrink-0">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                innerRadius={55}
                outerRadius={75}
                paddingAngle={4}
                dataKey="amount"
              >
                {data.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    fill={entry.color || "#10B981"}
                    stroke="#0F172A"
                    strokeWidth={2}
                  />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  backgroundColor: "#1E293B",
                  borderColor: "#334155",
                  borderRadius: "12px",
                  color: "#F8FAFC",
                  fontSize: "12px",
                }}
                formatter={(value: unknown) => [
                  formatCurrency(Number(value || 0)),
                  "Total",
                ]}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Legenda Lateral */}
        <div className="w-full space-y-2.5 overflow-y-auto max-h-48 pr-1">
          {data.map((item) => (
            <div
              key={item.category_id}
              className="flex items-center justify-between text-xs"
            >
              <div className="flex items-center gap-2 truncate pr-2">
                <span
                  className="w-2.5 h-2.5 rounded-full shrink-0"
                  style={{ backgroundColor: item.color || "#10B981" }}
                />
                <span className="font-medium text-primary truncate">
                  {item.category_name}
                </span>
              </div>
              <div className="text-right shrink-0">
                <span className="font-bold text-primary block">
                  {item.percentage}%
                </span>
                <span className="text-[10px] text-muted">
                  {formatCurrency(item.amount)}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
