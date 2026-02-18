import type { Meal } from '../types';
import { formatDayName, formatDate, isToday } from '../dateUtils';
import './DayCard.css';

interface Props {
  date: Date;
  meal?: Meal;
  onClick: () => void;
}

export function DayCard({ date, meal, onClick }: Props) {
  const today = isToday(date);
  const dayName = formatDayName(date);
  const isWeekend = dayName === 'Lördag' || dayName === 'Söndag';

  return (
    <button
      className={`day-card ${today ? 'day-card--today' : ''} ${isWeekend ? 'day-card--weekend' : ''} ${meal ? 'day-card--has-meal' : ''}`}
      onClick={onClick}
      aria-label={`${dayName} ${formatDate(date)}${meal ? `: ${meal.name}` : ', ingen middag inlagd'}`}
    >
      <div className="day-card__header">
        <span className="day-card__dayname">{dayName}</span>
        <span className="day-card__date">{formatDate(date)}</span>
      </div>
      <div className="day-card__body">
        {meal ? (
          <>
            <span className="day-card__meal-name">{meal.name}</span>
            {meal.notes && <span className="day-card__notes">{meal.notes}</span>}
          </>
        ) : (
          <span className="day-card__empty">+ Lägg till middag</span>
        )}
      </div>
    </button>
  );
}
