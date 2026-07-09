import { useState } from "react";
import {
  X,
  ChevronLeft,
  ChevronRight,
  Calendar as CalendarIcon,
} from "lucide-react";

interface DatePickerModalProps {
  isOpen: boolean;
  onClose: () => void;
  title?: string;
  selectedDate: string; // Formato YYYY-MM-DD
  onSelectDate: (date: string) => void;
}

export function DatePickerModal({
  isOpen,
  onClose,
  title = "Selecione uma Data",
  selectedDate,
  onSelectDate,
}: DatePickerModalProps) {
  const initialDate = selectedDate
    ? new Date(`${selectedDate}T00:00:00`)
    : new Date();
  const [currentMonth, setCurrentMonth] = useState(initialDate.getMonth());
  const [currentYear, setCurrentYear] = useState(initialDate.getFullYear());

  if (!isOpen) return null;

  const daysInMonth = new Date(currentYear, currentMonth + 1, 0).getDate();
  const firstDayOfWeek = new Date(currentYear, currentMonth, 1).getDay(); // 0 = Domingo

  const monthNames = [
    "Janeiro",
    "Fevereiro",
    "Março",
    "Abril",
    "Maio",
    "Junho",
    "Julho",
    "Agosto",
    "Setembro",
    "Outubro",
    "Novembro",
    "Dezembro",
  ];

  function handlePrevMonth() {
    if (currentMonth === 0) {
      setCurrentMonth(11);
      setCurrentYear((prev) => prev - 1);
    } else {
      setCurrentMonth((prev) => prev - 1);
    }
  }

  function handleNextMonth() {
    if (currentMonth === 11) {
      setCurrentMonth(0);
      setCurrentYear((prev) => prev - 1);
    } else {
      setCurrentMonth((prev) => prev + 1);
    }
  }

  function handleDayClick(day: number) {
    const formattedMonth = String(currentMonth + 1).padStart(2, "0");
    const formattedDay = String(day).padStart(2, "0");
    onSelectDate(`${currentYear}-${formattedMonth}-${formattedDay}`);
    onClose();
  }

  const todayStr = new Date().toISOString().split("T")[0];

  return (
    <div className="fixed inset-0 z-60 flex items-center justify-center p-4 bg-page/80 backdrop-blur-md animate-fade-in">
      <div className="bg-surface rounded-3xl shadow-2xl w-full max-w-xs sm:max-w-sm overflow-hidden border border-subtle/30 animate-in zoom-in-95 duration-200">
        {/* Cabeçalho */}
        <div className="p-4 sm:p-5 border-b border-subtle/20 flex justify-between items-center shrink-0">
          <div className="flex items-center gap-2">
            <CalendarIcon size={16} className="text-brand" />
            <h3 className="font-bold text-primary text-sm sm:text-base tracking-tight">
              {title}
            </h3>
          </div>
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 text-muted hover:text-primary rounded-full transition-colors cursor-pointer"
          >
            <X size={16} />
          </button>
        </div>

        {/* Controles de Navegação de Mês */}
        <div className="p-4 flex items-center justify-between bg-elevated/40 border-b border-subtle/10">
          <button
            type="button"
            onClick={handlePrevMonth}
            className="p-2 text-muted hover:text-primary hover:bg-surface rounded-xl transition-all cursor-pointer shadow-2xs border border-transparent hover:border-subtle/20"
          >
            <ChevronLeft size={18} />
          </button>

          <span className="font-extrabold text-sm sm:text-base text-primary tracking-tight">
            {monthNames[currentMonth]} {currentYear}
          </span>

          <button
            type="button"
            onClick={handleNextMonth}
            className="p-2 text-muted hover:text-primary hover:bg-surface rounded-xl transition-all cursor-pointer shadow-2xs border border-transparent hover:border-subtle/20"
          >
            <ChevronRight size={18} />
          </button>
        </div>

        {/* Grade de Dias da Semana */}
        <div className="p-4">
          <div className="grid grid-cols-7 gap-1 text-center mb-2">
            {["D", "S", "T", "Q", "Q", "S", "S"].map((day, idx) => (
              <span
                key={idx}
                className="text-[10px] font-extrabold uppercase text-muted tracking-wider"
              >
                {day}
              </span>
            ))}
          </div>

          {/* Grade Numérica de Dias */}
          <div className="grid grid-cols-7 gap-1">
            {Array.from({ length: firstDayOfWeek }).map((_, idx) => (
              <div key={`empty-${idx}`} className="h-9 sm:h-10" />
            ))}

            {Array.from({ length: daysInMonth }).map((_, idx) => {
              const dayNum = idx + 1;
              const formattedMonth = String(currentMonth + 1).padStart(2, "0");
              const formattedDay = String(dayNum).padStart(2, "0");
              const dateStr = `${currentYear}-${formattedMonth}-${formattedDay}`;

              const isSelected = selectedDate === dateStr;
              const isToday = todayStr === dateStr;

              return (
                <button
                  key={dayNum}
                  type="button"
                  onClick={() => handleDayClick(dayNum)}
                  className={`h-9 sm:h-10 rounded-xl text-xs sm:text-sm font-bold flex items-center justify-center transition-all cursor-pointer relative ${
                    isSelected
                      ? "bg-brand text-white shadow-md scale-105"
                      : isToday
                        ? "bg-brand/15 text-brand border border-brand/30"
                        : "text-primary hover:bg-elevated/80"
                  }`}
                >
                  <span>{dayNum}</span>
                  {isToday && !isSelected && (
                    <span className="absolute bottom-1 w-1 h-1 bg-brand rounded-full" />
                  )}
                </button>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
