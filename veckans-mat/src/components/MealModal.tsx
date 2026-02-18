import { useState, useEffect, useRef } from 'react';
import type { Meal } from '../types';
import { formatDayName, formatDate } from '../dateUtils';
import './MealModal.css';

interface Props {
  date: Date;
  meal?: Meal;
  onSave: (meal: Meal) => void;
  onDelete: () => void;
  onClose: () => void;
}

export function MealModal({ date, meal, onSave, onDelete, onClose }: Props) {
  const [name, setName] = useState(meal?.name ?? '');
  const [notes, setNotes] = useState(meal?.notes ?? '');
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    inputRef.current?.focus();
  }, []);

  useEffect(() => {
    function onKey(e: KeyboardEvent) {
      if (e.key === 'Escape') onClose();
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!name.trim()) return;
    onSave({
      id: meal?.id ?? crypto.randomUUID(),
      name: name.trim(),
      notes: notes.trim() || undefined,
    });
  }

  return (
    <div className="modal-backdrop" onClick={(e) => e.target === e.currentTarget && onClose()}>
      <div className="modal" role="dialog" aria-modal="true">
        <div className="modal__header">
          <div>
            <h2 className="modal__title">
              {meal ? 'Redigera middag' : 'Lägg till middag'}
            </h2>
            <p className="modal__subtitle">
              {formatDayName(date)}, {formatDate(date)}
            </p>
          </div>
          <button className="modal__close" onClick={onClose} aria-label="Stäng">✕</button>
        </div>

        <form className="modal__form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="meal-name">Maträtt</label>
            <input
              ref={inputRef}
              id="meal-name"
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="T.ex. Köttbullar med potatis"
              maxLength={80}
              required
            />
          </div>

          <div className="form-group">
            <label htmlFor="meal-notes">Anteckningar <span className="optional">(valfritt)</span></label>
            <input
              id="meal-notes"
              type="text"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="T.ex. vegansk variant, dubbbel sats..."
              maxLength={120}
            />
          </div>

          <div className="modal__actions">
            {meal && (
              <button
                type="button"
                className="btn btn--danger"
                onClick={onDelete}
              >
                Ta bort
              </button>
            )}
            <div className="modal__actions-right">
              <button type="button" className="btn btn--secondary" onClick={onClose}>
                Avbryt
              </button>
              <button type="submit" className="btn btn--primary" disabled={!name.trim()}>
                Spara
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
