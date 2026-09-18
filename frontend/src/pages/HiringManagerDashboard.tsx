import { useEffect, useState } from 'react';
import { NavBar } from '../components/NavBar';
import { CandidateMatchCard } from '../components/CandidateMatchCard';
import { apiClient, extractErrorMessage } from '../api/client';
import type { JobPosting, TopMatchesResponse } from '../api/types';

export function HiringManagerDashboard() {
  const [postings, setPostings] = useState<JobPosting[]>([]);
  const [selectedPostingId, setSelectedPostingId] = useState<number | null>(null);
  const [matches, setMatches] = useState<TopMatchesResponse | null>(null);
  const [loadingMatches, setLoadingMatches] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiClient
      .get<JobPosting[]>('/api/postings/assigned')
      .then(({ data }) => setPostings(data))
      .catch((err) => setError(extractErrorMessage(err)));
  }, []);

  const findMatches = async (postingId: number) => {
    setSelectedPostingId(postingId);
    setMatches(null);
    setError(null);
    setLoadingMatches(true);
    try {
      const { data } = await apiClient.get<TopMatchesResponse>(`/api/postings/${postingId}/matches`);
      setMatches(data);
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setLoadingMatches(false);
    }
  };

  return (
    <div className="page">
      <NavBar />
      <main className="content">
        <h2>Your assigned postings</h2>
        <div className="card-list">
          {postings.length === 0 && <p>No postings assigned to you yet.</p>}
          {postings.map((p) => (
            <div className={`card ${selectedPostingId === p.id ? 'card-selected' : ''}`} key={p.id}>
              <h3>{p.title}</h3>
              <p className="muted">{p.location ?? 'Location not specified'}</p>
              <div className="tag-row">
                {p.requiredSkills.map((s) => (
                  <span className="tag" key={s}>
                    {s}
                  </span>
                ))}
              </div>
              <button onClick={() => findMatches(p.id)} disabled={loadingMatches && selectedPostingId === p.id}>
                {loadingMatches && selectedPostingId === p.id ? 'Finding matches…' : 'Find top 5 candidates'}
              </button>
            </div>
          ))}
        </div>

        {error && <p className="error-text">{error}</p>}

        {matches && (
          <>
            <h2>Top candidates for “{matches.jobTitle}”</h2>
            {matches.topCandidates.length === 0 && (
              <p>No indexed candidate profiles matched yet — ask job seekers to upload resumes first.</p>
            )}
            <div className="card-list">
              {matches.topCandidates.map((c, i) => (
                <CandidateMatchCard candidate={c} rank={i + 1} key={c.candidateId} />
              ))}
            </div>
          </>
        )}
      </main>
    </div>
  );
}
