import type { CandidateMatchView } from '../api/types';

export function CandidateMatchCard({ candidate, rank }: { candidate: CandidateMatchView; rank: number }) {
  return (
    <div className="card match-card">
      <div className="match-header">
        <div>
          <span className="rank-badge">#{rank}</span>
          <strong>{candidate.fullName}</strong>
          {candidate.headline && <span className="muted"> — {candidate.headline}</span>}
        </div>
        <div className="score-badge">{candidate.score}/100</div>
      </div>

      <p>{candidate.rationale}</p>

      <div className="tag-row">
        {candidate.skills.map((s) => (
          <span className="tag" key={s}>
            {s}
          </span>
        ))}
      </div>

      <div className="strengths-gaps">
        <div>
          <h4>Strengths</h4>
          <ul>
            {candidate.strengths.map((s, i) => (
              <li key={i}>{s}</li>
            ))}
          </ul>
        </div>
        <div>
          <h4>Gaps</h4>
          <ul>
            {candidate.gaps.length === 0 ? <li className="muted">None noted</li> : null}
            {candidate.gaps.map((g, i) => (
              <li key={i}>{g}</li>
            ))}
          </ul>
        </div>
      </div>

      <div className="contact-row muted">
        {candidate.experienceYears != null && <span>{candidate.experienceYears} yrs experience</span>}
        {candidate.contactEmail && <span>{candidate.contactEmail}</span>}
        {candidate.resumeFileName && <span>{candidate.resumeFileName}</span>}
      </div>
    </div>
  );
}
