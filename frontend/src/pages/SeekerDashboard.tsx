import { useEffect, useState, type FormEvent } from 'react';
import { NavBar } from '../components/NavBar';
import { apiClient, extractErrorMessage } from '../api/client';
import type { CandidateProfile } from '../api/types';
import { useAuth } from '../context/AuthContext';

export function SeekerDashboard() {
  const { user } = useAuth();
  const [profile, setProfile] = useState<CandidateProfile | null>(null);
  const [fullName, setFullName] = useState(user?.fullName ?? '');
  const [headline, setHeadline] = useState('');
  const [skillsInput, setSkillsInput] = useState('');
  const [experienceYears, setExperienceYears] = useState('');
  const [education, setEducation] = useState('');
  const [summary, setSummary] = useState('');
  const [contactEmail, setContactEmail] = useState(user?.email ?? '');
  const [resumeFile, setResumeFile] = useState<File | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [savingProfile, setSavingProfile] = useState(false);
  const [uploadingResume, setUploadingResume] = useState(false);

  const loadProfile = async () => {
    try {
      const { data } = await apiClient.get<CandidateProfile>('/api/profile/me');
      setProfile(data);
      setFullName(data.fullName);
      setHeadline(data.headline ?? '');
      setSkillsInput(data.skills.join(', '));
      setExperienceYears(data.experienceYears?.toString() ?? '');
      setEducation(data.education ?? '');
      setSummary(data.summary ?? '');
      setContactEmail(data.contactEmail ?? '');
    } catch {
      // No profile yet — that's fine, the form below creates one.
    }
  };

  useEffect(() => {
    loadProfile();
  }, []);

  const onSaveProfile = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);
    setStatus(null);
    setSavingProfile(true);
    try {
      const { data } = await apiClient.post<CandidateProfile>('/api/profile', {
        fullName,
        headline,
        skills: skillsInput
          .split(',')
          .map((s) => s.trim())
          .filter(Boolean),
        experienceYears: experienceYears ? Number(experienceYears) : null,
        education,
        summary,
        contactEmail,
      });
      setProfile(data);
      setStatus('Profile saved.');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSavingProfile(false);
    }
  };

  const onUploadResume = async (e: FormEvent) => {
    e.preventDefault();
    if (!resumeFile) return;
    setError(null);
    setStatus(null);
    setUploadingResume(true);
    try {
      const formData = new FormData();
      formData.append('file', resumeFile);
      const { data } = await apiClient.post<CandidateProfile>('/api/profile/resume', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      setProfile(data);
      setStatus('Resume uploaded and indexed for matching.');
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setUploadingResume(false);
    }
  };

  return (
    <div className="page">
      <NavBar />
      <main className="content">
        <h2>Your profile</h2>
        <form className="card form-grid" onSubmit={onSaveProfile}>
          <label>
            Full name
            <input required value={fullName} onChange={(e) => setFullName(e.target.value)} />
          </label>
          <label>
            Headline
            <input
              value={headline}
              onChange={(e) => setHeadline(e.target.value)}
              placeholder="Senior Backend Engineer"
            />
          </label>
          <label className="span-2">
            Skills (comma separated)
            <input
              value={skillsInput}
              onChange={(e) => setSkillsInput(e.target.value)}
              placeholder="Java, Spring Boot, Kafka"
            />
          </label>
          <label>
            Years of experience
            <input
              type="number"
              min={0}
              value={experienceYears}
              onChange={(e) => setExperienceYears(e.target.value)}
            />
          </label>
          <label>
            Contact email
            <input type="email" value={contactEmail} onChange={(e) => setContactEmail(e.target.value)} />
          </label>
          <label className="span-2">
            Education
            <input value={education} onChange={(e) => setEducation(e.target.value)} />
          </label>
          <label className="span-2">
            Summary
            <textarea rows={3} value={summary} onChange={(e) => setSummary(e.target.value)} />
          </label>
          <button className="span-2" type="submit" disabled={savingProfile}>
            {savingProfile ? 'Saving…' : 'Save profile'}
          </button>
        </form>

        <h2>Resume</h2>
        <form className="card" onSubmit={onUploadResume}>
          <p className="muted">
            {profile?.resumeFileName
              ? `Current resume: ${profile.resumeFileName} ${profile.indexedForMatching ? '(indexed for matching)' : ''}`
              : 'No resume uploaded yet.'}
          </p>
          <input
            type="file"
            accept=".pdf,.txt,.md"
            onChange={(e) => setResumeFile(e.target.files?.[0] ?? null)}
          />
          <button type="submit" disabled={!resumeFile || uploadingResume}>
            {uploadingResume ? 'Uploading…' : 'Upload resume'}
          </button>
        </form>

        {error && <p className="error-text">{error}</p>}
        {status && <p className="success-text">{status}</p>}
      </main>
    </div>
  );
}
