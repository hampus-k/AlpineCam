import { useState } from 'react';
import { DayCard } from './components/DayCard';
import { MealModal } from './components/MealModal';
import { useWeekData } from './useWeekData';
import {
  getWeekStart,
  addDays,
  getWeekDays,
  getWeekNumber,
  toDateKey,
} from './dateUtils';
import './App.css';

export default function App() {
  const [weekStart, setWeekStart] = useState(() => getWeekStart(new Date()));
  const [selectedDate, setSelectedDate] = useState<Date | null>(null);
  const { data, setMeal, removeMeal } = useWeekData();

  const weekDays = getWeekDays(weekStart);
  const weekNumber = getWeekNumber(weekStart);

  function prevWeek() {
    setWeekStart((d) => addDays(d, -7));
  }

  function nextWeek() {
    setWeekStart((d) => addDays(d, 7));
  }

  function goToToday() {
    setWeekStart(getWeekStart(new Date()));
  }

  const isCurrentWeek =
    toDateKey(weekStart) === toDateKey(getWeekStart(new Date()));

  const selectedKey = selectedDate ? toDateKey(selectedDate) : null;
  const selectedMeal = selectedKey ? data[selectedKey] : undefined;

  const plannedCount = weekDays.filter((d) => data[toDateKey(d)]).length;

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header__inner">
          <div className="app-header__brand">
            <span className="app-header__icon" aria-hidden="true">🍽</span>
            <h1 className="app-header__title">Veckans Mat</h1>
          </div>
          <nav className="week-nav" aria-label="Veckonavigering">
            <button className="week-nav__btn" onClick={prevWeek} aria-label="Föregående vecka">
              ‹
            </button>
            <div className="week-nav__label">
              <span className="week-nav__week">Vecka {weekNumber}</span>
              {!isCurrentWeek && (
                <button className="week-nav__today" onClick={goToToday}>
                  Idag
                </button>
              )}
            </div>
            <button className="week-nav__btn" onClick={nextWeek} aria-label="Nästa vecka">
              ›
            </button>
          </nav>
        </div>
      </header>

      <main className="app-main">
        <div className="week-grid">
          {weekDays.map((day) => {
            const key = toDateKey(day);
            return (
              <DayCard
                key={key}
                date={day}
                meal={data[key]}
                onClick={() => setSelectedDate(day)}
              />
            );
          })}
        </div>

        <p className="week-summary">
          {plannedCount === 0
            ? 'Inga middagar planerade ännu'
            : `${plannedCount} av 7 dagar planerade`}
        </p>
      </main>

      {selectedDate && (
        <MealModal
          date={selectedDate}
          meal={selectedMeal}
          onSave={(meal) => {
            setMeal(toDateKey(selectedDate), meal);
            setSelectedDate(null);
          }}
          onDelete={() => {
            removeMeal(toDateKey(selectedDate));
            setSelectedDate(null);
          }}
          onClose={() => setSelectedDate(null)}
        />
      )}
    </div>
  );
}
