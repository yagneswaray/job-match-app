import { useEffect, useState, type FormEvent } from 'react';
import { NavBar } from '../components/NavBar';
import { apiClient, extractErrorMessage } from '../api/client';
import type { JobPosting } from '../api/types';

export function HrDashboard() {
  const [postings, setPostings] = useState<JobPosting[]>([]);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [skillsInput, setSkillsInput] = useState('');
  const [location, setLocation] = useState('');
  const [minExperienceYears, setMinExperienceYears] = useState('');
  const [hiringManagerEmail, setHiringManagerEmail] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const loadPostings = async () => {
    const { data } = await apiClient.get<JobPosting[]>('/api/postings/mine');
    setPostings(data);
  };

  useEffect(() => {
    loadPostings().catch((err) => setError(extractErrorMessage(err)));
  }, []);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await apiClient.post('/api/postings', {
        title,
        description,
        requiredSkills: skillsInput
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean),
        location: location || null,
        minExperienceYears: minExperienceYears ? Number(minExperienceYears) : null,
        hiringManagerEmail,
      });
      setTitle('');
      setDescription('');
      setSkillsInput('');
      setLocation('');
      setMinExperienceYears('');
      setHiringManagerEmail('');
      await loadPostings();
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="page">
      <NavBar />
      <main className="content">
        <h2>Publish a job posting</h2>
        <form className="card form-grid" onSubmit={onSubmit}>
          <label>
            Title
            <input required value={title} onChange={(e) => setTitle(e.target.value)} />
          </label>
          <label>
            Location
            <input value={location} onChange={(e) => setLocation(e.target.value)} placeholder="Remote / City" />
          </label>
          <label className="span-2">
            Description
            <textarea
              required
              rows={4}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Responsibilities, tech stack, team context…"
            />
          </label>
          <label className="span-2">
            Required skills (comma separated)
            <input
              required
              value={skillsInput}
              onChange={(e) => setSkillsInput(e.target.value)}
              placeholder="Java, Spring Boot, AWS"
            />
          </label>
          <label>
            Min. experience (years)
            <input
              type="number"
              min={0}
              value={minExperienceYears}
              onChange={(e) => setMinExperienceYears(e.target.value)}
            />
          </label>
          <label>
            Hiring manager email
            <input
              type="email"
              required
              value={hiringManagerEmail}
              onChange={(e) => setHiringManagerEmail(e.target.value)}
              placeholder="Must already be registered as HIRING_MANAGER"
            />
          </label>
          {error && <p className="error-text span-2">{error}</p>}
          <button className="span-2" type="submit" disabled={submitting}>
            {submitting ? 'Publishing…' : 'Publish posting'}
          </button>
        </form>

        <h2>Your postings</h2>
        <div className="card-list">
          {postings.length === 0 && <p>No postings yet.</p>}
          {postings.map((p) => (
            <div className="card" key={p.id}>
              <h3>{p.title}</h3>
              <p className="muted">{p.location ?? 'Location not specified'}</p>
              <p>{p.description}</p>
              <div className="tag-row">
                {p.requiredSkills.map((s) => (
                  <span className="tag" key={s}>
                    {s}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
}
